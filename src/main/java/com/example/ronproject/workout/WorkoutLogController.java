package com.example.ronproject.workout;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.ronproject.user.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workouts")
public class WorkoutLogController {

    private final WorkoutLogService workoutLogService;

    public WorkoutLogController(WorkoutLogService workoutLogService) {
        this.workoutLogService = workoutLogService;
    }

    @GetMapping
    Page<WorkoutLogResponse> getWorkoutLogs(CurrentUser currentUser, @PageableDefault(size = 50) Pageable pageable) {
        return workoutLogService.getWorkoutLogs(currentUser.getId(), pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkoutLogResponse createWorkoutLog(CurrentUser currentUser, @Valid @RequestBody WorkoutLogRequest request) {
        return workoutLogService.createWorkoutLog(currentUser.getId(), request);
    }

    @PutMapping("/{workoutLogId}")
    WorkoutLogResponse updateWorkoutLog(
            CurrentUser currentUser,
            @PathVariable UUID workoutLogId,
            @Valid @RequestBody WorkoutLogRequest request
    ) {
        return workoutLogService.updateWorkoutLog(currentUser.getId(), workoutLogId, request);
    }

    @DeleteMapping("/{workoutLogId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteWorkoutLog(CurrentUser currentUser, @PathVariable UUID workoutLogId) {
        workoutLogService.deleteWorkoutLog(currentUser.getId(), workoutLogId);
    }
}
