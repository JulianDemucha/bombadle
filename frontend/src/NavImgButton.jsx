import React from "react";
import { useNavigate } from "react-router-dom";

function NavImgButton({ to, imgSrc, altText, className, handleImageError: onError, style}) {
  const navigate = useNavigate();

  const handleClick = () => navigate(to);

  return (
    <button type="button" className={className} onClick={handleClick} onError={onError} style={style}>
      <img src={imgSrc} alt={altText} onError={(e) => (e.target.style.display = 'none')} />
    </button>
  );
}

export default NavImgButton;