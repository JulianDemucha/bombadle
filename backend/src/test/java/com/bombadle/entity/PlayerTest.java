package com.bombadle.entity;

import com.bombadle.enums.GameMode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private static Score score(GameMode gameMode) {
        return Score.builder()
                .gameMode(gameMode)
                .scoreTimestamp(Instant.now())
                .numberOfTries(3)
                .build();
    }

    @Nested
    class AddTodayScoreTests {

        /**
         * Regression test for the JSONB dirty-checking bug.
         *
         * completedModesToday is @JdbcTypeCode(SqlTypes.JSON). Hibernate snapshots JSON fields
         * via ImmutableMutabilityPlan, which stores the same object reference (no deep copy).
         * Calling Set.add() on the existing instance mutates both the snapshot and the current
         * value simultaneously — the dirty checker sees no change and never issues the UPDATE.
         *
         * The fix in markModeAsCompleted() replaces the field with a new Set instance so the
         * snapshot (old object) and the current value (new object) differ, triggering the UPDATE.
         */
        @Test
        void addTodayScore_replacesCompletedModesTodayReference() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>())
                    .build();
            Set<GameMode> snapshotRef = player.getCompletedModesToday();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertNotSame(snapshotRef, player.getCompletedModesToday(),
                    "addTodayScore must assign a new Set instance; in-place mutation is " +
                    "invisible to Hibernate's ImmutableMutabilityPlan dirty checker");
        }

        @Test
        void addTodayScore_addsGameModeToCompletedModesToday() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>())
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertTrue(player.hasGuessedToday(GameMode.CLASSIC));
            assertFalse(player.hasGuessedToday(GameMode.IMAGES));
        }

        @Test
        void addTodayScore_preservesExistingCompletedModes() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(Set.of(GameMode.IMAGES)))
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertTrue(player.hasGuessedToday(GameMode.CLASSIC));
            assertTrue(player.hasGuessedToday(GameMode.IMAGES));
        }

        @Test
        void addTodayScore_gameModeMismatch_throwsIllegalArgumentException() {
            Player player = Player.builder().build();
            Score mismatchedScore = score(GameMode.IMAGES);

            assertThrows(IllegalArgumentException.class,
                    () -> player.addTodayScore(GameMode.CLASSIC, mismatchedScore));
        }

        @Test
        void addTodayScore_incrementsTotalSuccessfulGuesses() {
            Player player = Player.builder().totalSuccessfulGuesses(5).build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(6, player.getTotalSuccessfulGuesses());
        }

        // --- real-time streak ---

        @Test
        void addTodayScore_firstSolveToday_incrementsCurrentStreak() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>())
                    .currentStreak(3).longestStreak(5)
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(4, player.getCurrentStreak());
            assertEquals(5, player.getLongestStreak()); // 4 < 5, unchanged
        }

        @Test
        void addTodayScore_firstSolveExceedsLongestStreak_updatesLongestStreak() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>())
                    .currentStreak(5).longestStreak(5)
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(6, player.getCurrentStreak());
            assertEquals(6, player.getLongestStreak());
        }

        @Test
        void addTodayScore_subsequentSolveToday_doesNotChangeStreak() {
            // Streak already counted when the first mode was solved; further solves that day
            // must not increment it again.
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(Set.of(GameMode.IMAGES)))
                    .currentStreak(4).longestStreak(5)
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(4, player.getCurrentStreak());
            assertEquals(5, player.getLongestStreak());
        }

        // --- real-time superstreak ---

        @Test
        void addTodayScore_completingLastMode_incrementsSuperstreak() {
            Set<GameMode> allExceptOne = new HashSet<>(EnumSet.allOf(GameMode.class));
            allExceptOne.remove(GameMode.QUOTES_STAGE_2);

            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(allExceptOne))
                    .currentSuperstreak(1).longestSuperstreak(3)
                    .build();

            player.addTodayScore(GameMode.QUOTES_STAGE_2, score(GameMode.QUOTES_STAGE_2));

            assertEquals(2, player.getCurrentSuperstreak());
            assertEquals(3, player.getLongestSuperstreak()); // 2 < 3, unchanged
        }

        @Test
        void addTodayScore_completingLastModeExceedsLongestSuperstreak_updatesLongest() {
            Set<GameMode> allExceptOne = new HashSet<>(EnumSet.allOf(GameMode.class));
            allExceptOne.remove(GameMode.CLASSIC);

            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(allExceptOne))
                    .currentSuperstreak(3).longestSuperstreak(3)
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(4, player.getCurrentSuperstreak());
            assertEquals(4, player.getLongestSuperstreak());
        }

        @Test
        void addTodayScore_notAllModesYetCompleted_doesNotChangeSuperstreak() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>())
                    .currentSuperstreak(2).longestSuperstreak(4)
                    .build();

            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(2, player.getCurrentSuperstreak());
            assertEquals(4, player.getLongestSuperstreak());
        }

        @Test
        void addTodayScore_allModesAlreadyDone_doesNotIncrementSuperstreaKAgain() {
            // Guard against double-increment if called twice with all modes already present.
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(EnumSet.allOf(GameMode.class)))
                    .currentSuperstreak(2).longestSuperstreak(4)
                    .build();

            // Simulate an unexpected duplicate call (e.g., CLASSIC already in set)
            player.addTodayScore(GameMode.CLASSIC, score(GameMode.CLASSIC));

            assertEquals(2, player.getCurrentSuperstreak()); // must not double-count
        }
    }

    @Nested
    class ResetDailyProgressTests {

        @Test
        void resetDailyProgress_replacesCompletedModesTodayReference() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(Set.of(GameMode.CLASSIC)))
                    .build();
            Set<GameMode> snapshotRef = player.getCompletedModesToday();

            player.resetDailyProgress();

            assertNotSame(snapshotRef, player.getCompletedModesToday(),
                    "resetDailyProgress must create a new Set reference; Set.clear() would be " +
                    "invisible to Hibernate's ImmutableMutabilityPlan dirty checker");
        }

        @Test
        void resetDailyProgress_clearsCompletedModesToday() {
            Player player = Player.builder()
                    .completedModesToday(new HashSet<>(Set.of(GameMode.CLASSIC, GameMode.IMAGES)))
                    .build();

            player.resetDailyProgress();

            assertTrue(player.getCompletedModesToday().isEmpty());
        }
    }

    @Nested
    class ResetStreaksIfThresholdsNotMetTests {

        /**
         * resetStreaksIfThresholdsNotMet() is called by evaluateDailyStreaks() at the 07:00 reset.
         * Its sole responsibility is to zero out counters for players who did not meet the daily
         * threshold. Increments happen in real-time inside addTodayScore().
         */

        @Test
        void resetStreaksIfThresholdsNotMet_completedAnyMode_streakIsLeftUntouched() {
            // Streak was already incremented in real-time; the reset must not touch it.
            Player player = Player.builder().currentStreak(4).longestStreak(5).build();

            player.resetStreaksIfThresholdsNotMet(true, false);

            assertEquals(4, player.getCurrentStreak());
            assertEquals(5, player.getLongestStreak());
        }

        @Test
        void resetStreaksIfThresholdsNotMet_completedNoMode_resetsCurrentStreakPreservingLongest() {
            Player player = Player.builder().currentStreak(7).longestStreak(7).build();

            player.resetStreaksIfThresholdsNotMet(false, false);

            assertEquals(0, player.getCurrentStreak());
            assertEquals(7, player.getLongestStreak());
        }

        @Test
        void resetStreaksIfThresholdsNotMet_completedAllModes_superstreakIsLeftUntouched() {
            Player player = Player.builder().currentSuperstreak(2).longestSuperstreak(3).build();

            player.resetStreaksIfThresholdsNotMet(true, true);

            assertEquals(2, player.getCurrentSuperstreak());
            assertEquals(3, player.getLongestSuperstreak());
        }

        @Test
        void resetStreaksIfThresholdsNotMet_completedSomeButNotAllModes_resetsSuperstreakOnly() {
            Player player = Player.builder()
                    .currentStreak(3).longestStreak(5)
                    .currentSuperstreak(3).longestSuperstreak(7)
                    .build();

            player.resetStreaksIfThresholdsNotMet(true, false);

            assertEquals(3, player.getCurrentStreak());   // unchanged
            assertEquals(0, player.getCurrentSuperstreak()); // reset: not all modes
            assertEquals(7, player.getLongestSuperstreak()); // preserved
        }

        @Test
        void resetStreaksIfThresholdsNotMet_completedNoMode_resetsBoth() {
            Player player = Player.builder()
                    .currentStreak(5).longestStreak(8)
                    .currentSuperstreak(2).longestSuperstreak(4)
                    .build();

            player.resetStreaksIfThresholdsNotMet(false, false);

            assertEquals(0, player.getCurrentStreak());
            assertEquals(0, player.getCurrentSuperstreak());
            assertEquals(8, player.getLongestStreak());
            assertEquals(4, player.getLongestSuperstreak());
        }
    }
}
