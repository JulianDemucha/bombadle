package com.bombadle.repository;

import com.bombadle.entity.ModeExclusionHistory;
import com.bombadle.enums.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModeExclusionHistoryRepository extends JpaRepository<ModeExclusionHistory, Long> {
    Optional<ModeExclusionHistory> findByGameMode(GameMode gameMode);
}
