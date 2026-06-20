import React from 'react';
import './QuotesModePage.css';
import Header from "../../../components/Header.jsx";
import Footer from "../../../components/Footer.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import QuoteStageOne from './components/QuoteStageOne.jsx';
import QuoteStageTwo from './components/QuoteStageTwo.jsx';
import Top3Leaderboard from '../../../components/Top3Leaderboard.jsx';
import useQuotesModeGame from './hooks/useQuotesModeGame.js';
import PreviousCharacterCard from "../ClassicModePage/components/PreviousCharacterCard.jsx";

function QuotesModePage() {
    const {
        prompt,
        stageOneGuesses,
        isStageOneWon,
        handleGuessStageOne,
        stageTwoGuesses,
        isStageTwoWon,
        isAnonymousAndWon,
        handleGuessStageTwo,
        isAnimatingSuccess,
        winSectionRef,
        stageTwoRef,
        isLeaderboardExpanded,
        topThree,
        currentUserRow,
        isCurrentUserInTopThree
    } = useQuotesModeGame();

    const showSeparator = Boolean(currentUserRow) && !isCurrentUserInTopThree && topThree.length >= 3;

    return (
        <div className="quotes-mode-page classic-mode-page">
            <Header />
            <div className="classic-mode-content">
                <ImgTextBanner text="Dokończ dzisiejszy cytat!" altText="Dokończ Cytat" />

                <QuoteStageOne
                    prompt={prompt}
                    stageOneGuesses={stageOneGuesses}
                    isStageOneWon={isStageOneWon}
                    handleGuessStageOne={handleGuessStageOne}
                />

                {isStageOneWon && (
                    <div ref={stageTwoRef} style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                        <QuoteStageTwo
                            prompt={prompt}
                            stageTwoGuesses={stageTwoGuesses}
                            isStageTwoWon={isStageTwoWon}
                            isAnimatingSuccess={isAnimatingSuccess}
                            handleGuessStageTwo={handleGuessStageTwo}
                        />
                    </div>
                )}

                <div ref={winSectionRef} style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    {isLeaderboardExpanded ? (
                        <Top3Leaderboard
                            topThree={topThree}
                            ctaLabel="Zobacz pełny ranking"
                            title={`Zgadłeś w ${stageTwoGuesses.length} próbach!`}
                            showSeparator={showSeparator}
                            currentUserRow={currentUserRow}
                            isAnonymousAndWon={isAnonymousAndWon}
                            leaderboardPath="/leaderboard/quotes"
                        />
                    ) : (
                        <Top3Leaderboard
                            topThree={topThree}
                            ctaLabel="Zobacz pełny ranking"
                            className={stageTwoGuesses.length > 0 ? 'leaderboard-section--after-guesses' : ''}
                            leaderboardPath="/leaderboard/quotes"
                        />

                    )}
                    {isLeaderboardExpanded && (
                        <div style={{ marginTop: '20px', width: '100%', display: 'flex', justifyContent: 'center' }}>
                            <PreviousCharacterCard endpoint="/api/character-card/QUOTES_STAGE_2/previous-character-card" />
                        </div>
                    )}
                </div>
            </div>
            <Footer />
        </div>
    );
}

export default QuotesModePage;