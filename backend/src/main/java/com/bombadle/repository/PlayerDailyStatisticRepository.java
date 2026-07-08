package com.bombadle.repository;

import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlayerDailyStatisticRepository extends JpaRepository<PlayerDailyStatistic, Long> {

    boolean existsByPlayerIdAndGameModeAndPuzzleDate(Long playerId, GameMode gameMode, LocalDate puzzleDate);

    List<PlayerDailyStatistic> findAllByPlayerId(Long playerId);

    /**
     * Returns each of the player's recorded solves paired with the finalized end-of-day solver count
     * for its (gameMode, puzzleDate), via a LEFT JOIN to {@link com.bombadle.entity.DailySolverStatistic}.
     * The aggregate count is {@code null} for the current in-progress day (no aggregate row written
     * until the next reset), which yields a {@code null} percentile on read.
     * Each row is {@code [PlayerDailyStatistic, Integer totalSolvers]}.
     */
    @Query("""
            SELECT s, d.totalSolvers
            FROM PlayerDailyStatistic s
            LEFT JOIN DailySolverStatistic d
                ON d.gameMode = s.gameMode AND d.puzzleDate = s.puzzleDate
            WHERE s.player.id = :playerId
            ORDER BY s.puzzleDate ASC, s.gameMode ASC
            """)
    List<Object[]> findChartRowsByPlayerId(@Param("playerId") Long playerId);

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

    @Modifying
    @Query("DELETE FROM PlayerDailyStatistic s WHERE s.player.id = :playerId")
    int deleteByPlayerId(@Param("playerId") Long playerId);
}
