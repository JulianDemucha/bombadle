package com.bombadle.service.game;

import com.bombadle.entity.AnonymousGuessList;
import com.bombadle.repository.AnonymousGuessListRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymousGuessListServiceTest {

    @Mock
    private AnonymousGuessListRepository repo;

    @InjectMocks
    private AnonymousGuessListService anonymousGuessListService;

    @Nested
    class DeleteTests {

        @Test
        void delete_validList_callsRepositoryDelete() {
            // Arrange
            AnonymousGuessList guessList = mock(AnonymousGuessList.class);

            // Act
            anonymousGuessListService.delete(guessList);

            // Assert
            verify(repo).delete(guessList);
        }
    }

    @Nested
    class TruncateTableTests {

        @Test
        void truncateTable_called_callsRepositoryTruncate() {
            // Arrange
            // Act
            anonymousGuessListService.truncateTable();

            // Assert
            verify(repo).truncateTable();
        }
    }

    @Nested
    class SaveTests {

        @Test
        void save_validList_savesAndReturnsList() {
            // Arrange
            AnonymousGuessList guessList = mock(AnonymousGuessList.class);
            when(repo.save(guessList)).thenReturn(guessList);

            // Act
            AnonymousGuessList result = anonymousGuessListService.save(guessList);

            // Assert
            assertEquals(guessList, result);
            verify(repo).save(guessList);
        }
    }
}