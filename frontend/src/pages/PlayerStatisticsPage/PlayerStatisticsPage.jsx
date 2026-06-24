import React from 'react';
import { useNavigate } from 'react-router-dom';
import './PlayerStatisticsPage.css';
import Header from '../../components/Header.jsx';
import Footer from '../../components/Footer.jsx';
import usePlayerStatistics from './hooks/usePlayerStatistics.js';
import StreakPanel from './components/StreakPanel.jsx';
import GuessesByModePanel from './components/GuessesByModePanel.jsx';
import StatViewSelector from './components/StatViewSelector.jsx';
import StatisticsChart from './components/StatisticsChart.jsx';

export default function PlayerStatisticsPage() {
    const {
        loading,
        error,
        detailed,
        chartData,
        selectedMode,
        setSelectedMode,
        selectedMetric,
        setSelectedMetric,
        metricLabel,
        modeLabel,
        availableModes,
        availableMetrics,
    } = usePlayerStatistics();

    const navigate = useNavigate();

    return (
        <div className="player-statistics-page">
            <Header />
            <div className="player-statistics-container">
                <button onClick={() => navigate(-1)} className="back-button">
                    Powrót
                </button>
                <h1>Twoje statystyki</h1>

                {loading && <p className="player-statistics-message">Ładowanie statystyk...</p>}
                {error && <p className="player-statistics-message">{error}</p>}

                {!loading && !error && (
                    <>
                        <StreakPanel detailed={detailed} />
                        <GuessesByModePanel detailed={detailed} />

                        <StatViewSelector
                            availableModes={availableModes}
                            selectedMode={selectedMode}
                            onModeChange={setSelectedMode}
                            availableMetrics={availableMetrics}
                            selectedMetric={selectedMetric}
                            onMetricChange={setSelectedMetric}
                        />

                        <StatisticsChart data={chartData} metricLabel={metricLabel} modeLabel={modeLabel} />
                    </>
                )}
            </div>
            <Footer />
        </div>
    );
}
