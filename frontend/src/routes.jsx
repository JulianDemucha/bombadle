import MainPage from "./MainPage.jsx";
import Header from "./Header.jsx";
import React from "react";
import LoginPage from "./LoginPage.jsx";
import Footer from "./Footer.jsx";
import RegisterPage from "./RegisterPage.jsx";
import UnauthenticatedRoute from "./auth/UnauthenticatedRoute.jsx";
import AuthenticatedRoute from "./auth/AuthenticatedRoute.jsx";
import UserSettingsPage from "./UserSettingsPage.jsx";

export const routes = [
    { path: '/', element: <> <MainPage /></> },
    { path: '/Login', element: <UnauthenticatedRoute><LoginPage/></UnauthenticatedRoute> },
    { path: '/Register', element: <UnauthenticatedRoute><RegisterPage/></UnauthenticatedRoute> },
    { path: '/Profile/', element: <AuthenticatedRoute><UserSettingsPage/></AuthenticatedRoute> }
]