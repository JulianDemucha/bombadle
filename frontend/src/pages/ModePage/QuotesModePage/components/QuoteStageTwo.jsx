import React from 'react';
import CharacterSearchBar from '../../../../components/CharacterSearchBar.jsx';
import ImgTextBanner from "../../../../components/ImgTextBanner.jsx";

const QuoteStageTwo = ({
                           prompt,
                           stageTwoGuesses,
                           isStageTwoWon,
                           isAnimatingSuccess,
                           handleGuessStageTwo
                       }) => {
    const getTargetHeader = (target) => {
        switch (target) {
            case 'SPEAKER': return 'Kto to powiedział? Zgadnij!';
            case 'RECIPIENT': return 'Do kogo to powiedziano? Zgadnij!';
            case 'SUBJECT': return 'O kim to powiedziano? Zgadnij!';
            default: return 'Zgadnij postać! Zgadnij!';
        }
    };

    return (
        <div className="quote-stage-two-container">
            <div style={{ width: '100%', display: 'flex', justifyContent: 'center' }}>
                <ImgTextBanner text={getTargetHeader(prompt?.quoteTarget)} altText="Cel Cytatu" />
            </div>

            <div style={{ opacity: isStageTwoWon ? 0.6 : 1, position: 'relative', zIndex: 20, marginTop: '-20px' }}>
                <CharacterSearchBar
                    onSelectCharacterId={handleGuessStageTwo}
                    disabled={isStageTwoWon || isAnimatingSuccess}
                />
            </div>

            {stageTwoGuesses.length > 0 && (
                <div className="quote-stage-two-guesses">
                    {stageTwoGuesses.map((guess, idx) => {
                        const isCorrect = Boolean(guess.correct ?? guess.name?.match === 'MATCH');

                        let rowClass = "stage-two-guess-row";
                        rowClass += isCorrect ? " is-correct" : " is-wrong";

                        if (guess.isNewAnimation) {
                            rowClass += isCorrect ? " tada-animation" : " shake-animation";
                        }

                        const imageSrc = guess.imageSrc || '/avatar/AVATAR_DEFAULT.jpg';
                        const charName = guess.name?.value || guess.name || '???';

                        return (
                            <div key={guess.id || idx} className={rowClass}>
                                <img src={imageSrc} alt={charName} className="stage-two-guess-avatar" />
                                <span className="stage-two-guess-name">{charName}</span>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
};

export default QuoteStageTwo;