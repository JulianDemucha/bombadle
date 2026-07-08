import React from "react";
import {useNavigate} from "react-router-dom";
import {useAuth} from "../auth/UseAuth.jsx";
import "./style/NavImgButton.css";

function NavImgButton({
                          to, imgSrc, altText, className, onError
                          , style, hideIfAuthenticated, hideIfNotAuthenticated
                      }) {
    const navigate = useNavigate();
    const {user} = useAuth();
    const handleClick = () => navigate(to);
    if ((hideIfAuthenticated && user) || (hideIfNotAuthenticated && !user)) {
        return (<></>);
    } else {
        return (
            <button type="button" className={className} onClick={handleClick} style={style}>
                <span className="nav-img-button__frame">
                    <img src={imgSrc} alt={altText} onError={onError}/>
                    {/* Overlay suppresses the Chromium Lens hover chip; see NavImgButton.css. */}
                    <span className="nav-img-button__overlay" aria-hidden="true"/>
                </span>
            </button>
        );
    }
}

export default NavImgButton;