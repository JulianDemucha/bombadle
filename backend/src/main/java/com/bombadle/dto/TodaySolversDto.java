package com.bombadle.dto;

/**
 * Per-mode "solved today" counts for a leaderboard view.
 *
 * @param loggedIn  number of logged-in players who solved the mode today (= leaderboard participants)
 * @param anonymous number of anonymous sessions that solved the mode today
 */
public record TodaySolversDto(long loggedIn, long anonymous) {
}
