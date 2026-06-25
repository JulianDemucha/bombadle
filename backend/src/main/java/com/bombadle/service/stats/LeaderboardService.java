package com.bombadle.service.stats;

import com.bombadle.dto.FullLeaderboardEntryDto;
import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.ScoreNotFoundException;
import com.bombadle.repository.ScoreRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private final ScoreRepository repo;


    @Cacheable(value = "top-3-leaderboard", key = "#gameMode.name()")
    public List<LeaderboardEntryDto> getTop3Leaderboard(GameMode gameMode) {
        return repo.findTop3(gameMode);
    }

    @Cacheable(value = "classic-leaderboard", key = "#gameMode.name() + '-' + #page")
    public Page<FullLeaderboardEntryDto> getPagedLeaderboard(GameMode gameMode, int page) {
        return repo.findPagedLeaderboard(gameMode, PageRequest.of(page, 10));
    }

    public List<Score> getTop10Leaderboard() {
        return repo.findTop10ByOrderByScoreTimestampAsc();
    }

    // todo: repair if needed
//    public Optional<Player> getPlayerByPositionInLeaderboard(int position) {
//        List<Score> leaderboard = getPagedLeaderboard();
//        if (position >= 0 && position < leaderboard.size()) {
//            Player player = leaderboard.get(position).getPlayer();
//            log.info("Returning player at leaderboard position {}: {}", position, player.getLogin());
//            return Optional.of(leaderboard.get(position).getPlayer());
//        } else {
//            log.warn("Requested position {} is out of leaderboard bounds (size: {})", position, leaderboard.size());
//            return Optional.empty();
//        }
//    }

    public Long getPlayerRankById(GameMode gameMode, Long playerId) {
        Optional<Score> playerScoreOpt = repo.findByPlayerIdAndGameMode(playerId, gameMode);
        if (playerScoreOpt.isEmpty()) {
            throw new ScoreNotFoundException("Could not find score for player with id " + playerId + " in mode " + gameMode);
        }
        Long rank = repo.findRankByPlayerId(gameMode, playerId);
        log.info("Player with ID {} has rank {} in mode {}", playerId, rank, gameMode);
        return rank;
    }

    public LeaderboardEntryDto getRankedEntryByPlayerId(GameMode gameMode, Long playerId) {
        return repo.findLeaderboardRankedEntryByPlayerId(gameMode, playerId)
                .orElseThrow(() -> new ScoreNotFoundException("Could not find score for player with id " + playerId + " in mode " + gameMode));
    }

    public Optional<Score> getLatestScore() {
        return repo.findTopByOrderByScoreTimestampDesc();
    }

    public int countParticipants(GameMode gameMode) {
        return (int) repo.countByGameMode(gameMode);
    }


}
