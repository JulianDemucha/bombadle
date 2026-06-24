import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from '../../api/axios';
import './login-register-page.css';
import AuthHeader from '../../components/AuthHeader';
import Footer from "../../components/Footer.jsx";

function EmailVerificationPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const [email, setEmail] = useState(location.state?.email || '');
    const [code, setCode] = useState('');
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [isVerifying, setIsVerifying] = useState(false);
    const [isResending, setIsResending] = useState(false);
    const [resendDisabled, setResendDisabled] = useState(false);
    const [countdown, setCountdown] = useState(0);

    useEffect(() => {
        if (!location.state?.email) {
            navigate('/login');
        }
    }, [location, navigate]);

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

    const handleVerify = async (e) => {
        e.preventDefault();
        setIsVerifying(true);
        setError('');
        setMessage('');
        try {
            await axios.post('/api/auth/verify-email', { email, code });
            alert('Konto aktywowane! Możesz się teraz zalogować.');
            navigate('/login');
        } catch (err) {
            handleOtpError(err);
        } finally {
            setIsVerifying(false);
        }
    };

    const handleResend = async () => {
        setIsResending(true);
        setError('');
        setMessage('');
        try {
            await axios.post('/api/auth/initiate-verify-email', { email });
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
            <form className="login-container" onSubmit={handleVerify}>
                <h1>Aktywacja Konta</h1>
                <p className="instruction-text">Za chwilę dostaniesz kod aktywacyjny na Twój adres e-mail: <strong>{email}</strong></p>
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
                {error && <div className="form-error">{error}</div>}
                
                <button type="submit" disabled={isVerifying || isResending}>
                    {isVerifying ? 'Weryfikowanie...' : 'Aktywuj konto'}
                </button>
                <button 
                    type="button" 
                    onClick={handleResend} 
                    disabled={resendDisabled || isVerifying || isResending} 
                    className="btn-secondary"
                >
                    {resendDisabled ? `Wyślij ponownie za ${countdown}s` : 'Wyślij kod ponownie'}
                </button>
                {message && <div className="form-success">{message}</div>}
            </form>
            <Footer />
        </div>
    );
}

export default EmailVerificationPage;