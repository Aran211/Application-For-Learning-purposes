package com.example.ronproject.workout;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, UUID> {

    Page<WorkoutLog> findAllByUserId(UUID userId, Pageable pageable);

    Optional<WorkoutLog> findByIdAndUserId(UUID id, UUID userId);
}
