package com.bombadle.service.stats;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import com.bombadle.service.cache.CacheService;
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
    private final LeaderboardService leaderboardService;
    private final CacheService cacheService;

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

    @Transactional
    @CacheEvict(value = "top-3-leaderboard", allEntries = true, condition = "@scoreRepository.count() <= 3")
    public Score registerScore(Player player, int numberOfTries) {
        Score score = Score.builder()
                .player(player)
                .scoreTimestamp(Instant.now())
                .numberOfTries(numberOfTries)
                .build();

        Score savedScore = repo.save(score);
        evictClassicLeaderboardLastPage();
        return savedScore;
    }

    @Transactional
    public Score registerScoreWithTimestamp(Player player, int numberOfTries, Instant timestamp) {
        Optional<Instant> latestTimestamp = repo.findLatestScoreTimestamp();
        boolean isHistoricalInsert = latestTimestamp.isPresent() && timestamp.isBefore(latestTimestamp.get());

        Score score = Score.builder()
                .player(player)
                .scoreTimestamp(timestamp)
                .numberOfTries(numberOfTries)
                .build();

        Score savedScore = repo.save(score);
        if (isHistoricalInsert) {
            cacheService.clear("classic-leaderboard");
        } else {
            evictClassicLeaderboardLastPage();
        }
        evictTop3CacheIfNeeded(timestamp);

        return savedScore;
    }

    private void evictClassicLeaderboardLastPage() {
        long count = repo.count();
        int lastPage = count > 0 ? (int) ((count - 1) / 10) : 0;
        cacheService.evictCacheEntry("classic-leaderboard", lastPage);
    }

    private void evictTop3CacheIfNeeded(Instant newTimestamp) {
        List<LeaderboardEntryDto> currentTop3 = leaderboardService.getTop3Leaderboard();

        if (currentTop3.size() < 3 || !newTimestamp.isAfter(currentTop3.get(2).scoreTimeStamp())) {
            cacheService.evictCache("top-3-leaderboard");
        }
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
