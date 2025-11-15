package com.bombadle.service;

import com.bombadle.dto.PlayerDto;
import com.bombadle.exception.UsernameAlreadyTakenException;
import com.bombadle.dto.request.PlayerUpdateRequest;
import com.bombadle.dto.mapper.PlayerMapper;
import com.bombadle.entity.Player;
import com.bombadle.enums.AvatarImage;
import com.bombadle.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {
    @Mock
    PlayerRepository repo;
    @InjectMocks
    PlayerService playerService;
    @Mock
    PlayerMapper playerMapper;

    private Authentication mockAuthWithEmail(String email) {
        Authentication auth = mock(Authentication.class);
        UserDetails ud = mock(UserDetails.class);
        when(ud.getUsername()).thenReturn(email);
        when(auth.getPrincipal()).thenReturn(ud);
        return auth;
    }

    private PlayerDto getExamplePlayerDto(String email) {
        return new PlayerDto(
                1L,
                "test",
                email,
                "ROLE_USER",
                "AVATAR_DEFAULT",
                "2025-11-10T14:22:27.123Z",
                "2025-11-10T14:22:27.123Z",
                false,
                null,
                0,
                "LOCAL"
        );
    }

    @Test
    void getAuthenticatedPlayerShouldReturnDtoWhenUserExists() {
        String email = "test@gmail.com";
        Authentication auth = mockAuthWithEmail(email);
        Player player = new Player();
        player.setEmail("test@test.test");
        PlayerDto dto = getExamplePlayerDto(email);
        when(repo.findByEmail(email)).thenReturn(Optional.of(player));
        when(playerMapper.toDto(player)).thenReturn(dto);

        PlayerDto returnedDto = playerService.getAuthenticatedPlayer(auth);

        assertEquals(dto, returnedDto);

        // check if findByEmail() and toDto() have been called
        verify(repo).findByEmail(email);
        verify(playerMapper).toDto(player);
    }

    @Test
    void getAuthenticatedPlayerShouldThrowWhenUserDoesNotExist() {
        String email = "test@test.test";
        Authentication auth = mockAuthWithEmail(email);

        when(repo.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> playerService.getAuthenticatedPlayer(auth));
        verify(repo).findByEmail(email);
    }

    @Test
    void updatePlayerShouldReturnDtoWhenDataIsValid() {
        String email = "test@test.com";
        Authentication auth = mockAuthWithEmail(email);
        Player existingPlayer = Player.builder().id(1L).email("test@test.com").login("test").build();
        existingPlayer.setEmail("test@test.com");

        PlayerDto dto = getExamplePlayerDto(email);

        when(repo.findByEmail(email)).thenReturn(Optional.of(existingPlayer));
        when(playerMapper.toDto(existingPlayer)).thenReturn(dto);

        PlayerUpdateRequest request = new PlayerUpdateRequest("testtest", "AVATAR_DEFAULT");
        PlayerDto returnedDto = playerService.updatePlayer(request, auth);

        assertEquals(dto, returnedDto);
        assertEquals("testtest", existingPlayer.getLogin());
        assertEquals(AvatarImage.AVATAR_DEFAULT, existingPlayer.getAvatarImage());

        verify(repo).findByEmail(email);
        verify(playerMapper).toDto(existingPlayer);
        verify(repo).save(existingPlayer);
    }

    @Test
    void updatePlayerShouldThrowWhenUserDoesNotExist() {
        String email = "test@test.com";
        Authentication auth = mockAuthWithEmail(email);
        PlayerUpdateRequest request = new PlayerUpdateRequest("testtest", "AVATAR_DEFAULT");

        when(repo.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> playerService.updatePlayer(request,auth));
        verify(repo).findByEmail(email);
    }

    @Test
    void updatePlayerShouldThrowWhenLoginLengthIsInvalid() {
        String email = "test@test.com";
        Authentication auth = mockAuthWithEmail(email);
        Player existingPlayer = Player.builder().id(1L).email(email).login("test").build();

        PlayerUpdateRequest request = new PlayerUpdateRequest
                ("te", "AVATAR_DEFAULT"); // login length = 2 <3

        PlayerUpdateRequest request2 = new PlayerUpdateRequest
                ("testtesttesttestt", "AVATAR_DEFAULT"); // login length = 17 >16


        when(repo.findByEmail(email)).thenReturn(Optional.of(existingPlayer));


        assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(request, auth));
        assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(request2, auth));

        verify(repo, times(2)).findByEmail(email);
    }

    @Test
    void updatePlayerShouldThrowWhenLoginIsAlreadyTaken() {
        String email = "test@test.com";
        Authentication auth = mockAuthWithEmail(email);
        Player existingPlayer = Player.builder().id(1L).email(email).login("test").build();

        PlayerUpdateRequest request = new PlayerUpdateRequest
                ("test1", "AVATAR_DEFAULT"); // not the same as in existingPlayer

        when(repo.findByEmail(email)).thenReturn(Optional.of(existingPlayer));
        when(repo.existsByLogin(request.login())).thenReturn(true);

        assertThrows(UsernameAlreadyTakenException.class, () -> playerService.updatePlayer(request, auth));

        verify(repo).findByEmail(email);

    }

    @Test
    void updatePlayerShouldThrowWhenAvatarImageIsInvalid() {
        String email = "test@test.com";
        Authentication auth = mockAuthWithEmail(email);
        Player existingPlayer = Player.builder().id(1L).email(email).login("test").build();

        PlayerUpdateRequest request = new PlayerUpdateRequest
                ("test", "pararara ramapampampam"); // not the same as in existingPlayer

        when(repo.findByEmail(email)).thenReturn(Optional.of(existingPlayer));

        assertThrows(IllegalArgumentException.class, () -> playerService.updatePlayer(request, auth));

        verify(repo).findByEmail(email);

    }


}