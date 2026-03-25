package com.bombadle.service;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;

    public Optional<Player> findByEmail(String email){
        return repo.findByEmail(email);
    }

    public List<Player> getAllPlayers() {
        return repo.findAllByOrderByIdAsc();
    }

    public PlayerDto getAuthenticatedPlayer(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Player player = repo.findByEmail(userDetails.getUsername()) // getUsername returns email
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User from token has NOT been found in the database: " + userDetails.getUsername() //email
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
    public PlayerDto updatePlayer(PlayerUpdateRequest request, Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // user details subject is email instead of username -> getUsername() returns email
        // get player and update it with new values
        Player updatedPlayer = repo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User from token has NOT been found: " + userDetails.getUsername()));

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
    public void deletePlayer(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // user details subject is email instead of username -> getUsername() returns email
        String email = userDetails.getUsername();

        //deleteByEmail() returns number of deleted rows in database
        int deleted = repo.deleteByEmail(email);

        if (deleted == 0)
            throw new UsernameNotFoundException("User from token has NOT been found: " + email);
    }

    private Boolean isNullOrIsBlank(String s) {
        if (s == null) {
            return true;
        } else return s.isBlank();
    }
}
