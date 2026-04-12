package com.example.ronproject.memo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.example.ronproject.auth.AuthResponse;
import com.example.ronproject.auth.RegisterRequest;
import com.example.ronproject.user.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class MemoIntegrationTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        memoRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void getMemosReturnsOnlyCurrentUsersMemosOrderedByUpdatedAtDesc() throws Exception {
        AuthResponse primaryUser = registerUser("ron@example.com", "ron");
        AuthResponse otherUser = registerUser("other@example.com", "other");

        String firstMemoId = createMemo(primaryUser.token(), "First memo", "First content")
                .andReturn()
                .getResponse()
                .getContentAsString();
        String createdMemoId = OBJECT_MAPPER.readTree(firstMemoId).get("id").asText();

        createMemo(primaryUser.token(), "Second memo", "Second content")
                .andExpect(status().isCreated());

        createMemo(otherUser.token(), "Other memo", "Should be hidden")
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/memos/{memoId}", createdMemoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(primaryUser.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new MemoRequest("Updated first", "Updated content"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(primaryUser.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Updated first"))
                .andExpect(jsonPath("$[1].title").value("Second memo"));
    }

    @Test
    void createMemoTrimsWhitespace() throws Exception {
        AuthResponse user = registerUser("ron@example.com", "ron");

        mockMvc.perform(post("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new MemoRequest("  Training Plan  ", "  Bench press  ")) ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Training Plan"))
                .andExpect(jsonPath("$.content").value("Bench press"));
    }

    @Test
    void createMemoRejectsInvalidPayload() throws Exception {
        AuthResponse user = registerUser("ron@example.com", "ron");

        mockMvc.perform(post("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new MemoRequest(" ", "content"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title must not be blank"));
    }

    @Test
    void updateMemoReturnsNotFoundForAnotherUsersMemo() throws Exception {
        AuthResponse owner = registerUser("owner@example.com", "owner");
        AuthResponse otherUser = registerUser("other@example.com", "other");

        String responseBody = createMemo(owner.token(), "Private memo", "owner content")
                .andReturn()
                .getResponse()
                .getContentAsString();
        String createdMemoId = OBJECT_MAPPER.readTree(responseBody).get("id").asText();

        mockMvc.perform(put("/api/memos/{memoId}", createdMemoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new MemoRequest("Updated", "Updated"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMemoRemovesOwnedMemo() throws Exception {
        AuthResponse user = registerUser("ron@example.com", "ron");

        String responseBody = createMemo(user.token(), "Delete me", "Soon gone")
                .andReturn()
                .getResponse()
                .getContentAsString();
        String createdMemoId = OBJECT_MAPPER.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/memos/{memoId}", createdMemoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/memos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteMemoReturnsNotFoundForAnotherUsersMemo() throws Exception {
        AuthResponse owner = registerUser("owner@example.com", "owner");
        AuthResponse otherUser = registerUser("other@example.com", "other");

        String responseBody = createMemo(owner.token(), "Private memo", "owner content")
                .andReturn().getResponse().getContentAsString();
        String createdMemoId = OBJECT_MAPPER.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/memos/{memoId}", createdMemoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser.token())))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions createMemo(
            String token,
            String title,
            String content
    ) throws Exception {
        return mockMvc.perform(post("/api/memos")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(new MemoRequest(title, content))));
    }

    private AuthResponse registerUser(String email, String username) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new RegisterRequest(email, username, "Password123"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return OBJECT_MAPPER.readValue(responseBody, AuthResponse.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
