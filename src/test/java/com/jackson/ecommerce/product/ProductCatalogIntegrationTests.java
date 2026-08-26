package com.jackson.ecommerce.product;

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
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:productcatalog;DB_CLOSE_DELAY=-1;MODE=MSSQLServer",
                "app.storage.upload-dir=target/test-uploads"
        })
@ActiveProfiles("test")
class ProductCatalogIntegrationTests {
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
    void memberCanCreateAndPublicCanBrowseProduct() throws Exception {
        Cookie seller = loginNewMember("catalog");
        String name = "Demo Product " + System.nanoTime();

        mockMvc.perform(post("/api/v1/products")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(product(name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.currency").value("TWD"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value(name))
                .andExpect(jsonPath("$.items[0].stock").value(8));
    }

    @Test
    void onlyOwnerCanUpdateOrDeleteProduct() throws Exception {
        Cookie owner = loginNewMember("owner");
        Cookie otherMember = loginNewMember("other");
        long productId = createProduct(owner, "Owned Product " + System.nanoTime());

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .cookie(otherMember)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(product("Not Yours"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/products/{id}", productId)
                        .cookie(owner)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanListSoftDeletedProduct() throws Exception {
        Cookie owner = loginNewMember("adminview");
        long productId = createProduct(owner, "Admin View Product " + System.nanoTime());
        mockMvc.perform(delete("/api/v1/products/{id}", productId).cookie(owner).with(csrf()))
                .andExpect(status().isNoContent());

        Cookie admin = login("admin@example.com", "ChangeMe_Admin_123!");
        mockMvc.perform(get("/api/v1/admin/products").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.items[?(@.id == " + productId + ")].deleted").value(true));
    }

    @Test
    void ownerCanUploadValidImageAndPublicCanReadIt() throws Exception {
        Cookie owner = loginNewMember("image");
        long productId = createProduct(owner, "Image Product " + System.nanoTime());
        byte[] imageBytes = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

        MvcResult upload = mockMvc.perform(multipart("/api/v1/products/{id}/images", productId)
                        .file(new MockMultipartFile("file", "demo.png", "image/png", imageBytes))
                        .cookie(owner)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.image.mediaType").value("image/png"))
                .andReturn();

        long imageId = objectMapper.readTree(upload.getResponse().getContentAsString())
                .get("image").get("id").asLong();
        mockMvc.perform(get("/api/v1/products/{productId}/images/{imageId}", productId, imageId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void unsupportedImageTypeIsRejected() throws Exception {
        Cookie owner = loginNewMember("badimage");
        long productId = createProduct(owner, "Bad Image Product " + System.nanoTime());

        mockMvc.perform(multipart("/api/v1/products/{id}/images", productId)
                        .file(new MockMultipartFile("file", "demo.gif", "image/gif", new byte[]{1}))
                        .cookie(owner)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    private ProductRequest product(String name) {
        return new ProductRequest(name, "A demo product", new java.math.BigDecimal("199.90"), 8, "Demo");
    }

    private long createProduct(Cookie seller, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/products")
                        .cookie(seller)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(product(name))))
                .andExpect(status().isCreated())
                .andReturn();
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
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(identifier, password))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
    }
}
