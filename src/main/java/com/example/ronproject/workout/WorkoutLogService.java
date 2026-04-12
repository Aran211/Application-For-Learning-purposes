package com.example.ronproject.workout;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.ronproject.user.UserAccount;
import com.example.ronproject.user.UserAccountRepository;

@Service
public class WorkoutLogService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogService.class);

    private final WorkoutLogRepository workoutLogRepository;
    private final UserAccountRepository userAccountRepository;

    public WorkoutLogService(
            WorkoutLogRepository workoutLogRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.workoutLogRepository = workoutLogRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<WorkoutLogResponse> getWorkoutLogs(UUID userId) {
        return workoutLogRepository.findAllByUserIdOrderByWorkoutDateDescCreatedAtDesc(userId).stream()
                .map(WorkoutLogResponse::from)
                .toList();
    }

    @Transactional
    public WorkoutLogResponse createWorkoutLog(UUID userId, WorkoutLogRequest request) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        WorkoutLog workoutLog = new WorkoutLog();
        workoutLog.setUser(userAccount);
        workoutLog.setExercise(request.exercise().trim());
        workoutLog.setSetsCompleted(request.setsCompleted());
        workoutLog.setRepsCompleted(request.repsCompleted());
        workoutLog.setWeightKg(request.weightKg());
        workoutLog.setWorkoutDate(request.workoutDate());
        workoutLog.setNotes(request.notes() == null ? null : request.notes().trim());
        WorkoutLog saved = workoutLogRepository.save(workoutLog);
        log.info("Workout log created: id={}, exercise={}, userId={}", saved.getId(), saved.getExercise(), userId);
        return WorkoutLogResponse.from(saved);
    }

    @Transactional
    public WorkoutLogResponse updateWorkoutLog(UUID userId, UUID workoutLogId, WorkoutLogRequest request) {
        WorkoutLog workoutLog = workoutLogRepository.findByIdAndUserId(workoutLogId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found"));
        workoutLog.setExercise(request.exercise().trim());
        workoutLog.setSetsCompleted(request.setsCompleted());
        workoutLog.setRepsCompleted(request.repsCompleted());
        workoutLog.setWeightKg(request.weightKg());
        workoutLog.setWorkoutDate(request.workoutDate());
        workoutLog.setNotes(request.notes() == null ? null : request.notes().trim());
        return WorkoutLogResponse.from(workoutLog);
    }

    @Transactional
    public void deleteWorkoutLog(UUID userId, UUID workoutLogId) {
        WorkoutLog workoutLog = workoutLogRepository.findByIdAndUserId(workoutLogId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log not found"));
        workoutLogRepository.delete(workoutLog);
        log.info("Workout log deleted: id={}, userId={}", workoutLogId, userId);
    }
}
