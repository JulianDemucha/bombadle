package com.bombadle.service.stats;

import com.bombadle.dto.FullLeaderboardEntryDto;
import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.dto.StreakLeaderboardEntryDto;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import com.bombadle.exception.ScoreNotFoundException;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.repository.ScoreRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class LeaderboardService {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);
    private static final int STREAK_PAGE_SIZE = 10;
    private final ScoreRepository repo;
    private final PlayerRepository playerRepository;


    @Cacheable(value = "top-3-leaderboard", key = "#gameMode.name()")
    public List<LeaderboardEntryDto> getTop3Leaderboard(GameMode gameMode) {
        return repo.findTop3(gameMode);
    }

    @Cacheable(value = "full-leaderboard", key = "#gameMode.name() + '-' + #page")
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

    /*
     * Player-level streak rankings. Not cached: unlike the per-mode score leaderboards (whose data
     * is fixed for the day), streak counters change throughout the day as players solve, so these
     * must always read the current standings. Rank is assigned by position in the ordered result.
     */

    public List<StreakLeaderboardEntryDto> getStreakTop3() {
        return toRankedTop3(
                playerRepository.findTop3ByCurrentStreakGreaterThanOrderByCurrentStreakDescLongestStreakDescIdAsc(0)
        );
    }

    public List<StreakLeaderboardEntryDto> getSuperstreakTop3() {
        return toRankedTop3(
                playerRepository.findTop3ByCurrentSuperstreakGreaterThanOrderByCurrentSuperstreakDescLongestSuperstreakDescIdAsc(0)
        );
    }

    public Page<StreakLeaderboardEntryDto> getStreakPagedLeaderboard(int page) {
        Pageable pageable = PageRequest.of(page, STREAK_PAGE_SIZE);
        return toRankedPage(
                playerRepository.findByCurrentStreakGreaterThanOrderByCurrentStreakDescLongestStreakDescIdAsc(0, pageable),
                pageable
        );
    }

    public Page<StreakLeaderboardEntryDto> getSuperstreakPagedLeaderboard(int page) {
        Pageable pageable = PageRequest.of(page, STREAK_PAGE_SIZE);
        return toRankedPage(
                playerRepository.findByCurrentSuperstreakGreaterThanOrderByCurrentSuperstreakDescLongestSuperstreakDescIdAsc(0, pageable),
                pageable
        );
    }

    private List<StreakLeaderboardEntryDto> toRankedTop3(List<Player> players) {
        List<StreakLeaderboardEntryDto> ranked = new ArrayList<>(players.size());
        for (int i = 0; i < players.size(); i++) {
            ranked.add(StreakLeaderboardEntryDto.of(players.get(i), i + 1L));
        }
        return ranked;
    }

    private Page<StreakLeaderboardEntryDto> toRankedPage(Page<Player> players, Pageable pageable) {
        long base = pageable.getOffset();
        List<Player> content = players.getContent();
        List<StreakLeaderboardEntryDto> ranked = new ArrayList<>(content.size());
        for (int i = 0; i < content.size(); i++) {
            ranked.add(StreakLeaderboardEntryDto.of(content.get(i), base + i + 1));
        }
        return new PageImpl<>(ranked, pageable, players.getTotalElements());
    }
}
