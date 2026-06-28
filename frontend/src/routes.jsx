import MainPage from "./pages/MainPage/MainPage.jsx";
import React from "react";
import LoginPage from "./pages/LoginRegisterPage/LoginPage.jsx";
import RegisterPage from "./pages/LoginRegisterPage/RegisterPage.jsx";
import UnauthenticatedRoute from "./auth/UnauthenticatedRoute.jsx";
import AuthenticatedRoute from "./auth/AuthenticatedRoute.jsx";
import PlayerSettingsPage from "./pages/PlayerSettingsPage/PlayerSettingsPage.jsx";
import PlayerStatisticsPage from "./pages/PlayerStatisticsPage/PlayerStatisticsPage.jsx";
import ClassicModePage from "./pages/ModePage/ClassicModePage/ClassicModePage.jsx";
import QuotesModePage from "./pages/ModePage/QuotesModePage/QuotesModePage.jsx";
import ImagesModePage from "./pages/ModePage/ImagesModePage/ImagesModePage.jsx";
import LeaderboardPage from "./pages/LeaderboardPage/LeaderboardPage.jsx";
import StreakLeaderboardPage from "./pages/StreakLeaderboardPage/StreakLeaderboardPage.jsx";
import EmailVerificationPage from "./pages/LoginRegisterPage/EmailVerificationPage.jsx";
import ForgotPasswordPage from "./pages/LoginRegisterPage/ForgotPasswordPage.jsx";
import {Navigate} from "react-router-dom";

export const routes = [
    {path: '/', element: <> <MainPage/></>},
    {path: '/Login', element: <UnauthenticatedRoute><LoginPage/></UnauthenticatedRoute>},
    {path: '/Register', element: <UnauthenticatedRoute><RegisterPage/></UnauthenticatedRoute>},
    {path: '/verify-email', element: <UnauthenticatedRoute><EmailVerificationPage/></UnauthenticatedRoute>},
    {path: '/forgot-password', element: <UnauthenticatedRoute><ForgotPasswordPage/></UnauthenticatedRoute>},
    {path: '/Profile/', element: <AuthenticatedRoute><PlayerSettingsPage/></AuthenticatedRoute>},
    {path: '/statistics', element: <AuthenticatedRoute><PlayerStatisticsPage/></AuthenticatedRoute>},
    {path: '/Classic', element: <ClassicModePage/>},
    {path: '/Quotes', element: <QuotesModePage/>},
    {path: '/Images', element: <ImagesModePage/>},
    {path: '/leaderboard/streak', element: <StreakLeaderboardPage variant="streak"/>},
    {path: '/leaderboard/superstreak', element: <StreakLeaderboardPage variant="superstreak"/>},
    {path: '/leaderboard/:mode', element: <LeaderboardPage />},
    {path: '/leaderboard', element: <Navigate to="/leaderboard/classic" replace/>},
    {path: '/login-success', element: <Navigate to="/" replace/>}
]