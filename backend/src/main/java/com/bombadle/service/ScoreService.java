package com.bombadle.service;

import com.bombadle.entity.Score;
import com.bombadle.repository.ScoreRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ScoreService {

    private final ScoreRepository repo;

    public Score saveScore(Score score){
        return repo.save(score);
    }

    public List<Score> getAllScores(){
        return repo.findAll();
    }

    public Optional<Score> getLatestScore(){
        return repo.findTopByOrderByScoreTimestampDesc();
    }

    public Optional<Score> getScoreByPlayerId(Long playerId){
        return repo.findByPlayerId(playerId);
    }

    public Optional<Score> getScoreById(Long id){
        return repo.findById(id);
    }
    public void deleteScoreById(Long id) {
        repo.deleteById(id);
    }

    public List<Score> getAllScoresSortedAsc(){
        return repo.findAllOrderByScoreTimestampAsc();
    }
}
