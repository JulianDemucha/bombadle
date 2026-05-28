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

const WIN_ANIMATION_DELAY_MS = 5900;
const WIN_SCROLL_DURATION_MS = 700;
const SEARCH_INDEX_ENDPOINT = '/api/character-card/search-index';
const GUESS_LIST_ENDPOINT = '/api/guess-list/classic/player/'; // +user.id
const ANONYMOUS_RECOVERY_ENDPOINT = '/api/players/anonymous/me';
const GUESS_ENDPOINT_BASE = '/api/card-guessing/classic/guess';
const ANONYMOUS_GUESS_ENDPOINT_BASE = '/api/card-guessing/classic/anonymous-guess';
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
    wins: user?.totalGuesses ?? '?',
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
    const [isAnonymousAndWon, setIsAnonymousAndWon] = useState(false);
    const [isLeaderboardExpanded, setIsLeaderboardExpanded] = useState(false);
    const [isAnimatingSuccess, setIsAnimatingSuccess] = useState(false);
    const [topThree, setTopThree] = useState([]);
    const [currentUserRow, setCurrentUserRow] = useState(null);
    const [isCurrentUserInTopThree, setIsCurrentUserInTopThree] = useState(false);
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
            if (scrollAnimationRef.current) {
                cancelAnimationFrame(scrollAnimationRef.current);
            }
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

        const easeInOutCubic = (t) => (t < 0.5
            ? 4 * t * t * t
            : 1 - Math.pow(-2 * t + 2, 3) / 2);

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

        if (scrollAnimationRef.current) {
            cancelAnimationFrame(scrollAnimationRef.current);
        }
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
                        console.error('Blad pobierania danych gracza w leaderboardzie:', error);
                        setCurrentUserRow(buildFallbackCurrentUserRow(user, latestGuessesCountRef.current, latestWinTimeLabelRef.current));
                    }
                }
            } else if (isWon && !user) {
                const winTime = localStorage.getItem('anonymousWinTime');
                latestWinTimeLabelRef.current = formatTimeLabel(winTime ? parseInt(winTime, 10) : null);
                const fallbackRow = buildFallbackCurrentUserRow(null, latestGuessesCountRef.current, latestWinTimeLabelRef.current);
                fallbackRow.wins = '?';
                setCurrentUserRow(fallbackRow);
            } else {
                setCurrentUserRow(null);
            }

        } catch (error) {
            console.error('Blad pobierania TOP3 leaderboardu:', error);
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
            console.error('Blad pobierania listy postaci:', error);
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
            if (user) {
                // Logged-in user logic
                try {
                    const response = await apiFetch(GUESS_LIST_ENDPOINT + user.id);
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
            } else {
                // Not-Logged-in user logic
                const today = new Date().toISOString().split('T')[0];
                const lastPlayed = localStorage.getItem('lastPlayedDate');
                const now = new Date();

                if (lastPlayed !== today && now.getHours() >= 7) {
                    localStorage.removeItem('anonymousGuessList');
                    localStorage.removeItem('anonymousWinTime');
                    localStorage.setItem('lastPlayedDate', today);
                }

                let storedGuesses = JSON.parse(localStorage.getItem('anonymousGuessList') || '[]');

                if (storedGuesses.length === 0) {
                    try {
                        const res = await apiFetch(ANONYMOUS_RECOVERY_ENDPOINT);
                        const sessionData = res.data;

                        // POPRAWKA: .guessList.guessList zamiast .guessList.guesses
                        if (sessionData && sessionData.guessList && sessionData.guessList.guessList) {
                            const recoveredItems = sessionData.guessList.guessList;

                            if (recoveredItems.length > 0) {
                                storedGuesses = recoveredItems.map(item => ({
                                    guessAttempt: item,
                                    correct: item.correct || item.name?.match === 'MATCH'
                                }));
                                localStorage.setItem('anonymousGuessList', JSON.stringify(storedGuesses));
                            }

                            if (sessionData.hasGuessedToday) {
                                setIsWon(true);
                                setIsAnonymousAndWon(true);
                                setIsLeaderboardExpanded(true);

                                if (sessionData.scoreTimestamp) {
                                    const winTime = new Date(sessionData.ScoreTimestamp).getTime();
                                    localStorage.setItem('anonymousWinTime', winTime.toString());
                                    latestWinTimeLabelRef.current = formatTimeLabel(winTime);
                                }
                            }
                        }
                    } catch (error) {
                        console.error('Brak anonimowej sesji na serwerze lub błąd pobierania', error);
                    }
                }

                const lastGuess = storedGuesses[storedGuesses.length - 1];
                if (lastGuess && lastGuess.correct) {
                    setIsWon(true);
                    setIsAnonymousAndWon(true);
                    setIsLeaderboardExpanded(true);
                }

                const mappedRows = storedGuesses.map((item, index) => {
                    const guessAttempt = item.guessAttempt;
                    const selectedCard = findSelectedCard({ item, guessAttempt, cardsById, cardsByName });
                    return mapGuessAttemptToRow(guessAttempt, selectedCard, `${selectedCard?.id || 'guess'}-${index}`);
                });

                setGuesses(mappedRows.reverse());
            }
        };

        if (characterCards.length > 0) {
            loadGame();
        }
    }, [characterCards, user, cardsById, cardsByName]);

    const handleSelectCharacterId = useCallback(async (cardId) => {
        if (isWon || isAnimatingSuccess) return;

        const selectedCard = cardsById[cardId];
        if (!selectedCard) return;

        try {
            let response;
            if (user) {
                response = await apiFetch(`${GUESS_ENDPOINT_BASE}/${cardId}`, { method: 'POST' });
            } else {
                response = await apiFetch(`${ANONYMOUS_GUESS_ENDPOINT_BASE}/${cardId}`, { method: 'POST' });
            }

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

            if (!user) {
                const anonymousGuesses = JSON.parse(localStorage.getItem('anonymousGuessList') || '[]');
                anonymousGuesses.push({ ...response.data, characterCardId: cardId });
                localStorage.setItem('anonymousGuessList', JSON.stringify(anonymousGuesses));
            }

            if (correct) {
                const winTimestamp = Date.now();
                latestWinTimeLabelRef.current = formatTimeLabel(winTimestamp);
                if (!user) {
                    localStorage.setItem('anonymousWinTime', winTimestamp);
                    setIsAnonymousAndWon(true);
                }
                setIsAnimatingSuccess(true);

                setTimeout(() => {
                    setIsWon(true);
                    setIsAnimatingSuccess(false);

                    setTimeout(() => {
                        smoothScrollToWinSection();
                    }, 150);

                    setTimeout(() => {
                        setIsLeaderboardExpanded(true);
                    }, 250);
                }, WIN_ANIMATION_DELAY_MS);
            }
        } catch (error) {
            console.error('Blad wysylania guessa:', error);
        }
    }, [cardsById, isAnimatingSuccess, isWon, smoothScrollToWinSection, user, cardsByName]);

    return {
        guesses,
        hasGuesses: guesses.length > 0,
        isWon,
        isAnonymousAndWon,
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
