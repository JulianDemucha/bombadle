import React from 'react';
import {
    CartesianGrid,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from 'recharts';

export default function StatisticsChart({ data, metricLabel, modeLabel }) {
    if (!data || data.length === 0) {
        return (
            <div className="statistics-chart statistics-chart--empty">
                <p>Brak danych dla wybranego trybu. Zagraj kilka dni, aby zobaczyć wykres.</p>
            </div>
        );
    }

    return (
        <div className="statistics-chart">
            <h3 className="statistics-chart__title">{`${metricLabel} — ${modeLabel}`}</h3>
            <ResponsiveContainer width="100%" height={320}>
                <LineChart data={data} margin={{ top: 10, right: 20, bottom: 10, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--primary)" opacity={0.2} />
                    <XAxis dataKey="puzzleDate" stroke="var(--primary)" tick={{ fontSize: 12 }} />
                    <YAxis stroke="var(--primary)" tick={{ fontSize: 12 }} allowDecimals={false} />
                    <Tooltip
                        labelStyle={{ color: 'var(--primary)' }}
                        contentStyle={{
                            backgroundColor: 'var(--secondary)',
                            border: '2px solid var(--primary)',
                            fontFamily: "'Courier New', monospace",
                        }}
                        formatter={(value) => [value, metricLabel]}
                    />
                    <Line
                        type="monotone"
                        dataKey="value"
                        name={metricLabel}
                        stroke="var(--accent)"
                        strokeWidth={2}
                        dot={{ r: 3 }}
                        activeDot={{ r: 5 }}
                        connectNulls
                    />
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
}
