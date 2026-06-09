import React, {useEffect} from 'react';
import './pages/MainPage/MainPage.jsx';
import {BrowserRouter, Routes, Route} from 'react-router-dom';
import {routes} from "./routes.jsx";
import {AuthProvider} from "./auth/AuthProvider.jsx";
import {setupSilentRefresh} from "./api/axios.js";

function App() {
    useEffect(() => {
        setupSilentRefresh();
    }, []);
    return (
        <>
            <BrowserRouter>
                <AuthProvider>
                    <main style={{minHeight: '80vh'}}>
                        <Routes>
                            {routes.map(r => (
                                <Route key={r.path} path={r.path} element={r.element}/>
                            ))}
                        </Routes>
                    </main>
                </AuthProvider>
            </BrowserRouter>
        </>
    );
}

export default App;
