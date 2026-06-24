package com.bombadle.repository;

import com.bombadle.dto.LeaderboardEntryDto;
import com.bombadle.entity.Score;
import com.bombadle.enums.GameMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    // todo - add pagination to this method
    //  (return 10 values per page)
    List<Score> findTop10ByOrderByScoreTimestampAsc();

    Optional<Score> findTopByOrderByScoreTimestampDesc();

    @Query("SELECT MAX(s.scoreTimestamp) FROM Score s")
    Optional<Instant> findLatestScoreTimestamp();

    @Query("""
            SELECT new com.bombadle.dto.LeaderboardEntryDto(
                p.id,
                ROW_NUMBER() OVER(ORDER BY s.scoreTimestamp ASC),
                p.displayName,
                p.avatarImage,
                s.scoreTimestamp,
                s.numberOfTries,
                p.totalSuccessfulGuesses
            )
            FROM Score s
            JOIN s.player p
            WHERE s.gameMode = :gameMode 
            ORDER BY s.scoreTimestamp ASC
            LIMIT 3
            """)
    List<LeaderboardEntryDto> findTop3(@Param("gameMode") GameMode gameMode);

    @Query("""
            SELECT new com.bombadle.dto.LeaderboardEntryDto(
                p.id,
                (
                    SELECT COUNT(s2) + 1
                    FROM Score s2
                    WHERE s2.gameMode = :gameMode AND s2.scoreTimestamp < s.scoreTimestamp
                ),
                p.displayName,
                p.avatarImage,
                s.scoreTimestamp,
                s.numberOfTries,
                p.totalSuccessfulGuesses
            )
            FROM Score s
            JOIN s.player p
            WHERE p.id = :playerId AND s.gameMode = :gameMode
            """)
    Optional<LeaderboardEntryDto> findLeaderboardRankedEntryByPlayerId(@Param("gameMode") GameMode gameMode, @Param("playerId") Long playerId);

    @Query("""
                SELECT COUNT(s) + 1
                FROM Score s 
                WHERE s.gameMode = :gameMode AND s.scoreTimestamp < (
                    SELECT playerScore.scoreTimestamp 
                    FROM Score playerScore 
                    WHERE playerScore.player.id = :playerId AND playerScore.gameMode = :gameMode
                )
            """)
    Long findRankByPlayerId(@Param("gameMode") GameMode gameMode, @Param("playerId") Long playerId);

    Optional<Score> findByPlayerIdAndGameMode(Long playerId, GameMode gameMode);

    @Query("""
                SELECT COUNT(s) + 1
                FROM Score s 
                WHERE s.scoreTimestamp < (
                    SELECT playerScore.scoreTimestamp 
                    FROM Score playerScore 
                    WHERE playerScore.player.id = :playerId
                )
            """)
    Long findRankByPlayerId(@Param("playerId") Long playerId);

    @Query(value = """
            SELECT new com.bombadle.dto.LeaderboardEntryDto(
                p.id,
                ROW_NUMBER() OVER(ORDER BY s.scoreTimestamp ASC),
                p.displayName,
                p.avatarImage,
                s.scoreTimestamp,
                s.numberOfTries,
                p.totalSuccessfulGuesses
            )
            FROM Score s
            JOIN s.player p
            WHERE s.gameMode = :gameMode
            ORDER BY s.scoreTimestamp ASC
            """,
            countQuery = """
                    SELECT COUNT(s.id)
                    FROM Score s
                    WHERE s.gameMode = :gameMode
                    """)
    Page<LeaderboardEntryDto> findPagedLeaderboard(@Param("gameMode") GameMode gameMode, Pageable pageable);

    @EntityGraph(attributePaths = "player")
    Page<Score> findAll(Pageable pageable);

    Optional<Score> findByPlayerId(Long playerId);

    Optional<Score> findByPlayerEmail(String playerEmail);

    @Modifying
    @Query("DELETE FROM Score s WHERE s.player.id = :playerId")
    int deleteByPlayerId(@Param("playerId") Long playerId);

    Optional<Score> findById(Long id);

    void deleteById(Long playerId);
}
