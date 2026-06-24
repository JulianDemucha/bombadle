package com.bombadle.repository;

import com.bombadle.entity.PlayerDailyStatistic;
import com.bombadle.enums.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PlayerDailyStatisticRepository extends JpaRepository<PlayerDailyStatistic, Long> {

    boolean existsByPlayerIdAndGameModeAndPuzzleDate(Long playerId, GameMode gameMode, LocalDate puzzleDate);
}
