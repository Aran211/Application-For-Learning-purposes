package com.example.ronproject.memo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, UUID> {

    List<Memo> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Memo> findByIdAndUserId(UUID id, UUID userId);
}
