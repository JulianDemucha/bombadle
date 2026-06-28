import { useEffect, useState } from 'react';
import { apiFetch } from '../../../api/api.js';

export default function useBasicStatistics() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let active = true;

        (async () => {
            const res = await apiFetch('/api/players/me/statistics/basic');
            if (!active) return;
            if (res.ok) setStats(res.data);
            setLoading(false);
        })();

        return () => {
            active = false;
        };
    }, []);

    return { stats, loading };
}
