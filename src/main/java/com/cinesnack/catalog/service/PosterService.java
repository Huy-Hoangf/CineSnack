package com.cinesnack.catalog.service;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PosterService {
    private static final Logger log = LoggerFactory.getLogger(PosterService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path directory;

    public PosterService() { this(Path.of("uploads", "posters")); }
    PosterService(Path directory) { this.directory = directory.toAbsolutePath().normalize(); }

    public String luu(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES) throw bad("Ảnh phải có dung lượng tối đa 5 MB.");
        BufferedImage image = doc(file);
        Path destination = directory.resolve(UUID.randomUUID() + ".png");
        try {
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                if (!ImageIO.write(image, "png", output)) throw new IOException("PNG writer unavailable");
            }
            return "/posters/" + destination.getFileName();
        } catch (IOException ex) {
            try { Files.deleteIfExists(destination); } catch (IOException cleanup) { log.error("Không thể xóa ảnh chưa lưu", cleanup); }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh poster.", ex);
        }
    }

    public void xoaAnhMoi(String url) {
        if (!url.matches("/posters/[0-9a-f-]{36}\\.png")) return;
        try { Files.deleteIfExists(directory.resolve(url.substring("/posters/".length()))); }
        catch (IOException ex) { log.error("Không thể xóa poster khi giao dịch thất bại: {}", url, ex); }
    }

    private BufferedImage doc(MultipartFile file) {
        try (InputStream input = file.getInputStream(); ImageInputStream stream = ImageIO.createImageInputStream(input)) {
            if (stream == null) throw bad("Ảnh không hợp lệ.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw bad("Chỉ hỗ trợ ảnh JPG hoặc PNG hợp lệ.");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png")) throw bad("Chỉ hỗ trợ ảnh JPG hoặc PNG.");
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 4000 || height > 4000) throw bad("Kích thước ảnh tối đa là 4000 × 4000 pixel.");
                BufferedImage image = reader.read(0);
                if (image == null) throw bad("Ảnh không hợp lệ.");
                return image;
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh không hợp lệ hoặc bị lỗi.", ex); }
    }

    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
