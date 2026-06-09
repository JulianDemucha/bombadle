import './login-register-page.css';
import './GoogleButton.css'
import React, {useState} from "react";
import Footer from "../../components/Footer.jsx";
import AuthHeader from '../../components/AuthHeader';
import axios from "../../api/axios.js";
import {useNavigate} from "react-router-dom";
import {useAuth} from "../../auth/UseAuth.jsx";

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
    const [unverifiedEmail, setUnverifiedEmail] = useState("");
    const navigate = useNavigate();
    const {reload} = useAuth();

    const handleMergeConfirmation = () => {
        const anonymousGuesses = localStorage.getItem('anonymousGuessList');
        if (anonymousGuesses && anonymousGuesses !== '[]') {
            if (window.confirm("Czy chcesz zapisać wynik zdobyty przed zalogowaniem?")) {
                const sessionId = localStorage.getItem('bombadle_anonymous_session_id');
                if (sessionId) {
                    document.cookie = `bombadle_anonymous_session_id=${sessionId}; path=/; max-age=60`;
                } else {
                     document.cookie = "TRIGGER_MERGE=true; path=/; max-age=60";
                }
                
                localStorage.removeItem('anonymousGuessList');
                localStorage.removeItem('anonymousWinTime');
                localStorage.removeItem('lastPlayedDate');
                localStorage.removeItem('bombadle_anonymous_session_id');
            }
        }
    };

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

        handleMergeConfirmation();
        setLoading(true);

        try {
            const res = await axios.post("/api/auth/authenticate", {email, password});

            if (res.status === 201 || res.status === 200) {
                await reload();
                navigate("/");
            }

        } catch (err) {
            if (err?.response) {
                const {status, data} = err.response;
                if (status === 403 && data?.error === "Unverified Email" && data?.email) {
                    setUnverifiedEmail(data.email);
                } else if (status === 401 || status === 403) {
                    setErrors(prev => ({...prev, general: data?.message || "Nieprawidłowy email lub hasło."}));
                } else if (status === 409) {
                    setErrors(prev => ({...prev, general: data?.message || "Błąd podczas logowania."}));
                } else {
                     setErrors(prev => ({...prev, general: data?.message || "Wystąpił błąd."}));
                }
            } else {
                setErrors(prev => ({...prev, general: "Błąd połączenia z serwerem. Spróbuj ponownie."}));
            }
        } finally {
            setLoading(false);
        }
    };

    const handleGoogleLogin = () => {
        handleMergeConfirmation();
        window.location.href = '/oauth2/authorization/google';
    };

    const handleSendActivationCode = async () => {
        setLoading(true);
        try {
             await axios.post('/api/auth/initiate-verify-email', { email: unverifiedEmail });
             navigate('/verify-email', { state: { email: unverifiedEmail } });
        } catch(err) {
             setErrors(prev => ({...prev, general: "Nie udało się wysłać kodu. Spróbuj ponownie."}));
        } finally {
             setLoading(false);
        }
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

                <div className="loginFooter">
                    Nie masz konta? <a href="/register">Zarejestruj się</a>
                </div>
            </form>
            <Footer/>
        </div>
    )
}

export default LoginPage;
