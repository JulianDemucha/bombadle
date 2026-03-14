import React from 'react';
import Footer from "../../../components/Footer.jsx";
import Header from "../../../components/Header.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import CharacterSearchBar from "../../../components/CharacterSearchBar.jsx";
import GuessList from "../../../components/GuessList.jsx";

function ClassicModePage() {
    const handleImageError = (e) => {
        //todo make placeholders for all img / buttons
        e.target.src = 'https://placehold.co/544x192/9E6B5D/FFFFFF?text=Przycisk&font=sans-serif';
    };

    return (
        <>
            <Header logoClassName='logo logo-75'/>
            <br/>
            <ImgTextBanner text = 'Zgadnij dzisiejszą postać' altText="ok"/>
            <CharacterSearchBar/>
            <GuessList/>
            <Footer/>
        </>
    );
}

export default ClassicModePage;
