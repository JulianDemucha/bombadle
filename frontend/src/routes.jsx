import MainPage from "./pages/MainPage/MainPage.jsx";
import Header from "./components/Header.jsx";
import React from "react";
import LoginPage from "./pages/LoginRegisterPage/LoginPage.jsx";
import Footer from "./components/Footer.jsx";
import RegisterPage from "./pages/LoginRegisterPage/RegisterPage.jsx";
import UnauthenticatedRoute from "./auth/UnauthenticatedRoute.jsx";
import AuthenticatedRoute from "./auth/AuthenticatedRoute.jsx";
import PlayerSettingsPage from "./pages/PlayerSettingsPage/PlayerSettingsPage.jsx";
import ClassicModePage from "./pages/ModePage/ClassicModePage/ClassicModePage.jsx";
import LeaderboardPage from "./pages/LeaderboardPage/LeaderboardPage.jsx";
import {Navigate} from "react-router-dom";

export const routes = [
    {path: '/', element: <> <MainPage/></>},
    {path: '/Login', element: <UnauthenticatedRoute><LoginPage/></UnauthenticatedRoute>},
    {path: '/Register', element: <UnauthenticatedRoute><RegisterPage/></UnauthenticatedRoute>},
    {path: '/Profile/', element: <AuthenticatedRoute><PlayerSettingsPage/></AuthenticatedRoute>},
    {path: '/Classic/', element: <ClassicModePage/>},
    {path: '/leaderboard/classic', element: <LeaderboardPage />},
    {path: '/login-success', element: <Navigate to="/" replace/>}
]