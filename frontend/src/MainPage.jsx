import React from 'react';
import './style/img-buttons.css';

function MainPage() {
    const handleImageError = (e) => {
        //todo make placeholders for all img / buttons
        e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
    };

    return (
        <>


            <div className="buttons-container">

                <button className="image-button image-button-responsive" type="button">
                    <img
                        src="/img/button_classic.png"
                        alt="Tryb 'Klasyczny'"
                        onError={handleImageError}
                    />
                </button>


                <button className="image-button image-button-responsive" type="button">
                    <img
                        src="/img/button_quote_mode.png"
                        alt="Tryb 'Cytaty'"
                        onError={handleImageError}
                    />
                </button>

                <button className="image-button image-button-responsive" type="button">
                    <img
                        src="/img/button_training_mode.png"
                        alt="Tryb 'Trening"
                        onError={handleImageError}
                    />
                </button>

                <button className="image-button login-mobile" type="button">
                    <img
                        src="/img/LoginButton.png"
                        alt="Zaloguj się"
                        onError={handleImageError}
                    />
                </button>

                {/*<div className="button-mode-choice-container">*/}
                {/*    <img*/}
                {/*        src="/img/button_mode_choice.png"*/}
                {/*        alt="Wybierz tryb gry"*/}
                {/*        onError={handleImageError}*/}
                {/*    />*/}
                {/*</div>*/}
            </div>
        </>
    );
}

export default MainPage;
