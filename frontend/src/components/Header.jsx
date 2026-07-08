import React from 'react';
import '../style/logo.css';
import '../pages/LoginRegisterPage/login-button.css';
import '../pages/PlayerSettingsPage/profile-button.css';
import NavImgButton from "./NavImgButton.jsx";
import BackArrowButton from "./BackArrowButton.jsx";
import logoImage from '../assets/bombadle_logo.png';
import logoMobileImage from '../assets/bombadle_logo_mobile.png';
import loginButtonImage from '../assets/buttons/login_button.png';
import profileButtonImage from '../assets/buttons/profile_button.png';

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function Header({logoClassName, showBackButton}) {
    logoClassName = logoClassName || 'logo logo-desktop';
    return (
        <header className="header-container">

            {/*     BACK BUTTON     */}
            {showBackButton && <BackArrowButton />}

            {/*     LOGO     */}
            <NavImgButton
                to="/"
                imgSrc={logoImage}
                altText="logo"
                className={logoClassName}
                onError={handleImageError}
            />

            {/*     LOGIN BUTTON     */}
            <NavImgButton
                to="/login"
                imgSrc={loginButtonImage}
                altText="Zaloguj się"
                className="image-button login-desktop"
                onError={handleImageError}
                hideIfAuthenticated={true}
            />

            {/*     PROFILE SETTINGS BUTTON     */}
            <NavImgButton
                to="/profile"
                imgSrc={profileButtonImage}
                altText="Ustawienia Profilu"
                className="image-button profile-button"
                onError={handleImageError}
                hideIfNotAuthenticated={true}
            />

            {/*     LOGO MOBILE     */}
            <NavImgButton
                to="/"
                imgSrc={logoMobileImage}
                altText="logoMobile"
                className="logo logo-mobile"
                onError={handleImageError}
            />
        </header>
    );
}

export default Header;