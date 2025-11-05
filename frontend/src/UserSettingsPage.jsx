import "./style/profile-settings-page.css";
import Footer from "./Footer.jsx";
import {useAuth} from "./auth/UseAuth.jsx";
import {useEffect, useState} from "react";
import {apiFetch} from "./api.js";
import axios from "axios";
import {useNavigate} from "react-router-dom";
import AvatarPicker from "./AvatarPicker.jsx";


export default function UserSettingsPage() {

    const [login, setLogin] = useState("");
    const [avatar, setAvatar] = useState(null);
    const [saving, setSaving] = useState(false);
    // const [showAvatarPicker, setShowAvatarPicker] = useState(false);

    const context = useAuth();
    const user = context.user;
    const reload = context.reload;
    const logout = context.logout;

    useEffect(() => {
        if (user) {
            setLogin(user.login ?? "");
            setAvatar(user.avatarImage ?? null);
        }
    }, [user]);

    const handleAvatarSelected = (avatarUrl) => {
        setAvatar(avatarUrl);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const body = {
                // if login didnt change -> null -> backend wont change the value if null is given
                login: (login === (user.login ?? "")) ? null : (login.trim() === "" ? null : login.trim()),

                // if avatarLogin didnt change -> null
                avatarImage: (avatar === (user.avatarImage ?? null)) ? null : (avatar ?? null)
            };

            const res = await apiFetch("/api/players/me", {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(body),
            });

            if (res.status === 401) {
                alert("Twoja sesja wygasła. Zaloguj się ponownie.");
                return;
            }

            if (!res.ok) {
                const serverMsg = typeof res.data === "string" ?
                    res.data
                    :
                    (res.data?.message || JSON.stringify(res.data));
                alert("Aktualizacja nie powiodła się: " + (serverMsg || `status ${res.status}`));
            } else {
                // alert("Zapisano zmiany pomyślnie.");
                await reload();


            }
        } catch (err) {
            console.error("Update failed:", err);
            alert("Błąd podczas zapisu: " + (err.message || err));
        } finally {
            setSaving(false);
        }

    };

    const handleDelete = async (e) => {
        e.preventDefault();

        const password =
            prompt("Wprowadź hasło, aby potwierdzić permanentne usunięcie konta Anuluj aby nie zmieniać.");
        const email = user.email;
        try {
            await axios.post("/api/auth/authenticate", { email, password });
        } catch (error) {
            console.error("Błąd uwierzytelniania:", error);
            alert("Błędne hasło!");
            return;
        }

        try {
            setSaving(true);
            const res = await apiFetch(`/api/players/me?email=${encodeURIComponent(email)}`, {
                method: "DELETE",
                headers: {"Content-Type": "application/json"}
            });

            if (res.status === 401) {
                alert("Twoja sesja wygasła. Zaloguj się ponownie.");
                return;
            }

            if (!res.ok) {
                const serverMsg = typeof res.data === "string" ?
                    res.data
                    :
                    (res.data?.message || JSON.stringify(res.data));
                alert("Aktualizacja nie powiodła się: " + (serverMsg || `status ${res.status}`));
            } else {
                alert("Konto usunięto pomyślnie.");
                await logout();


            }
        } catch (err) {
            console.error("Update failed:", err);
            alert("Błąd podczas zapisu: " + (err.message || err));
        } finally {
            setSaving(false);
        }
    }

    const navigate = useNavigate();

    const avatarText = avatar ?? user?.avatarImage ?? "AVATAR_DEFAULT";
    return (
        <div className="settings-panel-wrapper">

            <div className="container">
                <h2>Statystyki</h2>
                <div>
                    <span>Zgadnięto:</span>
                    <span> {user.totalGuesses} razy</span>
                </div>
                <div>
                    <span>Top 3:</span>
                    <span> 0 razy</span> {/* add user.timesTop3 or sth */}
                </div>
            </div>

            <div className="container">

                <h1>Ustawienia Konta</h1>

                <div className="profile-header">
                    <img className="avatar"
                         src={`./avatar/${avatarText}.jpg`}
                         alt="User Avatar"/>
                    <div className="profile-info">
                        <h2>{login}</h2>
                        <p>{user.email}</p>
                        <AvatarPicker onAvatarSelect={handleAvatarSelected} />
                        {/*<div style={{fontSize: 12, marginTop: 6}}>*/}
                        {/*    {avatar ? `Wybrano: ${avatar}` : (user?.avatarImage ? `Aktualny avatar: ${user.avatarImage}` : "Brak avatara")}*/}
                        {/*</div>*/}
                    </div>
                </div>

                <form action="#" method="POST" className="settings-form" onSubmit={handleSubmit}>

                    <div className="form-section">
                        <h3>Dane osobowe</h3>
                        <div className="form-group">
                            <label htmlFor="name">Imię i nazwisko</label>
                            <input type="text" name="name" id="name" value={login}
                                   onChange={(e) => setLogin(e.target.value)}
                                   minLength={3}
                                   maxLength={16}
                            />
                        </div>

                        <div className="form-group">
                            <label htmlFor="email">Adres email</label>
                            <input type="email" name="email" id="email" value={user?.email ?? ""}
                                   disabled
                            />
                        </div>
                    </div>

                    <div className="form-section">
                        <h3>Bezpieczeństwo</h3>
                        <div className="form-group">
                            <label>Hasło</label>
                            <button type="button" className="btn-link">
                                Zmień hasło
                            </button>
                        </div>
                    </div>

                    <div className="form-section">
                        <h3>Strefa niebezpieczna</h3>
                        <div>
                            <p>Ta operacja jest nieodwracalna. Wszystkie Twoje dane zostaną trwale usunięte!</p>
                            <br/>
                            <button type="button" className="btn btn-danger" onClick={handleDelete}>
                                Usuń konto
                            </button>

                        </div>
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="btn btn-logout" disabled={saving} onClick={() => logout()}>
                            Wyloguj się
                        </button>
                        <button type="button" className="btn btn-secondary" onClick={() => navigate('/')}>
                            Anuluj
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? "Zapisuję..." : "Zapisz zmiany"}
                        </button>
                    </div>

                </form>
            </div>
            <Footer/>
        </div>
    );
}


