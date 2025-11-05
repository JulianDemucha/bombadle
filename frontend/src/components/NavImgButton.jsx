import React from "react";
import {useNavigate} from "react-router-dom";
import {useAuth} from "../auth/UseAuth.jsx";

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
                <img src={imgSrc} alt={altText} onError={onError}/>
            </button>
        );
    }
}

export default NavImgButton;