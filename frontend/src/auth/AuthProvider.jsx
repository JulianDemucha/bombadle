import React, {useCallback, useEffect, useRef, useState} from "react";
import {AuthContext} from "./AuthContext";
import {apiFetch} from "../api/api.js";
import {useNavigate} from "react-router-dom";
import axios, {setupSilentRefresh} from "../api/axios.js";
import {clearAnonymousProgress} from "../api/anonymousProgress.js";

export function AuthProvider({children}) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const latestLoadUserRequestRef = useRef(0);

    const loadUser = useCallback(async () => {
        const requestId = latestLoadUserRequestRef.current + 1;
        latestLoadUserRequestRef.current = requestId;
        setLoading(true);

        const res = await apiFetch('/api/players/me');
        if (latestLoadUserRequestRef.current !== requestId) return;

        setUser(res.ok ? res.data ?? null : null);
        if (res.ok && res.data) {
            // Authenticated via any path (login, register, OAuth round-trip): the anonymous
            // win flags / win timestamps are now meaningless. This is the single reliable
            // place to clear them — the merge-prompt hook is unmounted after an OAuth redirect.
            clearAnonymousProgress();
            setupSilentRefresh();
        }
        setLoading(false);
    }, []);

    useEffect(() => {
        loadUser();
    }, [loadUser]);

    async function logout() {
        const xsrfToken = document.cookie
            .split('; ')
            .find(row => row.startsWith('XSRF-TOKEN'))
            ?.split('=')[1];

        axios.defaults.headers.common['X-XSRF-TOKEN'] = xsrfToken;

        await apiFetch("/api/auth/logout", {method: "POST"});
        setUser(null);
        navigate("/login");
    }

    return (
        <AuthContext.Provider value={{user, loading, reload: loadUser, logout}}>
            {children}
        </AuthContext.Provider>
    );
}
