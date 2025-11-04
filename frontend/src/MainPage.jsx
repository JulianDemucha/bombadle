import React from 'react';
import './style/img-buttons.css';
import NavImgButton from "./NavImgButton.jsx";
import Footer from "./Footer.jsx";
import Header from "./Header.jsx";

function MainPage() {
    const handleImageError = (e) => {
        //todo make placeholders for all img / buttons
        e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
    };

    return (
        <>
            <Header/>
            <div className="buttons-container">

                <NavImgButton
                    to="/"
                    imgSrc="/img/button_classic.png"
                    altText="Tryb 'Klasyczny'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/"
                    imgSrc="/img/button_quote_mode.png"
                    altText="Tryb 'Cytaty'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/"
                    imgSrc="/img/button_training_mode.png"
                    altText="Tryb 'Trening"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />


                <NavImgButton
                    to="/login"
                    imgSrc="/img/LoginButton.png"
                    altText="Zaloguj się"
                    className="image-button login-mobile"
                    onError={handleImageError}
                    hideIfAuthenticated={true}
                />

            </div>
            <Footer />
        </>
    );
}

export default MainPage;
