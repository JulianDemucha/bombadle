package com.bombadle.service.admin;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class CharacterCardImageService {
    private static final String IMAGE_DIR = "src/main/resources/static/images/character_cards";
    private static final String PENDING_DIR = IMAGE_DIR + "/pending";

    public String storePendingImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        Path pendingDir = Paths.get(PENDING_DIR);
        Files.createDirectories(pendingDir);
        String filename = System.currentTimeMillis() + "_" + sanitizeFilename(file.getOriginalFilename());
        Path target = pendingDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public String buildImageSrc(String name) {
        return "/images/character_cards/" + slugify(name) + ".jpg";
    }

    public String buildSlug(String name) {
        return slugify(name);
    }

    public void applyPendingImage(String tempPath, String name) throws IOException {
        if (tempPath == null) {
            return;
        }
        Path src = Paths.get(tempPath);
        if (!Files.exists(src)) {
            throw new IllegalArgumentException("Pending image not found: " + tempPath);
        }
        Path finalDir = Paths.get(IMAGE_DIR);
        Files.createDirectories(finalDir);
        Path dest = finalDir.resolve(slugify(name) + ".jpg");
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deletePendingImage(String tempPath) throws IOException {
        if (tempPath == null) {
            return;
        }
        Path src = Paths.get(tempPath);
        Files.deleteIfExists(src);
    }

    public void renameImage(String oldName, String newName) throws IOException {
        Path finalDir = Paths.get(IMAGE_DIR);
        Path src = finalDir.resolve(slugify(oldName) + ".jpg");
        if (!Files.exists(src)) {
            return;
        }
        Path dest = finalDir.resolve(slugify(newName) + ".jpg");
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
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
        if (filename == null) {
            return "upload.jpg";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
