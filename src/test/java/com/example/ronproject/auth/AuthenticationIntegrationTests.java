package com.example.ronproject.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.ronproject.security.RevokedTokenRepository;
import com.example.ronproject.user.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIntegrationTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @BeforeEach
    void setUp() {
        revokedTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    // ───── Register ─────

    @Test
    void registerReturnsJwtToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ron@example.com", "ron", "Password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ron@example.com"))
                .andExpect(jsonPath("$.username").value("ron"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        registerUser("ron@example.com", "ron");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ron@example.com", "ron2", "Password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        registerUser("ron@example.com", "ron");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("other@example.com", "ron", "Password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    // ───── Login ─────

    @Test
    void loginReturnsJwtTokenForRegisteredUser() throws Exception {
        registerUser("ron@example.com", "ron");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ron@example.com", "Password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("ron@example.com"))
                .andExpect(jsonPath("$.username").value("ron"));
    }

    @Test
    void loginRejectsInvalidPassword() throws Exception {
        registerUser("ron@example.com", "ron");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ron@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    // ───── Logout ─────

    @Test
    void logoutRevokesToken() throws Exception {
        String token = registerUser("ron@example.com", "ron").token();

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutDoesNotAffectOtherTokens() throws Exception {
        registerUser("ron@example.com", "ron");

        String token1 = loginUser("ron@example.com").token();
        String token2 = loginUser("ron@example.com").token();

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token1)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token1)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token2)))
                .andExpect(status().isOk());
    }

    // ───── Soft delete ─────

    @Test
    void deleteAccountSoftDeletesAndPreventsLogin() throws Exception {
        String token = registerUser("ron@example.com", "ron").token();

        mockMvc.perform(delete("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("ron@example.com", "Password123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedAccountEmailCanBeReRegistered() throws Exception {
        String token = registerUser("ron@example.com", "ron").token();

        mockMvc.perform(delete("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("ron@example.com", "ron2", "Password123")))
                .andExpect(status().isCreated());
    }

    // ───── JWT security ─────

    @Test
    void protectedEndpointRejectsMissingJwt() throws Exception {
        mockMvc.perform(get("/api/memos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointAcceptsValidJwt() throws Exception {
        String token = registerUser("ron@example.com", "ron").token();

        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void protectedEndpointRejectsTamperedJwt() throws Exception {
        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer this.is.not-a-valid-jwt"))
                .andExpect(status().isForbidden());
    }

    // ───── Helpers ─────

    private AuthResponse registerUser(String email, String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, username, "Password123")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(body, AuthResponse.class);
    }

    private AuthResponse loginUser(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Password123")))
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(body, AuthResponse.class);
    }

    private String registerJson(String email, String username, String password) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(new RegisterRequest(email, username, password));
    }

    private String loginJson(String email, String password) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(new LoginRequest(email, password));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
