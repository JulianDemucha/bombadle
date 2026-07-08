package com.bombadle.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GuessImageGeneratorServiceTest {

    private final GuessImageGeneratorService generator = new GuessImageGeneratorService();

    @TempDir
    Path outputDir;

    private InputStream jpegInputStream() throws IOException {
        BufferedImage img = new BufferedImage(600, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    @Test
    void generateGuessImages_produces9LevelFiles() throws IOException {
        generator.generateGuessImages(jpegInputStream(), outputDir);

        for (int i = 1; i <= 9; i++) {
            Path file = outputDir.resolve("lvl_" + i + ".jpg");
            assertTrue(Files.exists(file), "Expected file: lvl_" + i + ".jpg");
        }
        assertEquals(9, Files.list(outputDir).count());
    }

    @Test
    void generateGuessImages_allLevelsAre500x500() throws IOException {
        generator.generateGuessImages(jpegInputStream(), outputDir);

        for (int i = 1; i <= 9; i++) {
            Path file = outputDir.resolve("lvl_" + i + ".jpg");
            BufferedImage img = ImageIO.read(file.toFile());
            assertNotNull(img, "lvl_" + i + ".jpg must be a valid JPEG");
            assertEquals(500, img.getWidth(), "lvl_" + i + " width");
            assertEquals(500, img.getHeight(), "lvl_" + i + " height");
        }
    }

    @Test
    void generateGuessImages_createsOutputDirIfMissing() throws IOException {
        Path nested = outputDir.resolve("new_dir");

        generator.generateGuessImages(jpegInputStream(), nested);

        assertTrue(Files.isDirectory(nested));
        assertTrue(Files.exists(nested.resolve("lvl_1.jpg")));
    }

    @Test
    void generateGuessImages_invalidInputStream_throwsIllegalArgumentException() {
        InputStream garbage = new ByteArrayInputStream(new byte[]{0, 1, 2, 3});
        assertThrows(IllegalArgumentException.class, () ->
                generator.generateGuessImages(garbage, outputDir)
        );
    }
}
