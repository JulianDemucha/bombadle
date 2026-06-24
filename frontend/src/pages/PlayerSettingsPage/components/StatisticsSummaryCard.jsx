import React from 'react';
import { useNavigate } from 'react-router-dom';
import useBasicStatistics from '../hooks/useBasicStatistics.js';

function formatPercentile(fraction) {
    if (fraction === null || fraction === undefined) return '—';
    return `top ${Math.round(fraction * 100)}%`;
}

export default function StatisticsSummaryCard() {
    const { stats, loading } = useBasicStatistics();
    const navigate = useNavigate();

    return (
        <div className="container">
            <h2>Statystyki</h2>

            {loading ? (
                <p>Ładowanie...</p>
            ) : (
                <>
                    <div>
                        <span>Aktualny superstreak:</span>
                        <span> {stats?.currentSuperstreak ?? 0}</span>
                    </div>
                    <div>
                        <span>Zgadnięto:</span>
                        <span> {stats?.totalGuesses ?? 0} razy</span>
                    </div>
                    <div>
                        <span>Top 3:</span>
                        <span> {stats?.totalTop3Finishes ?? 0} razy</span>
                    </div>
                    <div>
                        <span>Średni percentyl:</span>
                        <span> {formatPercentile(stats?.averageLeaderboardPercentile)}</span>
                    </div>
                </>
            )}

            <button
                type="button"
                className="btn btn-secondary"
                style={{ marginTop: '1rem' }}
                onClick={() => navigate('/Statistics')}
            >
                Więcej statystyk
            </button>
        </div>
    );
}
