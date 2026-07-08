package com.bombadle.service.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Slf4j
@Service
public class GuessImageGeneratorService {

    private static final int FINAL_SIZE = 500;

    // Index i → lvl_(i+1).jpg
    private static final int[] PIXEL_LEVELS = {10, 15, 20, 29, 41, 59, 84, 182, 500};

    public void generateGuessImages(InputStream sourceImage, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        BufferedImage original = ImageIO.read(sourceImage);
        if (original == null) {
            throw new IllegalArgumentException("Could not decode source image");
        }

        // Standardize to 500×500 with BICUBIC
        BufferedImage base = resizeImage(original, FINAL_SIZE, FINAL_SIZE,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        for (int i = 0; i < PIXEL_LEVELS.length; i++) {
            int res = PIXEL_LEVELS[i];
            int level = i + 1;
            Path dest = outputDir.resolve("lvl_" + level + ".jpg");

            if (res == FINAL_SIZE) {
                // lvl_9: clean 500×500 base
                writeJpeg(base, dest, 0.95f);
            } else {
                // Shrink with BILINEAR, then stretch back with NEAREST_NEIGHBOR
                BufferedImage shrunk = resizeImage(base, res, res,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                BufferedImage pixelated = resizeImage(shrunk, FINAL_SIZE, FINAL_SIZE,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                writeJpeg(pixelated, dest, 0.90f);
            }
        }

        log.info("Generated {} guess-image levels in {}", PIXEL_LEVELS.length, outputDir);
    }

    private BufferedImage resizeImage(BufferedImage src, int width, int height, Object interpolation) {
        // Always output TYPE_INT_RGB — strips alpha so JPEG encoding works without issues
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            g.drawImage(src, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private void writeJpeg(BufferedImage image, Path dest, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter available in this JVM");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);

        try (FileImageOutputStream out = new FileImageOutputStream(dest.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }
}
