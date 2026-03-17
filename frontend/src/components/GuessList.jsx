import React, { useState, useEffect } from 'react';
import './style/GuessList.css';

const GuessList = () => {
    // const [guesses, setGuesses] = useState([]);
    const [guesses, setGuesses] = useState([
        {
            id: 1,
            name: 'Kapitan Bomba',
            image: 'kapitan_bomba.jpg',
            gender: 'Mezczyzna',
            race: 'Człowiek',
            isAlive: 'Tak',
            affiliation: 'Gwiezdna Flota',
            firstAppearance: '1',
            status: { gender: 'correct', race: 'correct', isAlive: 'correct', affiliation: 'correct', firstAppearance: 'wrong' }
        },
        {
            id: 4,
            name: 'Marik1234',
            image: 'marik1234.jpg',
            gender: 'Mężczyzna',
            race: 'Nieznana',
            isAlive: 'Nie',
            affiliation: 'marik1234',
            firstAppearance: '104',
            status: { gender: 'wrong', race: 'correct', isAlive: 'correct', affiliation: 'wrong', firstAppearance: 'partial' }
        },
        {
            id: 5,
            name: 'Michal Glus',
            image: 'michal_glus.jpg',
            gender: 'Mężczyzna',
            race: 'Kurvinox',
            isAlive: 'Tak',
            affiliation: 'Gwiezdna Flota, Kosmici, Sługa sułtana kosmitów',
            firstAppearance: '55',
            status: { gender: 'correct', race: 'correct', isAlive: 'wrong', affiliation: 'wrong', firstAppearance: 'wrong' }
        }
    ]);
    useEffect(() => {
    }, []);

    return (
        <div className="game-wrapper">
            <div className="board-container">
                <div className="guess-grid board-header">
                    <div className="header-label">Avatar</div>
                    <div className="header-label">Imie</div>
                    <div className="header-label">Płeć</div>
                    <div className="header-label">Rasa</div>
                    <div className="header-label">Żywy</div>
                    <div className="header-label">Powiązania</div>
                    <div className="header-label">Pierwsze pojawienie</div>
                </div>

                <div className="guesses-stack">
                    {guesses.map((guess, index) => (
                        <div key={guess.id || index} className="guess-grid guess-row">

                            <div className="tile avatar-tile">
                                <img src={`/character_card_avatars/${guess.image}`} alt={guess.name} />
                            </div>

                            <div className={`tile text-tile ${guess.status.name}`} data-fulltext={guess.name}><span className="tile-text">{guess.name}</span></div>
                            <div className={`tile text-tile ${guess.status.gender}`} data-fulltext={guess.gender}><span className="tile-text">{guess.gender}</span></div>
                            <div className={`tile text-tile ${guess.status.race}`} data-fulltext={guess.race}><span className="tile-text">{guess.race}</span></div>
                            <div className={`tile text-tile ${guess.status.isAlive}`} data-fulltext={guess.isAlive}><span className="tile-text">{guess.isAlive}</span></div>
                            <div className={`tile text-tile ${guess.status.affiliation}`} data-fulltext={guess.affiliation}><span className="tile-text">{guess.affiliation}</span></div>
                            <div className={`tile text-tile ${guess.status.firstAppearance}`} data-fulltext={guess.firstAppearance}><span className="tile-text">{guess.firstAppearance}</span></div>

                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default GuessList;