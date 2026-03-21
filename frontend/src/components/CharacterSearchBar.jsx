import React, { useEffect, useMemo, useState } from 'react';
import './style/CharacterSearchBar.css';
import { apiFetch } from '../api/api.js';

const normalizeText = (value) =>
    (value || '')
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');

const CharacterSearchBar = ({ onSelectCharacterId }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [characterCards, setCharacterCards] = useState([]);

    useEffect(() => {
        const fetchCharacterCards = async () => {
            try {
                const response = await apiFetch('/api/character-card/search-index');
                setCharacterCards(response.data || []);
            } catch (error) {
                console.error('Błąd pobierania listy postaci:', error);
                setCharacterCards([]);
            }
        };

        fetchCharacterCards();
    }, []);

    const filteredCards = useMemo(() => {
        const term = normalizeText(searchTerm.trim());
        if (!term) return [];

        return characterCards
            .filter((card) => normalizeText(card.name).includes(term))
            .slice(0, 10);
    }, [searchTerm, characterCards]);

    return (
        <div className="search-container">
            <input
                type="text"
                className="search-input-image"
                placeholder="Wpisz nazwę..."
                value={searchTerm}
                onChange={(e) => {
                    const value = e.target.value;
                    setSearchTerm(value);
                    setIsOpen(value.trim().length > 0);
                }}
                onFocus={() => setIsOpen(searchTerm.trim().length > 0)}
            />

            {isOpen && (
                <div className="search-dropdown-background">

                    <ul className="search-dropdown-scrollbox">

                        {filteredCards.length === 0 && (
                            <li className="search-dropdown-empty">Brak wynikow</li>
                        )}

                        {filteredCards.map((characterCard) => (
                            <li key={characterCard.id} className="search-dropdown-item"
                                onClick={() => {
                                    if (onSelectCharacterId) {
                                        onSelectCharacterId(characterCard.id);
                                    }
                                    setSearchTerm('');
                                    setIsOpen(false);
                                }
                            }>
                                <img
                                    src={characterCard.imageSrc || characterCard.image_src}
                                    alt={characterCard.name}
                                    className="pixelated-icon dropdown-avatar"
                                />
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