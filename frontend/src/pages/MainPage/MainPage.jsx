import
    React from 'react';
import '../../style/img-buttons.css';
import NavImgButton from "../../components/NavImgButton.jsx";
import Footer from "../../components/Footer.jsx";
import Header from "../../components/Header.jsx";

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
                    to="/Classic"
                    imgSrc="src/assets/buttons/button_classic.png"
                    altText="Tryb 'Klasyczny'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/Quotes"
                    imgSrc="src/assets/buttons/button_quote_mode.png"
                    altText="Tryb 'Cytaty'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/"
                    imgSrc="src/assets/buttons/button_training_mode.png"
                    altText="Tryb 'Trening"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />


                <NavImgButton
                    to="/login"
                    imgSrc="src/assets/buttons/login_button.png"
                    altText="Zaloguj się"
                    className="image-button login-mobile"
                    onError={handleImageError}
                    hideIfAuthenticated={true}
                />

            </div>
            <Footer/>
        </>
    );
}

export default MainPage;
