import React, {useState} from 'react';
import './avatar-picker.css';
import {AVATAR_LIST, getAvatarUrl} from "./AvatarList.js";


/**
 * Inner component for AvatarPicker
 */
function AvatarModal({onClose, onSelect}) {
    const handleSelect = (avatar) => {
        onSelect(avatar); //save chosen avatar
        onClose();
    };

    return (
        <div
            className="modal-overlay"
            onClick={onClose} //background click closes modal
        >
            <div
                className="modal-content"
                onClick={(e) => e.stopPropagation()}
                // prevent closing after clicking on modal content
            >
                <h3 className="modal-title">Wybierz awatar</h3>

                <div className="avatar-grid">
                    {AVATAR_LIST.map((avatar) => (
                        <img
                            key={avatar}
                            src={getAvatarUrl(avatar)}
                            alt="Avatar option"
                            className="avatar-option"
                            onClick={() => handleSelect(avatar)}
                        />
                    ))}
                </div>

                <button
                    type="button"
                    className="modal-close-btn"
                    onClick={onClose}
                    aria-label="Zamknij"
                >
                    &times;
                </button>
            </div>
        </div>
    );
}

/**
 * Button for choosing avatar
 * needs one property: `onAvatarSelect`
 */
function AvatarPicker({onAvatarSelect}) {
    const [isModalOpen, setIsModalOpen] = useState(false);

    const handleSaveAvatar = (avatarUrl) => {
        // Return the value to outer component
        if (onAvatarSelect) {
            onAvatarSelect(avatarUrl);
        }
        console.log('Wybrano nowy awatar:', avatarUrl);
    };

    return (
        <>
            <button
                type="button"
                className="btn-link"
                onClick={() => setIsModalOpen(true)}
            >
                Zmień awatar
            </button>

            {isModalOpen && (
                <AvatarModal
                    onClose={() => setIsModalOpen(false)}
                    onSelect={handleSaveAvatar}
                />
            )}
        </>
    );
}

export default AvatarPicker;
