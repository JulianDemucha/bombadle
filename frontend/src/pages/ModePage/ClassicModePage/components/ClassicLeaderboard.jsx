import React from 'react';
import { useNavigate } from 'react-router-dom';

function ClassicLeaderboard({
    topThree = [],
    ctaLabel,
    className = '',
    title,
    currentUserRow,
    showSeparator = false,
    dailyCounterText = null,
    isAnonymousAndWon = false
}) {
    const navigate = useNavigate();
    const containerClassName = `stamped-banner-container leaderboard-wrapper leaderboard-section ${className}`.trim();

    return (
        <div className={containerClassName}>
            {title && <h3 className="leaderboard-title">{title}</h3>}

            <div className="leaderboard-table">
                <div className="leaderboard-header">
                    <span className="text-center">#</span>
                    <span className="text-left">Gracz</span>
                    <span className="text-center">Czas</span>
                    <span className="text-center">Próby</span>
                    <span className="text-center">Wins</span>
                </div>

                {topThree.map((player) => (
                    <div
                        key={player.playerId ?? player.rank}
                        className={`leaderboard-row${player.isCurrentUser ? ' current-user' : ''}`}
                    >
                        <span className="text-center">{player.rank}</span>
                        <div className="leaderboard-player">
                            <img
                                src={player.avatar || '/avatar/AVATAR_DEFAULT.jpg'}
                                alt="av"
                                className="leaderboard-avatar"
                            />
                            <span className="text-left text-ellipsis">{player.name}</span>
                        </div>
                        <span className="text-center">{player.time}</span>
                        <span className="text-center">{player.attempts}</span>
                        <span className="text-center">{player.wins}</span>
                    </div>
                ))}

                {showSeparator && <div className="leaderboard-separator">...</div>}

                {currentUserRow && (
                    <div className="leaderboard-row current-user">
                        <span className="text-center">{currentUserRow.rank}</span>
                        <div className="leaderboard-player">
                            <img
                                src={currentUserRow.avatar || '/avatar/AVATAR_DEFAULT.jpg'}
                                alt="av"
                                className="leaderboard-avatar"
                            />
                            <span className="text-left text-ellipsis">{currentUserRow.name}</span>
                        </div>
                        <span className="text-center">{currentUserRow.time}</span>
                        <span className="text-center">{currentUserRow.attempts}</span>
                        <span className="text-center">{currentUserRow.wins}</span>
                    </div>
                )}
            </div>

            {isAnonymousAndWon && (
                <div className="login-prompt-container">
                    <p className="login-prompt-text">Zaloguj się, aby zapisać swój wynik i pojawić się w rankingu!</p>
                    <button
                        type="button"
                        className="login-prompt-button"
                        onClick={() => {
                            const currentUrl = encodeURIComponent(window.location.pathname + window.location.search);
                            navigate(`/login?target=${currentUrl}`);
                        }}
                    >
                        Zaloguj się / Zarejestruj
                    </button>
                </div>
            )}

            {dailyCounterText && <p className="daily-solved-counter">{dailyCounterText}</p>}

            <button type="button" className="leaderboard-cta">
                {ctaLabel}
            </button>
        </div>
    );
}

export default ClassicLeaderboard;
