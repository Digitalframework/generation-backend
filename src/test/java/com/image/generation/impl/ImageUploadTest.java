package com.image.generation.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadTest {

    private final ImageUpload imageUpload = new ImageUpload();

    @Test
    void acceptsA1024PngAndHashesIt() throws IOException {
        ImageUpload.UploadedImage uploaded = imageUpload.read(new MockMultipartFile(
                "file", "sofa.png", "image/png", imageBytes("png", 1024, 1024)));

        assertThat(uploaded.widthPx()).isEqualTo(1024);
        assertThat(uploaded.heightPx()).isEqualTo(1024);
        assertThat(uploaded.contentType()).isEqualTo("image/png");
        assertThat(uploaded.sha256()).hasSize(64);
    }

    @Test
    void acceptsA1024Jpeg() throws IOException {
        ImageUpload.UploadedImage uploaded = imageUpload.read(new MockMultipartFile(
                "file", "sofa.jpg", "image/jpeg", imageBytes("jpg", 1024, 1024)));

        assertThat(uploaded.widthPx()).isEqualTo(1024);
        assertThat(uploaded.heightPx()).isEqualTo(1024);
        assertThat(uploaded.contentType()).isEqualTo("image/jpeg");
        assertThat(uploaded.sha256()).hasSize(64);
    }

    /**
     * The bytes decide, not the part's declared type — a JPEG announced as a PNG is
     * stored as the JPEG it is, so the content endpoint serves a header a browser can act on.
     */
    @Test
    void takesTheContentTypeFromTheBytesRatherThanTheClaim() throws IOException {
        ImageUpload.UploadedImage uploaded = imageUpload.read(new MockMultipartFile(
                "file", "sofa.png", "image/png", imageBytes("jpg", 1024, 1024)));

        assertThat(uploaded.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void rejectsWrongDimensions() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.png", "image/png", imageBytes("png", 512, 512));

        assertThatThrownBy(() -> imageUpload.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1024x1024");
    }

    @Test
    void rejectsAFormatThatIsNeitherPngNorJpeg() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.gif", "image/gif", imageBytes("gif", 1024, 1024));

        assertThatThrownBy(() -> imageUpload.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PNG and JPEG files");
    }

    @Test
    void rejectsNonImageContentEvenWhenTheContentTypeClaimsPng() {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.png", "image/png", "not a png".getBytes());

        assertThatThrownBy(() -> imageUpload.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PNG and JPEG files");
    }

    @Test
    void rejectsAnEmptyUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "sofa.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> imageUpload.read(file)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsAnyPathFromTheFileName() throws IOException {
        ImageUpload.UploadedImage uploaded = imageUpload.read(new MockMultipartFile(
                "file", "C:\\tmp\\sofa.png", "image/png", imageBytes("png", 1024, 1024)));

        assertThat(uploaded.fileName()).isEqualTo("sofa.png");
    }

    private static byte[] imageBytes(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
