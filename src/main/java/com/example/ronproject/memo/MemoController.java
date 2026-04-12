package com.example.ronproject.memo;

import java.util.List;
import java.util.UUID;

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
@RequestMapping("/api/memos")
public class MemoController {

    private final MemoService memoService;

    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping
    List<MemoResponse> getMemos(CurrentUser currentUser) {
        return memoService.getUserMemos(currentUser.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MemoResponse createMemo(CurrentUser currentUser, @Valid @RequestBody MemoRequest request) {
        return memoService.createMemo(currentUser.getId(), request);
    }

    @PutMapping("/{memoId}")
    MemoResponse updateMemo(
            CurrentUser currentUser,
            @PathVariable UUID memoId,
            @Valid @RequestBody MemoRequest request
    ) {
        return memoService.updateMemo(currentUser.getId(), memoId, request);
    }

    @DeleteMapping("/{memoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMemo(CurrentUser currentUser, @PathVariable UUID memoId) {
        memoService.deleteMemo(currentUser.getId(), memoId);
    }
}
