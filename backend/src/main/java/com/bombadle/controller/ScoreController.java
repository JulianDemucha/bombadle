package com.bombadle.controller;

import com.bombadle.entity.Score;
import com.bombadle.service.ScoreService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/scores")
@AllArgsConstructor
public class ScoreController {
    private final ScoreService scoreService;

    @PostMapping
    public ResponseEntity<Score> createScore(@RequestBody Score score) {
        Score saved = scoreService.saveScore(score);
        return ResponseEntity
                .created(URI.create("/api/scores/" + saved.getId()))
                .body(saved);
    }

    @DeleteMapping("/{id}")
    public void deleteScore(@RequestBody Score score) {
        scoreService.deleteScoreById(score.getId());
    }

}

