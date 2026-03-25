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

const pickLeaderboardItems = (data) => {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.leaderboard)) return data.leaderboard;
    if (Array.isArray(data?.topThree)) return data.topThree;
    if (Array.isArray(data?.items)) return data.items;
    if (Array.isArray(data?.data)) return data.data;
    return [];
};

const toAvatarPath = (avatarImage) => {
    if (!avatarImage) return '/avatar/AVATAR_DEFAULT.jpg';
    if (String(avatarImage).startsWith('/')) return avatarImage;
    return `/avatar/${avatarImage}.jpg`;
};

const formatLeaderboardTime = (value) => {
    if (!value) return '--:--';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
};

const mapLeaderboardEntryToRow = (entry, index, currentUser) => {
    const rank = entry?.rank ?? index + 1;
    const playerId = entry?.playerId ?? entry?.id ?? null;
    const playerName =
        entry?.playerLogin ||
        entry?.login ||
        entry?.name ||
        entry?.username ||
        (playerId ? `Gracz #${playerId}` : `Gracz #${rank}`);
    const wins = entry?.wins ?? entry?.totalGuesses ?? 0;
    const attempts = entry?.numberOfTries ?? entry?.attempts ?? '-';
    const userId = currentUser?.id ?? currentUser?.playerId ?? null;
    const userLogin = String(currentUser?.login ?? '').toLowerCase();
    const isCurrentUser =
        (playerId !== null && userId !== null && String(playerId) === String(userId)) ||
        (Boolean(userLogin) && String(playerName).toLowerCase() === userLogin);

    return {
        rank,
        name: playerName,
        attempts,
        time: formatLeaderboardTime(entry?.scoreTimeStamp ?? entry?.time),
        wins,
        avatar: toAvatarPath(entry?.playerAvatarImage ?? entry?.avatarImage ?? entry?.avatar),
        playerId,
        isCurrentUser
    };
};

const findCurrentUserRow = (entries, currentUser) => {
    const mappedRows = pickLeaderboardItems(entries).map((entry, index) =>
        mapLeaderboardEntryToRow(entry, index, currentUser)
    );
    return mappedRows.find((row) => row.isCurrentUser) || null;
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
    mapGuessAttemptToRow,
    pickLeaderboardItems,
    mapLeaderboardEntryToRow,
    findCurrentUserRow
};

