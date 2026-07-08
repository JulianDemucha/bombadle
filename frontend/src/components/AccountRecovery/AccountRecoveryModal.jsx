import React from 'react';
import './AccountRecoveryModal.css';

export default function AccountRecoveryModal({
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
    close,
    submitEmailStep,
    submitConfirmStep,
    handleResend,
}) {
    if (!isOpen) return null;

    return (
        <div className="account-recovery-prompt" role="dialog" aria-modal="true" aria-labelledby="account-recovery-prompt__title">
            <button
                type="button"
                className="account-recovery-prompt__backdrop"
                aria-label="Zamknij"
                onClick={close}
            />
            <div className="account-recovery-prompt__dialog">
                {successMessage ? (
                    <>
                        <h2 id="account-recovery-prompt__title" className="account-recovery-prompt__title">
                            Konto odzyskane
                        </h2>
                        <p className="account-recovery-prompt__message">{successMessage}</p>
                        <div className="account-recovery-prompt__actions">
                            <button
                                type="button"
                                className="account-recovery-prompt__button account-recovery-prompt__button--primary"
                                onClick={close}
                            >
                                OK
                            </button>
                        </div>
                    </>
                ) : step === 1 ? (
                    <form onSubmit={submitEmailStep}>
                        <h2 id="account-recovery-prompt__title" className="account-recovery-prompt__title">
                            Odzyskaj konto
                        </h2>
                        <p className="account-recovery-prompt__message">
                            Podaj adres e-mail usuniętego konta, aby otrzymać kod odzyskiwania.
                        </p>
                        <div className="account-recovery-prompt__input-group">
                            <label htmlFor="account-recovery-email">EMAIL</label>
                            <input
                                type="email"
                                id="account-recovery-email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        {error && <div className="form-error">{error}</div>}
                        <div className="account-recovery-prompt__actions">
                            <button
                                type="button"
                                className="account-recovery-prompt__button account-recovery-prompt__button--secondary"
                                onClick={close}
                            >
                                Anuluj
                            </button>
                            <button
                                type="submit"
                                className="account-recovery-prompt__button account-recovery-prompt__button--primary"
                                disabled={loading}
                            >
                                {loading ? 'Wysyłanie...' : 'Wyślij kod'}
                            </button>
                        </div>
                    </form>
                ) : (
                    <form onSubmit={submitConfirmStep}>
                        <h2 id="account-recovery-prompt__title" className="account-recovery-prompt__title">
                            Wpisz kod odzyskiwania
                        </h2>
                        <p className="account-recovery-prompt__message">
                            Na adres <strong>{email}</strong> wysłaliśmy kod. Jeśli logowałeś się hasłem (nie przez
                            Google), ustaw też nowe hasło — będzie potrzebne do zalogowania.
                        </p>
                        <div className="account-recovery-prompt__input-group">
                            <label htmlFor="account-recovery-code">KOD OTP</label>
                            <input
                                type="text"
                                id="account-recovery-code"
                                value={code}
                                onChange={(e) => setCode(e.target.value)}
                                required
                                maxLength={6}
                            />
                        </div>
                        <div className="account-recovery-prompt__input-group">
                            <label htmlFor="account-recovery-password">NOWE HASŁO (opcjonalnie)</label>
                            <input
                                type="password"
                                id="account-recovery-password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                minLength={8}
                                maxLength={24}
                                placeholder="Pozostaw puste, jeśli logujesz się przez Google"
                            />
                        </div>
                        {error && <div className="form-error">{error}</div>}
                        <div className="account-recovery-prompt__actions">
                            <button
                                type="button"
                                className="account-recovery-prompt__button account-recovery-prompt__button--secondary"
                                onClick={handleResend}
                                disabled={resendDisabled || loading}
                            >
                                {resendDisabled ? `Wyślij ponownie za ${countdown}s` : 'Wyślij kod ponownie'}
                            </button>
                            <button
                                type="submit"
                                className="account-recovery-prompt__button account-recovery-prompt__button--primary"
                                disabled={loading}
                            >
                                {loading ? 'Wysyłanie...' : 'Odzyskaj konto'}
                            </button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}
