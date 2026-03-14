import React, { useState } from 'react';
import './style/CharacterSearchBar.css';
import character_cards from '../data/character_cards.json'

const CharacterSearchBar = () => {
    const [searchTerm, setSearchTerm] = useState('');
    const [isOpen, setIsOpen] = useState(false);

    return (
        <div className="search-container">
            <input
                type="text"
                className="search-input-image"
                placeholder="Wpisz nazwę..."
                value={searchTerm}
                onChange={(e) => {
                    setSearchTerm(e.target.value);
                    setIsOpen(e.target.value.length > 0);
                }}
            />

            {isOpen && (
                <div className="search-dropdown-background">

                    <ul className="search-dropdown-scrollbox">

                        {character_cards.map((characterCard, id) => (
                            <li key={id} className="search-dropdown-item"
                                onClick={() => {
                                    setSearchTerm('');
                                    setIsOpen(false);
                                }
                            }>
                                <img src={`/character_card_avatars/${characterCard.image}`} alt={characterCard.name} className="pixelated-icon dropdown-avatar" />
                                <span>{characterCard.name}</span>
                            </li>
                        ))}

                    </ul>
                </div>
            )}
        </div>
    );
};

export default CharacterSearchBar;