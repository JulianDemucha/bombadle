const normalizeLabel = (value) =>
    String(value ?? '')
        .replaceAll('_', ' ')
        .toLowerCase()
        .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());

const mapGenderLabel = (value) => {
    const normalized = String(value ?? '').trim().toLowerCase();
    if (normalized === 'male') return 'Mężczyzna';
    if (normalized === 'female') return 'Kobieta';
    return normalizeLabel(value);
};

const mapMatchToTileStatus = (match) => {
    if (match === 'MATCH') return 'correct';
    if (match === 'NOT_MATCH') return 'wrong';
    if (match === 'HIGHER' || match === 'LOWER') return 'wrong';
    return 'partial';
};

const normalizeKey = (value) =>
    String(value ?? '')
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');

const extractGuessAttempt = (entry) => entry?.guessAttempt || entry;

const pickGuessListItems = (data) => {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.guessList)) return data.guessList;
    if (Array.isArray(data?.guesses)) return data.guesses;
    if (Array.isArray(data?.items)) return data.items;
    return [];
};

const mapGuessAttemptToRow = (guessAttempt, selectedCard, fallbackId) => ({
    id: fallbackId,
    name: guessAttempt?.name?.value || selectedCard?.name || 'Nieznana postac',
    imageSrc: selectedCard?.imageSrc || selectedCard?.image_src || '/avatar/AVATAR_DEFAULT.jpg',
    gender: mapGenderLabel(guessAttempt?.gender?.value),
    race: normalizeLabel(guessAttempt?.race?.value),
    isAlive: guessAttempt?.alive?.value ? 'Tak' : 'Nie',
    colors: Array.isArray(guessAttempt?.colors?.value)
        ? guessAttempt.colors.value.map(normalizeLabel).join(', ')
        : normalizeLabel(guessAttempt?.colors?.value),
    affiliation: Array.isArray(guessAttempt?.affiliations?.value)
        ? guessAttempt.affiliations.value.map(normalizeLabel).join(', ')
        : normalizeLabel(guessAttempt?.affiliations?.value),
    firstAppearance: String(guessAttempt?.firstAppearanceEpisode?.value ?? ''),
    status: {
        name: mapMatchToTileStatus(guessAttempt?.name?.match),
        gender: mapMatchToTileStatus(guessAttempt?.gender?.match),
        race: mapMatchToTileStatus(guessAttempt?.race?.match),
        isAlive: mapMatchToTileStatus(guessAttempt?.alive?.match),
        colors: mapMatchToTileStatus(guessAttempt?.colors?.match),
        affiliation: mapMatchToTileStatus(guessAttempt?.affiliations?.match),
        firstAppearance: mapMatchToTileStatus(guessAttempt?.firstAppearanceEpisode?.match)
    },
    meta: {
        firstAppearanceDirection: guessAttempt?.firstAppearanceEpisode?.match
    }
});

export {
    normalizeKey,
    extractGuessAttempt,
    pickGuessListItems,
    mapGuessAttemptToRow
};

