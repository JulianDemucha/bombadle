import React from 'react';
import './style/GuessList.css';

const GuessList = ({ guesses = [] }) => {

    if (!guesses || guesses.length === 0) {
        return null;
    }

    return (
        <div className="game-wrapper">
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
                        const isNew = index === 0;
                        return (
                            <div key={guess.id || index} className={`guess-grid guess-row ${isNew ? 'new-row' : 'existing-row'}`}>

                                <div className="tile avatar-tile">
                                    <img
                                        src={guess.imageSrc || (guess.image ? `/character_card_avatars/${guess.image}` : '/avatar/AVATAR_DEFAULT.jpg')}
                                        alt={guess.name}
                                    />
                                </div>

                                <div className="tile text-tile name-tile" data-fulltext={guess.name}>
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.name}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className={`tile text-tile gender-tile ${guess.status.gender}`} data-fulltext={guess.gender}>
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.gender}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className={`tile text-tile race-tile ${guess.status.race}`} data-fulltext={guess.race}>
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.race}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className={`tile text-tile alive-tile ${guess.status.isAlive}`} data-fulltext={guess.isAlive}>
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.isAlive}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className={`tile text-tile colors-tile ${guess.status.colors}`} data-fulltext={guess.colors}>
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.colors}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className={`tile text-tile affiliation-tile ${guess.status.affiliation}`} data-fulltext={guess.affiliation}>
                                    <div className="tile-inner">
                                        <div className="tile-front"></div>
                                        <div className="tile-back">
                                            <span className="tile-text">{guess.affiliation}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className={`tile text-tile first-appearance-tile ${guess.status.firstAppearance}`} data-fulltext={guess.firstAppearance}>
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