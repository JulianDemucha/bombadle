/* komponent bazowany na https://freefrontend.com/css-login-forms/ */
import './style/login-page.css';
import './style/GoogleButton.css';
import './style/logo.css';
import React from "react";
import Footer from "./Footer.jsx";

function RegisterPage() {
    return (
        <>

            <div className="login-page">
                <img
                    src="/img/bombadle_logo.png"
                    alt="logo"
                    className="logo logo-desktop"
                    style={{maxWidth: '500px', marginBottom: '0.5rem'}}
                />
                <img
                    src="/img/bombadle_logo_mobile.png"
                    alt="logoMobile"
                    className="logo logo-mobile"
                /><br/><br/>
                <div className="login-container">
                    <h1>REJESTRACJA</h1>

                    <div className="input-group">
                        <label htmlFor="email">EMAIL</label>
                        <input type="email" id="email" placeholder="twoj@email.com"/>
                    </div>

                    <div className="input-group">
                        <label htmlFor="username">NAZWA UŻYTKOWNIKA</label>
                        <input type="username" id="username" placeholder="Kapitan Bomba "/>
                    </div>

                    <div className="input-group">
                        <label htmlFor="password">HASŁO</label>
                        <input type="password" id="password" placeholder="••••••••"/>
                    </div>
                    <div className="input-group">
                        <label htmlFor="password">POWTÓRZ HASŁO</label>
                        <input type="password" id="password" placeholder="••••••••"/>
                    </div>


                    <button type="submit">ZAREJESTRUJ SIĘ</button>

                    <div className="divider">LUB</div>

                    <button type="button" className="login-with-google-btn">
                        ZALOGUJ SIĘ PRZEZ GOOGLE
                    </button>

                    <div className="loginFooter">
                        Masz już konto? <a href="/login">Zaloguj się</a>
                    </div>
                </div>
                <Footer className="defFooter"/>
            </div>
        </>
    )
}

export default RegisterPage;