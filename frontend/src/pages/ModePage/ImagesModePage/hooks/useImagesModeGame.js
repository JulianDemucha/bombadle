import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { apiFetch } from '../../../../api/api.js';
import { syncAnonymousWonModes } from '../../../../api/anonymousProgress.js';
import { useAuth } from '../../../../auth/UseAuth.jsx';
import confetti from 'canvas-confetti';
import {
    extractGuessAttempt,
    mapLeaderboardEntryToRow,
    normalizeKey,
    pickGuessListItems,
    pickLeaderboardItems
} from '../../ClassicModePage/utils/classicModeMappers.js';

// ----- ANIMATION CONFIG -----
const WIN_ANIMATION_DELAY_MS = 100;
const WIN_SCROLL_DURATION_MS = 700;
const DELAY_LEADERBOARD_MS = 450;
// ----------------------------

const SEARCH_INDEX_ENDPOINT = '/api/character-card/search-index';
const GET_GUESS_LIST_ENDPOINT = '/api/guess-list/images';
const GUESS_ENDPOINT_BASE = '/api/card-guessing/images/guess';

const LEADERBOARD_TOP3_ENDPOINT = '/api/leaderboard/IMAGES/top3';
const LEADERBOARD_PLAYER_ENDPOINT_BASE = '/api/leaderboard/IMAGES/player';
const LEADERBOARD_TODAY_SOLVERS_ENDPOINT = '/api/leaderboard/IMAGES/today-solvers';

const formatTimeLabel = (value) => {
    if (!value) return '--:--';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '--:--';
    return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
};

const triggerWinAnimation = () => {
    const count = 200;
    const defaults = { origin: { y: 0.7 } };
    const fire = (particleRatio, opts) => {
        confetti({
            ...defaults,
            ...opts,
            particleCount: Math.floor(count * particleRatio)
        });
    };

    fire(0.25, { spread: 50, startVelocity: 55, origin: { x: 0, y: 1 }, angle: 60 });
    fire(0.25, { spread: 50, startVelocity: 55, origin: { x: 1, y: 1 }, angle: 120 });
    fire(0.2, { spread: 60, startVelocity: 45, origin: { x: 0, y: 1 }, angle: 45 });
    fire(0.2, { spread: 60, startVelocity: 45, origin: { x: 1, y: 1 }, angle: 135 });
    fire(0.1, { spread: 120, decay: 0.91, scalar: 0.8, origin: { x: 0.5, y: 1 }, startVelocity: 60, angle: 90 });
};

