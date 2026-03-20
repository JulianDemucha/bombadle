/* css komponentu bazowany na https://freefrontend.com/css-login-forms/ */
import './login-register-page.css';
import './GoogleButton.css'
import '../../style/logo.css'
import React, {useState} from "react";
import Footer from "../../components/Footer.jsx";
import NavImgButton from "../../components/NavImgButton.jsx";
import axios from "axios";
import {setupSilentRefresh} from "../../api/axios.js";
import {useNavigate} from "react-router-dom";
import {useAuth} from "../../auth/UseAuth.jsx";

const handleImageError = (e) => {
    //todo make placeholders for all img / buttons
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};
const validateEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [errors, setErrors] = useState({
        email: "",
        password: "",
        general: ""
    });
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();
    const {reload} = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!email || !password) {
            setErrors(prev => ({...prev, general: "Wypełnij wymagane pola."}));
            return;
        }

        if (!validateEmail(email)) {
            setErrors(prev => ({...prev, email: "Nieprawidłowy adres e-mail."}));
            return;
        }

        setLoading(true);

        try {
            const res = await axios.post("/api/auth/authenticate", {email, password});

            if (res.status === 201 || res.status === 200) {
                await reload(); // Reload user data to update auth state immediately
                navigate("/");
                setupSilentRefresh();
            }

        } catch (err) {
            if (err?.response) {
                const {status, data} = err.response;
                if (status === 401) {
                    setErrors(prev => ({...prev, general: data?.message || "Nieprawidłowy email lub hasło."}));
                } else if (status === 409) {

                    setErrors(prev => ({...prev, general: data?.message || "Błąd podczas logowania."}));
                }
            } else {
                setErrors(prev => ({...prev, general: "Błąd połączenia z serwerem. Spróbuj ponownie."}));
            }
        } finally {
            setLoading(false);
        }

    };
    return (<>

        {/*     LOGO     */}
        <NavImgButton
            to="/"
            imgSrc="src/assets/bombadle_logo.png"
            altText="logo"
            className="logo logo-desktop"
            onError={handleImageError}
        />

        {/*     LOGO MOBILE     */}
        <NavImgButton
            to="/"
            imgSrc="src/assets/bombadle_logo_mobile.png"
            altText="logoMobile"
            className="logo logo-mobile"
            onError={handleImageError}
        />
        <div className="login-register-page">
            <form className="login-container" onSubmit={handleSubmit} noValidate>
                <h1>LOGOWANIE</h1>

                <div className="input-group">
                    <label htmlFor="email">EMAIL</label>
                    <input
                        type="email"
                        id="email"
                        placeholder="twoj@email.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        aria-describedby="email-error"
                    />
                    {errors.email && <div id="email-error" className="field-error">{errors.email}</div>}
                </div>

                <div className="input-group">
                    <label htmlFor="password">HASŁO</label>
                    <input
                        type="password"
                        id="password"
                        placeholder="••••••••"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        aria-describedby="password-error"
                    />
                </div>

                {errors.general && <div className="form-error" role="alert">{errors.general}</div>}
                <button type="submit" disabled={loading}>
                    {loading ? "Ładowanie..." : "ZALOGUJ SIĘ"}
                </button>

                <div className="divider">LUB</div>
                <div aria-live="polite" className="form-message">
                </div>
                <button type="button" className="login-with-google-btn">
                    ZALOGUJ SIĘ PRZEZ GOOGLE
                </button>

                <div className="loginFooter">
                    Nie masz konta? <a href="/register">Zarejestruj się</a>
                </div>
            </form>
            <Footer/>
        </div>
    </>)
}

export default LoginPage;