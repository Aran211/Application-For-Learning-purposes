package com.example.ronproject.workout;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkoutLogResponse(
        UUID id,
        String exercise,
        Integer setsCompleted,
        Integer repsCompleted,
        Double weightKg,
        LocalDate workoutDate,
        String notes,
        Instant createdAt
) {
    static WorkoutLogResponse from(WorkoutLog workoutLog) {
        return new WorkoutLogResponse(
                workoutLog.getId(),
                workoutLog.getExercise(),
                workoutLog.getSetsCompleted(),
                workoutLog.getRepsCompleted(),
                workoutLog.getWeightKg(),
                workoutLog.getWorkoutDate(),
                workoutLog.getNotes(),
                workoutLog.getCreatedAt()
        );
    }
}
