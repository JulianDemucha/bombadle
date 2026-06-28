import { useEffect, useState } from 'react';
import { apiFetch } from '../../../api/api.js';

const SUPERSTREAK_TOP3_ENDPOINT = '/api/leaderboard/superstreak/top3';

/**
 * Smart hook for the main-page top-3 superstreak widget. Fetches the player-level superstreak
 * ranking via apiFetch (the only allowed backend call path) and exposes a flat { topThree, loading }
 * object for the dumb page/component to render.
 */
const useSuperstreakTop3 = () => {
    const [topThree, setTopThree] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let active = true;

        const load = async () => {
            setLoading(true);
            const response = await apiFetch(SUPERSTREAK_TOP3_ENDPOINT);
            if (!active) return;

            setTopThree(response.ok && Array.isArray(response.data) ? response.data : []);
            setLoading(false);
        };

        load();
        return () => {
            active = false;
        };
    }, []);

    return { topThree, loading };
};

export default useSuperstreakTop3;
