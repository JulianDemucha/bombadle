import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../../../api/api';

function PreviousCharacterCard() {
    const [previousCharacter, setPreviousCharacter] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchPreviousCharacter = async () => {
            try {
                setLoading(true);
                const response = await apiFetch('/api/character-card/previous-character-card');
                setPreviousCharacter(response.data);
            } catch (err) {
                setError('Nie udało się załadować poprzedniej postaci.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchPreviousCharacter();
    }, []);

    if (loading) {
        return (
            <div className="previous-character-slot">
                <span className="previous-character-label">Ładowanie...</span>
            </div>
        );
    }

    if (error || !previousCharacter) {
        return (
            <div className="previous-character-slot">
                <span className="previous-character-label">{error || 'Brak danych o poprzedniej postaci.'}</span>
            </div>
        );
    }

    return (
        <div className="previous-character-slot">
            <span className="previous-character-label">Poprzednią postacią był:</span>
            <div className="previous-character-card">
                <div className="previous-character-player">
                    <img
                        src={previousCharacter.imageSrc}
                        alt={previousCharacter.name}
                        className="previous-character-avatar"
                    />
                    <span className="previous-character-name">{previousCharacter.name}</span>
                </div>
            </div>
        </div>
    );
}

export default PreviousCharacterCard;
