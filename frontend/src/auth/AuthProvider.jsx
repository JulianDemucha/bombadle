import React, {useEffect, useState} from "react";
import {AuthContext} from "./AuthContext";
import {apiFetch} from "../api/api.js";
import {useNavigate} from "react-router-dom";
import axios from "../api/axios.js";

export function AuthProvider({children}) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    async function loadUser() {
        setLoading(true);
        try {
            const res = await axios.get('/api/players/me');
            setUser(res.data);
        } catch (err) {
            if (err.response?.status === 401) {
                setUser(null);
            } else {
                console.error('Błąd podczas ładowania użytkownika:', err);
                setUser(null);
            }

        }finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        loadUser();
    }, []);

    async function logout() {
        const xsrfToken = document.cookie
            .split('; ')
            .find(row => row.startsWith('XSRF-TOKEN'))
            ?.split('=')[1];

        axios.defaults.headers.common['X-XSRF-TOKEN'] = xsrfToken;
        
        try {
            await apiFetch("/api/auth/logout", {method: "POST"});
        } catch (error) {
            console.warn("Logout request failed, proceeding to clear local state", error);
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
