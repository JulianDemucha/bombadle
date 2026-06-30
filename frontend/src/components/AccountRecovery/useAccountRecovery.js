import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../../api/api.js';

/**
 * Smart hook for the account-recovery modal, shared between LoginPage and RegisterPage
 * (the one existing exception to one-hook-per-page is useAnonymousMergePrompt — this hook
 * follows the same shared pattern).
 *
 * Usage in a page:
 *   const recovery = useAccountRecovery();
 *   <button onClick={recovery.open}>Odzyskaj konto</button>
 *   <AccountRecoveryModal {...recovery} />
 */
export default function useAccountRecovery() {
    const [isOpen, setIsOpen] = useState(false);
    const [step, setStep] = useState(1); // 1 = email, 2 = code + optional new password
    const [email, setEmail] = useState('');
    const [code, setCode] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [resendDisabled, setResendDisabled] = useState(false);
    const [countdown, setCountdown] = useState(0);

    useEffect(() => {
        if (countdown <= 0) {
            setResendDisabled(false);
            return;
        }
        const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
        return () => clearTimeout(timer);
    }, [countdown]);

    const open = useCallback(() => {
        setIsOpen(true);
        setStep(1);
        setEmail('');
        setCode('');
        setNewPassword('');
        setError('');
        setSuccessMessage('');
        setResendDisabled(false);
        setCountdown(0);
    }, []);

    const close = useCallback(() => {
        setIsOpen(false);
    }, []);

    const sendRecoveryCode = useCallback(async (rateLimitedMessage) => {
        setLoading(true);
        setError('');
        const res = await apiFetch('/api/auth/initiate-recover-account', {
            method: 'POST',
            body: JSON.stringify({ email }),
        });
        setLoading(false);

        if (res.ok) {
            setStep(2);
            setResendDisabled(true);
            setCountdown(60);
            return;
        }

        if (res.status === 429) {
            const waitTime = res.data?.['seconds-to-wait'] || 60;
            setError(rateLimitedMessage(waitTime));
            setStep(2);
            setResendDisabled(true);
            setCountdown(waitTime);
        } else {
            setError('Nie udało się wysłać kodu. Sprawdź, czy email jest poprawny.');
        }
    }, [email]);

    const submitEmailStep = useCallback(async (e) => {
        e.preventDefault();
        await sendRecoveryCode(() => 'Zbyt wiele prób. Kod z poprzedniego żądania jest wciąż ważny.');
    }, [sendRecoveryCode]);

    const handleResend = useCallback(async () => {
        await sendRecoveryCode((waitTime) => `Zbyt wiele prób. Możesz wysłać kolejny kod za ${waitTime} sekund.`);
    }, [sendRecoveryCode]);

    const submitConfirmStep = useCallback(async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        const res = await apiFetch('/api/auth/confirm-recover-account', {
            method: 'POST',
            body: JSON.stringify({ email, code, newPassword: newPassword || null }),
        });
        setLoading(false);

        if (res.ok) {
            setSuccessMessage(
                'Konto zostało odzyskane! Jeśli ustawiłeś nowe hasło, zaloguj się nim. ' +
                'Jeśli korzystałeś z logowania przez Google, zaloguj się ponownie przez Google.'
            );
            return;
        }

        switch (res.status) {
            case 403:
                setError('Nieprawidłowy kod weryfikacyjny.');
                break;
            case 404:
                setError('Nie znaleziono usuniętego konta dla podanego adresu e-mail.');
                break;
            case 410:
                setError('Kod wygasł. Poproś o nowy.');
                break;
            default:
                setError(res.data?.message || 'Wystąpił błąd. Spróbuj ponownie.');
        }
    }, [email, code, newPassword]);

    return {
        isOpen,
        step,
        email,
        setEmail,
        code,
        setCode,
        newPassword,
        setNewPassword,
        loading,
        error,
        successMessage,
        resendDisabled,
        countdown,
        open,
        close,
        submitEmailStep,
        submitConfirmStep,
        handleResend,
    };
}
