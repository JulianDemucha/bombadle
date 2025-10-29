/* css komponentu bazowany na https://freefrontend.com/css-login-forms/ */
import './style/login-register-page.css';
import './style/GoogleButton.css';
import './style/logo.css';
import React, {useCallback, useEffect, useRef, useState} from "react";
import Footer from "./Footer.jsx";
import NavImgButton from "./ImgNavButton.jsx";
import axios from "axios";

const handleImageError = (e) => {
    e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
};

const validateEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
const MIN_PASSWORD_LEN = 8;
const MAX_PASSWORD_LEN = 24;
const MIN_USERNAME_LEN = 3;
const MAX_USERNAME_LEN = 16;

/**
 *  { exists: boolean } OR ERROR ON ENDPOINT
 */
function useDebouncedCheck({ value, minLen = 1, url, fieldSetter, delay = 500, fieldName}) {
    const controllerRef = useRef(null);
    const mountedRef = useRef(true);
    useEffect(() => {
        mountedRef.current = true;
        return () => { mountedRef.current = false; };
    }, []);

    useEffect(() => {
        if (!value || value.length < minLen) {
            fieldSetter("");
            return;
        }

        //TODO CHECK CONTROLLER
        const controller = new AbortController();
        controllerRef.current = controller;
        let active = true;

        const timer = setTimeout(async () => {
            try {
                const res = await axios.get(url, { signal: controller.signal });
                if (!mountedRef.current || !active) return;
                if (res.data?.exists) {
                    fieldSetter(
                        fieldName === "email" ?
                            "Ten email jest już zajęty!"
                            :
                            "Ta nazwa użytkownika jest już zajęta!",
                    );
                } else {
                    fieldSetter("");
                }
            } catch (err) {
                if (err?.code === "ERR_CANCELED" || err?.name === "CanceledError" || err?.message === "canceled") {
                    return;
                }
                console.error("Check request failed:", err);
            }
        }, delay);

        return () => {
            active = false;
            clearTimeout(timer);
            if (controllerRef.current) {
                controllerRef.current.abort();
            }
        };
    }, [value, minLen, url, fieldSetter, delay, fieldName]);
}

