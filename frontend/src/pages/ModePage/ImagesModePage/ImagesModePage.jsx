import React, {useState, useEffect} from 'react';
import './ImagesModePage.css';
import Header from "../../../components/Header.jsx";
import Footer from "../../../components/Footer.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import CharacterSearchBar from "../../../components/CharacterSearchBar.jsx";
import Top3Leaderboard from '../../../components/Top3Leaderboard.jsx';
import NavImgButton from '../../../components/NavImgButton.jsx';
import PreviousCharacterCard from "../ClassicModePage/components/PreviousCharacterCard.jsx";
import useImagesModeGame from './hooks/useImagesModeGame.js';
import GlobalLoader from '../../../components/GlobalLoader.jsx';

function ImagesModePage() {
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
        isLoading,
        imageTimestamp
    } = useImagesModeGame();

    const [isImageLoading, setIsImageLoading] = useState(true);
    const [isFirstLoad, setIsFirstLoad] = useState(true);
    const [displayedImage, setDisplayedImage] = useState('');

    const currentImageUrl = `/api/card-guessing/images/current?t=${imageTimestamp}`;

    useEffect(() => {
        setIsImageLoading(true);

        const img = new Image();
        img.src = currentImageUrl;

        img.onload = () => {
            setDisplayedImage(currentImageUrl);
            setIsImageLoading(false);
            setIsFirstLoad(false);
        };

        img.onerror = () => {
            setDisplayedImage(currentImageUrl);
            setIsImageLoading(false);
            setIsFirstLoad(false);
        };
    }, [currentImageUrl]);

    const showSeparator = Boolean(currentUserRow) && !isCurrentUserInTopThree && topThree.length >= 3;

    return (
        <div className="images-mode-page classic-mode-page">
            <Header/>
            <div className="classic-mode-content">
                <ImgTextBanner text="Zgadnij postać ze zdjęcia!" altText="Zgadnij ze zdjęcia"/>

                <div className="images-mode-picture-container">
                    {isFirstLoad && isImageLoading && (
                        <div className="images-mode-loader-overlay">
                            <GlobalLoader text="Ładowanie obrazu..." small={true}/>
                        </div>
                    )}

                    {displayedImage && (
                        <div className="images-mode-picture-wrapper">
                            <img
                                src={displayedImage}
                                alt="Zagadka - obraz postaci"
                                className="images-mode-picture"
                            />
                        </div>
                    )}
                </div>

                <div className="images-mode-search-container" style={{opacity: isWon ? 0.6 : 1}}>
                    <CharacterSearchBar
                        onSelectCharacterId={handleSelectCharacterId}
                        disabled={isWon || isAnimatingSuccess}
                    />
                </div>

                {(hasGuesses || isLoading) && (
                    <div className="images-stage-guesses">
                        {isLoading ? (
                            <GlobalLoader text="Ładowanie prób..." small={true}/>
                        ) : (
                            guesses.map((guess, idx) => {
                                const isCorrect = Boolean(guess.correct);

                                let rowClass = "images-guess-row stage-two-guess-row";
                                rowClass += isCorrect ? " is-correct" : " is-wrong";

                                if (guess.isNewAnimation) {
                                    rowClass += isCorrect ? " tada-animation" : " shake-animation";
                                }

                                const imageSrc = guess.imageSrc || '/avatar/AVATAR_DEFAULT.jpg';
                                const charName = guess.name || '???';

                                return (
                                    <div key={guess.id || idx} className={rowClass}>
                                        <img src={imageSrc} alt={charName} className="stage-two-guess-avatar"/>
                                        <span className="stage-two-guess-name">{charName}</span>
                                    </div>
                                );
                            })
                        )}
                    </div>
                )}

                <div ref={winSectionRef} className="images-mode-win-section">
                    {isLeaderboardExpanded ? (
                        <Top3Leaderboard
                            topThree={topThree}
                            ctaLabel="Zobacz pełny ranking"
                            title={`Zgadłeś w ${guesses.length} próbach!`}
                            showSeparator={showSeparator}
                            currentUserRow={currentUserRow}
                            isAnonymousAndWon={isAnonymousAndWon}
                            dailyCounterText={`dziś zgadło ${todaySolvers} graczy`}
                            leaderboardPath="/leaderboard/images"
                        />
                    ) : (
                        <Top3Leaderboard
                            topThree={topThree}
                            ctaLabel="Zobacz pełny ranking"
                            className={hasGuesses ? 'leaderboard-section--after-guesses' : ''}
                            dailyCounterText={`dziś zgadło ${todaySolvers} graczy`}
                            leaderboardPath="/leaderboard/images"
                        />
                    )}

                    <div className="images-mode-previous-character">
                        <PreviousCharacterCard endpoint="/api/character-card/IMAGES/previous-character-card"/>
                    </div>
                </div>

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

export default ImagesModePage;