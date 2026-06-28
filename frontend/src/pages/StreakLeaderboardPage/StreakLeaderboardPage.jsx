import React from 'react';
import { useNavigate } from 'react-router-dom';
import useStreakLeaderboard from './hooks/useStreakLeaderboard';
import './StreakLeaderboardPage.css';
import Footer from '../../components/Footer';
import Header from '../../components/Header';
import LeaderboardModeSwitcher from '../../components/LeaderboardModeSwitcher';
import StreakFlame from '../../components/StreakFlame';
import InfoTooltip from '../../components/InfoTooltip';
import { STREAK_TOOLTIP, SUPERSTREAK_TOOLTIP } from '../../components/streakInfo';

const VARIANT_CONFIG = {
    streak: {
        title: 'Ranking Serii',
        columnLabel: 'Seria',
        tooltip: STREAK_TOOLTIP,
        flameVariant: 'default',
        valueKey: 'currentStreak',
    },
    superstreak: {
        title: 'Ranking Superserii',
        columnLabel: 'Superseria',
        tooltip: SUPERSTREAK_TOOLTIP,
        flameVariant: 'super',
        valueKey: 'currentSuperstreak',
    },
};

const avatarSrc = (avatarImage) =>
    avatarImage ? `/avatar/${avatarImage}.jpg` : '/avatar/AVATAR_DEFAULT.jpg';

const StreakLeaderboardPage = ({ variant = 'streak' }) => {
    const config = VARIANT_CONFIG[variant] || VARIANT_CONFIG.streak;

    const {
        leaderboardData,
        loading,
        error,
        currentPage,
        goToNextPage,
        goToPreviousPage,
        goToFirstPage,
        goToLastPage,
    } = useStreakLeaderboard(variant);

    const navigate = useNavigate();

    const isLoading = loading && !leaderboardData;
    const content = isLoading ? Array.from({ length: 10 }, () => ({})) : (leaderboardData?.content || []);
    const totalPages = leaderboardData?.totalPages || 1;
    const first = leaderboardData?.first ?? true;
    const last = leaderboardData?.last ?? true;

    return (
        <div className="streak-leaderboard">
            <Header/>
            <div className="streak-leaderboard__container">
                <button onClick={() => navigate(-1)} className="streak-leaderboard__back">
                    Powrót
                </button>
                <h1 className="streak-leaderboard__title">{config.title}</h1>
                <LeaderboardModeSwitcher currentMode={variant}/>

                {error ? (
                    <div className="streak-leaderboard__error">Wystąpił błąd: {error}</div>
                ) : (
                    <>
                        <div className="streak-leaderboard__table">
                            <div className="streak-leaderboard__header">
                                <span className="streak-leaderboard__cell--center">#</span>
                                <span className="streak-leaderboard__cell--left">Gracz</span>
                                <span className="streak-leaderboard__cell--center">
                                    {config.columnLabel}<InfoTooltip text={config.tooltip} />
                                </span>
                            </div>

                            {content.map((player, idx) => {
                                if (isLoading) {
                                    return (
                                        <div
                                            key={`skeleton-${idx}`}
                                            className="streak-leaderboard__row streak-leaderboard__row--skeleton"
                                        />
                                    );
                                }

                                return (
                                    <div key={player.playerId ?? idx} className="streak-leaderboard__row">
                                        <span className="streak-leaderboard__cell--center">{player.rank}</span>
                                        <div className="streak-leaderboard__player">
                                            <img
                                                src={avatarSrc(player.playerAvatarImage)}
                                                alt="avatar"
                                                className="streak-leaderboard__avatar"
                                            />
                                            <span className="streak-leaderboard__cell--left streak-leaderboard__name">
                                                {player.playerDisplayName}
                                            </span>
                                        </div>
                                        <span className="streak-leaderboard__cell--center">
                                            <StreakFlame value={player[config.valueKey]} variant={config.flameVariant} />
                                        </span>
                                    </div>
                                );
                            })}

                            {!isLoading && content.length === 0 && (
                                <div className="streak-leaderboard__empty">Brak graczy w tym rankingu.</div>
                            )}
                        </div>

                        <div className="streak-leaderboard__pagination">
                            <button onClick={goToFirstPage} disabled={first || isLoading}>&lt;&lt;</button>
                            <button onClick={goToPreviousPage} disabled={first || isLoading}>&lt;</button>
                            <span>Strona {currentPage + 1} z {totalPages}</span>
                            <button onClick={goToNextPage} disabled={last || isLoading}>&gt;</button>
                            <button onClick={goToLastPage} disabled={last || isLoading}>&gt;&gt;</button>
                        </div>
                    </>
                )}
            </div>
            <Footer/>
        </div>
    );
};

export default StreakLeaderboardPage;
