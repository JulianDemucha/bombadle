import React from 'react';
import './style/StreakFlame.css';

const StreakFlame = ({ value }) => (
    <span className="streak-flame" title={`Aktualny streak: ${value ?? 0}`}>
        <svg className="streak-flame__icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <path d="M13 2c.4 3.1-1.2 5-2.8 6.6C8.4 10.4 7 12 7 14.7A5 5 0 0 0 17 15c0-2.2-1-3.8-2-5.3-.5.7-1.1 1.1-1.9 1.3C14 8.7 14 5.3 13 2z" />
            <path className="streak-flame__inner" d="M12 13c.6 1 1.6 1.7 1.6 3a1.9 1.9 0 0 1-3.8.1c0-1 .7-1.6 1.1-2.3.4.3.8.4 1.1.4-.2-.5-.2-.9 0-1.2z" />
        </svg>
        <span className="streak-flame__value">{value ?? 0}</span>
    </span>
);

export default StreakFlame;
