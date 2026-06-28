package com.bombadle.repository;

import com.bombadle.entity.DailySolverStatistic;
import com.bombadle.enums.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailySolverStatisticRepository extends JpaRepository<DailySolverStatistic, Long> {

    boolean existsByGameModeAndPuzzleDate(GameMode gameMode, LocalDate puzzleDate);
}
