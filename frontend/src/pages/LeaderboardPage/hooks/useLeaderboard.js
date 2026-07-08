import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch } from '../../../api/api.js';

const VALID_MODES = ['classic', 'quotes', 'images'];

const useLeaderboard = (mode = 'classic') => {
    const normalizedMode = VALID_MODES.includes(mode) ? mode : null;

    const [leaderboardData, setLeaderboardData] = useState(null);
    const [todaySolvers, setTodaySolvers] = useState({ loggedIn: 0, anonymous: 0 });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [trackedMode, setTrackedMode] = useState(normalizedMode);

    if (normalizedMode !== trackedMode) {
        setTrackedMode(normalizedMode);
        setCurrentPage(0);
        setLeaderboardData(null);
        setTodaySolvers({ loggedIn: 0, anonymous: 0 });
        setError(null);
    }

    const requestIdRef = useRef(0);

    const fetchLeaderboard = useCallback(async (targetMode, page) => {
        if (!targetMode) {
            setError('Nieznany tryb rankingu.');
            setLeaderboardData(null);
            return;
        }

        const requestId = ++requestIdRef.current;
        setLoading(true);
        setError(null);

        const response = await apiFetch(`/api/leaderboard/${targetMode}?page=${page}`);

        if (requestId !== requestIdRef.current) {
            return;
        }

        if (response.ok) {
            setLeaderboardData(response.data);
        } else {
            setError(response.data?.message || 'Wystąpił błąd podczas pobierania danych');
            setLeaderboardData(null);
        }

        setLoading(false);
    }, []);

    useEffect(() => {
        fetchLeaderboard(normalizedMode, currentPage);
    }, [normalizedMode, currentPage, fetchLeaderboard]);

    const fetchTodaySolvers = useCallback(async (targetMode) => {
        if (!targetMode) {
            setTodaySolvers({ loggedIn: 0, anonymous: 0 });
            return;
        }

        const response = await apiFetch(`/api/leaderboard/${targetMode}/today-solvers`);

        if (response.ok && response.data) {
            const { loggedIn = 0, anonymous = 0 } = response.data;
            setTodaySolvers({ loggedIn, anonymous });
        }
    }, []);

    useEffect(() => {
        fetchTodaySolvers(normalizedMode);
    }, [normalizedMode, fetchTodaySolvers]);

    const goToNextPage = () => {
        if (leaderboardData && !leaderboardData.last) {
            setCurrentPage((prev) => prev + 1);
        }
    };

    const goToPreviousPage = () => {
        if (currentPage > 0) {
            setCurrentPage((prev) => prev - 1);
        }
    };

    const goToFirstPage = () => {
        if (currentPage > 0) {
            setCurrentPage(0);
        }
    };

    const goToLastPage = () => {
        if (leaderboardData && !leaderboardData.last) {
            setCurrentPage(leaderboardData.totalPages - 1);
        }
    };

    return {
        leaderboardData,
        loggedInSolvers: todaySolvers.loggedIn,
        anonymousSolvers: todaySolvers.anonymous,
        loading,
        error,
        currentPage,
        goToNextPage,
        goToPreviousPage,
        goToFirstPage,
        goToLastPage,
    };
};

export default useLeaderboard;