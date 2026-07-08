import React from 'react';
import './MainPage.css';
import '../../style/img-buttons.css';
import NavImgButton from "../../components/NavImgButton.jsx";
import Footer from "../../components/Footer.jsx";
import Header from "../../components/Header.jsx";
import Top3SuperstreakBoard from "../../components/Top3SuperstreakBoard.jsx";
import DailyResetTimer from "../../components/DailyResetTimer.jsx";
import useSuperstreakTop3 from "./hooks/useSuperstreakTop3.js";
import classicButtonImage from '../../assets/buttons/button_classic.png';
import quoteButtonImage from '../../assets/buttons/button_quote_mode.png';
import imagesButtonImage from '../../assets/buttons/button_images_mode.png';
import loginButtonImage from '../../assets/buttons/login_button.png';

function MainPage() {
    const { topThree, loading } = useSuperstreakTop3();

    const handleImageError = (e) => {
        //todo make placeholders for all img / buttons
        e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
    };

    return (
        <>
            <Header/>
            <DailyResetTimer/>
            <div className="buttons-container">

                <NavImgButton
                    to="/Classic"
                    imgSrc={classicButtonImage}
                    altText="Tryb 'Klasyczny'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/Quotes"
                    imgSrc={quoteButtonImage}
                    altText="Tryb 'Cytaty'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/Images"
                    imgSrc={imagesButtonImage}
                    altText="Tryb 'Zdjęcia'"
                    className="image-button image-button-responsive"
                    onError={handleImageError}
                />

                <NavImgButton
                    to="/login"
                    imgSrc={loginButtonImage}
                    altText="Zaloguj się"
                    className="image-button login-mobile"
                    onError={handleImageError}
                    hideIfAuthenticated={true}
                />

            </div>

            <div className="main-page-superstreak">
                <Top3SuperstreakBoard topThree={topThree} loading={loading}/>
            </div>

            <Footer/>
        </>
    );
}

export default MainPage;