package com.bombadle.service.stats;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.cache.CacheService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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

    public Optional<Score> findByPlayerId(Long playerId) {
        return repo.findByPlayerId(playerId);
    }

    public Optional<Score> findScoreByPlayerEmail(String playerEmail) {
        return repo.findByPlayerEmail(playerEmail);
    }

    public void manualDelete(Score score) {
        repo.delete(score);
    }

    public Optional<Instant> findLatestScoreTimestamp(){
        return repo.findLatestScoreTimestamp();
    }

    public Score save(Score score) {
        return repo.save(score);
    }

    public Optional<Score> getScoreById(Long id) {
        return repo.findById(id);
    }

    @Caching(evict = {
            @CacheEvict(value = "top-3-leaderboard", allEntries = true),
            @CacheEvict(value = "full-leaderboard", allEntries = true)
    })
    public void deleteScoreById(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "top-3-leaderboard", allEntries = true),
            @CacheEvict(value = "full-leaderboard", allEntries = true)
    })
    public void deleteAllInBatch() {
        repo.deleteAllInBatch();
    }

    public boolean existsById(Long id) {
        return repo.existsById(id);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "top-3-leaderboard", allEntries = true),
            @CacheEvict(value = "full-leaderboard", allEntries = true)
    })
    public void deleteAllByPlayerId(Long playerId) {
        repo.deleteByPlayerId(playerId);
    }


}
