import React from 'react';
import '../style/logo.css';
import { Link } from 'react-router-dom';
import BackArrowButton from "./BackArrowButton.jsx";

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function AuthHeader() {
    return (
        <header className="header-container auth-header">
            <div style={{ position: 'absolute', left: '20px', top: '50%', transform: 'translateY(-50%)' }}>
                <BackArrowButton />
            </div>

            <Link to="/" style={{ display: 'block', textDecoration: 'none' }}>
                <img
                    src="/src/assets/bombadle_logo.png"
                    alt="logo"
                    className="logo logo-desktop"
                    onError={handleImageError}
                />
                <img
                    src="/src/assets/bombadle_logo_mobile.png"
                    alt="logoMobile"
                    className="logo logo-mobile"
                    onError={handleImageError}
                />
            </Link>
        </header>
    );
}

export default AuthHeader;
