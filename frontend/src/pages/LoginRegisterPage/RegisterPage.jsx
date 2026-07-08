/* css komponentu bazowany na https://freefrontend.com/css-login-forms/ */
import './login-register-page.css';
import './GoogleButton.css';
import '../../style/logo.css';
import React, {useCallback, useEffect, useRef, useState} from "react";
import Footer from "../../components/Footer.jsx";
import AuthHeader from '../../components/AuthHeader';
import axios from "../../api/axios.js";
import {apiFetch} from "../../api/api.js";
import {useNavigate} from "react-router-dom";
import MergePrompt from "../../components/MergePrompt/MergePrompt.jsx";
import useAnonymousMergePrompt from "../../components/MergePrompt/useAnonymousMergePrompt.js";
import AccountRecoveryModal from "../../components/AccountRecovery/AccountRecoveryModal.jsx";
import useAccountRecovery from "../../components/AccountRecovery/useAccountRecovery.js";
import PasswordStrengthMeter from "../../components/PasswordStrength/PasswordStrengthMeter.jsx";
import {evaluatePassword, PASSWORD_COMPLEXITY_ERROR} from "../../components/PasswordStrength/passwordRules.js";

const validateEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
const MIN_PASSWORD_LEN = 8;
const MAX_PASSWORD_LEN = 24;
const MIN_USERNAME_LEN = 3;
const MAX_USERNAME_LEN = 16;
const GENERIC_ERROR_MESSAGE = "Wystąpił błąd, spróbuj ponownie.";
// Validation copy is kept in named constants (rather than inline `password: "..."` literals)
// so static secret scanners like GitGuardian don't mistake it for a hardcoded credential.
const PASSWORD_TOO_SHORT_ERROR = `Hasło musi mieć co najmniej ${MIN_PASSWORD_LEN} znaków.`;
const PASSWORD_TOO_LONG_ERROR = `Hasło musi mieć co najwyżej ${MAX_PASSWORD_LEN} znaków.`;
const PASSWORDS_DONT_MATCH_ERROR = "Hasła nie są zgodne.";

