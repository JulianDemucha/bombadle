import React, { useEffect, useMemo, useState, useRef } from 'react';
import './style/CharacterSearchBar.css';
import { apiFetch } from '../api/api.js';
import { normalizeForSearch } from '../utils/textNormalization.js';

const CharacterSearchBar = ({ onSelectCharacterId, disabled = false }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [characterCards, setCharacterCards] = useState([]);
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const listRef = useRef(null);
    const inputRef = useRef(null);

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
        const term = normalizeForSearch(searchTerm.trim());
        if (!term) return [];

        const matchesCard = (card) =>
            normalizeForSearch(card.name).includes(term) ||
            (card.aliases || []).some((alias) => normalizeForSearch(alias).includes(term));

        return characterCards
            .filter(matchesCard)
            .slice(0, 10);
    }, [searchTerm, characterCards]);

    useEffect(() => {
        setSelectedIndex(-1);
    }, [filteredCards, isOpen]);

    useEffect(() => {
        if (selectedIndex >= 0 && listRef.current) {
            const listItems = listRef.current.children;
            if (listItems[selectedIndex]) {
                listItems[selectedIndex].scrollIntoView({ block: 'nearest' });
            }
        }
    }, [selectedIndex]);

    const handleSelect = (characterCard) => {
        if (disabled) return;
        if (onSelectCharacterId) {
            onSelectCharacterId(characterCard.id);
        }
        
        setSearchTerm('');
        setIsOpen(false);
        
        // Używamy setTimeout, aby React zdążył wyczyścić input (stan searchTerm)
        // Zanim wywoła się zdarzenie onFocus, które ponownie otworzyłoby listę
        setTimeout(() => {
            if (inputRef.current) {
                inputRef.current.focus();
            }
        }, 0);
    };

    const handleKeyDown = (e) => {
        if (disabled) return;
        if (!isOpen) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            if (filteredCards.length > 0) {
                setSelectedIndex((prev) => 
                    prev < filteredCards.length - 1 ? prev + 1 : 0
                );
            }
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            if (filteredCards.length > 0) {
                setSelectedIndex((prev) => 
                    prev > 0 ? prev - 1 : filteredCards.length - 1
                );
            }
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (selectedIndex >= 0 && selectedIndex < filteredCards.length) {
                handleSelect(filteredCards[selectedIndex]);
            }
        } else if (e.key === 'Escape') {
            setIsOpen(false);
        }
    };

    return (
        <div className="search-container">
            <input
                ref={inputRef}
                type="text"
                className="search-input-image"
                placeholder="Wpisz nazwę..."
                value={searchTerm}
                onChange={(e) => {
                    const value = e.target.value;
                    setSearchTerm(value);
                    setIsOpen(value.trim().length > 0);
                }}
                onFocus={(e) => setIsOpen(e.target.value.trim().length > 0)}
                onKeyDown={handleKeyDown}
                disabled={disabled}
            />

            {isOpen && (
                <div className="search-dropdown-background">

                    <ul className="search-dropdown-scrollbox" ref={listRef}>

                        {filteredCards.length === 0 && (
                            <li className="search-dropdown-empty">Brak wynikow</li>
                        )}

                        {filteredCards.map((characterCard, index) => (
                            <li key={characterCard.id} 
                                className={`search-dropdown-item ${index === selectedIndex ? 'selected' : ''}`}
                                onMouseEnter={() => setSelectedIndex(index)}
                                onClick={() => handleSelect(characterCard)}
                            >
                                <img
                                    src={characterCard.imageSrc || characterCard.image_src}
                                    alt={characterCard.name}
                                    className="dropdown-avatar"
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