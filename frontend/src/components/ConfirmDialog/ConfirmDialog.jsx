import React from 'react';
import './ConfirmDialog.css';

/**
 * Generic confirmation/info modal. Dumb component — no logic, no API calls.
 * Omit `onCancel` to render a single-button info dialog (e.g. an acknowledgement).
 */
export default function ConfirmDialog({
    isOpen,
    title,
    message,
    confirmLabel = 'Potwierdź',
    cancelLabel = 'Anuluj',
    variant = 'primary',
    onConfirm,
    onCancel,
}) {
    if (!isOpen) return null;

    const dismiss = onCancel ?? onConfirm;

    return (
        <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog__title">
            <button type="button" className="confirm-dialog__backdrop" aria-label="Zamknij" onClick={dismiss} />
            <div className="confirm-dialog__panel">
                {title && (
                    <h2 id="confirm-dialog__title" className="confirm-dialog__title">{title}</h2>
                )}
                {message && <p className="confirm-dialog__message">{message}</p>}
                <div className="confirm-dialog__actions">
                    {onCancel && (
                        <button
                            type="button"
                            className="confirm-dialog__button confirm-dialog__button--secondary"
                            onClick={onCancel}
                        >
                            {cancelLabel}
                        </button>
                    )}
                    <button
                        type="button"
                        className={`confirm-dialog__button confirm-dialog__button--${variant}`}
                        onClick={onConfirm}
                    >
                        {confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
}
