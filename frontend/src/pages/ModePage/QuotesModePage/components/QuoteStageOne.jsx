import React, { useMemo, useState } from 'react';
import '../QuotesModePage.css';
import GlobalLoader from '../../../../components/GlobalLoader.jsx';
import { formatQuoteDialogue } from '../utils/quotesModeMappers.js';

const QuoteStageOne = ({
                           prompt,
                           stageOneGuesses,
                           isStageOneWon,
                           handleGuessStageOne
                       }) => {

    const [clickedOption, setClickedOption] = useState(null);

    const correctAnswerClicked = useMemo(() => {
        if (!isStageOneWon || stageOneGuesses.length === 0) return null;
        return stageOneGuesses[stageOneGuesses.length - 1];
    }, [isStageOneWon, stageOneGuesses]);

    if (!prompt) return <GlobalLoader text="Wczytywanie cytatu..." />;

    const handleOptionClick = (optionText) => {
        setClickedOption(optionText);
        handleGuessStageOne(optionText);
    };

    const dialogueLines = formatQuoteDialogue(prompt.quoteBeginning);

    return (
        <div className="quote-stage-one-container">
            <div className="quote-bubble">

                <div className="quote-text-container">
                    <p className="quote-text">
                        {dialogueLines.map((line, index) => (
                            <React.Fragment key={index}>
                                {line}{index === dialogueLines.length - 1 ? '...' : ''}
                                {index < dialogueLines.length - 1 && <br />}
                            </React.Fragment>
                        ))}
                    </p>
                    <div className="quote-meta">
                        <span className="quote-episode">Odcinek: {prompt.appearanceEpisode}</span>
                    </div>
                </div>

                <div className="quote-options-grid">
                    {prompt.options.map((optionText, index) => {
                        const isGuessed = stageOneGuesses.includes(optionText);
                        const isCorrect = isStageOneWon && optionText === correctAnswerClicked;

                        let buttonClass = "quote-option-btn";

                        if (isCorrect) {
                            buttonClass += " is-correct";
                            if (optionText === clickedOption) {
                                buttonClass += " tada-animation";
                            }
                        } else if (isGuessed || isStageOneWon) {
                            buttonClass += " is-wrong";
                            if (!isStageOneWon && optionText === clickedOption) {
                                buttonClass += " shake-animation";
                            }
                        }

                        return (
                            <button
                                key={index}
                                className={buttonClass}
                                onClick={() => handleOptionClick(optionText)}
                                disabled={isGuessed || isStageOneWon}
                            >
                                {optionText}
                            </button>
                        );
                    })}
                </div>

            </div>
        </div>
    );
};

export default QuoteStageOne;