import { useState, useEffect, useCallback, useRef } from 'react';
import { apiFetch } from '../../../api/api.js';

const VALID_VARIANTS = ['streak', 'superstreak'];

/**
 * Smart hook for the player-level streak / superstreak paged leaderboards. Mirrors the pagination
 * logic of useLeaderboard (page state + request-id guard against out-of-order responses) but hits
 * the non-mode-keyed endpoints /api/leaderboard/{streak|superstreak} and has no today-solvers call.
 */
const useStreakLeaderboard = (variant = 'streak') => {
    const normalizedVariant = VALID_VARIANTS.includes(variant) ? variant : null;

    const [leaderboardData, setLeaderboardData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);
    const [trackedVariant, setTrackedVariant] = useState(normalizedVariant);

    if (normalizedVariant !== trackedVariant) {
        setTrackedVariant(normalizedVariant);
        setCurrentPage(0);
        setLeaderboardData(null);
        setError(null);
    }

    const requestIdRef = useRef(0);

    const fetchLeaderboard = useCallback(async (target, page) => {
        if (!target) {
            setError('Nieznany ranking.');
            setLeaderboardData(null);
            return;
        }

        const requestId = ++requestIdRef.current;
        setLoading(true);
        setError(null);

        const response = await apiFetch(`/api/leaderboard/${target}?page=${page}`);

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
        fetchLeaderboard(normalizedVariant, currentPage);
    }, [normalizedVariant, currentPage, fetchLeaderboard]);

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
        loading,
        error,
        currentPage,
        goToNextPage,
        goToPreviousPage,
        goToFirstPage,
        goToLastPage,
    };
};

export default useStreakLeaderboard;
