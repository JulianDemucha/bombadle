package com.bombadle.dto;

import com.bombadle.entity.Feedback;

import java.time.Instant;

public record FeedbackDto(
        Long id,
        String title,
        String description,
        String authorDisplayName,
        Instant createdAt
) {
    public static FeedbackDto from(Feedback feedback, String authorDisplayName) {
        return new FeedbackDto(
                feedback.getId(),
                feedback.getTitle(),
                feedback.getDescription(),
                authorDisplayName,
                feedback.getCreatedAt()
        );
    }
}
