import React from 'react';

const StreakTile = ({ label, value }) => (
    <div className="streak-panel__tile">
        <span className="streak-panel__value">{value ?? 0}</span>
        <span className="streak-panel__label">{label}</span>
    </div>
);

export default function StreakPanel({ detailed }) {
    return (
        <div className="streak-panel">
            <StreakTile label="Aktualny streak" value={detailed?.currentStreak} />
            <StreakTile label="Najdłuższy streak" value={detailed?.longestStreak} />
            <StreakTile label="Aktualny superstreak" value={detailed?.currentSuperstreak} />
            <StreakTile label="Najdłuższy superstreak" value={detailed?.longestSuperstreak} />
        </div>
    );
}
