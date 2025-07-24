package com.bombadle.service;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private final ScoreRepository repo;

    public List<Score> getSortedLeaderboard(){
        return repo.findAllOrderByScoreTimestampAsc();
    }

    public List<Score> getTop10Leaderboard(){
        return repo.findTop10ByOrderByScoreTimestampAsc();
    }

    public Optional<Player> getPlayerByPositionInLeaderboard(int position) {
        List<Score> leaderboard = getSortedLeaderboard();
        if (position >= 0 && position < leaderboard.size()) {
            Player player = leaderboard.get(position).getPlayer();
            log.info("Returning player at leaderboard position {}: {}", position, player.getLogin());
            return Optional.of(leaderboard.get(position).getPlayer());
        } else {
            log.warn("Requested position {} is out of leaderboard bounds (size: {})", position, leaderboard.size());
            return Optional.empty();
        }
    }

    public Optional<Score> getLatestScore(){
        return repo.findTopByOrderByScoreTimestampDesc();
    }


}
