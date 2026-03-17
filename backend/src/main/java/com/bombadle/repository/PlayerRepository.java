package com.bombadle.repository;

import com.bombadle.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByLogin(String Login);
    Optional<Player> findByEmail(String email);
    List<Player> findAllByOrderByIdAsc();
    Boolean existsByLogin(String Login);
    Boolean existsByEmail(String email);
    int deleteByEmail(String Login);
    @Modifying
    @Transactional
    @Query("UPDATE Player p SET p.hasGuessedToday = false")
    void resetAllGuessFlags();
}
