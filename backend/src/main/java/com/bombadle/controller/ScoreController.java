package com.bombadle.controller;

import com.bombadle.entity.Score;
import com.bombadle.service.ScoreService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scores")
@AllArgsConstructor
public class ScoreController {
    private final ScoreService scoreService;

}

