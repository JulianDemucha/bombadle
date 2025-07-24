package com.bombadle.repository;

import com.bombadle.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {
    List<Score> findTop10ByOrderByScoreTimestampAsc(); //zwraca pierwsze 10 score
    Optional<Score> findTopByOrderByScoreTimestampDesc(); //zwraca najnowszy score
    List<Score> findAllOrderByScoreTimestampAsc();
    Optional<Score> findByPlayerId(Long playerId);
    Optional<Score> findById(Long id);
    void deleteById(Long playerId);
}
