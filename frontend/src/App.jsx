import React from 'react';
import './MainPage.jsx';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Header from "./Header.jsx";
import './style/login-button.css';
import {routes} from "./routes.jsx";

function App() {

    return (
        <>
            <BrowserRouter>
                <Header />

                <main style={{ minHeight: '80vh' }}>
                    <Routes>
                        {routes.map(r => (
                            <Route key={r.path} path={r.path} element={r.element} />
                        ))}
                    </Routes>
                </main>

            </BrowserRouter>
        </>
    );
}

export default App;