const buildFallbackCurrentUserRow = (user, attempts, timeLabel) => ({
    rank: '-',
    name: user?.displayName || user?.login || 'Ty',
    time: timeLabel || '--:--',
    attempts: attempts > 0 ? attempts : '-',
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

const mapNameOnlyGuessToRow = (guessAttempt, selectedCard, idFallback) => {
    return {
        id: idFallback,
        name: guessAttempt?.name?.value || selectedCard?.name || '???',
        imageSrc: selectedCard?.imageSrc || '/avatar/AVATAR_DEFAULT.jpg',
        correct: guessAttempt?.name?.match === 'MATCH'
    };
};

function useImagesModeGame() {
    const { user, loading: authLoading } = useAuth();
    const [guesses, setGuesses] = useState([]);
    const [characterCards, setCharacterCards] = useState([]);
    const [isWon, setIsWon] = useState(false);
    const [isAnonymousAndWon, setIsAnonymousAndWon] = useState(false);
    const [isLeaderboardExpanded, setIsLeaderboardExpanded] = useState(false);
    const [isAnimatingSuccess, setIsAnimatingSuccess] = useState(false);
    const [topThree, setTopThree] = useState([]);
    const [todaySolvers, setTodaySolvers] = useState(0);
    const [currentUserRow, setCurrentUserRow] = useState(null);
    const [isCurrentUserInTopThree, setIsCurrentUserInTopThree] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [imageTimestamp, setImageTimestamp] = useState(Date.now()); // Wymuszanie pobrania nowej grafiki

    const winSectionRef = useRef(null);
    const scrollAnimationRef = useRef(null);
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
        latestGuessesCountRef.current = guesses.length;
    }, [guesses.length]);

    useEffect(() => {
        return () => {
            if (scrollAnimationRef.current) cancelAnimationFrame(scrollAnimationRef.current);
        };
    }, []);

    const smoothScrollToWinSection = useCallback(() => {
        const target = winSectionRef.current;
        if (!target) return;

        const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (prefersReducedMotion) {
            target.scrollIntoView({ block: 'center' });
            return;
        }

        const startY = window.scrollY;
        const targetRect = target.getBoundingClientRect();
        const targetY = startY + targetRect.top - (window.innerHeight - targetRect.height) / 2;
        const distance = targetY - startY;
        const startTime = performance.now();

        const easeInOutCubic = (t) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2);

        const step = (now) => {
            const elapsed = now - startTime;
            const progress = Math.min(elapsed / WIN_SCROLL_DURATION_MS, 1);
            const eased = easeInOutCubic(progress);

            window.scrollTo(0, startY + distance * eased);

            if (progress < 1) {
                scrollAnimationRef.current = requestAnimationFrame(step);
            } else {
                scrollAnimationRef.current = null;
            }
        };

        if (scrollAnimationRef.current) cancelAnimationFrame(scrollAnimationRef.current);
        scrollAnimationRef.current = requestAnimationFrame(step);
    }, []);

    const loadLeaderboard = useCallback(async () => {
        try {
            const topThreeResponse = await apiFetch(LEADERBOARD_TOP3_ENDPOINT);
            const topThreeRows = pickLeaderboardItems(topThreeResponse.data).map((entry, index) =>
                mapLeaderboardEntryToRow(entry, index, user)
            );
            setTopThree(topThreeRows);

            const isCurrentInTopThree = topThreeRows.some((row) => row.isCurrentUser);
            setIsCurrentUserInTopThree(isCurrentInTopThree);

            const solversResponse = await apiFetch(LEADERBOARD_TODAY_SOLVERS_ENDPOINT);
            if (solversResponse.ok && solversResponse.data) {
                const { loggedIn = 0, anonymous = 0 } = solversResponse.data;
                setTodaySolvers(loggedIn + anonymous);
            }

            if (isWon && user && !isCurrentInTopThree) {
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
                        console.error('Błąd pobierania danych gracza w leaderboardzie:', error);
                        setCurrentUserRow(buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current));
                    }
                }
            } else if (isWon && !user) {
                const fallbackRow = buildFallbackCurrentUserRow(null, latestGuessesCountRef.current, latestWinTimeLabelRef.current);
                setCurrentUserRow({ ...fallbackRow, currentStreak: 1 });
            } else {
                setCurrentUserRow(null);
            }

        } catch (error) {
            console.error('Błąd pobierania TOP3 leaderboardu:', error);
            setTopThree([]);
            setCurrentUserRow(null);
            setIsCurrentUserInTopThree(false);
        }
    }, [user, isWon]);

    const loadCharacterCards = useCallback(async () => {
        try {
            const response = await apiFetch(SEARCH_INDEX_ENDPOINT);
            setCharacterCards(response.data || []);
        } catch (error) {
            console.error('Błąd pobierania listy postaci:', error);
            setCharacterCards([]);
        }
    }, []);

    useEffect(() => {
        loadCharacterCards();
    }, [loadCharacterCards]);

    useEffect(() => {
        loadLeaderboard();
    }, [isWon, loadLeaderboard]);

    useEffect(() => {
        const loadGame = async () => {
            setIsLoading(true);
            try {
                const response = await apiFetch(GET_GUESS_LIST_ENDPOINT);
                const items = pickGuessListItems(response.data);

                // Dla anonima pobieramy sesję raz: ustawia flagi "won" + cookie i zwraca dto,
                // z którego czytamy czas zgadnięcia (scoreTimestamps), bo GuessAttempt nie ma timestampu.
                const anonymousSession = user ? null : await syncAnonymousWonModes();

                if (user && user.completedModesToday?.includes('IMAGES')) {
                    setIsWon(true);
                    setIsLeaderboardExpanded(true);
                    if (user.todayScoresTimestamps?.['IMAGES']) {
                        latestWinTimeLabelRef.current = formatTimeLabel(user.todayScoresTimestamps['IMAGES']);
                    }
                }

                if (items.length > 0) {
                    const lastGuess = items[items.length - 1];
                    const isLastGuessCorrect = extractGuessAttempt(lastGuess)?.name?.match === 'MATCH';

                    if (isLastGuessCorrect) {
                        setIsWon(true);
                        setIsLeaderboardExpanded(true);
                        if (user) {
                            if (user.todayScoresTimestamps?.['IMAGES']) {
                                latestWinTimeLabelRef.current = formatTimeLabel(user.todayScoresTimestamps['IMAGES']);
                            }
                        } else {
                            setIsAnonymousAndWon(true);
                            latestWinTimeLabelRef.current = formatTimeLabel(anonymousSession?.scoreTimestamps?.['IMAGES']);
                        }
                    }

                    const mappedRows = items.map((item, index) => {
                        const guessAttempt = extractGuessAttempt(item);
                        const selectedCard = findSelectedCard({ item, guessAttempt, cardsById, cardsByName });

                        return mapNameOnlyGuessToRow(
                            guessAttempt,
                            selectedCard,
                            item?.id ?? `${selectedCard?.id || 'guess'}-${index}`
                        );
                    });

                    setGuesses(mappedRows.reverse());
                } else {
                    setGuesses([]);
                }
            } catch (error) {
                console.error('Błąd ładowania stanu gry Images:', error);
            } finally {
                setIsLoading(false);
            }
        };

        if (characterCards.length > 0 && !authLoading) {
            loadGame();
        }
    }, [characterCards, user, authLoading, cardsById, cardsByName]);

    const handleSelectCharacterId = useCallback(async (cardId) => {
        if (isWon || isAnimatingSuccess) return;

        const selectedCard = cardsById[cardId];
        if (!selectedCard) return;

        try {
            const response = await apiFetch(`${GUESS_ENDPOINT_BASE}/${cardId}`, { method: 'POST' });

            const dataToExtract = response.data?.guessResponse || response.data;
            const guessAttempt = dataToExtract?.guessAttempt;
            const correct = dataToExtract?.correct || guessAttempt?.name?.match === 'MATCH';

            const newRow = {
                ...mapNameOnlyGuessToRow(
                    guessAttempt,
                    selectedCard,
                    `${cardId}-${Date.now()}`
                ),
                isNewAnimation: true
            };

            setGuesses((prev) => [newRow, ...prev]);

            setImageTimestamp(Date.now());

            if (correct) {
                const winTimestamp = Date.now();
                latestWinTimeLabelRef.current = formatTimeLabel(winTimestamp);

                if (!user) {
                    setIsAnonymousAndWon(true);
                }
                setIsAnimatingSuccess(true);

                setTimeout(() => {
                    triggerWinAnimation();
                    setIsWon(true);
                    setIsAnimatingSuccess(false);

                    setTimeout(() => {
                        smoothScrollToWinSection();
                    }, 150);

                    setTimeout(() => {
                        setIsLeaderboardExpanded(true);
                    }, DELAY_LEADERBOARD_MS);
                }, WIN_ANIMATION_DELAY_MS);
            }
        } catch (error) {
            console.error('Błąd wysyłania zgadnięcia:', error);
        }
    }, [cardsById, isAnimatingSuccess, isWon, smoothScrollToWinSection, user]);

    return {
        guesses,
        hasGuesses: guesses.length > 0,
        isWon,
        isAnonymousAndWon,
        isLeaderboardExpanded,
        isAnimatingSuccess,
        topThree,
        todaySolvers,
        currentUserRow,
        isCurrentUserInTopThree,
        handleSelectCharacterId,
        winSectionRef,
        isLoading,
        imageTimestamp
    };
}

export default useImagesModeGame;