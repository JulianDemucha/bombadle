package com.bombadle.service.stats;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ScoreService {

    private final ScoreRepository repo;

    public Score saveScore(Score score) {
        if (score == null)
            throw new IllegalArgumentException("Score cannot be null");

        return repo.save(score);
    }

    public List<Score> getAllScores() {
        return repo.findAll();
    }

    public Page<Score> getAllScores(Pageable pageable) {
        return repo.findAll(pageable);
    }

    public Optional<Score> findScoreByPlayerId(Long playerId) {
        return repo.findByPlayerId(playerId);
    }

    public Optional<Score> findScoreByPlayerEmail(String playerEmail) {
        return repo.findByPlayerEmail(playerEmail);
    }

    @Transactional
    @CacheEvict(value = "top-3-leaderboard", allEntries = true, condition = "@scoreRepository.count() <= 3")
    public Score registerScore(Player player, int numberOfTries) {
        return registerScoreWithTimestamp(player, numberOfTries, Instant.now());
    }

    @Transactional
    @CacheEvict(value = "top-3-leaderboard", allEntries = true, condition = "@scoreRepository.count() <= 3")
    public Score registerScoreWithTimestamp(Player player, int numberOfTries, Instant timestamp) {
        Score score = Score.builder()
                .player(player)
                .scoreTimestamp(timestamp)
                .numberOfTries(numberOfTries)
                .build();
        return repo.save(score);
    }

    public Optional<Score> getScoreById(Long id) {
        return repo.findById(id);
    }

    @CacheEvict(value = "top-3-leaderboard", allEntries = true)
    public void deleteScoreById(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    @CacheEvict(value = "top-3-leaderboard", allEntries = true)
    public void deleteAllInBatch() {
        repo.deleteAllInBatch();
    }

    public boolean existsById(Long id) {
        return repo.existsById(id);
    }


}
