import { useEffect, useState } from 'react';
import { apiFetch } from '../../api/api.js';

// One request per page load, shared by the login and register pages.
let providersRequest = null;

const loadProviders = () => {
    if (!providersRequest) {
        providersRequest = apiFetch('/api/auth/providers').then((res) => {
            if (!res.ok) providersRequest = null;
            return res.ok ? res.data : null;
        });
    }
    return providersRequest;
};

// Google stays visible until the server says it is disabled, so production has no layout shift.
export default function useAuthProviders() {
    const [providers, setProviders] = useState({ google: true });

    useEffect(() => {
        let active = true;
        loadProviders().then((data) => {
            if (active && data) setProviders({ google: data.google !== false });
        });
        return () => {
            active = false;
        };
    }, []);

    return providers;
}
