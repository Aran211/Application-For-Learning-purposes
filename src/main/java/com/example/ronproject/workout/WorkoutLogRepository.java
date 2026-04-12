package com.example.ronproject.workout;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {

    List<WorkoutLog> findAllByUserIdOrderByWorkoutDateDescCreatedAtDesc(UUID userId);

    Optional<WorkoutLog> findByIdAndUserId(UUID id, UUID userId);
}
