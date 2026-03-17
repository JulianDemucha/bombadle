package com.bombadle.service.game;

import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.repository.GuessListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuessListService {
    private final GuessListRepository guessListRepository;

    public Optional<GuessList> findByPlayerId(long playerId) {
        return guessListRepository.findById(playerId);
    }

    public List<GuessAttempt> getGuessListByPlayerId(long playerId) {
        return guessListRepository.findById(playerId)
                .map(GuessList::getGuesses)
                .orElse(List.of());
    }

    public GuessList findByPlayerOrElseCreateNew(Player player) {
        return guessListRepository.findByPlayerId(player.getId())
                .orElseGet(() -> new GuessList(player));
    }

    @Transactional
    public void registerGuess(Player player, GuessAttempt guessResponse) {
        GuessList guessList = findByPlayerOrElseCreateNew(player);
        guessList.getGuesses().add(guessResponse);
        guessListRepository.save(guessList);
    }

    @Transactional
    public void registerGuess(GuessList guessList, GuessAttempt guessResponse) {
        guessList.getGuesses().add(guessResponse);
        guessListRepository.save(guessList);
    }

    public void truncateTable(){
        guessListRepository.truncateTable();
        guessListRepository.flush();
    }
}
