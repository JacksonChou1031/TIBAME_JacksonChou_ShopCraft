package com.jackson.ecommerce.member;

import com.jackson.ecommerce.member.api.LoginRequest;
import com.jackson.ecommerce.member.api.RegisterRequest;
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
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.Matchers.greaterThan;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:admindashboard;DB_CLOSE_DELAY=-1;MODE=MSSQLServer")
@ActiveProfiles("test")
class AdminDashboardIntegrationTests {
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
    void adminCanViewDashboardCounts() throws Exception {
        registerMember("dashboard-admin-check");
        Cookie admin = login("admin@example.com", "ChangeMe_Admin_123!");

        mockMvc.perform(get("/api/v1/admin/dashboard").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.productCount").value(0))
                .andExpect(jsonPath("$.orderCount").value(0));
    }

    @Test
    void memberCannotViewDashboard() throws Exception {
        String username = registerMember("dashboard-member-check");
        Cookie member = login(username, PASSWORD);

        mockMvc.perform(get("/api/v1/admin/dashboard").cookie(member))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotViewDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openApiDocumentationIsPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Ecommerce MVP API"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/dashboard']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth").exists())
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.email").exists())
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/dashboard'].get.security[0].cookieAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/dashboard'].get.security[0].cookieAuth[0]").value("ADMIN"))
                .andExpect(jsonPath("$.paths['/api/v1/checkout'].post.parameters[?(@.name == 'X-XSRF-TOKEN')]").isNotEmpty());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    private String registerMember(String prefix) throws Exception {
        String suffix = Long.toString(System.nanoTime());
        String username = prefix + suffix;
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RegisterRequest(
                                username + "@example.com", username, PASSWORD,
                                "Dashboard Member", "0912345678"))))
                .andExpect(status().isCreated());
        return username;
    }

    private Cookie login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(identifier, password))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
    }
}
