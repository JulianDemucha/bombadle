import { apiFetch } from './api.js';

const FLAG_PREFIX = 'anonymousModeWon_';

export function anonymousWonFlagKey(mode) {
    return `${FLAG_PREFIX}${mode}`;
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
        if (localStorage.key(i)?.startsWith(FLAG_PREFIX)) return true;
    }
    return false;
}

/** Removes every "won as anonymous" flag. Called once the player is authenticated. */
export function clearAnonymousWonFlags() {
    const keysToRemove = [];
    for (let i = 0; i < localStorage.length; i += 1) {
        const key = localStorage.key(i);
        if (key?.startsWith(FLAG_PREFIX)) keysToRemove.push(key);
    }
    keysToRemove.forEach((key) => localStorage.removeItem(key));
}
