import React, { useRef, useEffect } from 'react';
import ConfirmDialog from '../ConfirmDialog/ConfirmDialog.jsx';
import { TITLE_MAX, DESC_MAX, TITLE_MIN, DESC_MIN } from './useFeedback.js';
import './FeedbackModal.css';

export default function FeedbackModal({ hook }) {
    const descRef = useRef(null);

    const {
        isOpen,
        mode,
        title,
        description,
        setTitle,
        setDescription,
        isSubmitting,
        isValid,
        handleClose,
        handleSubmit,
        successOpen,
        dismissSuccess,
        errorOpen,
        dismissError,
        navigateToLogin,
    } = hook;

    useEffect(() => {
        const el = descRef.current;
        if (!el) return;
        el.style.height = 'auto';
        el.style.height = el.scrollHeight + 'px';
    }, [description]);

    return (
        <>
            {isOpen && (
                <div className="feedback-modal" role="dialog" aria-modal="true" aria-labelledby="feedback-modal__title">
                    <button
                        type="button"
                        className="feedback-modal__backdrop"
                        aria-label="Zamknij"
                        onClick={handleClose}
                    />
                    <div className="feedback-modal__panel">
                        <h2 id="feedback-modal__title" className="feedback-modal__title">Wyślij nam wiadomość</h2>

                        {mode === 'form' ? (
                            <>
                                <p className="feedback-modal__subtitle">
                                    Brakuje jakiejś postaci? Napotkałeś błąd? Napisz o tym poniżej.
                                </p>
                                <div className="feedback-modal__field">
                                    <label className="feedback-modal__label" htmlFor="feedback-modal-title">Tytuł</label>
                                    <input
                                        id="feedback-modal-title"
                                        type="text"
                                        className="feedback-modal__input"
                                        value={title}
                                        onChange={e => setTitle(e.target.value)}
                                        disabled={isSubmitting}
                                        maxLength={TITLE_MAX}
                                    />
                                    <span className={`feedback-modal__char-count${title.length >= TITLE_MAX ? ' feedback-modal__char-count--limit' : ''}`}>
                                        {title.length}/{TITLE_MAX}
                                    </span>
                                </div>
                                <div className="feedback-modal__field">
                                    <label className="feedback-modal__label" htmlFor="feedback-modal-description">Opis</label>
                                    <textarea
                                        id="feedback-modal-description"
                                        ref={descRef}
                                        className="feedback-modal__textarea"
                                        value={description}
                                        onChange={e => setDescription(e.target.value)}
                                        disabled={isSubmitting}
                                        maxLength={DESC_MAX}
                                    />
                                    <span className={`feedback-modal__char-count${description.length >= DESC_MAX ? ' feedback-modal__char-count--limit' : ''}`}>
                                        {description.length}/{DESC_MAX}
                                    </span>
                                </div>
                                <div className="feedback-modal__actions">
                                    <button
                                        type="button"
                                        className="feedback-modal__button feedback-modal__button--secondary"
                                        onClick={handleClose}
                                        disabled={isSubmitting}
                                    >
                                        Anuluj
                                    </button>
                                    <button
                                        type="button"
                                        className="feedback-modal__button feedback-modal__button--primary"
                                        onClick={handleSubmit}
                                        disabled={isSubmitting || !isValid}
                                    >
                                        Wyślij
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <p className="feedback-modal__message">
                                    Musisz być zalogowany, aby wysłać opinię.
                                </p>
                                <div className="feedback-modal__actions">
                                    <button
                                        type="button"
                                        className="feedback-modal__button feedback-modal__button--secondary"
                                        onClick={handleClose}
                                    >
                                        Zamknij
                                    </button>
                                    <button
                                        type="button"
                                        className="feedback-modal__button feedback-modal__button--primary"
                                        onClick={navigateToLogin}
                                    >
                                        Zaloguj się
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            )}

            <ConfirmDialog
                isOpen={successOpen}
                title="Dziękujemy!"
                message="Dziękujemy za opinię."
                confirmLabel="OK"
                onConfirm={dismissSuccess}
            />
            <ConfirmDialog
                isOpen={errorOpen}
                title="Błąd"
                message="Nie udało się wysłać opinii. Spróbuj ponownie."
                confirmLabel="OK"
                onConfirm={dismissError}
            />
        </>
    );
}
