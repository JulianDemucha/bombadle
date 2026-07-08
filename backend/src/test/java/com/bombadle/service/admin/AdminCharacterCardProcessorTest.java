package com.bombadle.service.admin;

import com.bombadle.dto.queue.PendingCardCreatePayload;
import com.bombadle.dto.queue.PendingCardDeletePayload;
import com.bombadle.dto.queue.PendingCardUpdatePayload;
import com.bombadle.dto.request.AdminCharacterCardRequest;
import com.bombadle.dto.request.AdminCharacterCardUpdateRequest;
import com.bombadle.dto.request.AdminQuoteRequest;
import com.bombadle.entity.CharacterCard;
import com.bombadle.entity.CurrentCardState;
import com.bombadle.entity.Quote;
import com.bombadle.enums.GameMode;
import com.bombadle.enums.Gender;
import com.bombadle.repository.CharacterCardRepository;
import com.bombadle.repository.CurrentCardStateRepository;
import com.bombadle.repository.QuoteRepository;
import com.bombadle.service.cache.CacheService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCharacterCardProcessorTest {

    @InjectMocks
    private AdminCharacterCardProcessor processor;

    @Mock private CharacterCardRepository characterCardRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private CurrentCardStateRepository currentCardStateRepository;
    @Mock private CharacterCardImageService imageService;
    @Mock private GuessImageGeneratorService guessImageGenerator;
    @Mock private CacheService cacheService;

    @TempDir
    Path tempDir;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AdminQuoteRequest validQuote() {
        return new AdminQuoteRequest("Who said it?", List.of("A", "B"), "A", "SPEAKER", 1);
    }

    private AdminCharacterCardRequest createRequest(String name) {
        return new AdminCharacterCardRequest(name, "MALE", null, null, null, null, null, null,
                List.of(validQuote()));
    }

    private AdminCharacterCardUpdateRequest updateRequest(String name) {
        return new AdminCharacterCardUpdateRequest(name, null, null, null, null, null, null, null,
                null, null);
    }

    private CharacterCard cardWithId(Long id) {
        CharacterCard c = CharacterCard.createNewEmpty();
        c.setId(id);
        c.setName("Existing Card");
        c.setGender(Gender.MALE);
        return c;
    }

    private void stubSaveAndFlushWithId(Long id) {
        when(characterCardRepository.saveAndFlush(any())).thenAnswer(inv -> {
            CharacterCard c = inv.getArgument(0);
            c.setId(id);
            return c;
        });
    }

    private TransactionSynchronization captureRegisteredSynchronization(MockedStatic<TransactionSynchronizationManager> txMgr) {
        ArgumentCaptor<TransactionSynchronization> captor = ArgumentCaptor.forClass(TransactionSynchronization.class);
        txMgr.verify(() -> TransactionSynchronizationManager.registerSynchronization(captor.capture()));
        return captor.getValue();
    }

    // ── processCreate ─────────────────────────────────────────────────────────

    @Nested
    class ProcessCreateTests {

        @Test
        void nameAlreadyExists_throws_doesNotSave() {
            when(characterCardRepository.existsByName("Sigma")).thenReturn(true);
            PendingCardCreatePayload payload = new PendingCardCreatePayload(createRequest("Sigma"), "tmp.jpg", "guess.jpg");

            assertThrows(IllegalArgumentException.class, () -> processor.processCreate(payload));
            verify(characterCardRepository, never()).saveAndFlush(any());
        }

        @Test
        void validPayload_savesCardWithCorrectFields_andCreatesQuote() {
            AdminCharacterCardRequest req = createRequest("Sigma");
            PendingCardCreatePayload payload = new PendingCardCreatePayload(req, "tmp.jpg", "guess.jpg");

            when(characterCardRepository.existsByName("Sigma")).thenReturn(false);
            stubSaveAndFlushWithId(42L);
            when(imageService.buildImageSrc(42L)).thenReturn("/images/character_cards/42.jpg");

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processCreate(payload);
            }

            ArgumentCaptor<CharacterCard> captor = ArgumentCaptor.forClass(CharacterCard.class);
            verify(characterCardRepository).saveAndFlush(captor.capture());
            assertEquals("Sigma", captor.getValue().getName());
            assertEquals(Gender.MALE, captor.getValue().getGender());
            assertEquals("/images/character_cards/42.jpg", captor.getValue().getImageSrc());
            verify(quoteRepository).save(any(Quote.class));
        }

        @Test
        void validPayload_afterCommit_appliesDisplayImageAndGeneratesGuessImages() throws IOException {
            Path tempImg = Files.createFile(tempDir.resolve("display.jpg"));
            Path tempGuess = Files.createFile(tempDir.resolve("guess.jpg"));
            Path guessOutputDir = tempDir.resolve("output");

            when(characterCardRepository.existsByName("Sigma")).thenReturn(false);
            stubSaveAndFlushWithId(42L);
            when(imageService.buildImageSrc(42L)).thenReturn("/images/character_cards/42.jpg");
            when(imageService.getGuessImageOutputDir(42L)).thenReturn(guessOutputDir);

            PendingCardCreatePayload payload = new PendingCardCreatePayload(
                    createRequest("Sigma"), tempImg.toString(), tempGuess.toString());

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processCreate(payload);
                captureRegisteredSynchronization(txMgr).afterCommit();
            }

            verify(imageService).scaleAndApplyDisplayImage(tempImg.toString(), 42L);
            verify(guessImageGenerator).generateGuessImages(any(), eq(guessOutputDir));
        }
    }

    // ── processUpdate ─────────────────────────────────────────────────────────

    @Nested
    class ProcessUpdateTests {

        @Test
        void cardNotFound_throws() {
            when(characterCardRepository.findById(1L)).thenReturn(Optional.empty());
            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, updateRequest("X"), null, null);

            assertThrows(IllegalArgumentException.class, () -> processor.processUpdate(payload));
        }

        @Test
        void newNameCollision_throws_doesNotSave() {
            CharacterCard existing = cardWithId(1L);
            existing.setName("OldName");
            when(characterCardRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(characterCardRepository.existsByName("NewName")).thenReturn(true);

            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(1L, updateRequest("NewName"), null, null);

            assertThrows(IllegalArgumentException.class, () -> processor.processUpdate(payload));
            verify(characterCardRepository, never()).saveAndFlush(any());
        }

        @Test
        void noImages_updatesFieldsOnly_doesNotRegisterSynchronization() {
            CharacterCard existing = cardWithId(5L);
            when(characterCardRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(characterCardRepository.saveAndFlush(any())).thenReturn(existing);
            when(imageService.buildImageSrc(5L)).thenReturn("/images/character_cards/5.jpg");

            AdminCharacterCardUpdateRequest req = new AdminCharacterCardUpdateRequest(
                    null, "FEMALE", null, null, null, null, null, null, null, null);
            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(5L, req, null, null);

            processor.processUpdate(payload);

            verify(characterCardRepository).saveAndFlush(existing);
            verifyNoInteractions(guessImageGenerator);
        }

        @Test
        void withImages_schedulesAfterCommit_appliesBothImages() throws IOException {
            Path tempImg = Files.createFile(tempDir.resolve("display.jpg"));
            Path tempGuess = Files.createFile(tempDir.resolve("guess.jpg"));
            Path guessOutputDir = tempDir.resolve("output");

            CharacterCard existing = cardWithId(5L);
            when(characterCardRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(characterCardRepository.saveAndFlush(any())).thenReturn(existing);
            when(imageService.buildImageSrc(5L)).thenReturn("/images/character_cards/5.jpg");
            when(imageService.getGuessImageOutputDir(5L)).thenReturn(guessOutputDir);

            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(
                    5L, updateRequest(null), tempImg.toString(), tempGuess.toString());

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processUpdate(payload);
                captureRegisteredSynchronization(txMgr).afterCommit();
            }

            verify(imageService).scaleAndApplyDisplayImage(tempImg.toString(), 5L);
            verify(guessImageGenerator).generateGuessImages(any(), eq(guessOutputDir));
        }

        @Test
        void quoteIdsToRemove_nullsCurrentCardStateRefFirst_thenDeletesQuote() {
            CharacterCard existing = cardWithId(5L);
            when(characterCardRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(characterCardRepository.saveAndFlush(any())).thenReturn(existing);
            when(imageService.buildImageSrc(5L)).thenReturn("/images/character_cards/5.jpg");

            Quote quote = mock(Quote.class);
            when(quote.getId()).thenReturn(10L);
            when(quote.getCharacterCard()).thenReturn(existing);
            when(quoteRepository.findById(10L)).thenReturn(Optional.of(quote));

            CurrentCardState state = new CurrentCardState();
            state.setCurrentQuote(quote);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.of(state));

            AdminCharacterCardUpdateRequest req = new AdminCharacterCardUpdateRequest(
                    null, null, null, null, null, null, null, null, null, List.of(10L));
            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(5L, req, null, null);

            processor.processUpdate(payload);

            assertNull(state.getCurrentQuote());
            verify(currentCardStateRepository).save(state);
            verify(quoteRepository).delete(quote);
        }

        @Test
        void quoteIdsToRemove_quoteFromWrongCard_throws() {
            CharacterCard existing = cardWithId(5L);
            CharacterCard otherCard = cardWithId(99L);
            when(characterCardRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(characterCardRepository.saveAndFlush(any())).thenReturn(existing);
            when(imageService.buildImageSrc(5L)).thenReturn("/images/character_cards/5.jpg");

            Quote quote = mock(Quote.class);
            when(quote.getCharacterCard()).thenReturn(otherCard);
            when(quoteRepository.findById(10L)).thenReturn(Optional.of(quote));

            AdminCharacterCardUpdateRequest req = new AdminCharacterCardUpdateRequest(
                    null, null, null, null, null, null, null, null, null, List.of(10L));
            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(5L, req, null, null);

            assertThrows(IllegalArgumentException.class, () -> processor.processUpdate(payload));
            verify(quoteRepository, never()).delete(any());
        }

        @Test
        void quotesToAdd_createsNewQuotes() {
            CharacterCard existing = cardWithId(5L);
            when(characterCardRepository.findById(5L)).thenReturn(Optional.of(existing));
            when(characterCardRepository.saveAndFlush(any())).thenReturn(existing);
            when(imageService.buildImageSrc(5L)).thenReturn("/images/character_cards/5.jpg");

            AdminCharacterCardUpdateRequest req = new AdminCharacterCardUpdateRequest(
                    null, null, null, null, null, null, null, null,
                    List.of(validQuote(), validQuote()), null);
            PendingCardUpdatePayload payload = new PendingCardUpdatePayload(5L, req, null, null);

            processor.processUpdate(payload);

            verify(quoteRepository, times(2)).save(any(Quote.class));
        }
    }

    // ── processDelete ─────────────────────────────────────────────────────────

    @Nested
    class ProcessDeleteTests {

        @Test
        void cardNotFound_skipsWithoutError() {
            when(characterCardRepository.existsById(99L)).thenReturn(false);

            processor.processDelete(new PendingCardDeletePayload(99L));

            verify(characterCardRepository, never()).deleteById(anyLong());
            verifyNoInteractions(currentCardStateRepository);
        }

        @Test
        void cardFound_deletesCard_schedulesFileDeletionAfterCommit() throws IOException {
            when(characterCardRepository.existsById(5L)).thenReturn(true);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.empty());

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processDelete(new PendingCardDeletePayload(5L));

                verify(characterCardRepository).deleteById(5L);

                captureRegisteredSynchronization(txMgr).afterCommit();
            }

            verify(imageService).deleteDisplayImage(5L);
            verify(imageService).deleteGuessImageDir(5L);
        }

        @Test
        void cardInCurrentCards_removedBeforeDelete() {
            CharacterCard card = cardWithId(5L);
            CurrentCardState state = new CurrentCardState();
            state.getCurrentCards().put(GameMode.CLASSIC, card);

            when(characterCardRepository.existsById(5L)).thenReturn(true);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.of(state));

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processDelete(new PendingCardDeletePayload(5L));
            }

            assertFalse(state.getCurrentCards().containsKey(GameMode.CLASSIC));
            verify(currentCardStateRepository).save(state);
            verify(characterCardRepository).deleteById(5L);
        }

        @Test
        void cardInPreviousCards_removedBeforeDelete() {
            CharacterCard card = cardWithId(5L);
            CurrentCardState state = new CurrentCardState();
            state.getPreviousCards().put(GameMode.CLASSIC, card);

            when(characterCardRepository.existsById(5L)).thenReturn(true);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.of(state));

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processDelete(new PendingCardDeletePayload(5L));
            }

            assertFalse(state.getPreviousCards().containsKey(GameMode.CLASSIC));
            verify(currentCardStateRepository).save(state);
        }

        @Test
        void cardHasCurrentQuote_quotedNulledBeforeDelete() {
            CharacterCard card = cardWithId(5L);
            Quote quote = mock(Quote.class);
            when(quote.getCharacterCard()).thenReturn(card);

            CurrentCardState state = new CurrentCardState();
            state.setCurrentQuote(quote);

            when(characterCardRepository.existsById(5L)).thenReturn(true);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.of(state));

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processDelete(new PendingCardDeletePayload(5L));
            }

            assertNull(state.getCurrentQuote());
            verify(currentCardStateRepository).save(state);
            verify(characterCardRepository).deleteById(5L);
        }

        @Test
        void cardHasPreviousQuote_quotedNulledBeforeDelete() {
            CharacterCard card = cardWithId(5L);
            Quote quote = mock(Quote.class);
            when(quote.getCharacterCard()).thenReturn(card);

            CurrentCardState state = new CurrentCardState();
            state.setPreviousQuote(quote);

            when(characterCardRepository.existsById(5L)).thenReturn(true);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.of(state));

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processDelete(new PendingCardDeletePayload(5L));
            }

            assertNull(state.getPreviousQuote());
            verify(currentCardStateRepository).save(state);
        }

        @Test
        void cardNotInCurrentCardState_stateNotSaved() {
            CharacterCard otherCard = cardWithId(99L);
            CurrentCardState state = new CurrentCardState();
            state.getCurrentCards().put(GameMode.CLASSIC, otherCard);

            when(characterCardRepository.existsById(5L)).thenReturn(true);
            when(currentCardStateRepository.findById(1)).thenReturn(Optional.of(state));

            try (MockedStatic<TransactionSynchronizationManager> txMgr = mockStatic(TransactionSynchronizationManager.class)) {
                processor.processDelete(new PendingCardDeletePayload(5L));
            }

            verify(currentCardStateRepository, never()).save(any());
            verify(characterCardRepository).deleteById(5L);
        }
    }
}
