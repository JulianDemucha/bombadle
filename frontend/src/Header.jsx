import React from 'react';
import './style/Header.css';
import './style/login-button.css';

const handleImageError = (e) => {
    //todo make placeholders for all img / buttons
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function Header() {
    return (
        <>
            <header className="header-container">
                <img
                    src="/img/bombadle_logo.png"
                    alt="logo"
                    onError={handleImageError}
                    className="logo logo-desktop"
                />

                <button className="image-button login-desktop" type="button">
                    <img
                        src="/img/LoginButton.png"
                        alt="Zaloguj się"
                        onError={handleImageError}
                    />
                </button>

            </header>
            <img
                src="/img/bombadle_logo_mobile.png"
                alt="logoMobile"
                onError={handleImageError}
                className="logo logo-mobile"
            />

        </>
    );
}

export default Header;