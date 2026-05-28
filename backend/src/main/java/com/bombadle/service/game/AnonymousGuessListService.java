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
    private final AnonymousSessionService anonymousSessionService;

//    public void registerGuess(AnonymousGuessList guessList, GuessAttempt guessAttempt) {
//        guessList.getGuesses().add(guessAttempt);
//        anonymousGuessListRepository.save(guessList);
//    }

    @Transactional
    public UUID registerGuessAndGetSessionId(AnonymousSession anonymousSession, GuessAttempt guessAttempt) {
        AnonymousGuessList guessList = anonymousSession.getGuessList();
        guessList.getGuesses().add(guessAttempt);

        if (guessAttempt.isCorrect()){
            anonymousSession.setHasGuessedToday(true);
            anonymousSession.setScoreTimestamp(Instant.now());
        }

        anonymousSession.setGuessList(repo.save(guessList));
        anonymousSession.setHasGuessedToday(guessAttempt.isCorrect());
        return anonymousSessionService.save(anonymousSession).getId();
    }

    @Transactional
    public void registerGuess(AnonymousSession anonymousSession, GuessAttempt guessAttempt) {
        AnonymousGuessList guessList = anonymousSession.getGuessList();
        guessList.getGuesses().add(guessAttempt);

        if (guessAttempt.isCorrect()){
            anonymousSession.setHasGuessedToday(true);
            anonymousSession.setScoreTimestamp(Instant.now());
        }

        anonymousSession.setGuessList(repo.save(guessList));
        anonymousSessionService.save(anonymousSession);
    }

    public void delete(AnonymousGuessList guessList){
        repo.delete(guessList);
    }

    public void truncateTable(){
        repo.truncateTable();
    }

}
