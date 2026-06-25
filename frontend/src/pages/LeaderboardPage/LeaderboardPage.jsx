import React from 'react';
import useLeaderboard from './hooks/useLeaderboard';
import './LeaderboardPage.css';
import {useNavigate, useParams} from 'react-router-dom';
import Footer from '../../components/Footer';
import Header from '../../components/Header';
import LeaderboardModeSwitcher from '../../components/LeaderboardModeSwitcher';
import StreakFlame from '../../components/StreakFlame';

const LeaderboardColumn = ({players, startIndex, isLoading}) => {
    const filledPlayers = [...players];
    while (filledPlayers.length < 5) {
        filledPlayers.push({isEmpty: true, id: `empty-${startIndex + filledPlayers.length}`});
    }

    return (
        <div className="leaderboard-column">
            <div className="leaderboard-header">
                <span className="text-center">#</span>
                <span className="text-left">Gracz</span>
                <span className="text-center">Próby</span>
                <span className="text-center">Streak</span>
                <span className="text-center">Czas</span>
            </div>
            {filledPlayers.map((player, idx) => {
                if (isLoading) {
                    return <div key={`skeleton-${startIndex + idx}`} className="leaderboard-row skeleton-row"></div>;
                }

                if (player.isEmpty) {
                    return (
                        <div key={player.id} className="leaderboard-row empty-row">
                            <span className="text-center">{startIndex + idx + 1}</span>
                            <div className="leaderboard-player empty-player">
                                <span>---</span>
                            </div>
                            <span className="text-center">-</span>
                            <span className="text-center">-</span>
                            <span className="text-center">--:--</span>
                        </div>
                    );
                }

                return (
                    <div key={player.playerId} className="leaderboard-row">
                        <span className="text-center">{player.rank}</span>
                        <div className="leaderboard-player">
                            <img
                                src={player.playerAvatarImage ? `/avatar/${player.playerAvatarImage}.jpg` : '/avatar/AVATAR_DEFAULT.jpg'}
                                alt="avatar"
                                className="leaderboard-avatar"
                            />
                            <span className="text-left text-ellipsis">{player.playerDisplayName}</span>
                        </div>
                        <span className="text-center">{player.numberOfTries}</span>
                        <span className="text-center"><StreakFlame value={player.currentStreak} /></span>
                        <span className="text-center">{new Date(player.scoreTimeStamp).toLocaleTimeString('pl-PL', {
                            hour: '2-digit',
                            minute: '2-digit'
                        })}</span>
                    </div>
                );
            })}
        </div>
    );
};

const LeaderboardPage = () => {
    const {mode} = useParams();
    const currentMode = (mode || 'classic').toLowerCase();

    const {
        leaderboardData,
        loading,
        error,
        currentPage,
        goToNextPage,
        goToPreviousPage,
        goToFirstPage,
        goToLastPage,
    } = useLeaderboard(currentMode);

    const navigate = useNavigate();

    const getTitle = () => {
        switch (currentMode) {
            case 'images':
                return 'Ranking Zdjęć';
            case 'quotes':
                return 'Ranking Cytatów';
            case 'classic':
                return 'Ranking Klasyczny';
            default:
                return 'Ranking';
        }
    };

    if (error) {
        return (
            <div className="leaderboard-page">
                <Header/>
                <div className="leaderboard-container">
                    <button onClick={() => navigate(-1)} className="back-button">
                        Powrót
                    </button>
                    <h1>{getTitle()}</h1>
                    <LeaderboardModeSwitcher currentMode={currentMode}/>
                    <div>Wystąpił błąd: {error}</div>
                </div>
                <Footer/>
            </div>
        );
    }

    const isLoading = loading && !leaderboardData;
    const content = isLoading ? Array(10).fill({}) : leaderboardData?.content || [];
    const totalPages = leaderboardData?.totalPages || 1;
    const first = leaderboardData?.first ?? true;
    const last = leaderboardData?.last ?? true;

    const leftColumn = content.slice(0, 5);
    const rightColumn = content.slice(5, 10);
    const globalStartIndex = currentPage * 10;

    return (
        <div className="leaderboard-page">
            <Header/>
            <div className="leaderboard-container">
                <button onClick={() => navigate(-1)} className="back-button">
                    Powrót
                </button>
                <h1>{getTitle()}</h1>
                <LeaderboardModeSwitcher currentMode={currentMode}/>
                <div className="leaderboard-table-full">
                    <LeaderboardColumn players={leftColumn} startIndex={globalStartIndex} isLoading={isLoading}/>
                    <LeaderboardColumn players={rightColumn} startIndex={globalStartIndex + 5} isLoading={isLoading}/>
                </div>
                <div className="pagination-controls">
                    <button onClick={goToFirstPage} disabled={first || isLoading}>&lt;&lt;</button>
                    <button onClick={goToPreviousPage} disabled={first || isLoading}>&lt;</button>
                    <span>Strona {currentPage + 1} z {totalPages}</span>
                    <button onClick={goToNextPage} disabled={last || isLoading}>&gt;</button>
                    <button onClick={goToLastPage} disabled={last || isLoading}>&gt;&gt;</button>
                </div>
            </div>
            <Footer/>
        </div>
    );
};

export default LeaderboardPage;