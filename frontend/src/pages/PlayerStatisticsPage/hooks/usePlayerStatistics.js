import { useEffect, useMemo, useState } from 'react';
import { apiFetch } from '../../../api/api.js';

export const MODE_LABELS = {
    CLASSIC: 'Klasyczny',
    QUOTES_STAGE_2: 'Cytaty',
    IMAGES: 'Zdjęcia',
};

// Ranked, user-facing modes (QUOTES_STAGE_1 is never recorded; STAGE_2 represents "Quotes").
const CHART_MODES = ['CLASSIC', 'QUOTES_STAGE_2', 'IMAGES'];

const METRICS = {
    tries: { label: 'Liczba prób' },
    position: { label: 'Pozycja w rankingu' },
    percentile: { label: 'Percentyl (%)' },
};

function metricValue(row, metric) {
    switch (metric) {
        case 'position':
            return row.leaderboardPosition;
        case 'percentile':
            return row.percentile == null ? null : Math.round(row.percentile * 100);
        case 'tries':
        default:
            return row.numberOfTries;
    }
}

export default function usePlayerStatistics() {
    const [detailed, setDetailed] = useState(null);
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [selectedMode, setSelectedMode] = useState('CLASSIC');
    const [selectedMetric, setSelectedMetric] = useState('tries');

    useEffect(() => {
        document.body.classList.add('scrollable-page');
        return () => document.body.classList.remove('scrollable-page');
    }, []);

    useEffect(() => {
        let active = true;

        (async () => {
            setLoading(true);
            setError(null);

            const [detailedRes, chartRes] = await Promise.all([
                apiFetch('/api/players/me/statistics/detailed'),
                apiFetch('/api/players/me/statistics/chart'),
            ]);

            if (!active) return;

            if (detailedRes.ok && chartRes.ok) {
                setDetailed(detailedRes.data);
                setHistory(chartRes.data ?? []);
            } else {
                setError('Nie udało się pobrać statystyk.');
            }

            setLoading(false);
        })();

        return () => {
            active = false;
        };
    }, []);

    const chartData = useMemo(
        () =>
            history
                .filter((row) => row.gameMode === selectedMode)
                .map((row) => ({
                    puzzleDate: row.puzzleDate,
                    value: metricValue(row, selectedMetric),
                })),
        [history, selectedMode, selectedMetric],
    );

    return {
        loading,
        error,
        detailed,
        hasHistory: history.length > 0,
        chartData,
        selectedMode,
        setSelectedMode,
        selectedMetric,
        setSelectedMetric,
        metricLabel: METRICS[selectedMetric].label,
        modeLabel: MODE_LABELS[selectedMode],
        availableModes: CHART_MODES.map((value) => ({ value, label: MODE_LABELS[value] })),
        availableMetrics: Object.entries(METRICS).map(([value, { label }]) => ({ value, label })),
    };
}
