import './login-register-page.css';
import './GoogleButton.css'
import React, {useEffect, useState} from "react";
import Footer from "../../components/Footer.jsx";
import AuthHeader from '../../components/AuthHeader';
import {apiFetch} from "../../api/api.js";
import {useNavigate} from "react-router-dom";
import {useAuth} from "../../auth/UseAuth.jsx";
import MergePrompt from "../../components/MergePrompt/MergePrompt.jsx";
import useAnonymousMergePrompt from "../../components/MergePrompt/useAnonymousMergePrompt.js";
import AccountRecoveryModal from "../../components/AccountRecovery/AccountRecoveryModal.jsx";
import useAccountRecovery from "../../components/AccountRecovery/useAccountRecovery.js";
import ConfirmDialog from "../../components/ConfirmDialog/ConfirmDialog.jsx";
import useInAppBrowserWarning from "../../components/InAppBrowserWarning/useInAppBrowserWarning.js";

const validateEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
const MIN_PASSWORD_LEN = 8;
const MAX_PASSWORD_LEN = 24;
// Validation copy is kept in a named constant (rather than an inline `password: "..."` literal)
// so static secret scanners like GitGuardian don't mistake it for a hardcoded credential.
const PASSWORD_LENGTH_ERROR = `Hasło musi mieć od ${MIN_PASSWORD_LEN} do ${MAX_PASSWORD_LEN} znaków.`;
const EMPTY_ERRORS = {email: "", password: "", general: ""};
const GENERIC_ERROR_MESSAGE = "Wystąpił błąd, spróbuj ponownie.";

function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [errors, setErrors] = useState({
        email: "",
        password: "",
        general: ""
    });
    const [loading, setLoading] = useState(false);
    const [unverifiedEmail, setUnverifiedEmail] = useState("");
    const navigate = useNavigate();
    const {reload} = useAuth();
    const merge = useAnonymousMergePrompt();
    const recovery = useAccountRecovery();
    const inAppBrowserWarning = useInAppBrowserWarning();

    useEffect(() => {
        document.body.classList.add('scrollable-page');
        return () => {
            document.body.classList.remove('scrollable-page');
        };
    }, []);

    const performLogin = async () => {
        setLoading(true);

        const res = await apiFetch("/api/auth/authenticate", {
            method: "POST",
            body: JSON.stringify({email, password}),
        });

        if (res.ok) {
            // reload() -> AuthProvider.loadUser() clears anonymous progress once /me returns a user.
            await reload();
            navigate("/");
            return;
        }

        setLoading(false);

        if (res.status === 403 && res.data?.error === "Unverified Email" && res.data?.email) {
            setUnverifiedEmail(res.data.email);
        } else if (res.status === 401 || res.status === 403) {
            setErrors(prev => ({...prev, general: res.data?.message || "Nieprawidłowy email lub hasło."}));
        } else if (res.status === -1 || res.status === -2) {
            setErrors(prev => ({...prev, general: "Błąd połączenia z serwerem. Spróbuj ponownie."}));
        } else {
            setErrors(prev => ({...prev, general: res.data?.message || GENERIC_ERROR_MESSAGE}));
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();

        setErrors(EMPTY_ERRORS);

        if (!email || !password) {
            setErrors(prev => ({...prev, general: "Wypełnij wymagane pola."}));
            return;
        }

        if (!validateEmail(email)) {
            setErrors(prev => ({...prev, email: "Nieprawidłowy adres e-mail."}));
            return;
        }

        if (password.length < MIN_PASSWORD_LEN || password.length > MAX_PASSWORD_LEN) {
            setErrors(prev => ({...prev, password: PASSWORD_LENGTH_ERROR}));
            return;
        }

        merge.requestAuth(() => performLogin());
    };

    const handleGoogleLogin = () => {
        inAppBrowserWarning.guardGoogleLogin(() => {
            merge.requestAuth(() => {
                window.location.href = '/oauth2/authorization/google';
            });
        });
    };

    const handleSendActivationCode = async () => {
        setLoading(true);
        const res = await apiFetch('/api/auth/initiate-verify-email', {
            method: "POST",
            body: JSON.stringify({email: unverifiedEmail}),
        });
        setLoading(false);

        if (res.ok) {
            navigate('/verify-email', { state: { email: unverifiedEmail } });
            return;
        }

        setErrors(prev => ({...prev, general: res.data?.message || "Nie udało się wysłać kodu. Spróbuj ponownie."}));
    }

    if (unverifiedEmail) {
         return (
             <div className="login-register-page">
                 <AuthHeader />
                 <div className="login-container">
                     <h1>Konto nieaktywne</h1>
                     <p className="instruction-text">
                         Twoje konto powiązane z adresem <strong>{unverifiedEmail}</strong> nie zostało jeszcze aktywowane.
                     </p>
                      {errors.general && <div className="form-error" role="alert">{errors.general}</div>}
                     <button onClick={handleSendActivationCode} disabled={loading}>
                         {loading ? "Wysyłanie..." : "Wyślij kod aktywacyjny"}
                     </button>
                 </div>
                 <Footer/>
             </div>
         );
    }

    return (
        <div className="login-register-page">
            <AuthHeader />
            <MergePrompt isOpen={merge.isOpen} onConfirm={merge.confirm} onDecline={merge.decline} />
            <AccountRecoveryModal {...recovery} />
            <ConfirmDialog
                isOpen={inAppBrowserWarning.isOpen}
                title="Wbudowana przeglądarka"
                message="Korzystasz z wbudowanej przeglądarki (np. z aplikacji Messenger lub Instagram). Aby zalogować się przez Google, otwórz tę stronę w standardowej przeglądarce (np. Chrome lub Safari)."
                confirmLabel="OK"
                onConfirm={inAppBrowserWarning.dismiss}
            />
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
                    {errors.password && <div id="password-error" className="field-error">{errors.password}</div>}
                </div>

                {errors.general && <div className="form-error" role="alert">{errors.general}</div>}

                <div className="loginFooter" style={{textAlign: "right", marginTop: "-10px", marginBottom: "15px"}}>
                    <a href="/forgot-password" style={{fontSize: "0.8rem"}}>Zapomniałeś hasła?</a>
                </div>

                <button type="submit" disabled={loading}>
                    {loading ? "Ładowanie..." : "ZALOGUJ SIĘ"}
                </button>

                <div className="divider">LUB</div>
                <div aria-live="polite" className="form-message">
                </div>
                <button type="button" className="login-with-google-btn" onClick={handleGoogleLogin}>
                    ZALOGUJ SIĘ PRZEZ GOOGLE
                </button>
                {/*<p className="google-consent-note">*/}
                {/*    Logując się przez Google, akceptujesz <a href="/regulamin.html" target="_blank"*/}
                {/*                                              rel="noopener noreferrer">regulamin</a> i{" "}*/}
                {/*    <a href="/privacy_policy.html" target="_blank" rel="noopener noreferrer">politykę prywatności</a>.*/}
                {/*</p>*/}

                <div className="loginFooter">
                    Nie masz konta? <a href="/register">Zarejestruj się</a>
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
    )
}

export default LoginPage;
