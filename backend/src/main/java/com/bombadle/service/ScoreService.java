package com.bombadle.service;

import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ScoreService {

    private final ScoreRepository repo;

    public Score saveScore(Score score) {
        if (score == null)
            throw new IllegalArgumentException("Score cannot be null");

        return repo.save(score);
    }

    public List<Score> getAllScores() {
        return repo.findAll();
    }

    public Optional<Score> getScoreByPlayerId(Long playerId) {
        return repo.findByPlayerId(playerId);
    }

    public Optional<Score> getScoreById(Long id) {
        return repo.findById(id);
    }

    public void deleteScoreById(Long id) {
        repo.deleteById(id);
    }

    @Transactional
    public int resetAllScores() {
        int count = (int) repo.count();
        repo.deleteAll();
        repo.flush();
        return count;
    }

    public boolean existsById(Long id) {
        return repo.existsById(id);
    }


}
