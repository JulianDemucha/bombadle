import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { apiFetch } from '../../../../api/api.js';
import {
    extractGuessAttempt,
    mapGuessAttemptToRow,
    normalizeKey,
    pickGuessListItems
} from '../utils/classicModeMappers.js';

const WIN_ANIMATION_DELAY_MS = 6000;
const SCROLL_DELAY_MS = 200;
const SEARCH_INDEX_ENDPOINT = '/api/character-card/search-index';
const GUESS_LIST_ENDPOINT = '/api/card-guessing/classic/guess-list';
const GUESS_ENDPOINT_BASE = '/api/card-guessing/classic/guess';

const findSelectedCard = ({ item, guessAttempt, cardsById, cardsByName }) => {
    const cardId = item?.characterCardId ?? item?.cardId ?? guessAttempt?.characterCardId;
    const cardFromId = cardId ? cardsById[cardId] : null;
    const guessName = guessAttempt?.name?.value;
    const cardFromName = guessName ? cardsByName[normalizeKey(guessName)] : null;
    return cardFromId || cardFromName || null;
};

function useClassicModeGame() {
    const [guesses, setGuesses] = useState([]);
    const [characterCards, setCharacterCards] = useState([]);
    const [isWon, setIsWon] = useState(false);
    const [isLeaderboardExpanded, setIsLeaderboardExpanded] = useState(false); // Add missing local state back
    const [isAnimatingSuccess, setIsAnimatingSuccess] = useState(false);
    const winSectionRef = useRef(null);

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
                    setIsWon(true);
                    setIsLeaderboardExpanded(true); // Don't forget to set expanded state on page load for completed games
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
        handleSelectCharacterId,
        winSectionRef
    };
}

export default useClassicModeGame;

