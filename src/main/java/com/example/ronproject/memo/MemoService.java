package com.example.ronproject.memo;

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
public class MemoService {

    private static final Logger log = LoggerFactory.getLogger(MemoService.class);

    private final MemoRepository memoRepository;
    private final UserAccountRepository userAccountRepository;

    public MemoService(MemoRepository memoRepository, UserAccountRepository userAccountRepository) {
        this.memoRepository = memoRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public List<MemoResponse> getUserMemos(UUID userId) {
        return memoRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(MemoResponse::from)
                .toList();
    }

    @Transactional
    public MemoResponse createMemo(UUID userId, MemoRequest request) {
        UserAccount userAccount = userAccountRepository.getReferenceById(userId);
        Memo memo = new Memo();
        memo.setUser(userAccount);
        memo.setTitle(request.title().trim());
        memo.setContent(request.content().trim());
        Memo saved = memoRepository.save(memo);
        log.info("Memo created: id={}, userId={}", saved.getId(), userId);
        return MemoResponse.from(saved);
    }

    @Transactional
    public MemoResponse updateMemo(UUID userId, UUID memoId, MemoRequest request) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo not found"));
        memo.setTitle(request.title().trim());
        memo.setContent(request.content().trim());
        return MemoResponse.from(memo);
    }

    @Transactional
    public void deleteMemo(UUID userId, UUID memoId) {
        Memo memo = memoRepository.findByIdAndUserId(memoId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo not found"));
        memoRepository.delete(memo);
        log.info("Memo deleted: id={}, userId={}", memoId, userId);
    }
}
