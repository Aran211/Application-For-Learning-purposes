package com.example.ronproject.workout;

import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutLogRequest(
        @NotBlank @Size(max = 100) String exercise,
        @NotNull @Min(1) Integer setsCompleted,
        @NotNull @Min(1) Integer repsCompleted,
        @NotNull @DecimalMin("0.0") Double weightKg,
        @NotNull LocalDate workoutDate,
        @Size(max = 500) String notes
) {
}
