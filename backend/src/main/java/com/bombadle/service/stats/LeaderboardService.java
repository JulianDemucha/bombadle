package com.bombadle.service.stats;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private final ScoreRepository repo;

    public List<LeaderboardEntryDto> getTop3Leaderboard(){
        return repo.findTop3();
    }

    public Page<LeaderboardEntryDto> getPagedLeaderboard(Pageable pageable) {
        return repo.findPagedLeaderboard(pageable);
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

    public Long getPlayerRankById(Long playerId) {
        Optional<Score> playerScoreOpt = repo.findByPlayerId(playerId);
        if (playerScoreOpt.isEmpty()) {
            //todo: custom exception
            throw new RuntimeException("Could not find player with id " + playerId);
        }
            Long rank = repo.findRankByPlayerId(playerId);
            log.info("Player with ID {} has rank {}", playerId, rank);
            return rank;
    }

    public LeaderboardEntryDto getRankedEntryByPlayerId(Long playerId) {
        return repo.findLeaderboardRankedEntryByPlayerId(playerId)
                //todo: custom exception
                .orElseThrow(() -> new RuntimeException("Could not find player with id " + playerId));
    }

    public Optional<Score> getLatestScore() {
        return repo.findTopByOrderByScoreTimestampDesc();
    }


}
