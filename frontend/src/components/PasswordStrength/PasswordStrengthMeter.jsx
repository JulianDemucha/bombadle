import React from "react";
import "./password-strength.css";
import usePasswordStrength from "./usePasswordStrength";

const RULE_LABELS = [
    ["length", "8–24 znaki"],
    ["uppercase", "Wielka litera"],
    ["digit", "Cyfra"],
    ["special", "Znak specjalny"],
];

/** Strength bar + rule checklist for a password field. Renders nothing until typing starts. */
export default function PasswordStrengthMeter({ password }) {
    const { rules, metCount, level, label } = usePasswordStrength(password);

    if (!password) return null;

    return (
        <div className="password-strength" aria-live="polite">
            <div className={`password-strength__bar password-strength__bar--${level}`}>
                {[0, 1, 2, 3].map((i) => (
                    <span
                        key={i}
                        className={
                            "password-strength__segment" +
                            (i < metCount ? " password-strength__segment--filled" : "")
                        }
                    />
                ))}
            </div>
            <span className="password-strength__label">Siła hasła: {label}</span>
            <ul className="password-strength__rules">
                {RULE_LABELS.map(([key, text]) => (
                    <li
                        key={key}
                        className={
                            "password-strength__rule" +
                            (rules[key] ? " password-strength__rule--met" : "")
                        }
                    >
                        {rules[key] ? "✓" : "•"} {text}
                    </li>
                ))}
            </ul>
        </div>
    );
}
