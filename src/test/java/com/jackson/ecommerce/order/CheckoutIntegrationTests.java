package com.jackson.ecommerce.order;

import com.jackson.ecommerce.member.api.LoginRequest;
import com.jackson.ecommerce.member.api.RegisterRequest;
import com.jackson.ecommerce.product.api.ProductRequest;
import com.jackson.ecommerce.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:checkoutcatalog;DB_CLOSE_DELAY=-1;MODE=MSSQLServer")
@ActiveProfiles("test")
class CheckoutIntegrationTests {
    private static final String PASSWORD = "Member123!";
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    void successfulCheckoutCreatesOrderSnapshotsStockAndClearsCart() throws Exception {
        Cookie seller = loginNewMember("checkoutseller");
        Cookie buyer = loginNewMember("checkoutbuyer");
        long productId = createProduct(seller, "Checkout Product " + System.nanoTime(), 5, "99.90");
        addToCart(buyer, productId, 2);

        MvcResult result = checkout(buyer, "checkout-success-1", checkoutBody("HOME_DELIVERY", null, null,
                "Taipei City", "MOCK_SUCCESS"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.shippingFee").value(100.0))
                .andExpect(jsonPath("$.totalAmount").value(299.8))
                .andExpect(jsonPath("$.replayed").value(false))
                .andReturn();
        long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("orderId").asLong();

        checkout(buyer, "checkout-success-1", checkoutBody("HOME_DELIVERY", null, null,
                "Taipei City", "MOCK_SUCCESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.replayed").value(true));

        mockMvc.perform(get("/api/v1/cart").cookie(buyer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void failedPaymentKeepsCartAndStockAndRecordsIdempotencyFailure() throws Exception {
        Cookie seller = loginNewMember("failseller");
        Cookie buyer = loginNewMember("failbuyer");
        long productId = createProduct(seller, "Failed Checkout Product " + System.nanoTime(), 5, "50.00");
        addToCart(buyer, productId, 2);

        checkout(buyer, "checkout-failure-1", checkoutBody("CONVENIENCE_STORE", "Seven Store", "A001",
                null, "MOCK_FAILURE"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));

        mockMvc.perform(get("/api/v1/cart").cookie(buyer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(2));
        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(5));
        checkout(buyer, "checkout-failure-1", checkoutBody("CONVENIENCE_STORE", "Seven Store", "A001",
                null, "MOCK_FAILURE"))
                .andExpect(status().isConflict());
    }

    @Test
    void shippingFieldsAndMockPaymentNumberAreValidated() throws Exception {
        Cookie seller = loginNewMember("validateseller");
        Cookie buyer = loginNewMember("validatebuyer");
        long productId = createProduct(seller, "Validation Checkout Product " + System.nanoTime(), 5, "10.00");
        addToCart(buyer, productId, 1);

        checkout(buyer, "checkout-validation-1", checkoutBody("HOME_DELIVERY", null, null, null, "MOCK_SUCCESS"))
                .andExpect(status().isBadRequest());
        checkout(buyer, "checkout-validation-2", checkoutBody("CONVENIENCE_STORE", null, null, null, "MOCK_SUCCESS"))
                .andExpect(status().isBadRequest());
        checkout(buyer, "checkout-validation-3", checkoutBody("HOME_DELIVERY", null, null, "Address", "OTHER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unavailableCartCannotBeCheckedOut() throws Exception {
        Cookie seller = loginNewMember("unavailableseller");
        Cookie buyer = loginNewMember("unavailablebuyer");
        long productId = createProduct(seller, "Unavailable Checkout Product " + System.nanoTime(), 5, "10.00");
        addToCart(buyer, productId, 1);
        mockMvc.perform(delete("/api/v1/products/{id}", productId).cookie(seller).with(csrf()))
                .andExpect(status().isNoContent());

        checkout(buyer, "checkout-unavailable-1", checkoutBody("HOME_DELIVERY", null, null,
                "Address", "MOCK_SUCCESS"))
                .andExpect(status().isConflict());
    }

    @Test
    void checkoutRequiresIdempotencyKey() throws Exception {
        Cookie seller = loginNewMember("keyseller");
        Cookie buyer = loginNewMember("keybuyer");
        long productId = createProduct(seller, "Key Checkout Product " + System.nanoTime(), 5, "10.00");
        addToCart(buyer, productId, 1);
        mockMvc.perform(post("/api/v1/checkout").cookie(buyer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(checkoutBody("HOME_DELIVERY", null, null,
                                "Address", "MOCK_SUCCESS"))))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions checkout(Cookie buyer, String key,
                                                                          Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/checkout").cookie(buyer).with(csrf())
                .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(body)));
    }

    private void addToCart(Cookie buyer, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").cookie(buyer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("productId", productId, "quantity", quantity))))
                .andExpect(status().isOk());
    }

    private Map<String, Object> checkoutBody(String method, String storeName, String storeCode,
                                             String address, String paymentNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("shippingMethod", method);
        body.put("recipientName", "Demo Recipient");
        body.put("recipientPhone", "0912345678");
        body.put("storeName", storeName);
        body.put("storeCode", storeCode);
        body.put("deliveryAddress", address);
        body.put("mockAccountName", "Demo Payment Account");
        body.put("mockAccountNumber", paymentNumber);
        return body;
    }

    private long createProduct(Cookie seller, String name, int stock, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products").cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new ProductRequest(name, "A checkout product", new BigDecimal(price), stock, "Demo"))))
                .andExpect(status().isCreated()).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asLong();
    }

    private Cookie loginNewMember(String prefix) throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String email = prefix + suffix + "@example.com";
        String username = prefix + suffix;
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegisterRequest(email, username, PASSWORD, "Test Member", "0912345678"))))
                .andExpect(status().isCreated());
        return login(username, PASSWORD);
    }

    private Cookie login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(identifier, password))))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
    }
}
