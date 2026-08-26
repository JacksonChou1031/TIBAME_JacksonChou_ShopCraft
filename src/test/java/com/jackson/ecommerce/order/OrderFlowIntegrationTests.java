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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:orderflow;DB_CLOSE_DELAY=-1;MODE=MSSQLServer")
@ActiveProfiles("test")
class OrderFlowIntegrationTests {
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
    void buyerSellerAndAdminCanSeeOnlyTheirOrderViews() throws Exception {
        Cookie seller = loginNewMember("flowseller");
        Cookie buyer = loginNewMember("flowbuyer");
        Cookie stranger = loginNewMember("flowstranger");
        long productId = createProduct(seller, "Flow Product " + System.nanoTime(), 4, "120.00");
        long orderId = checkout(buyer, productId);

        mockMvc.perform(get("/api/v1/orders").cookie(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(orderId))
                .andExpect(jsonPath("$.items[0].status").value("PAID"));
        mockMvc.perform(get("/api/v1/seller/orders").cookie(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(orderId));
        mockMvc.perform(get("/api/v1/orders/{id}", orderId).cookie(stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/orders").cookie(loginAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orderId").value(orderId));
    }

    @Test
    void orderMovesThroughShipmentAndCompletionStates() throws Exception {
        Cookie seller = loginNewMember("stateseller");
        Cookie buyer = loginNewMember("statebuyer");
        long productId = createProduct(seller, "State Product " + System.nanoTime(), 3, "80.00");
        long orderId = checkout(buyer, productId);

        mockMvc.perform(post("/api/v1/seller/orders/{id}/prepare-shipment", orderId)
                        .cookie(seller).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/orders/{id}", orderId).cookie(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_SHIPMENT"));

        mockMvc.perform(post("/api/v1/seller/orders/{id}/ship", orderId)
                        .cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("trackingNumber", "MOCK-" + orderId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingNumber").value("MOCK-" + orderId));

        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .cookie(buyer).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.statusHistory[?(@.status == 'SHIPPED')]").exists());
    }

    @Test
    void illegalTransitionsAndDuplicateTrackingNumberAreRejected() throws Exception {
        Cookie seller = loginNewMember("illegalSeller");
        Cookie buyer = loginNewMember("illegalBuyer");
        long productId = createProduct(seller, "Illegal Product " + System.nanoTime(), 5, "30.00");
        long orderId = checkout(buyer, productId);

        mockMvc.perform(post("/api/v1/seller/orders/{id}/ship", orderId)
                        .cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("trackingNumber", "TOO-EARLY"))))
                .andExpect(status().isConflict());

        prepareAndShip(seller, orderId, "UNIQUE-TRACKING");

        long secondProductId = createProduct(seller, "Second Illegal Product " + System.nanoTime(), 2, "35.00");
        long secondOrderId = checkout(buyer, secondProductId);
        mockMvc.perform(post("/api/v1/seller/orders/{id}/prepare-shipment", secondOrderId)
                        .cookie(seller).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/seller/orders/{id}/ship", secondOrderId)
                        .cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("trackingNumber", "UNIQUE-TRACKING"))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .cookie(buyer).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/seller/orders/{id}/ship", orderId)
                        .cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("trackingNumber", "ANOTHER"))))
                .andExpect(status().isConflict());
    }

    private void prepareAndShip(Cookie seller, long orderId, String trackingNumber) throws Exception {
        mockMvc.perform(post("/api/v1/seller/orders/{id}/prepare-shipment", orderId)
                        .cookie(seller).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/seller/orders/{id}/ship", orderId)
                        .cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("trackingNumber", trackingNumber))))
                .andExpect(status().isOk());
    }

    private long checkout(Cookie buyer, long productId) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").cookie(buyer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("productId", productId, "quantity", 1))))
                .andExpect(status().isOk());
        MvcResult result = mockMvc.perform(post("/api/v1/checkout").cookie(buyer).with(csrf())
                        .header("Idempotency-Key", "order-flow-" + System.nanoTime())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "shippingMethod", "HOME_DELIVERY",
                                "recipientName", "Flow Buyer",
                                "recipientPhone", "0912345678",
                                "deliveryAddress", "Taipei City",
                                "mockAccountName", "Demo Account",
                                "mockAccountNumber", "MOCK_SUCCESS"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("orderId").asLong();
    }

    private long createProduct(Cookie seller, String name, int stock, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products").cookie(seller).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new ProductRequest(name, "An order flow product", new BigDecimal(price), stock, "Demo"))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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

    private Cookie loginAdmin() throws Exception {
        return login("admin@example.com", "ChangeMe_Admin_123!");
    }

    private Cookie login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(identifier, password))))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
    }
}
