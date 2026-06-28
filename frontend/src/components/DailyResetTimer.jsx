import React, { useEffect, useRef, useState } from 'react';
import './style/DailyResetTimer.css';

const WARSAW_TZ = 'Europe/Warsaw';
const RESET_HOUR = 7;

const warsawFormatter = new Intl.DateTimeFormat('en-US', {
    timeZone: WARSAW_TZ,
    hour12: false,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
});

function warsawParts(date) {
    const parts = warsawFormatter.formatToParts(date).reduce((acc, part) => {
        if (part.type !== 'literal') acc[part.type] = part.value;
        return acc;
    }, {});
    return {
        year: Number(parts.year),
        month: Number(parts.month),
        day: Number(parts.day),
        hour: Number(parts.hour) % 24,
        minute: Number(parts.minute),
        second: Number(parts.second),
    };
}

function warsawOffsetMs(date) {
    const p = warsawParts(date);
    const asUTC = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second);
    return asUTC - date.getTime();
}

function warsawWallClockToInstant(year, month, day, hour, minute, second) {
    const wallAsUTC = Date.UTC(year, month - 1, day, hour, minute, second);
    let instant = wallAsUTC - warsawOffsetMs(new Date(wallAsUTC));
    instant = wallAsUTC - warsawOffsetMs(new Date(instant));
    return instant;
}

function nextWarsawReset(now = new Date()) {
    const p = warsawParts(now);
    const secondsOfDay = p.hour * 3600 + p.minute * 60 + p.second;
    const dayOffset = secondsOfDay >= RESET_HOUR * 3600 ? 1 : 0; // Date.UTC handles day overflow
    return warsawWallClockToInstant(p.year, p.month, p.day + dayOffset, RESET_HOUR, 0, 0);
}

function formatRemaining(ms) {
    const totalSeconds = Math.max(0, Math.floor(ms / 1000));
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

function DailyResetTimer() {
    const targetRef = useRef(nextWarsawReset());
    const reloadedRef = useRef(false);
    const [remaining, setRemaining] = useState(() => targetRef.current - Date.now());

    useEffect(() => {
        const tick = () => {
            const diff = targetRef.current - Date.now();

            if (diff <= 0 && !reloadedRef.current) {
                reloadedRef.current = true;
                targetRef.current = nextWarsawReset();
                setRemaining(targetRef.current - Date.now());
                window.location.reload();
                return;
            }

            setRemaining(diff);
        };

        const intervalId = setInterval(tick, 1000);
        return () => clearInterval(intervalId);
    }, []);

    return (
        <div className="daily-reset-timer" role="timer" aria-live="off">
            <span className="daily-reset-timer__label">Następny reset za: </span>
            <span className="daily-reset-timer__value">{formatRemaining(remaining)}</span>
        </div>
    );
}

export default DailyResetTimer;
