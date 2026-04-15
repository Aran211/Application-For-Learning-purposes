package com.example.ronproject.memo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, UUID> {

    Page<Memo> findAllByUserId(UUID userId, Pageable pageable);

    Optional<Memo> findByIdAndUserId(UUID id, UUID userId);
}
