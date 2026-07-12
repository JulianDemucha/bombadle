import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './style/LeaderboardModeSwitcher.css';

const MODES = [
    { mode: 'classic', label: 'Klasyczny' },
    { mode: 'quotes', label: 'Cytaty' },
    { mode: 'images', label: 'Zdjęcia' },
    { mode: 'streak', label: 'Seria' },
    { mode: 'superstreak', label: 'Superseria' },
];

function LeaderboardModeSwitcher({ currentMode }) {
    const location = useLocation();
    const from = location.state?.from;

    return (
        <nav className="leaderboard-mode-switcher" aria-label="Wybór rankingu">
            {MODES.map(({ mode, label }) => (
                <Link
                    key={mode}
                    to={`/leaderboard/${mode}`}
                    state={from ? { from } : undefined}
                    replace
                    className={`leaderboard-mode-switcher__item${mode === currentMode ? ' is-active' : ''}`}
                    aria-current={mode === currentMode ? 'page' : undefined}
                >
                    {label}
                </Link>
            ))}
        </nav>
    );
}

export default LeaderboardModeSwitcher;