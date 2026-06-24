import React, { useState, useEffect } from 'react';
import { apiFetch } from '../../../../api/api';
import GlobalLoader from '../../../../components/GlobalLoader.jsx';

function PreviousCharacterCard({ endpoint = '/api/character-card/CLASSIC/previous-character-card' }) {
    const [previousCharacter, setPreviousCharacter] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchPreviousCharacter = async () => {
            try {
                setLoading(true);
                const response = await apiFetch(endpoint);
                setPreviousCharacter(response.data);
            } catch (err) {
                setError('Nie udało się załadować poprzedniej postaci.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchPreviousCharacter();
    }, [endpoint]);

    if (loading) {
        return (
            <div className="previous-character-slot" style={{ minHeight: '80px', justifyContent: 'center' }}>
                <GlobalLoader text="Ładowanie..." small />
            </div>
        );
    }

    if (error || !previousCharacter) {
        return (
            <div className="previous-character-slot">
                <span className="previous-character-label">{error || 'Brak danych.'}</span>
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