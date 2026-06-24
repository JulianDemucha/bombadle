package com.bombadle.service.game;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.entity.AnonymousSession;
import com.bombadle.repository.AnonymousGuessListRepository;
import com.bombadle.service.player.AnonymousSessionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AnonymousGuessListService {
    private final AnonymousGuessListRepository repo;

    public void delete(AnonymousGuessList guessList){
        repo.delete(guessList);
    }

    public void truncateTable(){
        repo.truncateTable();
    }

    public AnonymousGuessList save(AnonymousGuessList guessList){
        return repo.save(guessList);
    }

}
