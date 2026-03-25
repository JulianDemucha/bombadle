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
        topThree,
        currentUserRow,
        isCurrentUserInTopThree,
        handleSelectCharacterId,
        winSectionRef
    } = useClassicModeGame();

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
                            topThree={topThree}
                            ctaLabel="Zobacz pelny ranking"
                            title={`Zgadłeś w ${guesses.length} próbach!`}
                            showSeparator={!isCurrentUserInTopThree && Boolean(currentUserRow)}
                            currentUserRow={!isCurrentUserInTopThree ? currentUserRow : null}
                            dailyCounterText="Dzisiaj zgadło już 10 osób."
                        />
                    ) : (
                        <ClassicLeaderboard
                            topThree={topThree}
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
