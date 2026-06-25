import React from 'react';
import './TodaySolversInfo.css';

const TOOLTIP_TEXT = 'Ranking zawiera tylko graczy zalogowanych';

const TodaySolversInfo = ({ loggedIn = 0, anonymous = 0 }) => (
    <p className="today-solvers">
        <span className="today-solvers__text">
            dziś zgadło {loggedIn} zalogowanych oraz {anonymous} niezalogowanych
        </span>
        <span
            className="today-solvers__info"
            tabIndex={0}
            role="img"
            aria-label={TOOLTIP_TEXT}
        >
            ?
            <span className="today-solvers__tooltip" role="tooltip">
                {TOOLTIP_TEXT}
            </span>
        </span>
    </p>
);

export default TodaySolversInfo;
