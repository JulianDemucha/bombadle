package com.bombadle.service.game;

import com.bombadle.config.CurrentCharacterCardWrapper;
import com.bombadle.dto.GuessAttempt;
import com.bombadle.entity.CharacterCard;
import com.bombadle.enums.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchUtilsTest {

    @Mock
    private CurrentCharacterCardWrapper currentCharacterCardWrapper;

    @InjectMocks
    private MatchUtils matchUtils;

    @Nested
    class GetFullMatchTests {

        @Test
        void getFullMatch_validGuess_returnsAttemptWithAllMatches() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            when(guess.getName()).thenReturn("Michał Głuś");
            when(guess.getGender()).thenReturn(mock(Gender.class));
            when(guess.getRace()).thenReturn(mock(Race.class));
            when(guess.getAlive()).thenReturn(true);
            when(guess.getColors()).thenReturn(Set.of());
            when(guess.getAffiliations()).thenReturn(Set.of());
            when(guess.getFirstAppearanceEpisode()).thenReturn(1);

            // Act
            GuessAttempt result = MatchUtils.getFullMatch(guess);

            // Assert
            assertEquals(MatchType.MATCH, result.name().match());
            assertEquals(MatchType.MATCH, result.gender().match());
            assertEquals(MatchType.MATCH, result.race().match());
            assertEquals(MatchType.MATCH, result.affiliations().match());
            assertEquals(MatchType.MATCH, result.colors().match());
            assertEquals(MatchType.MATCH, result.affiliations().match());
            assertEquals(MatchType.MATCH, result.firstAppearanceEpisode().match());
        }
    }

    @Nested
    class CompareCharacterCardClassicTests {

        @Test
        void compareCharacterCardClassic_validGuess_fetchesCurrentCardAndCompares() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(1L);
            when(currentCharacterCardWrapper.get()).thenReturn(target);

            // Act
            GuessAttempt result = matchUtils.compareCharacterCardClassic(guess);

            // Assert
            assertEquals(MatchType.MATCH, result.name().match());
        }
    }

    @Nested
    class CompareCharacterCardsTests {

        @Test
        void compareCharacterCards_idsAreEqual_returnsFullMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(1L);

            // Act
            GuessAttempt result = matchUtils.compareCharacterCards(guess, target);

            // Assert
            assertEquals(MatchType.MATCH, result.name().match());
            assertEquals(MatchType.MATCH, result.affiliations().match());
        }

        @Test
        void compareCharacterCards_idsAreDifferent_returnsCalculatedMatch() {
            // Arrange
            CharacterCard guess = mock(CharacterCard.class);
            CharacterCard target = mock(CharacterCard.class);
            when(guess.getId()).thenReturn(1L);
            when(target.getId()).thenReturn(2L);

            when(guess.getName()).thenReturn("Sułtan Kosmitów");

            when(guess.getRace()).thenReturn(Race.Czlowiek);
            when(target.getRace()).thenReturn(Race.Kurvinox);

            Gender guessGender = mock(Gender.class);
            when(guess.getGender()).thenReturn(guessGender);
            when(target.getGender()).thenReturn(guessGender);

            when(guess.getAlive()).thenReturn(true);
            when(target.getAlive()).thenReturn(false);

            when(guess.getFirstAppearanceEpisode()).thenReturn(5);
            when(target.getFirstAppearanceEpisode()).thenReturn(5);

            Affiliation affiliation1 = mock(Affiliation.class);
            Affiliation affiliation2 = mock(Affiliation.class);
            when(guess.getAffiliations()).thenReturn(Set.of(affiliation1));
            when(target.getAffiliations()).thenReturn(Set.of(affiliation2));

            // Act
            GuessAttempt result = matchUtils.compareCharacterCards(guess, target);

            // Assert
            assertEquals(MatchType.NOT_MATCH, result.name().match());
            assertEquals(MatchType.MATCH, result.gender().match());
            assertEquals(MatchType.NOT_MATCH, result.alive().match());
            assertEquals(MatchType.NOT_MATCH, result.affiliations().match());
            assertEquals(MatchType.MATCH, result.firstAppearanceEpisode().match());
        }
    }

    @Nested
    class CheckNameTests {

        @Test
        void checkName_namesAreEqual_returnsMatch() {
            // Arrange
            String name = "Michał Głuś";
            String targetName = "Michał Głuś";

            // Act
            MatchType result = matchUtils.checkName(name, targetName);

            // Assert
            assertEquals(MatchType.MATCH, result);
        }

        @Test
        void checkName_namesAreDifferent_returnsNotMatch() {
            // Arrange
            String name = "Kapitan Bomba";
            String targetName = "Chorąży Torpeda";

            // Act
            MatchType result = matchUtils.checkName(name, targetName);

            // Assert
            assertEquals(MatchType.NOT_MATCH, result);
        }
    }

    @Nested
    class CheckAliveTests {

        @Test
        void checkAlive_bothAlive_returnsMatch() {
            // Arrange
            boolean alive = true;
            boolean targetAlive = true;

            // Act
            MatchType result = matchUtils.checkAlive(alive, targetAlive);

            // Assert
            assertEquals(MatchType.MATCH, result);
        }

        @Test
        void checkAlive_statusDiffers_returnsNotMatch() {
            // Arrange
            boolean alive = true;
            boolean targetAlive = false;

            // Act
            MatchType result = matchUtils.checkAlive(alive, targetAlive);

            // Assert
            assertEquals(MatchType.NOT_MATCH, result);
        }
    }

    @Nested
    class CheckFirstAppearanceEpisodeTests {

        @Test
        void checkFirstAppearanceEpisode_episodesAreEqual_returnsMatch() {
            // Arrange
            Integer guessEpisode = 10;
            Integer targetEpisode = 10;

            // Act
            MatchType result = matchUtils.checkFirstAppearanceEpisode(guessEpisode, targetEpisode);

            // Assert
            assertEquals(MatchType.MATCH, result);
        }

        @Test
        void checkFirstAppearanceEpisode_guessIsHigher_returnsLower() {
            // Arrange
            Integer guessEpisode = 15;
            Integer targetEpisode = 10;

            // Act
            MatchType result = matchUtils.checkFirstAppearanceEpisode(guessEpisode, targetEpisode);

            // Assert
            assertEquals(MatchType.LOWER, result);
        }

        @Test
        void checkFirstAppearanceEpisode_guessIsLower_returnsHigher() {
            // Arrange
            Integer guessEpisode = 5;
            Integer targetEpisode = 10;

            // Act
            MatchType result = matchUtils.checkFirstAppearanceEpisode(guessEpisode, targetEpisode);

            // Assert
            assertEquals(MatchType.HIGHER, result);
        }
    }

    @Nested
    class CheckSetMatchTests {

        @Test
        void checkColors_setsAreIdentical_returnsMatch() {
            // Arrange
            Set<Color> guessColors = Set.of(mock(Color.class));
            Set<Color> targetColors = Set.copyOf(guessColors);

            // Act
            MatchType result = matchUtils.checkColors(guessColors, targetColors);

            // Assert
            assertEquals(MatchType.MATCH, result);
        }

        @Test
        void checkColors_setsAreDisjoint_returnsNotMatch() {
            // Arrange
            Set<Color> guessColors = Set.of(mock(Color.class));
            Set<Color> targetColors = Set.of(mock(Color.class));

            // Act
            MatchType result = matchUtils.checkColors(guessColors, targetColors);

            // Assert
            assertEquals(MatchType.NOT_MATCH, result);
        }

        @Test
        void checkColors_setsOverlap_returnsNotFullMatch() {
            // Arrange
            Color sharedColor = mock(Color.class);
            Color uniqueGuessColor = mock(Color.class);

            Set<Color> guessColors = Set.of(sharedColor, uniqueGuessColor);
            Set<Color> targetColors = Set.of(sharedColor);

            // Act
            MatchType result = matchUtils.checkColors(guessColors, targetColors);

            // Assert
            assertEquals(MatchType.NOT_FULL_MATCH, result);
        }
    }
}