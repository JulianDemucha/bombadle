import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { apiFetch } from '../../../../api/api.js';
import { useAuth } from '../../../../auth/UseAuth.jsx';
import {
    extractGuessAttempt,
    mapLeaderboardEntryToRow,
    mapGuessAttemptToRow,
    normalizeKey,
    pickGuessListItems,
    pickLeaderboardItems
} from '../utils/classicModeMappers.js';

const WIN_ANIMATION_DELAY_MS = 6000;
const SEARCH_INDEX_ENDPOINT = '/api/character-card/search-index';
const GUESS_LIST_ENDPOINT = '/api/card-guessing/classic/guess-list';
const GUESS_ENDPOINT_BASE = '/api/card-guessing/classic/guess';
const LEADERBOARD_TOP3_ENDPOINT = '/api/leaderboard/top3';
const LEADERBOARD_PLAYER_ENDPOINT_BASE = '/api/leaderboard/player';

const formatTimeLabel = (value) => {
    if (!value) return '--:--';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
};

const pickGuessTimestamp = (entry) =>
    entry?.scoreTimeStamp ||
    entry?.timestamp ||
    entry?.timeStamp ||
    entry?.createdAt ||
    entry?.guessAttempt?.scoreTimeStamp ||
    null;

const buildFallbackCurrentUserRow = (user, attempts, timeLabel) => ({
    rank: '-',
    name: user?.login || 'Ty',
    time: timeLabel || '--:--',
    attempts: attempts > 0 ? attempts : '-',
    wins: user?.totalGuesses ?? 0,
    avatar: user?.avatarImage ? `/avatar/${user.avatarImage}.jpg` : '/avatar/AVATAR_DEFAULT.jpg',
    isCurrentUser: true
});

const findSelectedCard = ({ item, guessAttempt, cardsById, cardsByName }) => {
    const cardId = item?.characterCardId ?? item?.cardId ?? guessAttempt?.characterCardId;
    const cardFromId = cardId ? cardsById[cardId] : null;
    const guessName = guessAttempt?.name?.value;
    const cardFromName = guessName ? cardsByName[normalizeKey(guessName)] : null;
    return cardFromId || cardFromName || null;
};

