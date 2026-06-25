import React from 'react';
import InfoTooltip from '../../../components/InfoTooltip';
import './TodaySolversInfo.css';

const TOOLTIP_TEXT = 'Ranking zawiera tylko graczy zalogowanych';

const TodaySolversInfo = ({ loggedIn = 0, anonymous = 0 }) => (
    <p className="today-solvers">
        dziś zgadło {loggedIn} zalogowanych oraz {anonymous} niezalogowanych
        <InfoTooltip text={TOOLTIP_TEXT} />
    </p>
);

export default TodaySolversInfo;
