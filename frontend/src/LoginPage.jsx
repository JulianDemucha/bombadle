/* css komponentu bazowany na https://freefrontend.com/css-login-forms/ */
import './style/login-page.css';
import './style/GoogleButton.css'
import './style/logo.css'
import React from "react";
import Footer from "./Footer.jsx";
import NavImgButton from "./ImgNavButton.jsx";

const handleImageError = (e) => {
    //todo make placeholders for all img / buttons
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

function LoginPage() {
    return (<>

        {/*     LOGO     */}
        <NavImgButton
            to="/"
            imgSrc="/img/bombadle_logo.png"
            altText="logo"
            className="logo logo-desktop"
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
        <div className="login-page">



            <div className="login-container">
                <h1>LOGOWANIE</h1>

                <div className="input-group">
                    <label htmlFor="email">EMAIL</label>
                    <input type="email" id="email" placeholder="twoj@email.com"/>
                </div>

                <div className="input-group">
                    <label htmlFor="password">HASŁO</label>
                    <input type="password" id="password" placeholder="••••••••"/>
                </div>

                <button type="submit">ZALOGUJ SIĘ</button>

                <div className="divider">LUB</div>

                <button type="button" className="login-with-google-btn">
                    ZALOGUJ SIĘ PRZEZ GOOGLE
                </button>

                <div className="loginFooter">
                    Nie masz konta? <a href="/register">Zarejestruj się</a>
                </div>
            </div>
            <Footer className="defFooter"/>
        </div>
    </>)
}

export default LoginPage;