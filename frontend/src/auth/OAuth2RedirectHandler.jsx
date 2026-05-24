import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './UseAuth';
import { setupSilentRefresh } from '../api/axios';

function OAuth2RedirectHandler() {
    const { reload } = useAuth();
    const navigate = useNavigate();
    const processed = useRef(false);

    useEffect(() => {
        if (processed.current) return;
        processed.current = true;

        const handleLogin = async () => {
            try {
                await reload();
                setupSilentRefresh();
                navigate('/');
            } catch (error) {
                console.error("OAuth2 success handler error:", error);
                navigate('/login?error=oauth_failed');
            }
        };

        handleLogin();
    }, [reload, navigate]);

    return (
        <div style={{color: 'white', display:'flex', justifyContent:'center', marginTop:'50px', fontFamily: 'PixelifySans, sans-serif'}}>
            Logowanie...
        </div>
    );
}

export default OAuth2RedirectHandler;

