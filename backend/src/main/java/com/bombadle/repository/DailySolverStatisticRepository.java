package com.bombadle.repository;

import com.bombadle.entity.DailySolverStatistic;
import com.bombadle.enums.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailySolverStatisticRepository extends JpaRepository<DailySolverStatistic, Long> {

    boolean existsByGameModeAndPuzzleDate(GameMode gameMode, LocalDate puzzleDate);

    List<DailySolverStatistic> findByGameModeAndPuzzleDateBetweenOrderByPuzzleDateAsc(
            GameMode gameMode, LocalDate from, LocalDate to);
}
