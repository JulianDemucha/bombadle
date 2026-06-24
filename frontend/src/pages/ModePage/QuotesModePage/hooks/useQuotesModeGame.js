import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { apiFetch } from '../../../../api/api.js';
import { syncAnonymousWonModes } from '../../../../api/anonymousProgress.js';
import { useAuth } from '../../../../auth/UseAuth.jsx';
import confetti from 'canvas-confetti';
import {
    extractGuessAttempt,
    mapGuessAttemptToRow,
    normalizeKey,
    pickLeaderboardItems,
    mapLeaderboardEntryToRow
} from '../../ClassicModePage/utils/classicModeMappers.js';

// ----- WIN ANIMATION CONFIG -----
const DELAY_CONFETTI_MS_STAGE_ONE = 100;
const DELAY_SCROLL_MS_STAGE_ONE = 300;
const DELAY_CONFETTI_MS_STAGE_TWO = 100;
const DELAY_SCROLL_MS_STAGE_TWO = 400;
const DELAY_LEADERBOARD_MS = 450;
// --------------------------------

const SEARCH_INDEX_ENDPOINT = '/api/character-card/search-index';
const QUOTE_GAME_STATE_ENDPOINT = '/api/card-guessing/quotes/prompt';
const STAGE_1_GUESS_ENDPOINT = '/api/card-guessing/quotes/guess';
const STAGE_2_GUESS_ENDPOINT = '/api/card-guessing/quotes/guess';

const LEADERBOARD_TOP3_ENDPOINT = '/api/leaderboard/QUOTES_STAGE_2/top3';
const LEADERBOARD_PLAYER_ENDPOINT_BASE = '/api/leaderboard/QUOTES_STAGE_2/player';

const formatTimeLabel = (value) => {
    if (!value) return '--:--';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
};

const buildFallbackCurrentUserRow = (user, attempts, timeLabel) => ({
    rank: '-',
    name: user?.displayName || user?.login || 'Ty',
    time: timeLabel || '--:--',
    attempts: attempts > 0 ? attempts : '-',
    wins: user?.totalGuesses ?? '?',
    avatar: user?.avatarImage ? `/avatar/${user.avatarImage}.jpg` : '/avatar/AVATAR_DEFAULT.jpg',
    isCurrentUser: true
});

const triggerWinAnimation = () => {
    const count = 200;
    const defaults = { origin: { y: 0.7 } };
    const fire = (particleRatio, opts) => {
        confetti({ ...defaults, ...opts, particleCount: Math.floor(count * particleRatio) });
    };
    fire(0.25, { spread: 50, startVelocity: 55, origin: { x: 0, y: 1 }, angle: 60 });
    fire(0.25, { spread: 50, startVelocity: 55, origin: { x: 1, y: 1 }, angle: 120 });
    fire(0.2, { spread: 60, startVelocity: 45, origin: { x: 0, y: 1 }, angle: 45 });
    fire(0.2, { spread: 60, startVelocity: 45, origin: { x: 1, y: 1 }, angle: 135 });
    fire(0.1, { spread: 120, decay: 0.91, scalar: 0.8, origin: { x: 0.5, y: 1 }, startVelocity: 60, angle: 90 });
};

const findSelectedCard = ({ item, guessAttempt, cardsById, cardsByName }) => {
    const cardId = item?.characterCardId ?? item?.cardId ?? guessAttempt?.characterCardId;
    const cardFromId = cardId ? cardsById[cardId] : null;
    const guessName = guessAttempt?.name?.value || item?.name?.value;
    const cardFromName = guessName ? cardsByName[normalizeKey(guessName)] : null;
    return cardFromId || cardFromName || null;
};

const extractStageTwoCorrect = (item, guessAttempt) => {
    return item?.name?.match === 'MATCH' || guessAttempt?.name?.match === 'MATCH';
};

const extractStageOneText = (item) => {
    if (typeof item === 'string') return item;
    if (item && typeof item === 'object') {
        return item.guess?.value || item.guess;
    }
    return null;
};

