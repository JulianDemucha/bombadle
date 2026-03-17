package com.bombadle.repository;
import com.bombadle.entity.GuessList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface GuessListRepository extends JpaRepository<GuessList, Long> {
    Optional<GuessList> findByPlayerId(Long playerId);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE guess_list", nativeQuery = true)
    void truncateTable();
}
