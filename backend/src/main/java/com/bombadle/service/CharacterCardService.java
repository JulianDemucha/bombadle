package com.bombadle.service;

import com.bombadle.repository.CharacterCardRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CharacterCardService {
    private static final Logger log = LoggerFactory.getLogger(CharacterCardService.class);
    private final CharacterCardRepository repo;


}
