package com.jackson.ecommerce.member;

import com.jackson.ecommerce.member.api.LoginRequest;
import com.jackson.ecommerce.member.api.RegisterRequest;
import com.jackson.ecommerce.member.api.ChangePasswordRequest;
import com.jackson.ecommerce.member.api.UpdateProfileRequest;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:memberauth;DB_CLOSE_DELAY=-1;MODE=MSSQLServer")
@ActiveProfiles("test")
class MemberAuthIntegrationTests {
    private static final String PASSWORD = "Member123!";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void csrfEndpointProvidesTokenCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void memberCanRegisterWithNormalizedLoginFields() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "New.Member@Example.COM", "NewMember01", PASSWORD, "New Member", "0912345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.member@example.com"))
                .andExpect(jsonPath("$.username").value("newmember01"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void normalizedEmailAndUsernameCannotBeRegisteredTwice() throws Exception {
        register("duplicate@example.com", "duplicateuser");

        RegisterRequest duplicate = new RegisterRequest(
                "DUPLICATE@EXAMPLE.COM", "DuplicateUser", PASSWORD, "Another Member", "0911111111");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    void memberCanLoginAndAccessOwnProfileWithHttpOnlyCookie() throws Exception {
        String email = "login" + System.nanoTime() + "@example.com";
        register(email, "loginuser" + System.nanoTime());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = login.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
        assertNotNull(authCookie);
        assertTrue(authCookie.isHttpOnly());

        mockMvc.perform(get("/api/v1/auth/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void memberCanLoginUsingUsernameAndUpdateProfile() throws Exception {
        String email = "profile" + System.nanoTime() + "@example.com";
        String username = "profileuser" + System.nanoTime();
        register(email, username);
        Cookie authCookie = login(username);

        mockMvc.perform(put("/api/v1/auth/me")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new UpdateProfileRequest("Updated Member", "0987654321"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Member"))
                .andExpect(jsonPath("$.phone").value("0987654321"));
    }

    @Test
    void unauthenticatedMemberApiReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotAccessAdminApi() throws Exception {
        String email = "member" + System.nanoTime() + "@example.com";
        register(email, "memberuser" + System.nanoTime());
        Cookie authCookie = login(email);

        mockMvc.perform(get("/api/v1/admin/me").cookie(authCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void configuredInitialAdminCanAccessAdminApi() throws Exception {
        Cookie authCookie = login("admin@example.com", "ChangeMe_Admin_123!");

        mockMvc.perform(get("/api/v1/admin/me").cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void memberCanChangePasswordOnlyWithCurrentPassword() throws Exception {
        String email = "password" + System.nanoTime() + "@example.com";
        String username = "passworduser" + System.nanoTime();
        register(email, username);
        Cookie authCookie = login(email);

        ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, "NewMember123!");
        mockMvc.perform(put("/api/v1/auth/me/password")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNoContent());

        login(email, "NewMember123!");
    }

    @Test
    void logoutClearsAuthenticationCookie() throws Exception {
        String email = "logout" + System.nanoTime() + "@example.com";
        register(email, "logoutuser" + System.nanoTime());
        Cookie authCookie = login(email);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(authCookie)
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(JwtService.AUTH_COOKIE_NAME, 0));
    }

    @Test
    void wrongCurrentPasswordIsRejected() throws Exception {
        String email = "wrongpassword" + System.nanoTime() + "@example.com";
        register(email, "wrongpassworduser" + System.nanoTime());
        Cookie authCookie = login(email);

        mockMvc.perform(put("/api/v1/auth/me/password")
                        .cookie(authCookie)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new ChangePasswordRequest("WrongPassword123!", "NewMember123!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeRequestWithoutCsrfTokenIsRejected() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "csrf@example.com", "csrfuser", PASSWORD, "CSRF Member", "0912345678");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden());
    }

    private void register(String email, String username) throws Exception {
        RegisterRequest request = new RegisterRequest(email, username, PASSWORD, "Test Member", "0912345678");
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated());
    }

    private Cookie login(String email) throws Exception {
        return login(email, PASSWORD);
    }

    private Cookie login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginRequest(identifier, password))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(JwtService.AUTH_COOKIE_NAME))
                .andReturn();
        return result.getResponse().getCookie(JwtService.AUTH_COOKIE_NAME);
    }
}
