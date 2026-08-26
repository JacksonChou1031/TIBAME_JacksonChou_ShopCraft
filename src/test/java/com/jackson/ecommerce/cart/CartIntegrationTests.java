package com.jackson.ecommerce.cart;

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
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:cartcatalog;DB_CLOSE_DELAY=-1;MODE=MSSQLServer",
                "app.storage.upload-dir=target/test-cart-uploads"
        })
@ActiveProfiles("test")
class CartIntegrationTests {
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
    void memberCanAddDuplicateProductAndViewAccumulatedQuantity() throws Exception {
        Cookie seller = loginNewMember("seller");
        Cookie buyer = loginNewMember("buyer");
        long productId = createProduct(seller, "Cart Product " + System.nanoTime(), 5);

        addItem(buyer, productId, 2).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cart/items")
                        .cookie(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.items[0].purchasable").value(true));
    }

    @Test
    void cartCannotContainDifferentSellersAndCannotExceedStock() throws Exception {
        Cookie sellerOne = loginNewMember("sellerone");
        Cookie sellerTwo = loginNewMember("sellertwo");
        Cookie buyer = loginNewMember("buyerconflict");
        long firstProduct = createProduct(sellerOne, "First Cart Product " + System.nanoTime(), 2);
        long secondProduct = createProduct(sellerTwo, "Second Cart Product " + System.nanoTime(), 2);

        addItem(buyer, firstProduct, 1).andExpect(status().isOk());
        addItem(buyer, secondProduct, 1).andExpect(status().isConflict());
        addItem(buyer, firstProduct, 2).andExpect(status().isConflict());
        addItem(buyer, firstProduct, 0).andExpect(status().isBadRequest());
    }

    @Test
    void memberCanUpdateRemoveAndClearOwnCart() throws Exception {
        Cookie seller = loginNewMember("sellerupdate");
        Cookie buyer = loginNewMember("buyerupdate");
        long productId = createProduct(seller, "Update Cart Product " + System.nanoTime(), 10);
        addItem(buyer, productId, 2).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/cart/items/{productId}", productId)
                        .cookie(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson(productId, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));

        mockMvc.perform(delete("/api/v1/cart/items/{productId}", productId)
                        .cookie(buyer).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cart").cookie(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void unavailableProductRemainsInCartButIsNotPurchasable() throws Exception {
        Cookie seller = loginNewMember("sellerunavailable");
        Cookie buyer = loginNewMember("buyerunavailable");
        long productId = createProduct(seller, "Unavailable Cart Product " + System.nanoTime(), 4);
        addItem(buyer, productId, 1).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .cookie(seller).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/cart").cookie(buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].deleted").value(true))
                .andExpect(jsonPath("$.items[0].purchasable").value(false));
    }

    @Test
    void anonymousVisitorCannotAddToCart() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(itemJson(1, 1)))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions addItem(Cookie member, long productId, int quantity)
            throws Exception {
        return mockMvc.perform(post("/api/v1/cart/items")
                .cookie(member).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson(productId, quantity)));
    }

    private String itemJson(long productId, int quantity) throws Exception {
        return objectMapper.writeValueAsString(new CartItemJson(productId, quantity));
    }

    private long createProduct(Cookie seller, String name, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .cookie(seller).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new ProductRequest(name, "A cart product", new java.math.BigDecimal("99.90"), stock,
                                        "Demo"))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Cookie loginNewMember(String prefix) throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String email = prefix + suffix + "@example.com";
        String username = prefix + suffix;
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new RegisterRequest(email, username, PASSWORD, "Test Member", "0912345678"))))
                .andExpect(status().isCreated());
        return login(username, PASSWORD);
    }

    private Cookie login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(identifier, password))))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
    }

    private record CartItemJson(long productId, int quantity) {
    }
}