function useQuotesModeGame() {
    const { user } = useAuth();
    const [characterCards, setCharacterCards] = useState([]);
    const [prompt, setPrompt] = useState(null);

    const [stageOneGuesses, setStageOneGuesses] = useState([]);
    const [isStageOneWon, setIsStageOneWon] = useState(false);

    const [stageTwoGuesses, setStageTwoGuesses] = useState([]);
    const [isStageTwoWon, setIsStageTwoWon] = useState(false);
    const [isAnonymousAndWon, setIsAnonymousAndWon] = useState(false);
    const [isAnimatingSuccess, setIsAnimatingSuccess] = useState(false);

    const [isLeaderboardExpanded, setIsLeaderboardExpanded] = useState(false);
    const [topThree, setTopThree] = useState([]);
    const [currentUserRow, setCurrentUserRow] = useState(null);
    const [isCurrentUserInTopThree, setIsCurrentUserInTopThree] = useState(false);

    const winSectionRef = useRef(null);
    const stageTwoRef = useRef(null);
    const latestGuessesCountRef = useRef(0);
    const latestWinTimeLabelRef = useRef('--:--');

    const cardsById = useMemo(
        () => Object.fromEntries(characterCards.map((card) => [card.id, card])),
        [characterCards]
    );

    const cardsByName = useMemo(
        () => Object.fromEntries(characterCards.map((card) => [normalizeKey(card.name), card])),
        [characterCards]
    );

    useEffect(() => {
        latestGuessesCountRef.current = stageTwoGuesses.length;
    }, [stageTwoGuesses.length]);

    const loadLeaderboard = useCallback(async () => {
        try {
            const topThreeResponse = await apiFetch(LEADERBOARD_TOP3_ENDPOINT);
            const topThreeRows = pickLeaderboardItems(topThreeResponse.data).map((entry, index) =>
                mapLeaderboardEntryToRow(entry, index, user)
            );
            setTopThree(topThreeRows);

            const isCurrentInTopThree = topThreeRows.some((row) => row.isCurrentUser);
            setIsCurrentUserInTopThree(isCurrentInTopThree);

            if (isStageTwoWon && user && !isCurrentInTopThree) {
                const userId = user?.id ?? user?.playerId;
                if (userId) {
                    try {
                        const playerResponse = await apiFetch(`${LEADERBOARD_PLAYER_ENDPOINT_BASE}/${userId}`);
                        const playerEntry = playerResponse.data?.item || playerResponse.data;
                        if (playerEntry && typeof playerEntry === 'object') {
                            const mappedRow = mapLeaderboardEntryToRow(playerEntry, 0, user);
                            setCurrentUserRow({
                                ...buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current),
                                ...mappedRow,
                                name: mappedRow.name || user.login,
                                rank: mappedRow.rank ?? '-',
                                isCurrentUser: true
                            });
                        } else {
                            setCurrentUserRow(buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current));
                        }
                    } catch (error) {
                        console.error('Błąd pobierania danych gracza z leaderboardu:', error);
                        setCurrentUserRow(buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current));
                    }
                }
            } else if (isStageTwoWon && !user) {
                const winTime = localStorage.getItem('anonymousWinTime_QUOTES');
                latestWinTimeLabelRef.current = formatTimeLabel(winTime ? parseInt(winTime, 10) : null);
                const fallbackRow = buildFallbackCurrentUserRow(null, latestGuessesCountRef.current, latestWinTimeLabelRef.current);
                fallbackRow.wins = '?';
                setCurrentUserRow(fallbackRow);
            } else {
                setCurrentUserRow(null);
            }
        } catch (error) {
            console.error('Błąd pobierania TOP3 leaderboardu:', error);
            setTopThree([]);
            setCurrentUserRow(null);
            setIsCurrentUserInTopThree(false);
        }
    }, [user, isStageTwoWon]);

    useEffect(() => {
        loadLeaderboard();
    }, [isStageTwoWon, loadLeaderboard]);

    useEffect(() => {
        const initGame = async () => {
            try {
                const [cardsRes, stateRes] = await Promise.all([
                    apiFetch(SEARCH_INDEX_ENDPOINT),
                    apiFetch(QUOTE_GAME_STATE_ENDPOINT)
                ]);

                const fetchedCards = cardsRes.data || [];
                setCharacterCards(fetchedCards);

                const localCardsById = Object.fromEntries(fetchedCards.map((c) => [c.id, c]));
                const localCardsByName = Object.fromEntries(fetchedCards.map((c) => [normalizeKey(c.name), c]));

                const responseData = stateRes.data || {};
                const gameState = responseData.gameState || responseData;

                if (gameState.prompt) {
                    setPrompt(gameState.prompt);
                }

                setIsStageOneWon(!!gameState.isStageOnePassed);
                if (gameState.stageOneGuesses && gameState.stageOneGuesses.length > 0) {
                    const mappedS1 = gameState.stageOneGuesses.map(extractStageOneText).filter(Boolean);
                    setStageOneGuesses(mappedS1);
                }

                setIsStageTwoWon(!!gameState.isStageTwoPassed);
                if (gameState.stageTwoGuesses && gameState.stageTwoGuesses.length > 0) {
                    const mappedS2 = gameState.stageTwoGuesses.map((item, index) => {
                        const guessAttempt = extractGuessAttempt(item);
                        const selectedCard = findSelectedCard({ item, guessAttempt, cardsById: localCardsById, cardsByName: localCardsByName });

                        return {
                            ...mapGuessAttemptToRow(guessAttempt, selectedCard, `${selectedCard?.id || 's2guess'}-${index}`),
                            correct: extractStageTwoCorrect(item, guessAttempt),
                            isNewAnimation: false
                        };
                    });

                    setStageTwoGuesses(mappedS2.reverse());
                }

                if (gameState.isStageTwoPassed) {
                    setIsLeaderboardExpanded(true);
                    if (!user) {
                        setIsAnonymousAndWon(true);
                        const winTime = localStorage.getItem('anonymousWinTime_QUOTES');
                        if (winTime) {
                            latestWinTimeLabelRef.current = formatTimeLabel(parseInt(winTime, 10));
                        }
                    } else if (user && user.todayScoresTimestamps?.['QUOTES_STAGE_2']) {
                        latestWinTimeLabelRef.current = formatTimeLabel(user.todayScoresTimestamps['QUOTES_STAGE_2']);
                    }
                }

                if (!user) {
                    await syncAnonymousWonModes();
                }

            } catch (error) {
                console.error('Błąd pobierania zunifikowanego stanu gry Quotes:', error);
            }
        };

        initGame();
    }, [user]);

    const handleGuessStageOne = useCallback(async (selectedText) => {
        if (isStageOneWon) return;

        try {
            const res = await apiFetch(`${STAGE_1_GUESS_ENDPOINT}?guess=${encodeURIComponent(selectedText)}`, { method: 'POST' });
            const isCorrect = res.data?.correct || res.data?.guessResponse?.correct;

            setStageOneGuesses(prev => [...prev, selectedText]);

            if (isCorrect) {
                setIsStageOneWon(true);

                setTimeout(() => {
                    triggerWinAnimation();
                }, DELAY_CONFETTI_MS_STAGE_ONE);

                setTimeout(() => {
                    stageTwoRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }, DELAY_SCROLL_MS_STAGE_ONE);
            }
        } catch (error) {
            console.error('Błąd w Etapie 1:', error);
        }
    }, [isStageOneWon]);

    const handleGuessStageTwo = useCallback(async (cardId) => {
        if (!isStageOneWon || isStageTwoWon || isAnimatingSuccess) return;

        const selectedCard = cardsById[cardId];
        if (!selectedCard) return;

        try {
            const res = await apiFetch(`${STAGE_2_GUESS_ENDPOINT}/${cardId}`, { method: 'POST' });

            const dataToExtract = res.data?.guessResponse || res.data;
            const guessAttempt = dataToExtract?.guessAttempt;
            const correct = dataToExtract?.correct || guessAttempt?.name?.match === 'MATCH';

            const newRow = {
                ...mapGuessAttemptToRow(guessAttempt, selectedCard, `${cardId}-${Date.now()}`),
                correct: correct,
                isNewAnimation: true
            };

            setStageTwoGuesses(prev => [newRow, ...prev]);

            if (correct) {
                const winTimestamp = Date.now();
                latestWinTimeLabelRef.current = formatTimeLabel(winTimestamp);

                if (!user) {
                    setIsAnonymousAndWon(true);
                    localStorage.setItem('anonymousWinTime_QUOTES', winTimestamp.toString());
                }
                setIsAnimatingSuccess(true);

                setTimeout(() => {
                    triggerWinAnimation();
                    setIsStageTwoWon(true);
                    setIsAnimatingSuccess(false);
                }, DELAY_CONFETTI_MS_STAGE_TWO);

                setTimeout(() => {
                    winSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }, DELAY_SCROLL_MS_STAGE_TWO);

                setTimeout(() => {
                    setIsLeaderboardExpanded(true);
                }, DELAY_LEADERBOARD_MS);
            }
        } catch (error) {
            console.error('Błąd w Etapie 2:', error);
        }
    }, [cardsById, isStageOneWon, isStageTwoWon, isAnimatingSuccess, user]);

    return {
        prompt,
        stageOneGuesses,
        isStageOneWon,
        handleGuessStageOne,
        stageTwoGuesses,
        isStageTwoWon,
        isAnonymousAndWon,
        handleGuessStageTwo,
        isAnimatingSuccess,
        winSectionRef,
        stageTwoRef,
        isLeaderboardExpanded,
        topThree,
        currentUserRow,
        isCurrentUserInTopThree
    };
}

export default useQuotesModeGame;