package com.cinesnack.catalog.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class PosterServiceTest {
    @TempDir Path directory;

    @Test void storesPngAndDeletesItAfterRollback() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3, 3, BufferedImage.TYPE_INT_RGB), "png", output);
        var service = new PosterService(directory);
        var file = new MockMultipartFile("poster", "fake.jpg", "image/jpeg", output.toByteArray());
        String url = service.luu(file);
        Path stored = directory.resolve(url.substring("/posters/".length()));
        assertTrue(Files.isRegularFile(stored));
        assertNotNull(ImageIO.read(stored.toFile()));
        service.xoaAnhMoi(url);
        assertFalse(Files.exists(stored));
    }

    @Test void rejectsSpoofedImages() {
        var service = new PosterService(directory);
        var file = new MockMultipartFile("poster", "x.png", "image/png", "not an image".getBytes());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.luu(file)).getStatusCode().value());
    }
}
