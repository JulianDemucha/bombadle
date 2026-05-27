package com.bombadle.controller;

import com.bombadle.entity.Score;
import com.bombadle.service.stats.ScoreService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/scores")
@AllArgsConstructor
public class ScoreController {
    private final ScoreService scoreService;

    @GetMapping
    public Page<Score> getAllScores(Pageable pageable) {
        return scoreService.getAllScores(pageable);
    }

//    @PostMapping
//    public ResponseEntity<Score> createScore(@RequestBody Score score) {
//        Score saved = scoreService.saveScore(score);
//        return ResponseEntity
//                .created(URI.create("/api/scores/" + saved.getId()))
//                .body(saved);
//    }

    @DeleteMapping("/{id}")
    public void deleteScore(@RequestBody Score score) {
        scoreService.deleteScoreById(score.getId());
    }

}