function RegisterPage() {
    const [email, setEmail] = useState("");
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [acceptedTerms, setAcceptedTerms] = useState(false);
    const [acceptedPrivacy, setAcceptedPrivacy] = useState(false);

    const [errors, setErrors] = useState({
        email: "",
        username: "",
        password: "",
        confirmPassword: "",
        general: ""
    });

    const setUsernameError = useCallback((msg) => {
        setErrors(prev => ({ ...prev, username: msg }));
    }, [setErrors]);

    const setEmailError = useCallback((msg) => {
        setErrors(prev => ({ ...prev, email: msg }));
    }, [setErrors]);

    const [loading, setLoading] = useState(false);
    const [successMessage, setSuccessMessage] = useState("");

    useDebouncedCheck({
        value: username,
        minLen: 3,
        url: `/api/auth/check/username?username=${encodeURIComponent(username)}`,
        fieldSetter: setUsernameError,
        fieldName: "username"
    });

    useDebouncedCheck({
        value: email,
        minLen: 5,
        url: `/api/auth/check/email?email=${encodeURIComponent(email)}`,
        fieldSetter: setEmailError,
        fieldName: "email"
    });

    const handleSubmit = async (e) => {
        e.preventDefault();
        // reset
        setErrors({ email: "", username: "", password: "", confirmPassword: "", general: "" });
        setSuccessMessage("");

        // client-side validations
        if (!email || !username || !password) {
            setErrors(prev => ({ ...prev, general: "Wypełnij wymagane pola." }));
            return;
        }
        if (username.length < MIN_USERNAME_LEN) {
            setErrors(prev => ({ ...prev, username: `Nazwa użytkownika musi mieć co najmniej ${MIN_USERNAME_LEN} znaków.` }));
            return;
        }
        if (username.length > MAX_USERNAME_LEN) {
            setErrors(prev => ({ ...prev, username: `Nazwa użytkownika musi mieć co najwyżej ${MAX_USERNAME_LEN} znaków.` }));
            return;
        }
        if (password.length < MIN_PASSWORD_LEN) {
            setErrors(prev => ({ ...prev, password: `Hasło musi mieć co najmniej ${MIN_PASSWORD_LEN} znaków.` }));
            return;
        }
        if (password.length > MAX_PASSWORD_LEN) {
            setErrors(prev => ({ ...prev, password: `Hasło musi mieć co najwyżej ${MAX_PASSWORD_LEN} znaków.` }));
            return;
        }
        if (!validateEmail(email)) {
            setErrors(prev => ({ ...prev, email: "Nieprawidłowy adres e-mail." }));
            return;
        }
        if (password !== confirmPassword) {
            setErrors(prev => ({ ...prev, confirmPassword: "Hasła nie są zgodne." }));
            return;
        }
        if (!acceptedTerms || !acceptedPrivacy) {
            setErrors(prev => ({ ...prev, general: "Musisz zaakceptować regulamin i politykę prywatności." }));
            return;
        }



        setLoading(true);
        try {
            const res = await axios.post("/api/auth/register", { email, username, password });

            if (res.status === 201 || res.status === 200) {
                setSuccessMessage(res.data?.message || "Konto zostało utworzone. Możesz się zalogować.");
                setTimeout(() => window.location.href = "/login", 1200);

            } else {
                setErrors(prev => ({ ...prev, general: res.data?.message || "Rejestracja przebiegła częściowo." }));
            }

        } catch (err) {
            if (err?.response) {
                const { status, data } = err.response;
                if (status === 409) {
                    const msg = data?.message || "Email lub nazwa użytkownika już istnieją.";
                    if (data?.field === "email") {
                        setErrors(prev => ({ ...prev, email: msg }));
                    } else if (data?.field === "username") {
                        setErrors(prev => ({ ...prev, username: msg }));
                    } else {
                        setErrors(prev => ({ ...prev, general: msg }));
                    }
                } else if (status === 422 && data?.errors) {
                    setErrors(prev => ({ ...prev, ...data.errors }));
                } else {
                    setErrors(prev => ({ ...prev, general: data?.message || "Błąd podczas rejestracji." }));
                }
            } else if (err?.code === "ERR_CANCELED" || err?.name === "CanceledError") {
                // request anulowany (raczej nie wystąpi w submit)
            } else {
                setErrors(prev => ({ ...prev, general: "Błąd połączenia z serwerem. Spróbuj ponownie." }));
            }
        } finally {
            setLoading(false);
        }
    };


    return (
        <>
            <NavImgButton
                to="/"
                imgSrc="/img/bombadle_logo.png"
                altText="logo"
                className="logo logo-desktop"
                onError={handleImageError}
            />
            <NavImgButton
                to="/"
                imgSrc="/img/bombadle_logo_mobile.png"
                altText="logoMobile"
                className="logo logo-mobile"
                onError={handleImageError}
            />

            <div className="login-page">
                <form className="login-container" onSubmit={handleSubmit} noValidate>
                    <h1>REJESTRACJA</h1>

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
                        <label htmlFor="username">NAZWA UŻYTKOWNIKA</label>
                        <input
                            type="text"
                            id="username"
                            placeholder="Kapitan Bomba"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            aria-describedby="username-error"
                        />
                        {errors.username && <div id="username-error" className="field-error">{errors.username}</div>}
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
                        {errors.password && <div id="password-error" className="field-error">{errors.password}</div>}
                    </div>

                    <div className="input-group">
                        <label htmlFor="confirmPassword">POWTÓRZ HASŁO</label>
                        <input
                            type="password"
                            id="confirmPassword"
                            placeholder="••••••••"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                            aria-describedby="confirm-error"
                        />
                        {errors.confirmPassword && <div id="confirm-error" className="field-error">{errors.confirmPassword}</div>}
                    </div>

                    <div className="checkboxes">
                        <label className="custom-checkbox" htmlFor="terms">
                            <input
                                id="terms"
                                type="checkbox"
                                checked={acceptedTerms}
                                onChange={(e) => setAcceptedTerms(e.target.checked)}
                            />
                            <span className="checkmark" aria-hidden="true" />
                            <span className="checkbox-text">
                Akceptuję <a href="/regulamin.html" target="_blank" rel="noopener noreferrer">regulamin</a>
              </span>
                        </label>

                        <label className="custom-checkbox" htmlFor="privacy">
                            <input
                                id="privacy"
                                type="checkbox"
                                checked={acceptedPrivacy}
                                onChange={(e) => setAcceptedPrivacy(e.target.checked)}
                            />
                            <span className="checkmark" aria-hidden="true" />
                            <span className="checkbox-text">
                Akceptuję <a href="/privacy_policy.html" target="_blank" rel="noopener noreferrer">politykę prywatności / RODO</a>
              </span>
                        </label>
                    </div>

                    <div aria-live="polite" className="form-message">
                        {errors.general && <div className="form-error" role="alert">{errors.general}</div>}
                        {successMessage && <div className="form-success" role="status">{successMessage}</div>}
                    </div>

                    <button type="submit" className="submit-btn" disabled={loading}>
                        {loading ? "Ładowanie..." : "ZAREJESTRUJ SIĘ"}
                    </button>

                    <div className="divider">LUB</div>

                    <button type="button" className="login-with-google-btn">
                        ZALOGUJ SIĘ PRZEZ GOOGLE
                    </button>

                    <div className="loginFooter">
                        Masz już konto? <a href="/login">Zaloguj się</a>
                    </div>
                </form>

                <Footer />
            </div>
        </>
    );
}

export default RegisterPage;
