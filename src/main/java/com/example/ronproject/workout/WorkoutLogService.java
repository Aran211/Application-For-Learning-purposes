package com.example.ronproject.workout;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.ronproject.user.UserAccount;
import com.example.ronproject.user.UserAccountRepository;

@Service
public class WorkoutLogService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogService.class);
    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("workoutDate"),
            Sort.Order.desc("createdAt")
    );

    private final WorkoutLogRepository workoutLogRepository;
    private final UserAccountRepository userAccountRepository;

    public WorkoutLogService(
            WorkoutLogRepository workoutLogRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.workoutLogRepository = workoutLogRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public Page<WorkoutLogResponse> getWorkoutLogs(UUID userId, Pageable pageable) {
        Pageable effective = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        return workoutLogRepository.findAllByUserId(userId, effective).map(WorkoutLogResponse::from);
    }

    @Transactional
    public WorkoutLogResponse createWorkoutLog(UUID userId, WorkoutLogRequest request) {
        UserAccount userAccount = userAccountRepository.getReferenceById(userId);
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
