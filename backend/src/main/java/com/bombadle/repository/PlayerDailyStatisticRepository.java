package com.bombadle.repository;

import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlayerDailyStatisticRepository extends JpaRepository<PlayerDailyStatistic, Long> {

    boolean existsByPlayerIdAndGameModeAndPuzzleDate(Long playerId, GameMode gameMode, LocalDate puzzleDate);

    List<PlayerDailyStatistic> findByPlayerIdOrderByPuzzleDateAscGameModeAsc(Long playerId);

    @Query("""
            SELECT AVG((s.leaderboardPosition * 1.0) / s.totalParticipants)
            FROM PlayerDailyStatistic s
            WHERE s.player.id = :playerId
            """)
    Double findAveragePercentileByPlayerId(@Param("playerId") Long playerId);

    @Query("""
            SELECT COUNT(s)
            FROM PlayerDailyStatistic s
            WHERE s.player.id = :playerId AND s.leaderboardPosition <= 3
            """)
    long countTop3FinishesByPlayerId(@Param("playerId") Long playerId);

    @Query("""
            SELECT s.gameMode, COUNT(s)
            FROM PlayerDailyStatistic s
            WHERE s.player.id = :playerId
            GROUP BY s.gameMode
            """)
    List<Object[]> countByPlayerIdGroupedByGameMode(@Param("playerId") Long playerId);
}
