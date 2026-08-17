package com.image.generation.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PngUploadTest {

    private final PngUpload pngUpload = new PngUpload();

    @Test
    void acceptsA1024PngAndHashesIt() throws IOException {
        PngUpload.Png png = pngUpload.read(new MockMultipartFile(
                "file", "sofa.png", "image/png", pngBytes(1024, 1024)));

        assertThat(png.widthPx()).isEqualTo(1024);
        assertThat(png.heightPx()).isEqualTo(1024);
        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(png.sha256()).hasSize(64);
    }

    @Test
    void rejectsWrongDimensions() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.png", "image/png", pngBytes(512, 512));

        assertThatThrownBy(() -> pngUpload.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1024x1024");
    }

    @Test
    void rejectsNonPngContentEvenWhenTheContentTypeClaimsPng() {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.png", "image/png", "not a png".getBytes());

        assertThatThrownBy(() -> pngUpload.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PNG files");
    }

    @Test
    void rejectsAnEmptyUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> pngUpload.read(file)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsAnyPathFromTheFileName() throws IOException {
        PngUpload.Png png = pngUpload.read(new MockMultipartFile(
                "file", "C:\\tmp\\sofa.png", "image/png", pngBytes(1024, 1024)));

        assertThat(png.fileName()).isEqualTo("sofa.png");
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
