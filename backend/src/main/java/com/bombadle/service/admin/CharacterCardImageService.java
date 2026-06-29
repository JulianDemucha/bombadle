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

    private String imageDir = "src/main/resources/static/images/character_cards";
    private String pendingDir = imageDir + "/pending";

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

    public String buildImageSrc(Long id) {
        return "/images/character_cards/" + id + ".jpg";
    }

    public String buildSlug(String name) {
        return slugify(name);
    }

    public void applyPendingImage(String tempPath, Long id) throws IOException {
        if (tempPath == null) {
            return;
        }
        Path src = Paths.get(tempPath);
        if (!Files.exists(src)) {
            throw new IllegalArgumentException("Pending image not found: " + tempPath);
        }
        Path finalDirPath = Paths.get(imageDir);
        Files.createDirectories(finalDirPath);
        Path dest = finalDirPath.resolve(id + ".jpg");
        Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deletePendingImage(String tempPath) throws IOException {
        if (tempPath == null) {
            return;
        }
        Path src = Paths.get(tempPath);
        Files.deleteIfExists(src);
    }

    public void deleteDisplayImage(Long id) throws IOException {
        Path target = Paths.get(imageDir).resolve(id + ".jpg");
        Files.deleteIfExists(target);
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