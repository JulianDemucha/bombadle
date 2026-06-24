import React from 'react';
import './MergePrompt.css';

export default function MergePrompt({ isOpen, onConfirm, onDecline }) {
    if (!isOpen) return null;

    return (
        <div className="merge-prompt" role="dialog" aria-modal="true" aria-labelledby="merge-prompt__title">
            <button
                type="button"
                className="merge-prompt__backdrop"
                aria-label="Zamknij"
                onClick={onDecline}
            />
            <div className="merge-prompt__dialog">
                <h2 id="merge-prompt__title" className="merge-prompt__title">
                    Zapisać postępy z gry jako gość?
                </h2>
                <p className="merge-prompt__message">
                    Wykryliśmy wyniki zdobyte przed zalogowaniem. Czy chcesz przypisać je do swojego konta?
                </p>
                <div className="merge-prompt__actions">
                    <button
                        type="button"
                        className="merge-prompt__button merge-prompt__button--secondary"
                        onClick={onDecline}
                    >
                        Nie, dziękuję
                    </button>
                    <button
                        type="button"
                        className="merge-prompt__button merge-prompt__button--primary"
                        onClick={onConfirm}
                    >
                        Tak, zapisz
                    </button>
                </div>
            </div>
        </div>
    );
}
