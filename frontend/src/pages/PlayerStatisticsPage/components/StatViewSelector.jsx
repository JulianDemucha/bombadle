import React from 'react';

const ButtonGroup = ({ legend, options, selected, onSelect }) => (
    <div className="stat-selector__group">
        <span className="stat-selector__legend">{legend}</span>
        <div className="stat-selector__buttons">
            {options.map((option) => (
                <button
                    type="button"
                    key={option.value}
                    className={
                        'stat-selector__button' +
                        (option.value === selected ? ' stat-selector__button--active' : '')
                    }
                    onClick={() => onSelect(option.value)}
                >
                    {option.label}
                </button>
            ))}
        </div>
    </div>
);

export default function StatViewSelector({
    availableModes,
    selectedMode,
    onModeChange,
    availableMetrics,
    selectedMetric,
    onMetricChange,
}) {
    return (
        <div className="stat-selector">
            <ButtonGroup legend="Tryb" options={availableModes} selected={selectedMode} onSelect={onModeChange} />
            <ButtonGroup legend="Dane" options={availableMetrics} selected={selectedMetric} onSelect={onMetricChange} />
        </div>
    );
}
