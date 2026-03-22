import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import './style/GuessList.css';

const GuessList = ({ guesses = [] }) => {
    const [tooltip, setTooltip] = useState({ visible: false, text: '', x: 0, y: 0 });

    const handleMouseEnter = (e, text) => {
        if (!text) return;
        const rect = e.currentTarget.getBoundingClientRect();
        setTooltip({
            visible: true,
            text,
            x: rect.left + rect.width / 2,
            y: rect.bottom
        });
    };

    const handleMouseLeave = () => {
        setTooltip(prev => ({ ...prev, visible: false }));
    };

    if (!guesses || guesses.length === 0) {
        return null;
    }

    return (
        <div className="game-wrapper">
            {tooltip.visible && createPortal(
                <div 
                    className="floating-tooltip"
                    style={{ left: tooltip.x, top: tooltip.y }}
                >
                    {tooltip.text}
                </div>,
                document.body
            )}
            <div className="board-container">
                <div className="guess-grid board-header">
                    <div className="header-label">Avatar</div>
                    <div className="header-label">Imie</div>
                    <div className="header-label">Płeć</div>
                    <div className="header-label">Rasa</div>
                    <div className="header-label">Żywy</div>
                    <div className="header-label">Kolorystyka</div>
                    <div className="header-label">Powiązania</div>
                    <div className="header-label">Pierwsze pojawienie</div>
                </div>

                <div className="guesses-stack">
                    {guesses.map((guess, index) => {
                        const isNew = index === 0 && !!guess.isNewAnimation;
                        return (
                            <div key={guess.id || index} className={`guess-grid guess-row ${isNew ? 'new-row' : 'existing-row'}`}>

                                <div className="tile avatar-tile">
                                    <img
                                        src={guess.imageSrc || (guess.image ? `/character_card_avatars/${guess.image}` : '/avatar/AVATAR_DEFAULT.jpg')}
                                        alt={guess.name}
                                    />
                                </div>

                                <div 
                                    className="tile text-tile name-tile" 
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.name)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.name}</span>
                                        </div>
                                    </div>
                                </div>
                                <div 
                                    className={`tile text-tile gender-tile ${guess.status.gender}`}
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.gender)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.gender}</span>
                                        </div>
                                    </div>
                                </div>
                                <div 
                                    className={`tile text-tile race-tile ${guess.status.race}`}
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.race)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.race}</span>
                                        </div>
                                    </div>
                                </div>
                                <div 
                                    className={`tile text-tile alive-tile ${guess.status.isAlive}`}
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.isAlive)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.isAlive}</span>
                                        </div>
                                    </div>
                                </div>
                                <div 
                                    className={`tile text-tile colors-tile ${guess.status.colors}`}
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.colors)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.colors}</span>
                                        </div>
                                    </div>
                                </div>
                                <div 
                                    className={`tile text-tile affiliation-tile ${guess.status.affiliation}`}
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.affiliation)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.affiliation}</span>
                                        </div>
                                    </div>
                                </div>
                                <div 
                                    className={`tile text-tile first-appearance-tile ${guess.status.firstAppearance}`}
                                    onMouseEnter={(e) => handleMouseEnter(e, guess.firstAppearance)}
                                    onMouseLeave={handleMouseLeave}
                                >
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            {guess.meta?.firstAppearanceDirection === 'HIGHER' && <div className="background-arrow arrow-up"></div>}
                                            {guess.meta?.firstAppearanceDirection === 'LOWER' && <div className="background-arrow arrow-down"></div>}
                                            <span className="tile-text">{guess.firstAppearance}</span>
                                        </div>
                                    </div>
                                </div>

                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default GuessList;