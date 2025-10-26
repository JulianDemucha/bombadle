import MainPage from "./MainPage.jsx";
import Header from "./Header.jsx";
import React from "react";
import LoginPage from "./LoginPage.jsx";
import Footer from "./Footer.jsx";
import RegisterPage from "./RegisterPage.jsx";

export const routes = [
    { path: '/', element: <> <Header /> <MainPage/> <Footer /></> },
    { path: '/Login', element: <><LoginPage/></> },
    { path: '/Register', element: <><RegisterPage/></> }
]