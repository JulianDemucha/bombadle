import { useMemo } from "react";
import { evaluatePassword } from "./passwordRules";

const LEVEL_LABELS = { weak: "Słabe", medium: "Średnie", strong: "Silne" };

/** Derives strength level/label from a password using the shared rules. */
export default function usePasswordStrength(password) {
    return useMemo(() => {
        const { rules, metCount, meetsComplexity } = evaluatePassword(password);
        let level = "weak";
        if (metCount >= 4) level = "strong";
        else if (metCount >= 2) level = "medium";
        return {
            rules,
            metCount,
            meetsComplexity,
            level,
            label: password ? LEVEL_LABELS[level] : "",
        };
    }, [password]);
}
