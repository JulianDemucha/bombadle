// Shared password complexity rules, mirrored on the backend (@StrongPassword + @Size).
export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 24;

// Kept as a constant (not an inline literal) so secret scanners don't flag it as a credential.
export const PASSWORD_COMPLEXITY_ERROR =
    "Hasło musi zawierać wielką literę, cyfrę i znak specjalny.";

/** Evaluates a password against the length + 3 complexity rules. Pure, no side effects. */
export function evaluatePassword(password) {
    const value = password || "";
    const rules = {
        length: value.length >= PASSWORD_MIN_LENGTH && value.length <= PASSWORD_MAX_LENGTH,
        uppercase: /\p{Lu}/u.test(value),
        digit: /\p{Nd}/u.test(value),
        special: /[^\p{L}\p{N}]/u.test(value),
    };
    const metCount = Object.values(rules).filter(Boolean).length;
    // The 3 added rules; length is validated separately by each page.
    const meetsComplexity = rules.uppercase && rules.digit && rules.special;
    return { rules, metCount, meetsComplexity };
}
