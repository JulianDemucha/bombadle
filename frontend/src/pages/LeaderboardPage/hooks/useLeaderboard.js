import { useState, useEffect, useCallback } from 'react';
import { apiFetch } from '../../../api/api.js';

const useLeaderboard = () => {
    const [leaderboardData, setLeaderboardData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [currentPage, setCurrentPage] = useState(0);

    const fetchLeaderboard = useCallback(async (page) => {
        setLoading(true);
        setError(null);
        try {
            const response = await apiFetch(`/api/leaderboard?page=${page}`);
            setLeaderboardData(response.data);
        } catch (err) {
            setError(err.message || 'Wystąpił błąd podczas pobierania danych');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchLeaderboard(currentPage);
    }, [currentPage, fetchLeaderboard]);

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

export default useLeaderboard;
