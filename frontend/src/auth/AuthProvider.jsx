import React, { useEffect, useState } from "react";
import { AuthContext } from "./AuthContext";
import { apiFetch } from "../api.js";
import { useNavigate } from "react-router-dom";

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    async function loadUser() {
        setLoading(true);
        const res = await apiFetch("/api/players/me", { method: "GET" });
        if (res.status === 401) {
            setUser(null);
            setLoading(false);
            return;
        } else if (res.ok) setUser(res.data);
        else setUser(null);
        setLoading(false);
    }

    useEffect(() => {
        loadUser();
    }, []);

    async function logout() {
        await apiFetch("/api/auth/logout", { method: "POST" });
        setUser(null);

        navigate("/login");
    }

    return (
        <AuthContext.Provider value={{ user, loading, reload: loadUser, logout }}>
            {children}
        </AuthContext.Provider>
    );
}
