import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../../auth/UseAuth.jsx';
import { clearAnonymousWonFlags, hasAnyAnonymousWonFlag } from '../../api/anonymousProgress.js';

// Generous max-age so the cookie survives the OAuth round-trip to Google and back.
const MERGE_COOKIE_MAX_AGE_SECONDS = 300;

function armMerge() {
    document.cookie = `TRIGGER_MERGE=true; path=/; max-age=${MERGE_COOKIE_MAX_AGE_SECONDS}; SameSite=Lax; Secure`;
}

/**
 * Smart hook for the anonymous-session merge prompt. Intercepts a login/register/OAuth attempt:
 * if the visitor has won at least one mode while anonymous (and has not declined this session),
 * it opens a confirmation modal before letting the attempt proceed.
 *
 * Usage in a page:
 *   const merge = useAnonymousMergePrompt();
 *   const onSubmit = () => merge.requestAuth((withMerge) => doLogin());
 *   <MergePrompt isOpen={merge.isOpen} onConfirm={merge.confirm} onDecline={merge.decline} />
 */
export default function useAnonymousMergePrompt() {
    const { user } = useAuth();
    const [isOpen, setIsOpen] = useState(false);

    // Ephemeral "declined" state: reset naturally on page reload, never persisted.
    const declinedRef = useRef(false);
    const proceedRef = useRef(null);

    // Once authenticated, the anonymous-win flags are meaningless.
    useEffect(() => {
        if (user) clearAnonymousWonFlags();
    }, [user]);

    const requestAuth = useCallback((proceed) => {
        if (!hasAnyAnonymousWonFlag() || declinedRef.current) {
            proceed(false);
            return;
        }
        proceedRef.current = proceed;
        setIsOpen(true);
    }, []);

    const runPending = useCallback((withMerge) => {
        const proceed = proceedRef.current;
        proceedRef.current = null;
        setIsOpen(false);
        if (proceed) proceed(withMerge);
    }, []);

    const confirm = useCallback(() => {
        armMerge();
        runPending(true);
    }, [runPending]);

    const decline = useCallback(() => {
        // Don't ask again until the page is reloaded; "Yes" deliberately does not set this,
        // so a failed login attempt re-prompts on the next try.
        declinedRef.current = true;
        runPending(false);
    }, [runPending]);

    return {
        isOpen,
        requestAuth,
        confirm,
        decline,
        clearWonFlags: clearAnonymousWonFlags,
    };
}
