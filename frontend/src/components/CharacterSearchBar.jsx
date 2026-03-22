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
    const [selectedIndex, setSelectedIndex] = useState(-1);
    const listRef = React.useRef(null);

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
        if (onSelectCharacterId) {
            onSelectCharacterId(characterCard.id);
        }
        setSearchTerm('');
        setIsOpen(false);
    };

    const handleKeyDown = (e) => {
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
                onKeyDown={handleKeyDown}
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