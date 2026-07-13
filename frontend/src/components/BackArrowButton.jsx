import React from 'react';
import './style/BackArrowButton.css';
import { useBackNavigation } from './BackNavigation.js';
import backArrowButtonImage from '../assets/buttons/back_arrow_button.png';

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/32x32/0D1D14/FFFFFF?text=%3C';
};

const BackArrowButton = () => {
    const goBack = useBackNavigation();
    return (
        <button
            type="button"
            className="nav-back-button"
            onClick={goBack}
            aria-label="Powrót do poprzedniej strony"
        >
            <img
                className="nav-back-button-img"
                src={backArrowButtonImage}
                alt="Wstecz"
                onError={handleImageError}
            />
        </button>
    );
};

export default BackArrowButton;
