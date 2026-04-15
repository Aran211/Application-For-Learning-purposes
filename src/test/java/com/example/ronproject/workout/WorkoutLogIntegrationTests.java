package com.example.ronproject.workout;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest
@AutoConfigureMockMvc
class WorkoutLogIntegrationTests {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkoutLogRepository workoutLogRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @BeforeEach
    void setUp() {
        workoutLogRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void getWorkoutLogsReturnsOnlyCurrentUsersLogsOrderedByWorkoutDateThenCreatedAt() throws Exception {
        AuthResponse primaryUser = registerUser("ron@example.com", "ron");
        AuthResponse otherUser = registerUser("other@example.com", "other");

        createWorkoutLog(primaryUser.token(), "Bench Press", 3, 5, 100.0, LocalDate.of(2026, 4, 8), "first")
                .andExpect(status().isCreated());
        createWorkoutLog(primaryUser.token(), "Pull Up", 4, 8, 0.0, LocalDate.of(2026, 4, 9), "second")
                .andExpect(status().isCreated());
        createWorkoutLog(otherUser.token(), "Hidden", 3, 10, 20.0, LocalDate.of(2026, 4, 10), "hidden")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/workouts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(primaryUser.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].exercise").value("Pull Up"))
                .andExpect(jsonPath("$.content[1].exercise").value("Bench Press"));
    }

    @Test
    void createWorkoutLogTrimsExerciseAndNotes() throws Exception {
        AuthResponse user = registerUser("ron@example.com", "ron");

        createWorkoutLog(user.token(), "  Squat  ", 5, 5, 140.0, LocalDate.of(2026, 4, 9), "  Heavy day  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercise").value("Squat"))
                .andExpect(jsonPath("$.notes").value("Heavy day"));
    }

    @Test
    void createWorkoutLogRejectsInvalidPayload() throws Exception {
        AuthResponse user = registerUser("ron@example.com", "ron");

        createWorkoutLog(user.token(), "Deadlift", 0, 5, 180.0, LocalDate.of(2026, 4, 9), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("setsCompleted must be greater than or equal to 1"));
    }

    @Test
    void updateWorkoutLogReturnsNotFoundForAnotherUsersLog() throws Exception {
        AuthResponse owner = registerUser("owner@example.com", "owner");
        AuthResponse otherUser = registerUser("other@example.com", "other");

        String responseBody = createWorkoutLog(
                owner.token(),
                "Bench Press",
                3,
                5,
                100.0,
                LocalDate.of(2026, 4, 9),
                "owner"
        ).andReturn().getResponse().getContentAsString();
        String createdLogId = OBJECT_MAPPER.readTree(responseBody).get("id").asText();

        mockMvc.perform(put("/api/workouts/{workoutLogId}", createdLogId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(
                                new WorkoutLogRequest("Bench Press", 4, 6, 105.0, LocalDate.of(2026, 4, 10), "updated"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteWorkoutLogRemovesOwnedLog() throws Exception {
        AuthResponse user = registerUser("ron@example.com", "ron");

        String responseBody = createWorkoutLog(
                user.token(),
                "Row",
                4,
                10,
                60.0,
                LocalDate.of(2026, 4, 9),
                "delete"
        ).andReturn().getResponse().getContentAsString();
        String createdLogId = OBJECT_MAPPER.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/workouts/{workoutLogId}", createdLogId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workouts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void deleteWorkoutLogReturnsNotFoundForAnotherUsersLog() throws Exception {
        AuthResponse owner = registerUser("owner@example.com", "owner");
        AuthResponse otherUser = registerUser("other@example.com", "other");

        String responseBody = createWorkoutLog(
                owner.token(), "Bench Press", 3, 5, 100.0,
                LocalDate.of(2026, 4, 9), "owner"
        ).andReturn().getResponse().getContentAsString();
        String createdLogId = OBJECT_MAPPER.readTree(responseBody).get("id").asText();

        mockMvc.perform(delete("/api/workouts/{workoutLogId}", createdLogId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherUser.token())))
                .andExpect(status().isNotFound());
    }

    private org.springframework.test.web.servlet.ResultActions createWorkoutLog(
            String token,
            String exercise,
            int setsCompleted,
            int repsCompleted,
            double weightKg,
            LocalDate workoutDate,
            String notes
    ) throws Exception {
        return mockMvc.perform(post("/api/workouts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsString(
                        new WorkoutLogRequest(exercise, setsCompleted, repsCompleted, weightKg, workoutDate, notes))));
    }

    private AuthResponse registerUser(String email, String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBJECT_MAPPER.writeValueAsString(new RegisterRequest(email, username, "Password123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return OBJECT_MAPPER.readValue(body, AuthResponse.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
