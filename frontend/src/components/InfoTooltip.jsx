import React from 'react';
import './style/InfoTooltip.css';

const InfoTooltip = ({ text, className = '' }) => (
    <span className={`info-tooltip ${className}`.trim()}>
        <span
            className="info-tooltip__icon"
            tabIndex={0}
            role="img"
            aria-label={text}
        >
            ?
            <span className="info-tooltip__bubble" role="tooltip">
                {text}
            </span>
        </span>
    </span>
);

export default InfoTooltip;
