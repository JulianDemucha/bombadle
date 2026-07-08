import React, { useState, useEffect } from 'react';
import axios from '../../api/axios';
import { useNavigate } from 'react-router-dom';
import './login-register-page.css';
import AuthHeader from '../../components/AuthHeader';
import Footer from "../../components/Footer.jsx";
import PasswordStrengthMeter from "../../components/PasswordStrength/PasswordStrengthMeter.jsx";
import { evaluatePassword, PASSWORD_COMPLEXITY_ERROR } from "../../components/PasswordStrength/passwordRules.js";

function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [code, setCode] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [step, setStep] = useState(1);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(false);
    const [isResending, setIsResending] = useState(false);
    const [resendDisabled, setResendDisabled] = useState(false);
    const [countdown, setCountdown] = useState(0);
    const navigate = useNavigate();

    useEffect(() => {
        let timer;
        if (countdown > 0) {
            timer = setTimeout(() => setCountdown(countdown - 1), 1000);
        } else {
            setResendDisabled(false);
        }
        return () => clearTimeout(timer);
    }, [countdown]);

    useEffect(() => {
        let timer;
        if (message === 'Nowy kod został wysłany.') {
            timer = setTimeout(() => {
                setMessage('');
            }, 5000);
        }
        return () => clearTimeout(timer);
    }, [message]);

    const handleInitiateReset = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        setMessage('');
        try {
            await axios.post('/api/auth/initiate-reset-password', { email });
            setStep(2);
            setMessage('Wysłaliśmy kod resetujący na Twój adres e-mail.');
            setResendDisabled(true);
            setCountdown(60);
        } catch (err) {
            if (err.response?.status === 429) {
                 const secondsToWait = err.response.data['seconds-to-wait'] || 60;
                 setStep(2);
                 setResendDisabled(true);
                 setCountdown(secondsToWait);
            } else {
                 setError('Nie udało się wysłać kodu. Sprawdź, czy email jest poprawny.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleConfirmReset = async (e) => {
        e.preventDefault();
        if (!evaluatePassword(newPassword).meetsComplexity) {
            setError(PASSWORD_COMPLEXITY_ERROR);
            return;
        }
        setLoading(true);
        setError('');
        setMessage('');
        try {
            await axios.post('/api/auth/confirm-reset-password', { email, code, newPassword });
            setMessage('Hasło zostało zmienione! Możesz się teraz zalogować.');
            setTimeout(() => navigate('/login'), 3000);
        } catch (err) {
            handleOtpError(err);
        } finally {
            setLoading(false);
        }
    };
    
    const handleResend = async () => {
        setIsResending(true);
        setError('');
        setMessage('');
        try {
            await axios.post('/api/auth/initiate-reset-password', { email });
            setMessage('Nowy kod został wysłany.');
            setResendDisabled(true);
            setCountdown(60);
        } catch (err) {
            if (err.response?.status === 429) {
                const secondsToWait = err.response.data['seconds-to-wait'] || 60;
                setResendDisabled(true);
                setCountdown(secondsToWait);
            } else {
                setError('Nie udało się wysłać kodu. Spróbuj ponownie.');
            }
        } finally {
            setIsResending(false);
        }
    };

    const handleOtpError = (err) => {
        if (err.response) {
            switch (err.response.status) {
                case 410:
                    setError('Kod wygasł. Poproś o nowy.');
                    break;
                case 404:
                    setError('Kod nie został znaleziony lub został już użyty.');
                    break;
                case 403:
                    setError('Nieprawidłowy kod weryfikacyjny.');
                    break;
                default:
                    setError('Wystąpił błąd. Spróbuj ponownie.');
            }
        } else {
            setError('Błąd połączenia z serwerem.');
        }
    };

    return (
        <div className="login-register-page">
            <AuthHeader />
            {step === 1 ? (
                <form className="login-container" onSubmit={handleInitiateReset}>
                    <h1>Resetowanie Hasła</h1>
                    <p className="instruction-text">Podaj swój adres e-mail, aby otrzymać kod do zresetowania hasła.</p>
                    <div className="input-group">
                        <label htmlFor="email">EMAIL</label>
                        <input
                            type="email"
                            id="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>
                    {error && <div className="form-error">{error}</div>}
                    <button type="submit" disabled={loading}>
                        {loading ? 'Wysyłanie...' : 'Wyślij kod'}
                    </button>
                </form>
            ) : (
                <form className="login-container" onSubmit={handleConfirmReset}>
                    <h1>Nowe Hasło</h1>
                    <p className="instruction-text">Wprowadź kod OTP otrzymany na e-mail oraz swoje nowe hasło.</p>
                    <div className="input-group">
                        <label htmlFor="code">Kod OTP</label>
                        <input
                            type="text"
                            id="code"
                            value={code}
                            onChange={(e) => setCode(e.target.value)}
                            required
                        />
                    </div>
                    <div className="input-group">
                        <label htmlFor="newPassword">Nowe Hasło</label>
                        <input
                            type="password"
                            id="newPassword"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                            required
                            minLength={8}
                        />
                        <PasswordStrengthMeter password={newPassword} />
                    </div>
                    {error && <div className="form-error">{error}</div>}
                    
                    <button type="submit" disabled={loading || isResending}>
                        {loading ? 'Zapisywanie...' : 'Zmień hasło'}
                    </button>
                    <button 
                        type="button" 
                        onClick={handleResend} 
                        disabled={resendDisabled || loading || isResending}
                        className="btn-secondary"
                    >
                        {resendDisabled ? `Wyślij ponownie za ${countdown}s` : 'Wyślij kod ponownie'}
                    </button>
                    {message && <div className="form-success">{message}</div>}
                </form>
            )}
            <Footer />
        </div>
    );
}

export default ForgotPasswordPage;
