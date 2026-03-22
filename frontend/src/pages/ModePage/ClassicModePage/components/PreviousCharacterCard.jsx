import React from 'react';

function PreviousCharacterCard() {
    return (
        <div className="previous-character-slot">
            <span className="previous-character-label">Poprzednia postacia byl:</span>
            <div className="previous-character-card">
                <div className="previous-character-player">
                    <img
                        src="/images/character_cards/kapitan_bomba.jpg"
                        alt="Kapitan Bomba"
                        className="previous-character-avatar"
                    />
                    <span className="previous-character-name">Kapitan Bomba</span>
                </div>
            </div>
        </div>
    );
}

export default PreviousCharacterCard;

