package com.bombadle.service.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CharacterCardImageServiceTest {

    @InjectMocks
    private CharacterCardImageService imageService;

    @TempDir
    Path tempDir;

    private Path imageDirPath;
    private Path pendingDirPath;

    @BeforeEach
    void setUp() {
        imageDirPath = tempDir.resolve("images");
        pendingDirPath = imageDirPath.resolve("pending");

        ReflectionTestUtils.setField(imageService, "imageDir", imageDirPath.toString());
        ReflectionTestUtils.setField(imageService, "pendingDir", pendingDirPath.toString());
    }

    @Nested
    class StorePendingImageTests {

        @Test
        void storePendingImage_validFile_createsFileInPendingDir() throws IOException {
            MockMultipartFile file = new MockMultipartFile("file", "sigma_image.jpg", "image/jpeg", "test data".getBytes());

            String resultPath = imageService.storePendingImage(file);

            assertNotNull(resultPath);
            assertTrue(Files.exists(Path.of(resultPath)));
            assertTrue(resultPath.startsWith(pendingDirPath.toString()));
        }

        @Test
        void storePendingImage_nullFile_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    imageService.storePendingImage(null)
            );
        }
    }

    @Nested
    class ApplyPendingImageTests {

        @Test
        void applyPendingImage_validPath_movesFileToImageDir() throws IOException {
            Files.createDirectories(pendingDirPath);
            Path tempFile = Files.createFile(pendingDirPath.resolve("sigma_temp.jpg"));

            imageService.applyPendingImage(tempFile.toString(), "Sigma Card");

            assertFalse(Files.exists(tempFile));
            assertTrue(Files.exists(imageDirPath.resolve("sigma_card.jpg")));
        }

        @Test
        void applyPendingImage_nonExistentPath_throwsIllegalArgumentException() {
            String nonExistentPath = pendingDirPath.resolve("beta_ghost.jpg").toString();

            assertThrows(IllegalArgumentException.class, () ->
                    imageService.applyPendingImage(nonExistentPath, "sigma_card")
            );
        }
    }

    @Nested
    class DeletePendingImageTests {

        @Test
        void deletePendingImage_existingFile_deletesFile() throws IOException {
            Files.createDirectories(pendingDirPath);
            Path tempFile = Files.createFile(pendingDirPath.resolve("sigma_to_delete.jpg"));

            imageService.deletePendingImage(tempFile.toString());

            assertFalse(Files.exists(tempFile));
        }

        @Test
        void deletePendingImage_nonExistentFile_doesNotThrow() {
            assertDoesNotThrow(() ->
                    imageService.deletePendingImage("beta_non_existent.jpg")
            );
        }
    }

    @Nested
    class RenameImageTests {

        @Test
        void renameImage_existingFile_renamesFile() throws IOException {
            Files.createDirectories(imageDirPath);
            Path oldFile = Files.createFile(imageDirPath.resolve("beta_name.jpg"));

            imageService.renameImage("Beta Name", "Sigma Name");

            assertFalse(Files.exists(oldFile));
            assertTrue(Files.exists(imageDirPath.resolve("sigma_name.jpg")));
        }

        @Test
        void renameImage_nonExistentFile_doesNotThrow() {
            assertDoesNotThrow(() ->
                    imageService.renameImage("beta_missing", "sigma_new")
            );
        }
    }

    @Nested
    class UtilityTests {

        @Test
        void buildImageSrc_buildsCorrectPath() {
            assertEquals("/images/character_cards/sigma_card.jpg", imageService.buildImageSrc("Sigma Card"));
        }

        @Test
        void buildSlug_handlesSpecialCharacters() {
            assertEquals("a_o_z_z_c_n", imageService.buildSlug("ą ó ł ż ź ć ń"));
            assertEquals("sigma_card_with_spaces", imageService.buildSlug("  Sigma Card with Spaces  "));
            assertEquals("special_chars", imageService.buildSlug("!@#$%^&*()special_chars"));
        }
    }
}