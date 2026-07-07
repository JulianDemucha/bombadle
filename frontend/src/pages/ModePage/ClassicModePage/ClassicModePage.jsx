import React from 'react';
import './ClassicModePage.css';
import Footer from "../../../components/Footer.jsx";
import Header from "../../../components/Header.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import CharacterSearchBar from "../../../components/CharacterSearchBar.jsx";
import ClassicGuessList from "../../../components/ClassicGuessList.jsx";
import PreviousCharacterCard from './components/PreviousCharacterCard.jsx';
import GuessLegend from './components/GuessLegend.jsx';
import Top3Leaderboard from '../../../components/Top3Leaderboard.jsx';
import NavImgButton from '../../../components/NavImgButton.jsx';
import DailyResetTimer from '../../../components/DailyResetTimer.jsx';
import useClassicModeGame from './hooks/useClassicModeGame.js';

function ClassicModePage() {
    const {
        guesses,
        hasGuesses,
        isWon,
        isAnonymousAndWon,
        isLeaderboardExpanded,
        isAnimatingSuccess,
        topThree,
        todaySolvers,
        currentUserRow,
        isCurrentUserInTopThree,
        handleSelectCharacterId,
        winSectionRef,
        isLoading
    } = useClassicModeGame();

    const showSeparator = Boolean(currentUserRow) && !isCurrentUserInTopThree && topThree.length >= 3;

    return (
        <div className="classic-mode-page">
            <Header showBackButton/>
            <DailyResetTimer/>
            <div className="classic-mode-content">
                <ImgTextBanner text = 'Zgadnij dzisiejszą postać' altText="ok"/>

                <div style={{ opacity: (isWon) ? 0.6 : 1, marginTop: '-30px', position: 'relative', zIndex: 20 }}>
                    <CharacterSearchBar onSelectCharacterId={handleSelectCharacterId} disabled={isWon || isAnimatingSuccess} />
                </div>

                {(hasGuesses || isLoading) && <ClassicGuessList guesses={guesses} isLoading={isLoading} />}

                {hasGuesses && <GuessLegend />}

                <div ref={winSectionRef} style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    {isLeaderboardExpanded ? (
                        <Top3Leaderboard
                            topThree={topThree}
                            ctaLabel="Zobacz pelny ranking"
                            title={`Zgadłeś w ${guesses.length} próbach!`}
                            showSeparator={showSeparator}
                            currentUserRow={currentUserRow}
                            dailyCounterText={`dziś zgadło ${todaySolvers} graczy`}
                            isAnonymousAndWon={isAnonymousAndWon}
                        />
                    ) : (
                        <Top3Leaderboard
                            topThree={topThree}
                            ctaLabel="Zobacz pelny ranking"
                            className={hasGuesses ? 'leaderboard-section--after-guesses' : ''}
                            dailyCounterText={`dziś zgadło ${todaySolvers} graczy`}
                        />
                    )}
                </div>

                <PreviousCharacterCard />

                <NavImgButton
                    to="/login"
                    imgSrc="/src/assets/buttons/login_button.png"
                    altText="Zaloguj się"
                    className="image-button login-mobile"
                    hideIfAuthenticated={true}
                />

            </div>
            <Footer/>
        </div>
    );
}

export default ClassicModePage;