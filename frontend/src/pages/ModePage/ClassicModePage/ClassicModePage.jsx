import React from 'react';
import './ClassicModePage.css';
import Footer from "../../../components/Footer.jsx";
import Header from "../../../components/Header.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import CharacterSearchBar from "../../../components/CharacterSearchBar.jsx";
import GuessList from "../../../components/GuessList.jsx";

function ClassicModePage() {
    return (
        <div className="classic-mode-page">
            <Header logoClassName='logo logo-75'/>
            <div className="classic-mode-content">
                <ImgTextBanner text = 'Zgadnij dzisiejszą postać' altText="ok"/>
                <CharacterSearchBar/>
                <GuessList/>
            </div>
            <Footer/>
        </div>
    );
}

export default ClassicModePage;
