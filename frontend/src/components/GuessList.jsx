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
            affiliation: 'Gwiezdna Flota',
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

                            <div className={`tile ${guess.status.name}`}>{guess.name}</div>
                            <div className={`tile ${guess.status.gender}`}>{guess.gender}</div>
                            <div className={`tile ${guess.status.race}`}>{guess.race}</div>
                            <div className={`tile ${guess.status.isAlive}`}>{guess.isAlive}</div>
                            <div className={`tile ${guess.status.affiliation}`}>{guess.affiliation}</div>
                            <div className={`tile ${guess.status.firstAppearance}`}>{guess.firstAppearance}</div>

                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default GuessList;