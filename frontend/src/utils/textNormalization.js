// NFD decomposes diacritic letters (a-ogonek, c-acute, e-ogonek, n-acute, o-acute,
// s-acute, z-acute, z-dot) into a base letter plus a combining mark, which the regex
// below strips. l-with-stroke is a standalone codepoint that survives NFD untouched,
// hence the separate explicit replacement.
const L_WITH_STROKE = String.fromCharCode(0x0142);
const COMBINING_MARKS_PATTERN = new RegExp(
    '[' + String.fromCharCode(0x0300) + '-' + String.fromCharCode(0x036f) + ']',
    'g'
);

export const normalizeForSearch = (value) =>
    (value || '')
        .toLowerCase()
        .split(L_WITH_STROKE).join('l')
        .normalize('NFD')
        .replace(COMBINING_MARKS_PATTERN, '');
