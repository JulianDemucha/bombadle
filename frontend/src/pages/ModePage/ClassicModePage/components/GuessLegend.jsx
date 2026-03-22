import React from 'react';

function GuessLegend() {
    return (
        <div className="guess-legend" aria-label="Legenda statusow zgadywania">
            <div className="guess-legend-grid">
                <div className="guess-legend-item">
                    <span className="guess-legend-label">Imie</span>
                    <div className="guess-legend-tile guess-legend-name" />
                </div>
                <div className="guess-legend-item">
                    <span className="guess-legend-label">Poprawne</span>
                    <div className="guess-legend-tile guess-legend-correct" />
                </div>
                <div className="guess-legend-item">
                    <span className="guess-legend-label">Niepoprawne</span>
                    <div className="guess-legend-tile guess-legend-wrong" />
                </div>
                <div className="guess-legend-item">
                    <span className="guess-legend-label">Czesciowo<br />poprawne</span>
                    <div className="guess-legend-tile guess-legend-partial" />
                </div>
                <div className="guess-legend-item">
                    <span className="guess-legend-label">Wieksze</span>
                    <div className="guess-legend-tile guess-legend-wrong guess-legend-with-arrow">
                        <span className="guess-legend-arrow up" />
                    </div>
                </div>
                <div className="guess-legend-item">
                    <span className="guess-legend-label">Mniejsze</span>
                    <div className="guess-legend-tile guess-legend-wrong guess-legend-with-arrow">
                        <span className="guess-legend-arrow down" />
                    </div>
                </div>
            </div>
        </div>
    );
}

export default GuessLegend;

