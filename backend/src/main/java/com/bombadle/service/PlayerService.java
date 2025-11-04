package com.bombadle.service;

import com.bombadle.dto.PlayerDto;
import com.bombadle.dto.PlayerUpdateRequest;
import com.bombadle.dto.mapper.PlayerMapper;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.repository.PlayerRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PlayerService {
    private final PlayerRepository repo;
    private final PlayerMapper playerMapper;

    public List<Player> getAllPlayers() {
        return repo.findAllByOrderByIdAsc();
    }

    public ResponseEntity<PlayerDto> getAuthenticatedPlayer(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Player player = repo.findByEmail(userDetails.getUsername()) // getUsername returns email
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User from token has NOT been found in the database: " + userDetails.getUsername() //email
                ));
        return ResponseEntity.ok(playerMapper.toDto(player));
    }

    private Boolean isNullOrIsBlank(String s) {
        if (s == null) {
            return true;
        } else return s.isBlank();
    }

    @Transactional
    public ResponseEntity<?> updatePlayer(PlayerUpdateRequest request, Authentication authentication) {
        try {

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // user details subject is email instead of username -> getUsername() returns email
            Optional<Player> existingPlayer = repo.findByEmail(userDetails.getUsername());

            if (existingPlayer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Player updatedPlayer = existingPlayer.get();

            // IF playerUpdatableDto.login() is NOT blank or null, change users email
            if (!isNullOrIsBlank(request.login())) {
                if (request.login().length() < 3 || request.login().length() > 16) {
                    throw new ResponseStatusException((HttpStatus.CONFLICT),
                            "Username must be between 3 and 16 characters");
                }

                /*
                if provided login doesnt equal current login, check whether
                the user with provided login already exists in the database
                */

                if(!updatedPlayer.getLogin().equals(request.login())) {
                    if (repo.existsByLogin(request.login()))
                        throw new ResponseStatusException((HttpStatus.CONFLICT),
                                "Username "+request.login()+"already exists");
                }

                updatedPlayer.setLogin(request.login());
            }

            if (!isNullOrIsBlank(request.avatarImage())) {
                try {
                    AvatarImage newAvatar = AvatarImage.valueOf(request.avatarImage());

                    updatedPlayer.setAvatarImage(newAvatar);

                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Avatar image '" + request.avatarImage() + "' not supported");
                }
            }

            repo.save(updatedPlayer);

            return ResponseEntity.ok(updatedPlayer);

        } catch (ExpiredJwtException e) {
            log.debug("UpdatePlayer: Expired JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token wygasł");

        } catch (UsernameNotFoundException e) {
            log.debug("UpdatePlayer: Username not found");
            return ResponseEntity.badRequest().body("Username not found");

        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            log.debug("UpdatePlayer: Invalid JWT token");
            return ResponseEntity.badRequest().body("Invalid JWT token");

        } catch (Exception e) {
            log.error("UpdatePlayer: Unexpected error while extracting username from JWT token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
        }
    }

    @Transactional
    public ResponseEntity<?> deletePlayer(Authentication authentication) {
        try{
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // user details subject is email instead of username -> getUsername() returns email
            String email = userDetails.getUsername();

            //deleteByEmail() returns number of deleted rows in database
            return repo.deleteByEmail(email) > 0 ?
                    ResponseEntity.ok().build()
                    :
                    ResponseEntity.notFound().build();
        } catch (ExpiredJwtException e) {
            log.debug("DeletePlayer: Expired JWT token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token wygasł");

        } catch (UsernameNotFoundException e) {
            log.debug("DeletePlayer: Username not found");
            return ResponseEntity.badRequest().body("Username not found");

        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            log.debug("DeletePlayer: Invalid JWT token");
            return ResponseEntity.badRequest().body("Invalid JWT token");

        } catch (Exception e) {
            log.error("DeletePlayer: Unexpected error while extracting username from JWT token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
        }
    }
}
