package com.bombadle.service.game;

import com.bombadle.dto.GuessListDto;
import com.bombadle.entity.GuessList;
import com.bombadle.entity.Player;
import com.bombadle.enums.GameMode;
import com.bombadle.repository.GuessListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuessListService {
    private final GuessListRepository guessListRepository;

    @Cacheable(value = "guess-list", key = "#playerId + '-' + #gameMode")
    public GuessListDto getByPlayerId(long playerId, GameMode gameMode) {
        return new GuessListDto(
                guessListRepository.findByPlayerIdAndGameMode(playerId, gameMode)
                        .map(GuessList::getGuesses)
                        .orElse(List.of())
        );
    }

    public GuessList findByPlayerAndGameModeOrElseCreateNew(Player player, GameMode gameMode) {
        return guessListRepository.findByPlayerIdAndGameMode(player.getId(), gameMode)
                .orElseGet(() -> GuessList.builder().player(player).gameMode(gameMode).build());
    }

    public GuessList save(GuessList guessList){
        return guessListRepository.save(guessList);
    }

    public void manualDelete(GuessList guessList) {
        guessListRepository.delete(guessList);
    }

    public void truncateTable() {
        guessListRepository.truncateTable();
    }

    @Transactional
    public void deleteAllByPlayerId(long playerId) {
        guessListRepository.deleteByPlayerId(playerId);
    }
}
