import React from 'react';
import './style/GlobalLoader.css';

const GlobalLoader = ({ text = "Ładowanie...", small = false }) => {
    return (
        <div className={`global-loader-wrapper ${small ? 'loader-small' : ''}`}>
            <div className="global-loader-squares">
                <div className="global-loader-square"></div>
                <div className="global-loader-square"></div>
                <div className="global-loader-square"></div>
            </div>
            <span className="global-loader-text">{text}</span>
        </div>
    );
};

export default GlobalLoader;