function useClassicModeGame() {
    const { user } = useAuth();
    const [guesses, setGuesses] = useState([]);
    const [characterCards, setCharacterCards] = useState([]);
    const [isWon, setIsWon] = useState(false);
    const [isLeaderboardExpanded, setIsLeaderboardExpanded] = useState(false);
    const [isAnimatingSuccess, setIsAnimatingSuccess] = useState(false);
    const [topThree, setTopThree] = useState([]);
    const [currentUserRow, setCurrentUserRow] = useState(null);
    const [isCurrentUserInTopThree, setIsCurrentUserInTopThree] = useState(false);
    const winSectionRef = useRef(null);
    const latestGuessesCountRef = useRef(0);
    const latestWinTimeLabelRef = useRef('--:--');

    useEffect(() => {
        latestGuessesCountRef.current = guesses.length;
    }, [guesses.length]);

    const loadLeaderboard = useCallback(async (includeCurrentUserRow) => {
        try {
            const topThreeResponse = await apiFetch(LEADERBOARD_TOP3_ENDPOINT);
            const topThreeRows = pickLeaderboardItems(topThreeResponse.data).map((entry, index) =>
                mapLeaderboardEntryToRow(entry, index, user)
            );

            const isCurrentInTopThree = topThreeRows.some((row) => row.isCurrentUser);
            setTopThree(topThreeRows);
            setIsCurrentUserInTopThree(isCurrentInTopThree);

            if (!includeCurrentUserRow || isCurrentInTopThree) {
                setCurrentUserRow(null);
                return;
            }

            const userId = user?.id ?? user?.playerId;
            if (!userId) {
                setCurrentUserRow(buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current));
                return;
            }

            try {
                const playerResponse = await apiFetch(`${LEADERBOARD_PLAYER_ENDPOINT_BASE}/${userId}`);
                const fallbackRow = buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current);
                const playerEntry = playerResponse.data?.item || playerResponse.data;

                if (!playerEntry || typeof playerEntry !== 'object') {
                    setCurrentUserRow(fallbackRow);
                    return;
                }

                const mappedRow = mapLeaderboardEntryToRow(playerEntry, 0, user);
                setCurrentUserRow({
                    ...fallbackRow,
                    ...mappedRow,
                    name: mappedRow.name || fallbackRow.name,
                    rank: mappedRow.rank ?? fallbackRow.rank,
                    isCurrentUser: true
                });
            } catch (error) {
                console.error('Blad pobierania danych gracza w leaderboardzie:', error);
                setCurrentUserRow(buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current));
            }
        } catch (error) {
            console.error('Blad pobierania TOP3 leaderboardu:', error);
            setTopThree([]);
            setCurrentUserRow(null);
            setIsCurrentUserInTopThree(false);
        }
    }, [user]);

    const loadCharacterCards = useCallback(async () => {
        try {
            const response = await apiFetch(SEARCH_INDEX_ENDPOINT);
            setCharacterCards(response.data || []);
        } catch (error) {
            console.error('Blad pobierania listy postaci:', error);
            setCharacterCards([]);
        }
    }, []);

    useEffect(() => {
        loadCharacterCards();
    }, [loadCharacterCards]);

    useEffect(() => {
        loadLeaderboard(isWon);
    }, [isWon, loadLeaderboard]);

    const cardsById = useMemo(
        () => Object.fromEntries(characterCards.map((card) => [card.id, card])),
        [characterCards]
    );

    const cardsByName = useMemo(
        () => Object.fromEntries(characterCards.map((card) => [normalizeKey(card.name), card])),
        [characterCards]
    );

    useEffect(() => {
        const loadGuessList = async () => {
            try {
                const response = await apiFetch(GUESS_LIST_ENDPOINT);
                const items = pickGuessListItems(response.data);

                const lastGuess = items[items.length - 1];
                if (extractGuessAttempt(lastGuess)?.name?.match === 'MATCH') {
                    latestWinTimeLabelRef.current = formatTimeLabel(pickGuessTimestamp(lastGuess));
                    setIsWon(true);
                    setIsLeaderboardExpanded(true);
                }

                const mappedRows = items.map((item, index) => {
                    const guessAttempt = extractGuessAttempt(item);
                    const selectedCard = findSelectedCard({ item, guessAttempt, cardsById, cardsByName });

                    return mapGuessAttemptToRow(
                        guessAttempt,
                        selectedCard,
                        item?.id ?? `${selectedCard?.id || 'guess'}-${index}`
                    );
                });

                setGuesses(mappedRows.reverse());
            } catch (error) {
                console.error('Blad pobierania guess-list:', error);
            }
        };

        loadGuessList();
    }, [cardsById, cardsByName]);

    const handleSelectCharacterId = useCallback(async (cardId) => {
        if (isWon || isAnimatingSuccess) return;

        const selectedCard = cardsById[cardId];
        if (!selectedCard) return;

        try {
            const response = await apiFetch(`${GUESS_ENDPOINT_BASE}/${cardId}`, {
                method: 'POST'
            });

            const guessAttempt = response.data?.guessAttempt;
            const correct = response.data?.correct || guessAttempt?.name?.match === 'MATCH';

            const newRow = {
                ...mapGuessAttemptToRow(
                    guessAttempt,
                    selectedCard,
                    `${cardId}-${Date.now()}`
                ),
                isNewAnimation: true
            };
            setGuesses((prev) => [newRow, ...prev]);

            if (correct) {
                latestWinTimeLabelRef.current = formatTimeLabel(Date.now());
                setIsAnimatingSuccess(true);

                setTimeout(() => {
                    setIsWon(true);
                    setIsAnimatingSuccess(false);

                    setTimeout(() => {
                        winSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }, 0);

                    setTimeout(() => {
                        setIsLeaderboardExpanded(true);
                    }, 250);
                }, WIN_ANIMATION_DELAY_MS);
            }
        } catch (error) {
            console.error('Blad wysylania guessa:', error);
        }
    }, [cardsById, isAnimatingSuccess, isWon]);

    return {
        guesses,
        hasGuesses: guesses.length > 0,
        isWon,
        isLeaderboardExpanded,
        isAnimatingSuccess,
        topThree,
        currentUserRow,
        isCurrentUserInTopThree,
        handleSelectCharacterId,
        winSectionRef
    };
}

export default useClassicModeGame;

