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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
    private Path guessImagesDirPath;

    @BeforeEach
    void setUp() {
        imageDirPath = tempDir.resolve("character_cards");
        pendingDirPath = imageDirPath.resolve("pending");
        guessImagesDirPath = tempDir.resolve("images_mode");

        ReflectionTestUtils.setField(imageService, "imageDir", imageDirPath.toString());
        ReflectionTestUtils.setField(imageService, "pendingDir", pendingDirPath.toString());
        ReflectionTestUtils.setField(imageService, "guessImagesDir", guessImagesDirPath.toString());
    }

    // ── storePendingImage ─────────────────────────────────────────────────────

    @Nested
    class StorePendingImageTests {

        @Test
        void validFile_createsFileInPendingDir() throws IOException {
            MockMultipartFile file = new MockMultipartFile("file", "sigma_image.jpg", "image/jpeg", "test data".getBytes());

            String resultPath = imageService.storePendingImage(file);

            assertNotNull(resultPath);
            assertTrue(Files.exists(Path.of(resultPath)));
            assertTrue(resultPath.startsWith(pendingDirPath.toString()));
        }

        @Test
        void nullFile_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    imageService.storePendingImage(null)
            );
        }

        @Test
        void emptyFile_throwsIllegalArgumentException() {
            MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
            assertThrows(IllegalArgumentException.class, () ->
                    imageService.storePendingImage(emptyFile)
            );
        }
    }

    // ── deletePendingImage ────────────────────────────────────────────────────

    @Nested
    class DeletePendingImageTests {

        @Test
        void existingFile_deletesFile() throws IOException {
            Files.createDirectories(pendingDirPath);
            Path tempFile = Files.createFile(pendingDirPath.resolve("sigma_to_delete.jpg"));

            imageService.deletePendingImage(tempFile.toString());

            assertFalse(Files.exists(tempFile));
        }

        @Test
        void nonExistentFile_doesNotThrow() {
            assertDoesNotThrow(() ->
                    imageService.deletePendingImage("beta_non_existent.jpg")
            );
        }

        @Test
        void nullPath_doesNotThrow() {
            assertDoesNotThrow(() -> imageService.deletePendingImage(null));
        }
    }

    // ── buildImageSrc ─────────────────────────────────────────────────────────

    @Nested
    class BuildImageSrcTests {

        @Test
        void buildsCorrectIdBasedPath() {
            assertEquals("/images/character_cards/42.jpg", imageService.buildImageSrc(42L));
            assertEquals("/images/character_cards/1.jpg", imageService.buildImageSrc(1L));
        }
    }

    // ── scaleAndApplyDisplayImage ─────────────────────────────────────────────

    @Nested
    class ScaleAndApplyDisplayImageTests {

        @Test
        void validImage_creates150x150JpegAtFinalLocation() throws IOException {
            Files.createDirectories(pendingDirPath);
            Path tempImg = pendingDirPath.resolve("source.jpg");
            BufferedImage source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
            ImageIO.write(source, "jpeg", tempImg.toFile());

            imageService.scaleAndApplyDisplayImage(tempImg.toString(), 42L);

            Path finalPath = imageDirPath.resolve("42.jpg");
            assertTrue(Files.exists(finalPath));
            assertFalse(Files.exists(tempImg), "temp file should be deleted");

            BufferedImage result = ImageIO.read(finalPath.toFile());
            assertNotNull(result);
            assertEquals(150, result.getWidth());
            assertEquals(150, result.getHeight());
        }

        @Test
        void nullPath_doesNothing() throws IOException {
            assertDoesNotThrow(() -> imageService.scaleAndApplyDisplayImage(null, 42L));
        }

        @Test
        void nonExistentFile_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    imageService.scaleAndApplyDisplayImage("non_existent.jpg", 42L)
            );
        }
    }

    // ── deleteDisplayImage ────────────────────────────────────────────────────

    @Nested
    class DeleteDisplayImageTests {

        @Test
        void existingFile_deletesFile() throws IOException {
            Files.createDirectories(imageDirPath);
            Path imgFile = Files.createFile(imageDirPath.resolve("5.jpg"));

            imageService.deleteDisplayImage(5L);

            assertFalse(Files.exists(imgFile));
        }

        @Test
        void nonExistentFile_doesNotThrow() {
            assertDoesNotThrow(() -> imageService.deleteDisplayImage(99L));
        }
    }

    // ── deleteGuessImageDir ───────────────────────────────────────────────────

    @Nested
    class DeleteGuessImageDirTests {

        @Test
        void existingDirectory_deletesRecursively() throws IOException {
            Path cardDir = guessImagesDirPath.resolve("5");
            Files.createDirectories(cardDir);
            Files.createFile(cardDir.resolve("lvl_1.jpg"));
            Files.createFile(cardDir.resolve("lvl_9.jpg"));

            imageService.deleteGuessImageDir(5L);

            assertFalse(Files.exists(cardDir));
        }

        @Test
        void nonExistentDirectory_doesNotThrow() {
            assertDoesNotThrow(() -> imageService.deleteGuessImageDir(99L));
        }
    }

    // ── buildSlug ─────────────────────────────────────────────────────────────

    @Nested
    class BuildSlugTests {

        @Test
        void handlesSpecialCharacters() {
            assertEquals("a_o_z_z_c_n", imageService.buildSlug("ą ó ł ż ź ć ń"));
        }

        @Test
        void normalizesSpacesAndCase() {
            assertEquals("sigma_card_with_spaces", imageService.buildSlug("  Sigma Card with Spaces  "));
        }

        @Test
        void stripsNonAlphanumeric() {
            assertEquals("special_chars", imageService.buildSlug("!@#$%^&*()special_chars"));
        }
    }
}
