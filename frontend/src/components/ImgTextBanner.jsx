import React from 'react';
import './style/ImgTextBanner.css';
import bannerImage from '../assets/TitleBanner.png';
const ImgTextBanner = ({ text }) => {
    return (
        <div className="image-wrapper">
            <img src={bannerImage} alt="Tło banera" className="background-image" />

            <div className="title-banner-container">
                <h1 className="title-banner-text">{text}</h1>
            </div>
        </div>
    );
};

export default ImgTextBanner;