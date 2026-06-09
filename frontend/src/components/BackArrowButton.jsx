import React from 'react';
import { useNavigate } from 'react-router-dom';
import './style/BackArrowButton.css';

const BackArrowButton = () => {
    const navigate = useNavigate();
    return (
        <button
            type="button"
            className="back-arrow-button"
            onClick={() => navigate(-1)}
            aria-label="Powrót do poprzedniej strony"
        >
            <svg
                className="back-arrow-svg"
                viewBox="0 0 24 24"
                fill="none"
            >
                <path d="M15 18l-6-6 6-6" />
            </svg>
        </button>
    );
};

export default BackArrowButton;
