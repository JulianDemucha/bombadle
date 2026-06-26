import { apiFetch } from './api.js';

const WON_FLAG_PREFIX = 'anonymousModeWon_';
const WIN_TIME_PREFIX = 'anonymousWinTime_';

export function anonymousWonFlagKey(mode) {
    return `${WON_FLAG_PREFIX}${mode}`;
}

export function anonymousWinTimeKey(mode) {
    return `${WIN_TIME_PREFIX}${mode}`;
}

/**
 * Reads the anonymous session and persists, per game mode, the fact that it was won while
 * anonymous. Source of truth is AnonymousSessionDto.completedModesToday — the only reliable
 * signal, since GuessAttempt.isCorrect() is not serialized.
 *
 * Side effect: hitting /api/players/anonymous/me also ensures the ANON_SESSION_ID cookie
 * exists, which the backend later needs to perform the merge.
 */
export async function syncAnonymousWonModes() {
    const res = await apiFetch('/api/players/anonymous/me');
    if (!res.ok || !res.data) return;

    const completedModes = res.data.completedModesToday ?? [];
    completedModes.forEach((mode) => {
        localStorage.setItem(anonymousWonFlagKey(mode), '1');
    });
}

/** Whether at least one "won as anonymous" flag is currently stored. */
export function hasAnyAnonymousWonFlag() {
    for (let i = 0; i < localStorage.length; i += 1) {
        if (localStorage.key(i)?.startsWith(WON_FLAG_PREFIX)) return true;
    }
    return false;
}

/**
 * Removes every trace of anonymous-session progress from localStorage: both the "won as
 * anonymous" flags (anonymousModeWon_*) and the cached win timestamps (anonymousWinTime_*).
 * Called once the player is authenticated — at that point all of this is meaningless and the
 * backend merge (if any) has already run.
 */
export function clearAnonymousProgress() {
    const keysToRemove = [];
    for (let i = 0; i < localStorage.length; i += 1) {
        const key = localStorage.key(i);
        if (key?.startsWith(WON_FLAG_PREFIX) || key?.startsWith(WIN_TIME_PREFIX)) {
            keysToRemove.push(key);
        }
    }
    keysToRemove.forEach((key) => localStorage.removeItem(key));
}
