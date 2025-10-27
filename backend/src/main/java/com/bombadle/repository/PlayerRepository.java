package com.bombadle.repository;

import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByLogin(String Login);
    Optional<Player> findByEmail(String email);
    List<Player> findAllByOrderByIdAsc();
    Boolean existsByLogin(String Login);
    Boolean existsByEmail(String email);
}