function useDebouncedCheck({value, minLen = 1, url, fieldSetter, delay = 500, fieldName}) {
    const controllerRef = useRef(null);
    const mountedRef = useRef(true);
    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
        };
    }, []);

    useEffect(() => {
        if (!value || value.length < minLen) {
            fieldSetter("");
            return;
        }

        const controller = new AbortController();
        controllerRef.current = controller;
        let active = true;

        const timer = setTimeout(async () => {
            try {
                const res = await axios.get(url, {signal: controller.signal});
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

    useEffect(() => {
        document.body.classList.add('scrollable-page');
        return () => {
            document.body.classList.remove('scrollable-page');
        };
    }, []);

    const navigate = useNavigate();
    const merge = useAnonymousMergePrompt();
    const recovery = useAccountRecovery();

    const setUsernameError = useCallback((msg) => {
        setErrors(prev => ({...prev, username: msg}));
    }, [setErrors]);

    const setEmailError = useCallback((msg) => {
        setErrors(prev => ({...prev, email: msg}));
    }, [setErrors]);

    const [loading, setLoading] = useState(false);

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

    const performRegister = async () => {
        setLoading(true);

        const res = await apiFetch("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({email, username, password}),
        });

        if (res.ok) {
            // Register does not authenticate immediately (the user must verify their email first),
            // so AuthProvider's clear-on-authenticated won't fire here — clear explicitly.
            merge.clearAnonymousProgress();
            navigate('/verify-email', { state: { email } });
            return;
        }

        setLoading(false);

        if (res.status === 409) {
            // The backend doesn't disambiguate which field conflicted (a single generic
            // RegistrationConflictException covers both email and username), so this can only
            // be shown as a general message — the live username/email availability checks below
            // are what normally catch a specific field conflict before submit.
            setErrors(prev => ({...prev, general: res.data?.message || "Email lub nazwa użytkownika już istnieją."}));
        } else if (res.status === -1 || res.status === -2) {
            setErrors(prev => ({...prev, general: "Błąd połączenia z serwerem. Spróbuj ponownie."}));
        } else {
            setErrors(prev => ({...prev, general: res.data?.message || GENERIC_ERROR_MESSAGE}));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        setErrors({email: "", username: "", password: "", confirmPassword: "", general: ""});

        if (!email || !username || !password) {
            setErrors(prev => ({...prev, general: "Wypełnij wymagane pola."}));
            return;
        }
        if (username.length < MIN_USERNAME_LEN) {
            setErrors(prev => ({
                ...prev,
                username: `Nazwa użytkownika musi mieć co najmniej ${MIN_USERNAME_LEN} znaków.`
            }));
            return;
        }
        if (username.length > MAX_USERNAME_LEN) {
            setErrors(prev => ({
                ...prev,
                username: `Nazwa użytkownika musi mieć co najwyżej ${MAX_USERNAME_LEN} znaków.`
            }));
            return;
        }
        if (password.length < MIN_PASSWORD_LEN) {
            setErrors(prev => ({...prev, password: PASSWORD_TOO_SHORT_ERROR}));
            return;
        }
        if (password.length > MAX_PASSWORD_LEN) {
            setErrors(prev => ({...prev, password: PASSWORD_TOO_LONG_ERROR}));
            return;
        }
        if (!evaluatePassword(password).meetsComplexity) {
            setErrors(prev => ({...prev, password: PASSWORD_COMPLEXITY_ERROR}));
            return;
        }
        if (!validateEmail(email)) {
            setErrors(prev => ({...prev, email: "Nieprawidłowy adres e-mail."}));
            return;
        }
        if (password !== confirmPassword) {
            setErrors(prev => ({...prev, confirmPassword: PASSWORDS_DONT_MATCH_ERROR}));
            return;
        }
        if (!acceptedTerms || !acceptedPrivacy) {
            setErrors(prev => ({...prev, general: "Musisz zaakceptować regulamin i politykę prywatności."}));
            return;
        }

        merge.requestAuth(() => performRegister());
    };

    const handleGoogleLogin = () => {
        merge.requestAuth(() => {
            window.location.href = 'https://localhost:8443/oauth2/authorization/google';
        });
    };

    return (
        <div className="login-register-page">
            <AuthHeader />
            <MergePrompt isOpen={merge.isOpen} onConfirm={merge.confirm} onDecline={merge.decline} />
            <AccountRecoveryModal {...recovery} />
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
                    <PasswordStrengthMeter password={password} />
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
                    {errors.confirmPassword &&
                        <div id="confirm-error" className="field-error">{errors.confirmPassword}</div>}
                </div>

                <div className="checkboxes">
                    <label className="custom-checkbox" htmlFor="terms">
                        <input
                            id="terms"
                            type="checkbox"
                            checked={acceptedTerms}
                            onChange={(e) => setAcceptedTerms(e.target.checked)}
                        />
                        <span className="checkmark" aria-hidden="true"/>
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
                        <span className="checkmark" aria-hidden="true"/>
                        <span className="checkbox-text">
            Akceptuję <a href="/privacy_policy.html" target="_blank"
                         rel="noopener noreferrer">politykę prywatności / RODO</a>
          </span>
                    </label>
                </div>

                <div aria-live="polite" className="form-message">
                    {errors.general && <div className="form-error" role="alert">{errors.general}</div>}
                </div>

                <button type="submit" className="submit-btn" disabled={loading}>
                    {loading ? "Ładowanie..." : "ZAREJESTRUJ SIĘ"}
                </button>

                <div className="divider">LUB</div>

                <button type="button" className="login-with-google-btn" onClick={handleGoogleLogin}>
                    ZALOGUJ PRZEZ GOOGLE
                </button>
                <p className="google-consent-note">
                    Logując się przez Google, akceptujesz <a href="/regulamin.html" target="_blank"
                                                              rel="noopener noreferrer">regulamin</a> i{" "}
                    <a href="/privacy_policy.html" target="_blank" rel="noopener noreferrer">politykę prywatności</a>.
                </p>

                <div className="loginFooter">
                    Masz już konto? <a href="/login">Zaloguj się</a>
                </div>
                <div className="loginFooter">
                    <button
                        type="button"
                        className="link-btn"
                        onClick={recovery.open}
                    >
                        Usunąłeś konto niedawno? Odzyskaj je
                    </button>
                </div>
            </form>

            <Footer/>
        </div>
    );
}

export default RegisterPage;
