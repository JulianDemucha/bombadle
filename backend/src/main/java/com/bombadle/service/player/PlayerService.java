package com.bombadle.service.player;

import com.bombadle.dto.PlayerDto;
import com.bombadle.entity.Score;
import com.bombadle.exception.UsernameAlreadyTakenException;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;
    private final PlayerDeletionService playerDeletionService;

    public Optional<Player> findByEmail(String email){
        return repo.findByEmail(email);
    }

    public Optional<Player> findById(long id){
        return repo.findById(id);
    }

    public List<Player> getAllPlayers() {
        return repo.findAllByOrderByIdAsc();
    }

    public PlayerDto getAuthenticatedPlayer(long playerId) {

        Player player = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User from token has NOT been found in the database: " + playerId
                ));
        return PlayerDto.toDto(player);
    }

    public void registerScore(Player player, Score score) {
        player.setHasGuessedToday(true);
        player.setTotalSuccessfulGuesses(player.getTotalSuccessfulGuesses() + 1);
        player.setTodayScore(score);
        repo.save(player);
    }

    @Transactional
    public void resetAllScores() {
        repo.resetAllScores();
        repo.flush();
    }

    @Transactional
    public PlayerDto updatePlayer(PlayerUpdateRequest request, long playerId) {

        // get player and update it with new values
        Player updatedPlayer = repo.findById(playerId)
                .orElseThrow(() -> new UsernameNotFoundException("User from token has NOT been found: " + playerId));

        // IF playerUpdatableDto.login() is NOT blank or null, change users email
        if (!isNullOrIsBlank(request.login())) {
            int length = request.login().length();
            if (length < 3 || length > 16)
                throw new IllegalArgumentException("Username must be between 3 and 16 characters");


            if (!updatedPlayer.getLogin().equals(request.login())) {
                /*
                    if provided login doesn't equal current login, check whether
                    the user with provided login already exists in the database
                */
                if (repo.existsByLogin(request.login()))
                    throw new UsernameAlreadyTakenException("Username " + request.login() + " already exists");

            }

            updatedPlayer.setLogin(request.login());
        }

        if (!isNullOrIsBlank(request.avatarImage())) {
            try {
                AvatarImage newAvatar = AvatarImage.valueOf(request.avatarImage());

                updatedPlayer.setAvatarImage(newAvatar);

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Avatar image '" + request.avatarImage() + "' not supported");
            }
        }

        repo.save(updatedPlayer);

        return PlayerDto.toDto(updatedPlayer);
    }

    @Transactional
    public void deletePlayer(long playerId) {
        playerDeletionService.deletePlayerSelf(playerId);
    }

    private Boolean isNullOrIsBlank(String s) {
        if (s == null) {
            return true;
        } else return s.isBlank();
    }
}
