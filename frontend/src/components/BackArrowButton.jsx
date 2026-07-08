import React from 'react';
import { useNavigate } from 'react-router-dom';
import './style/BackArrowButton.css';
import backArrowButtonImage from '../assets/buttons/back_arrow_button.png';

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/32x32/0D1D14/FFFFFF?text=%3C';
};

const BackArrowButton = () => {
    const navigate = useNavigate();
    return (
        <button
            type="button"
            className="nav-back-button"
            onClick={() => navigate(-1)}
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
