import React from 'react';
import '../style/logo.css';
import '../pages/LoginRegisterPage/login-button.css';
import '../pages/PlayerSettingsPage/profile-button.css';
import NavImgButton from "./NavImgButton.jsx";

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function Header({logoClassName}) {
    logoClassName = logoClassName || 'logo logo-desktop';
    return (
        <header className="header-container">

            {/*     LOGO     */}
            <NavImgButton
                to="/"
                imgSrc="src/assets/bombadle_logo.png"
                altText="logo"
                className={logoClassName}
                onError={handleImageError}
            />

            {/*     LOGIN BUTTON     */}
            <NavImgButton
                to="/login"
                imgSrc="src/assets/buttons/login_button.png"
                altText="Zaloguj się"
                className="image-button login-desktop"
                onError={handleImageError}
                hideIfAuthenticated={true}
            />

            {/*     PROFILE SETTINGS BUTTON     */}
            <NavImgButton
                to="/profile"
                imgSrc="src/assets/buttons/profile_button.png"
                altText="Ustawienia Profilu"
                className="image-button profile-button"
                onError={handleImageError}
                hideIfNotAuthenticated={true}
            />

            {/*     LOGO MOBILE     */}
            <NavImgButton
                to="/"
                imgSrc="src/assets/bombadle_logo_mobile.png"
                altText="logoMobile"
                className="logo logo-mobile"
                onError={handleImageError}
            />
        </header>
    );
}

export default Header;