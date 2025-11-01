import React from 'react';
import './MainPage.jsx';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import './style/login-button.css';
import {routes} from "./routes.jsx";
import {AuthProvider} from "./auth/AuthProvider.jsx";

function App() {

    return (
        <>
            <BrowserRouter>
                <AuthProvider>
                <main style={{ minHeight: '80vh' }}>
                    <Routes>
                        {routes.map(r => (
                            <Route key={r.path} path={r.path} element={r.element} />
                        ))}
                    </Routes>
                </main>
                </AuthProvider>
            </BrowserRouter>
        </>
    );
}

export default App;
