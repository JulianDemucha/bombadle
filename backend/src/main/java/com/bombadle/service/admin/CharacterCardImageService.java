package com.bombadle.service.admin;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.Locale;

@Service
public class CharacterCardImageService {

    private String imageDir = "src/main/resources/static/images/character_cards";
    private String pendingDir = imageDir + "/pending";
    private String guessImagesDir = "src/main/resources/static/images/images_mode";

    // ── Staging ──────────────────────────────────────────────────────────────

    public String storePendingImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        Path pendingDirPath = Paths.get(pendingDir);
        Files.createDirectories(pendingDirPath);
        String filename = System.currentTimeMillis() + "_" + sanitizeFilename(file.getOriginalFilename());
        Path target = pendingDirPath.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public String storePendingGuessImage(MultipartFile file) throws IOException {
        // Reuses the same staging directory as the display image
        return storePendingImage(file);
    }

    public void deletePendingImage(String tempPath) throws IOException {
        if (tempPath == null) return;
        Files.deleteIfExists(Paths.get(tempPath));
    }

    // ── Display image (character_cards/<id>.jpg) ──────────────────────────────

    public String buildImageSrc(Long id) {
        return "/images/character_cards/" + id + ".jpg";
    }

    /**
     * Scales the staged display image to 150×150 (cover-crop, BICUBIC),
     * writes it as JPEG to the final location, and deletes the temp file.
     */
    public void scaleAndApplyDisplayImage(String tempPath, Long id) throws IOException {
        if (tempPath == null) return;
        Path src = Paths.get(tempPath);
        if (!Files.exists(src)) {
            throw new IllegalArgumentException("Pending display image not found: " + tempPath);
        }

        BufferedImage original = ImageIO.read(src.toFile());
        if (original == null) {
            throw new IllegalArgumentException("Could not decode display image: " + tempPath);
        }

        BufferedImage thumbnail = scaleToCoverAndCrop(original, 150, 150);

        Path finalDir = Paths.get(imageDir);
        Files.createDirectories(finalDir);
        writeJpeg(thumbnail, finalDir.resolve(id + ".jpg"), 0.90f);
        Files.deleteIfExists(src);
    }

    public void applyPendingImage(String tempPath, Long id) throws IOException {
        if (tempPath == null) return;
        Path src = Paths.get(tempPath);
        if (!Files.exists(src)) {
            throw new IllegalArgumentException("Pending image not found: " + tempPath);
        }
        Path finalDir = Paths.get(imageDir);
        Files.createDirectories(finalDir);
        Files.move(src, finalDir.resolve(id + ".jpg"), StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteDisplayImage(Long id) throws IOException {
        Files.deleteIfExists(Paths.get(imageDir).resolve(id + ".jpg"));
    }

    // ── Guess images (images_mode/<id>/) ──────────────────────────────────────

    public Path getGuessImageOutputDir(Long id) {
        return Paths.get(guessImagesDir).resolve(String.valueOf(id));
    }

    public void deleteGuessImageDir(Long id) throws IOException {
        Path dir = getGuessImageOutputDir(id);
        if (!Files.exists(dir)) return;
        try (var stream = Files.walk(dir).sorted(java.util.Comparator.reverseOrder())) {
            stream.forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete " + p, e);
                }
            });
        }
    }

    // ── Slug / actionKey helper ───────────────────────────────────────────────

    public String buildSlug(String name) {
        return slugify(name);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Scales {@code src} to cover {@code targetW×targetH}, then center-crops.
     * Output is always TYPE_INT_RGB (JPEG-safe).
     */
    private BufferedImage scaleToCoverAndCrop(BufferedImage src, int targetW, int targetH) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        double scale = Math.max((double) targetW / srcW, (double) targetH / srcH);
        int scaledW = (int) Math.ceil(srcW * scale);
        int scaledH = (int) Math.ceil(srcH * scale);

        BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, scaledW, scaledH, null);
        } finally {
            g.dispose();
        }

        // Center-crop into a fresh image (getSubimage shares pixel data)
        int x = (scaledW - targetW) / 2;
        int y = (scaledH - targetH) / 2;
        BufferedImage cropped = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D cg = cropped.createGraphics();
        try {
            cg.drawImage(scaled, 0, 0, targetW, targetH, x, y, x + targetW, y + targetH, null);
        } finally {
            cg.dispose();
        }
        return cropped;
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

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        String slug = normalized.replaceAll("[^a-z0-9]+", "_");
        slug = slug.replaceAll("_+", "_");
        slug = slug.replaceAll("^_+|_+$", "");
        return slug;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "upload.jpg";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
