package com.jackson.ecommerce.product.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class ProductImageStorage {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path root;

    public ProductImageStorage(@Value("${app.storage.upload-dir:uploads}") String uploadDirectory) {
        this.root = Paths.get(uploadDirectory).toAbsolutePath().normalize().resolve("products");
    }

    public StoredImage save(MultipartFile file) {
        String mediaType = file.getContentType();
        if (file.isEmpty()) {
            throw new com.jackson.ecommerce.common.web.BadRequestException("Image file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new com.jackson.ecommerce.common.web.BadRequestException("Image file must not exceed 5 MB");
        }
        if (!isAllowed(mediaType)) {
            throw new com.jackson.ecommerce.common.web.BadRequestException("Only JPEG, PNG and WebP images are allowed");
        }
        validateSignature(file, mediaType);
        String extension = switch (mediaType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new com.jackson.ecommerce.common.web.BadRequestException("Unsupported image type");
        };
        String storageKey = UUID.randomUUID() + extension;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(storageKey).normalize();
            if (!target.startsWith(root)) {
                throw new com.jackson.ecommerce.common.web.BadRequestException("Invalid image path");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return new StoredImage(storageKey, safeFilename(file.getOriginalFilename()), mediaType, file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store product image", exception);
        }
    }

    public InputStream open(String storageKey) {
        try {
            Path target = root.resolve(storageKey).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Invalid image path");
            }
            return Files.newInputStream(target);
        } catch (IOException exception) {
            throw new com.jackson.ecommerce.common.web.NotFoundException("Image file was not found");
        }
    }

    public void delete(String storageKey) {
        try {
            Path target = root.resolve(storageKey).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Invalid image path");
            }
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete product image", exception);
        }
    }

    private boolean isAllowed(String mediaType) {
        return "image/jpeg".equals(mediaType) || "image/png".equals(mediaType) || "image/webp".equals(mediaType);
    }

    private void validateSignature(MultipartFile file, String mediaType) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (mediaType) {
                case "image/jpeg" -> startsWith(header, new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
                case "image/png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
                case "image/webp" -> startsWith(header, new byte[]{0x52, 0x49, 0x46, 0x46})
                        && startsWithAt(header, new byte[]{0x57, 0x45, 0x42, 0x50}, 8);
                default -> false;
            };
            if (!valid) {
                throw new com.jackson.ecommerce.common.web.BadRequestException(
                        "Image content does not match its declared media type");
            }
        } catch (IOException exception) {
            throw new com.jackson.ecommerce.common.web.BadRequestException("Could not read image file");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return startsWithAt(value, prefix, 0);
    }

    private boolean startsWithAt(byte[] value, byte[] prefix, int offset) {
        if (value.length < offset + prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (value[offset + index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image";
        }
        return Paths.get(filename).getFileName().toString();
    }

    public record StoredImage(String storageKey, String originalFilename, String mediaType, long fileSize) {
    }
}
