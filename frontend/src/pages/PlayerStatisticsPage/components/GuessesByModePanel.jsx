import React from 'react';
import { MODE_LABELS } from '../hooks/usePlayerStatistics.js';

export default function GuessesByModePanel({ detailed }) {
    const guessesByMode = detailed?.guessesByMode ?? {};

    return (
        <div className="guesses-by-mode">
            <div className="guesses-by-mode__row guesses-by-mode__row--total">
                <span>Łącznie zgadnięć</span>
                <span>{detailed?.totalGuesses ?? 0}</span>
            </div>
            {Object.keys(MODE_LABELS).map((mode) => (
                <div className="guesses-by-mode__row" key={mode}>
                    <span>{MODE_LABELS[mode]}</span>
                    <span>{guessesByMode[mode] ?? 0}</span>
                </div>
            ))}
        </div>
    );
}
