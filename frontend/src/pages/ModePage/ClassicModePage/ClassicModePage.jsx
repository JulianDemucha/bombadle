import React, { useEffect, useMemo, useState } from 'react';
import './ClassicModePage.css';
import Footer from "../../../components/Footer.jsx";
import Header from "../../../components/Header.jsx";
import ImgTextBanner from "../../../components/ImgTextBanner.jsx";
import CharacterSearchBar from "../../../components/CharacterSearchBar.jsx";
import GuessList from "../../../components/GuessList.jsx";
import { apiFetch } from "../../../api/api.js";
import characterCards from "../../../data/character_cards.json";

const normalizeLabel = (value) => String(value ?? '')
    .replaceAll('_', ' ')
    .toLowerCase()
    .replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());

const mapMatchToTileStatus = (match) => {
    if (match === 'MATCH') return 'correct';
    if (match === 'NOT_MATCH') return 'wrong';
    if (match === 'HIGHER' || match === 'LOWER') return 'wrong'; // treat higher/lower as wrong (red)
    return 'partial';
};

const normalizeKey = (value) => String(value ?? '')
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
    imageSrc: selectedCard?.imageSrc || '/avatar/AVATAR_DEFAULT.jpg',
    gender: normalizeLabel(guessAttempt?.gender?.value),
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
        firstAppearanceDirection: guessAttempt?.firstAppearanceEpisode?.match // 'HIGHER' or 'LOWER' or undefined
    }
});

function ClassicModePage() {
    const [guesses, setGuesses] = useState([]);

    const cardsById = useMemo(
        () => Object.fromEntries(characterCards.map((card) => [card.id, card])),
        []
    );

    const cardsByName = useMemo(
        () => Object.fromEntries(characterCards.map((card) => [normalizeKey(card.name), card])),
        []
    );

    useEffect(() => {
        const loadGuessList = async () => {
            try {
                const response = await apiFetch('/api/card-guessing/classic/guess-list');
                const items = pickGuessListItems(response.data);

                const mappedRows = items.map((item, index) => {
                    const guessAttempt = extractGuessAttempt(item);
                    const cardId = item?.characterCardId ?? item?.cardId ?? guessAttempt?.characterCardId;
                    const cardFromId = cardId ? cardsById[cardId] : null;
                    const guessName = guessAttempt?.name?.value;
                    const cardFromName = guessName ? cardsByName[normalizeKey(guessName)] : null;
                    const selectedCard = cardFromId || cardFromName || null;

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

    const handleSelectCharacterId = async (cardId) => {
        const selectedCard = cardsById[cardId];
        if (!selectedCard) return;

        try {
            const response = await apiFetch(`/api/card-guessing/classic/guess/${cardId}`, {
                method: 'POST'
            });

            const newRow = mapGuessAttemptToRow(
                response.data?.guessAttempt,
                selectedCard,
                `${cardId}-${Date.now()}`
            );
            setGuesses((prev) => [newRow, ...prev]);
        } catch (error) {
            console.error('Blad wysylania guessa:', error);
        }
    };

    return (
        <div className="classic-mode-page">
            <Header logoClassName='logo logo-75'/>
            <div className="classic-mode-content">
                <ImgTextBanner text = 'Zgadnij dzisiejszą postać' altText="ok"/>
                <CharacterSearchBar onSelectCharacterId={handleSelectCharacterId}/>
                <GuessList guesses={guesses}/>
            </div>
            <Footer/>
        </div>
    );
}

export default ClassicModePage;
