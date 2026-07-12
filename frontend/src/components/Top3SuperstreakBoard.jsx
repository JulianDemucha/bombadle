import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import StreakFlame from './StreakFlame.jsx';
import InfoTooltip from './InfoTooltip.jsx';
import { SUPERSTREAK_TOOLTIP } from './streakInfo.js';
import './style/Top3SuperstreakBoard.css';

const avatarSrc = (avatarImage) =>
    avatarImage ? `/avatar/${avatarImage}.jpg` : '/avatar/AVATAR_DEFAULT.jpg';

function Top3SuperstreakBoard({ topThree = [], loading = false }) {
    const location = useLocation();
    return (
        <div className="superstreak-board">
            <h3 className="superstreak-board__title">Najdłuższe superserie</h3>

            <div className="superstreak-board__table">
                <div className="superstreak-board__header">
                    <span className="superstreak-board__cell--center">#</span>
                    <span className="superstreak-board__cell--left">Gracz</span>
                    <span className="superstreak-board__cell--center">
                        Superseria<InfoTooltip text={SUPERSTREAK_TOOLTIP} />
                    </span>
                </div>

                {loading && (
                    Array.from({ length: 3 }).map((_, idx) => (
                        <div key={`skeleton-${idx}`} className="superstreak-board__row superstreak-board__row--skeleton" />
                    ))
                )}

                {!loading && topThree.length === 0 && (
                    <div className="superstreak-board__empty">Brak superserii — bądź pierwszy!</div>
                )}

                {!loading && topThree.map((player) => (
                    <div key={player.playerId ?? player.rank} className="superstreak-board__row">
                        <span className="superstreak-board__cell--center">{player.rank}</span>
                        <div className="superstreak-board__player">
                            <img
                                src={avatarSrc(player.playerAvatarImage)}
                                alt="avatar"
                                className="superstreak-board__avatar"
                            />
                            <span className="superstreak-board__cell--left superstreak-board__name">
                                {player.playerDisplayName}
                            </span>
                        </div>
                        <span className="superstreak-board__cell--center">
                            <StreakFlame value={player.currentSuperstreak} variant="super" />
                        </span>
                    </div>
                ))}
            </div>

            <Link className="superstreak-board__cta" to="/leaderboard/superstreak" state={{ from: location.pathname }} replace>
                Zobacz pełny ranking superserii
            </Link>
        </div>
    );
}

export default Top3SuperstreakBoard;
