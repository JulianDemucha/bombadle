package com.bombadle.integration;

import com.bombadle.config.CurrentGameStateWrapper;
import com.bombadle.dto.request.RegisterRequest;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.Player;
import com.bombadle.entity.Score;
import com.bombadle.enums.Affiliation;
import com.bombadle.enums.Color;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.Gender;
import com.bombadle.enums.Race;
import com.bombadle.repository.AnonymousSessionRepository;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.repository.PlayerRepository;
import com.bombadle.repository.ScoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnonymousToRegisteredUserMergeIT extends BaseIT{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CharacterCardRepository characterCardRepository;

    @Autowired
    private CurrentGameStateWrapper currentCharacterCardWrapper;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private AnonymousSessionRepository anonymousSessionRepository;

    private CharacterCard wrongCard;
    private CharacterCard correctCard;


    @BeforeEach
    void setUp() {
        wrongCard = CharacterCard.builder()
                .name("Michał Głuś")
                .gender(Gender.MALE)
                .race(Race.Kurvinox)
                .alive(true)
                .firstAppearanceEpisode(56)
                .affiliations(Set.of(Affiliation.Kosmici, Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.NIEBIESKI))
                .build();
        correctCard = CharacterCard.builder()
                .name("Kapitan Bomba")
                .gender(Gender.MALE)
                .race(Race.Czlowiek)
                .alive(true)
                .firstAppearanceEpisode(1)
                .affiliations(Set.of(Affiliation.Gwiezdna_Flota))
                .colors(Set.of(Color.BIALY))
                .build();

        characterCardRepository.saveAll(List.of(wrongCard, correctCard));
        currentCharacterCardWrapper.set(GameMode.CLASSIC, correctCard);
    }

    @Test
    void anonymousSession_guestRegistersAfterGuessing_mergesDataSuccessfully() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("SigmaUserTest")
                .email("integration@test.com")
                .password("SecurePassword123!")
                .build();

        Cookie triggerMergeCookie = new Cookie("TRIGGER_MERGE", "true");

        String csrfToken = "sigma-csrf-token-123";
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", csrfToken);

        MvcResult firstGuessResult = mockMvc.perform(post("/api/card-guessing/classic/guess/" + wrongCard.getId())
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        Cookie anonSessionCookie = firstGuessResult.getResponse().getCookie("ANON_SESSION_ID");
        assertNotNull(anonSessionCookie);
        String sessionId = anonSessionCookie.getValue();

        mockMvc.perform(post("/api/card-guessing/classic/guess/" + correctCard.getId())
                        .cookie(anonSessionCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .cookie(anonSessionCookie, triggerMergeCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("ANON_SESSION_ID", 0))
                .andExpect(cookie().maxAge("TRIGGER_MERGE", 0));

        Player savedPlayer = playerRepository.findByEmail("integration@test.com").orElse(null);
        assertNotNull(savedPlayer);
        assertEquals("sigmausertest", savedPlayer.getLogin());
        assertEquals("SigmaUserTest", savedPlayer.getDisplayName());
        assertEquals("integration@test.com", savedPlayer.getEmail());

        List<Score> playerScores = scoreRepository.findAll();
        assertEquals(1, playerScores.size());

        Score mergedScore = playerScores.getFirst();
        assertEquals(savedPlayer.getId(), mergedScore.getPlayer().getId());
        assertEquals(2, mergedScore.getNumberOfTries());
        assertEquals(GameMode.CLASSIC, mergedScore.getGameMode());
        assertNotNull(mergedScore.getScoreTimestamp());

        boolean sessionExists = anonymousSessionRepository.existsById(UUID.fromString(sessionId));
        assertFalse(sessionExists);
    }
}