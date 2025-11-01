import React from 'react';
import './style/logo.css';
import './style/login-button.css';
import NavImgButton from "./NavImgButton.jsx";

const handleImageError = (e) => {
    //todo make placeholders for all img / buttons
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function Header() {
    return (
        <header className="header-container">

            {/*     LOGO     */}
            <NavImgButton
                to="/"
                imgSrc="/img/bombadle_logo.png"
                altText="logo"
                className="logo logo-desktop"
                onError={handleImageError}
            />

            {/*     LOGIN BUTTON     */}
            <NavImgButton
                to="/login"
                imgSrc="/img/LoginButton.png"
                altText="Zaloguj się"
                className="image-button login-desktop"
                onError={handleImageError}
            />

            {/*     LOGO MOBILE     */}
            <NavImgButton
                to="/"
                imgSrc="/img/bombadle_logo_mobile.png"
                altText="logoMobile"
                className="logo logo-mobile"
                onError={handleImageError}
            />
        </header>
    );
}

export default Header;