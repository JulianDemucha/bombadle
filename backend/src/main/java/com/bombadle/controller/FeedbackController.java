package com.bombadle.controller;

import com.bombadle.config.PlayerPrincipal;
import com.bombadle.dto.request.FeedbackRequest;
import com.bombadle.service.feedback.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Void> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal PlayerPrincipal principal
    ) {
        feedbackService.submit(request.title(), request.description(), principal.getPlayerId());
        return ResponseEntity.ok().build();
    }
}
