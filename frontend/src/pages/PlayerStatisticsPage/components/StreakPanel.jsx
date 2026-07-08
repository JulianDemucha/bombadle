import React from 'react';
import StreakFlame from '../../../components/StreakFlame.jsx';
import InfoTooltip from '../../../components/InfoTooltip.jsx';
import { STREAK_TOOLTIP, SUPERSTREAK_TOOLTIP } from '../../../components/streakInfo.js';

const StreakTile = ({ label, value, variant, tooltip }) => (
    <div className="streak-panel__tile">
        <StreakFlame value={value} variant={variant} size="lg" />
        <span className="streak-panel__label">
            {label}
            {tooltip && <InfoTooltip text={tooltip} />}
        </span>
    </div>
);

export default function StreakPanel({ detailed }) {
    return (
        <div className="streak-panel">
            <StreakTile label="Aktualna seria" value={detailed?.currentStreak} variant="default" tooltip={STREAK_TOOLTIP} />
            <StreakTile label="Najdłuższa seria" value={detailed?.longestStreak} variant="default" />
            <StreakTile label="Aktualna superseria" value={detailed?.currentSuperstreak} variant="super" tooltip={SUPERSTREAK_TOOLTIP} />
            <StreakTile label="Najdłuższa superseria" value={detailed?.longestSuperstreak} variant="super" />
        </div>
    );
}
