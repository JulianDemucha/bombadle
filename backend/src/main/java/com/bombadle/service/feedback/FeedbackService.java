package com.bombadle.service.feedback;

import com.bombadle.dto.FeedbackDto;
import com.bombadle.entity.Feedback;
import com.bombadle.repository.FeedbackRepository;
import com.bombadle.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final String UNKNOWN_AUTHOR = "[nieznany]";

    private final FeedbackRepository feedbackRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public void submit(String title, String description, Long authorPlayerId) {
        feedbackRepository.save(new Feedback(title, description, authorPlayerId, Instant.now()));
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDto> getFeedbackPage(Pageable pageable) {
        return feedbackRepository.findAll(pageable)
                .map(feedback -> FeedbackDto.from(feedback, resolveAuthorDisplayName(feedback.getPlayerId())));
    }

    @Transactional
    public void deleteOlderThan(Instant cutoff) {
        feedbackRepository.deleteByCreatedAtBefore(cutoff);
    }

    @Transactional
    public void nullifyAuthor(Long playerId) {
        feedbackRepository.nullifyPlayerIdByPlayerId(playerId);
    }

    private String resolveAuthorDisplayName(Long playerId) {
        if (playerId == null) {
            return UNKNOWN_AUTHOR;
        }
        return playerRepository.findById(playerId)
                .map(player -> player.getDisplayName())
                .orElse(UNKNOWN_AUTHOR);
    }
}
