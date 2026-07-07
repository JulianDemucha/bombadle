import React from 'react';
import '../style/logo.css';
import BackArrowButton from "./BackArrowButton.jsx";
import NavImgButton from "./NavImgButton.jsx";

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function AuthHeader() {
    return (
        <header className="header-container auth-header" style={{ width: '100%', marginBottom: '30px' }}>
            <BackArrowButton />

            <NavImgButton
                to="/"
                imgSrc="/src/assets/bombadle_logo.png"
                altText="logo"
                className="logo logo-desktop"
                onError={handleImageError}
            />
            <NavImgButton
                to="/"
                imgSrc="/src/assets/bombadle_logo_mobile.png"
                altText="logoMobile"
                className="logo logo-mobile"
                onError={handleImageError}
            />
        </header>
    );
}

export default AuthHeader;
