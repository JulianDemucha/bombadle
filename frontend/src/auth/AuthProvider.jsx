import React, {useCallback, useEffect, useRef, useState} from "react";
import {AuthContext} from "./AuthContext";
import {apiFetch} from "../api/api.js";
import {useNavigate} from "react-router-dom";
import axios, {setupSilentRefresh} from "../api/axios.js";

export function AuthProvider({children}) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();
    const latestLoadUserRequestRef = useRef(0);

    const loadUser = useCallback(async () => {
        const requestId = latestLoadUserRequestRef.current + 1;
        latestLoadUserRequestRef.current = requestId;
        setLoading(true);

        try {
            const res = await axios.get('/api/players/me');
            if (latestLoadUserRequestRef.current !== requestId) return;
            setUser(res.data ?? null);
            if(res.data) setupSilentRefresh();
        } catch (err) {
            if (latestLoadUserRequestRef.current !== requestId) return;
            setUser(null);
        } finally {
            if (latestLoadUserRequestRef.current === requestId) {
                setLoading(false);
            }
        }
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
        
        try {
            await apiFetch("/api/auth/logout", {method: "POST"});
        } catch {
            // Ignore logout errors
        } finally {
            setUser(null);
            navigate("/login");
        }
    }

    return (
        <AuthContext.Provider value={{user, loading, reload: loadUser, logout}}>
            {children}
        </AuthContext.Provider>
    );
}
