import React from 'react';
import './ClassicModePage.css';
import Footer from "../../../components/Footer.jsx";
import Header from "../../../components/Header.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import CharacterSearchBar from "../../../components/CharacterSearchBar.jsx";
import GuessList from "../../../components/GuessList.jsx";
import WinAnimation from "../../../components/WinAnimation.jsx";
import PreviousCharacterCard from './components/PreviousCharacterCard.jsx';
import GuessLegend from './components/GuessLegend.jsx';
import ClassicLeaderboard from './components/ClassicLeaderboard.jsx';
import useClassicModeGame from './hooks/useClassicModeGame.js';

function ClassicModePage() {
    const {
        guesses,
        hasGuesses,
        isWon,
        isLeaderboardExpanded,
        isAnimatingSuccess,
        handleSelectCharacterId,
        winSectionRef
    } = useClassicModeGame();

    const topThreeToday = [
        { rank: 1, name: 'Kacperek opa', attempts: 3, time: '08:12', wins: 42, avatar: '/avatar/AVATAR_JANUSZ.jpg' },
        { rank: 2, name: 'Mitolajek_2137', attempts: 4, time: '09:45', wins: 15, avatar: '/avatar/AVATAR_KURVINOX.jpg' },
        { rank: 3, name: 'Kryy1234', attempts: 5, time: '11:20', wins: 7, avatar: null }
    ];

    return (
        <div className="classic-mode-page">
            <WinAnimation isVisible={isWon} />
            <Header/>
            <div className="classic-mode-content">
                <ImgTextBanner text = 'Zgadnij dzisiejszą postać' altText="ok"/>
                
                <div style={{ pointerEvents: (isWon || isAnimatingSuccess) ? 'none' : 'auto', opacity: (isWon) ? 0.6 : 1, marginTop: '-30px', position: 'relative', zIndex: 20 }}>
                    <CharacterSearchBar onSelectCharacterId={handleSelectCharacterId}/>
                </div>

                {hasGuesses && <GuessList guesses={guesses}/>} 

                {hasGuesses && <GuessLegend />}
                
                <div ref={winSectionRef} style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    {isLeaderboardExpanded ? (
                        <ClassicLeaderboard
                            topThree={topThreeToday}
                            ctaLabel="Zobacz pelny ranking"
                            title={`Zgadłeś w ${guesses.length} próbach!`}
                            showSeparator
                            currentUserRow={{
                                rank: 124,
                                name: 'Ty',
                                time: '12:43',
                                attempts: guesses.length,
                                wins: 15,
                                avatar: '/avatar/AVATAR_DEFAULT.jpg'
                            }}
                            dailyCounterText="Dzisiaj zgadło już 10 osób."
                        />
                    ) : (
                        <ClassicLeaderboard
                            topThree={topThreeToday}
                            ctaLabel="Zobacz pelny ranking"
                            className={hasGuesses ? 'leaderboard-section--after-guesses' : ''}
                            dailyCounterText="Dzisiaj zgadło już 9 osób."
                        />
                    )}
                </div>

                <PreviousCharacterCard />

            </div>
            <Footer/>
        </div>
    );
}

export default ClassicModePage;
