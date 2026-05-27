import React, { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import './style/GuessList.css';

const GuessRow = ({ guess, isNew, onMouseEnter, onMouseLeave }) => {
    const rowRef = useRef(null);
    const [isAnimating, setIsAnimating] = useState(isNew);

    useEffect(() => {
        if (isNew) {
            const timer = setTimeout(() => {
                setIsAnimating(false);
            }, 6000); // Total animation duration
            return () => clearTimeout(timer);
        }
    }, [isNew]);

    const rowClassName = `guess-grid guess-row ${isAnimating ? 'new-row' : 'existing-row'}`;

    return (
        <div ref={rowRef} className={rowClassName} key={guess.id}>
            <div className="tile avatar-tile">
                <img
                    src={guess.imageSrc || (guess.image ? `/character_card_avatars/${guess.image}` : '/avatar/AVATAR_DEFAULT.jpg')}
                    alt={guess.name}
                />
            </div>
            <div 
                className="tile text-tile name-tile" 
                onMouseEnter={(e) => onMouseEnter(e, guess.name)}
                onMouseLeave={onMouseLeave}
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
                onMouseEnter={(e) => onMouseEnter(e, guess.gender)}
                onMouseLeave={onMouseLeave}
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
                onMouseEnter={(e) => onMouseEnter(e, guess.race)}
                onMouseLeave={onMouseLeave}
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
                onMouseEnter={(e) => onMouseEnter(e, guess.isAlive)}
                onMouseLeave={onMouseLeave}
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
                onMouseEnter={(e) => onMouseEnter(e, guess.colors)}
                onMouseLeave={onMouseLeave}
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
                onMouseEnter={(e) => onMouseEnter(e, guess.affiliation)}
                onMouseLeave={onMouseLeave}
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
                onMouseEnter={(e) => onMouseEnter(e, guess.firstAppearance)}
                onMouseLeave={onMouseLeave}
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
};


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
                    {guesses.map((guess, index) => (
                        <GuessRow 
                            key={guess.id || index}
                            guess={guess}
                            isNew={index === 0 && !!guess.isNewAnimation}
                            onMouseEnter={handleMouseEnter}
                            onMouseLeave={handleMouseLeave}
                        />
                    ))}
                </div>
            </div>
        </div>
    );
};

export default GuessList;