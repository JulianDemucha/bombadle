import "./profile-settings-page.css";
import Footer from "../../components/Footer.jsx";
import { useAuth } from "../../auth/UseAuth.jsx";
import { useEffect, useState } from "react";
import { apiFetch } from "../../api/api.js";
import axios from "../../api/axios.js";
import { useNavigate } from "react-router-dom";
import AvatarPicker from "./AvatarPicker.jsx";
import StatisticsSummaryCard from "./components/StatisticsSummaryCard.jsx";
import ConfirmDialog from "../../components/ConfirmDialog/ConfirmDialog.jsx";
import FeedbackModal from "../../components/FeedbackModal/FeedbackModal.jsx";
import { useFeedback } from "../../components/FeedbackModal/useFeedback.js";
import InfoTooltip from "../../components/InfoTooltip.jsx";
import useAccountDeletion from "./hooks/useAccountDeletion.js";

export default function PlayerSettingsPage() {
    const [displayName, setDisplayName] = useState("");
    const [avatar, setAvatar] = useState(null);
    const [saving, setSaving] = useState(false);

    const [pwdMode, setPwdMode] = useState("idle"); // 'idle' | 'change' | 'setup'
    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [pwdError, setPwdError] = useState("");
    const [pwdSuccess, setPwdSuccess] = useState("");

    const [saveMessage, setSaveMessage] = useState(null); // { type: 'error' | 'success', text }

    const feedbackHook = useFeedback();
    const deletion = useAccountDeletion();

    const context = useAuth();
    const user = context.user;
    const reload = context.reload;
    const logout = context.logout;
    const navigate = useNavigate();

    useEffect(() => {
        document.body.classList.add('scrollable-page');
        return () => {
            document.body.classList.remove('scrollable-page');
        };
    }, []);

    useEffect(() => {
        if (user) {
            setDisplayName(user.displayName ?? user.login ?? "");
            setAvatar(user.avatarImage ?? null);
        }
    }, [user]);

    const handleAvatarSelected = (avatarUrl) => {
        setAvatar(avatarUrl);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setSaveMessage(null);
        try {
            const body = {
                login: displayName === (user.displayName ?? user.login ?? "") ? null : (displayName.trim() === "" ? null : displayName.trim()),
                avatarImage: avatar === (user.avatarImage ?? null) ? null : avatar ?? null
            };

            const res = await apiFetch("/api/players/me", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body),
            });

            if (res.status === 401) {
                setSaveMessage({ type: "error", text: "Twoja sesja wygasła. Zaloguj się ponownie." });
                return;
            }

            if (!res.ok) {
                const serverMsg = typeof res.data === "string" ? res.data : (res.data?.message || JSON.stringify(res.data));
                setSaveMessage({ type: "error", text: "Aktualizacja nie powiodła się: " + (serverMsg || `status ${res.status}`) });
            } else {
                setSaveMessage({ type: "success", text: "Zmiany zostały zapisane." });
                await reload();
            }
        } catch (err) {
            setSaveMessage({ type: "error", text: "Błąd podczas zapisu: " + (err.message || err) });
        } finally {
            setSaving(false);
        }
    };

    const handlePasswordSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setPwdError("");
        setPwdSuccess("");

        try {
            if (pwdMode === "change") {
                await axios.put("/api/players/me/password", {
                    oldPassword,
                    newPassword
                });
                setPwdSuccess("Hasło zostało pomyślnie zmienione.");
            } else if (pwdMode === "setup") {
                await axios.put("/api/players/me/set-up-password", {
                    password: newPassword
                });
                setPwdSuccess("Hasło zostało ustawione. Od teraz możesz logować się klasycznie.");
                await reload();
            }
            setPwdMode("idle");
            setOldPassword("");
            setNewPassword("");
        } catch (err) {
            setPwdError(err.response?.data?.message || "Wystąpił błąd podczas zapisywania hasła.");
        } finally {
            setSaving(false);
        }
    };

    const handleGoBack = () => {
        if (window.history.length > 1) {
            navigate(-1);
            return;
        }
        navigate('/');
    };

    if (!user) return null;

    const avatarText = avatar ?? user?.avatarImage ?? "AVATAR_DEFAULT";

    return (
        <div className="settings-panel-wrapper">
            <div className="settings-top-bar">
                <button
                    type="button"
                    className="back-arrow-button"
                    onClick={handleGoBack}
                    aria-label="Powrot do poprzedniej strony"
                >
                    <svg className="back-arrow-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                        <path d="M15 5l-7 7 7 7" />
                    </svg>
                    <span className="back-arrow-text"></span>
                </button>
                <button
                    type="button"
                    className="back-arrow-button back-arrow-button--feedback"
                    style={{ marginLeft: 'auto' }}
                    onClick={feedbackHook.handleOpen}
                >
                    Zgłoś opinię
                </button>
            </div>

            <StatisticsSummaryCard />

            <div className="container">
                <h1>Ustawienia Konta</h1>

                <div className="profile-header">
                    <img className="avatar" src={`./avatar/${avatarText}.jpg`} alt="User Avatar" />
                    <div className="profile-info">
                        <h2>{displayName}</h2>
                        <p>{user.email}</p>
                        <AvatarPicker onAvatarSelect={handleAvatarSelected} />
                    </div>
                </div>

                <form className="settings-form" onSubmit={handleSubmit}>
                    <div className="form-section">
                        <h3>Dane osobowe</h3>
                        <div className="form-group">
                            <label htmlFor="name">Nazwa użytkownika</label>
                            <input
                                type="text"
                                name="name"
                                id="name"
                                value={displayName}
                                onChange={(e) => setDisplayName(e.target.value)}
                                minLength={3}
                                maxLength={16}
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="email">Adres email</label>
                            <input type="email" name="email" id="email" value={user?.email ?? ""} disabled />
                        </div>
                    </div>
                </form>

                <div className="form-section" style={{ marginTop: "30px" }}>
                    <h3>Bezpieczeństwo</h3>

                    {pwdMode === "idle" && (
                        <div className="form-group">
                            <label>Hasło</label>
                            <button
                                type="button"
                                className="btn btn-secondary"
                                onClick={() => setPwdMode(user.hasPassword ? "change" : "setup")}
                            >
                                {user.hasPassword ? "Zmień hasło" : "Ustaw hasło (Local Login)"}
                            </button>
                            {pwdSuccess && <div className="form-success">{pwdSuccess}</div>}
                        </div>
                    )}

                    {pwdMode !== "idle" && (
                        <form onSubmit={handlePasswordSubmit} style={{ marginTop: "15px", padding: "15px", border: "1px dashed var(--login_primary)", borderRadius: "8px" }}>
                            {user.hasPassword && (
                                <div className="form-group">
                                    <label>Stare hasło</label>
                                    <input
                                        type="password"
                                        value={oldPassword}
                                        onChange={e => setOldPassword(e.target.value)}
                                        required
                                    />
                                </div>
                            )}
                            <div className="form-group">
                                <label>Nowe hasło</label>
                                <input
                                    type="password"
                                    value={newPassword}
                                    onChange={e => setNewPassword(e.target.value)}
                                    required
                                    minLength={8}
                                />
                            </div>
                            {pwdError && <div className="form-error">{pwdError}</div>}
                            <div style={{ display: "flex", gap: "10px" }}>
                                <button type="submit" className="btn btn-primary" disabled={saving}>Zapisz hasło</button>
                                <button type="button" className="btn btn-secondary" onClick={() => { setPwdMode("idle"); setPwdError(""); }}>Anuluj</button>
                            </div>
                        </form>
                    )}
                </div>

                <div className="form-section" style={{ marginTop: "30px" }}>
                    <h3>Strefa niebezpieczna</h3>

                    {deletion.deleteMode === "idle" && (
                        <div>
                            <p>Ta operacja jest nieodwracalna. Wszystkie twoje dane zostaną trwale usunięte!</p>
                            {deletion.deleteError && <div className="form-error" style={{marginTop: "10px"}}>{deletion.deleteError}</div>}
                            <br />
                            <button type="button" className="btn btn-danger" onClick={deletion.handleInitiateDelete} disabled={deletion.saving}>
                                Rozpocznij usuwanie konta
                            </button>
                        </div>
                    )}

                    {deletion.deleteMode === "otp" && (
                        <form onSubmit={deletion.handleConfirmDelete} style={{ marginTop: "15px", padding: "15px", border: "1px dashed #dc3545", borderRadius: "8px" }}>
                            <p>Na adres <strong>{user.email}</strong> wysłaliśmy kod. Wprowadź go, aby definitywnie usunąć konto.</p>
                            <div className="form-group" style={{marginTop: "10px"}}>
                                <label>Kod OTP</label>
                                <input
                                    type="text"
                                    value={deletion.deleteCode}
                                    onChange={e => deletion.setDeleteCode(e.target.value)}
                                    required
                                    maxLength={6}
                                />
                            </div>

                            <div className="form-group form-group--checkbox">
                                <label className="account-deletion__checkbox-label">
                                    <input
                                        type="checkbox"
                                        checked={deletion.deleteAllDataNow}
                                        onChange={e => deletion.setDeleteAllDataNow(e.target.checked)}
                                    />
                                    Chcę usunąć wszystkie dane teraz
                                </label>
                                <InfoTooltip text="Jeśli pole zostanie niezaznaczone, Twoje dane będą zachowane przez 7 dni i w tym czasie będziesz mógł odzyskać konto. Po 7 dniach zostaną usunięte trwale. Jeśli zaznaczysz to pole, wszystkie dane zostaną usunięte natychmiast i bezpowrotnie." />
                            </div>

                            {deletion.deleteError && <div className="form-error">{deletion.deleteError}</div>}

                            <div style={{ display: "flex", gap: "10px", marginTop: "15px", flexWrap: "wrap" }}>
                                <button type="submit" className="btn btn-danger" disabled={deletion.saving}>Potwierdzam usunięcie</button>
                                <button
                                    type="button"
                                    className="btn btn-secondary"
                                    onClick={deletion.handleDeleteResend}
                                    disabled={deletion.deleteResendDisabled || deletion.saving}
                                >
                                    {deletion.deleteResendDisabled ? `Wyślij ponownie za ${deletion.deleteCountdown}s` : 'Wyślij kod ponownie'}
                                </button>
                                <button type="button" className="btn btn-secondary" onClick={deletion.cancelOtp}>Anuluj</button>
                            </div>
                        </form>
                    )}
                </div>

                {saveMessage && (
                    <div
                        className={saveMessage.type === "success" ? "form-success" : "form-error"}
                        style={{ marginTop: "20px" }}
                        role="alert"
                    >
                        {saveMessage.text}
                    </div>
                )}

                <div className="form-actions" style={{ marginTop: "40px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <button type="button" className="btn btn-logout" disabled={saving} onClick={() => logout()}>
                        Wyloguj się
                    </button>
                    <button type="submit" form="settings-form" className="btn btn-primary" disabled={saving} onClick={handleSubmit}>
                        {saving ? "Zapisuję..." : "Zapisz zmiany"}
                    </button>
                </div>
            </div>

            <FeedbackModal hook={feedbackHook} />

            <ConfirmDialog
                isOpen={deletion.showDeleteConfirm}
                title="Usunąć konto?"
                message="Czy na pewno chcesz usunąć konto? Otrzymasz kod potwierdzający na e-mail."
                confirmLabel="Tak, usuń"
                cancelLabel="Anuluj"
                variant="danger"
                onConfirm={deletion.confirmDeleteRequest}
                onCancel={deletion.cancelDeleteConfirm}
            />

            <ConfirmDialog
                isOpen={deletion.showDeletedInfo}
                title="Konto zostało usunięte"
                message="Przykro nam, że odchodzisz!"
                confirmLabel="OK"
                onConfirm={() => { deletion.dismissDeletedInfo(); logout(); }}
            />

            <Footer />
        </div>
    );
}