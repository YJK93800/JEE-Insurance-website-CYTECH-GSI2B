package com.assurcy2000.util;

import com.assurcy2000.dto.request.UploadedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentStorageTest {

    @TempDir Path root;

    static UploadedFile file(String name, byte[] content) {
        return new UploadedFile(name, content.length, () -> new ByteArrayInputStream(content));
    }

    static final byte[] PDF = "%PDF-1.7 contenu".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    @Test
    void typeIsDetectedFromContentNotFromName() {
        assertEquals(Optional.of(AttachmentStorage.Kind.PDF), AttachmentStorage.detect(file("x.txt", PDF)));
        assertEquals(Optional.of(AttachmentStorage.Kind.PNG), AttachmentStorage.detect(file("x.pdf", PNG)));
        assertEquals(Optional.of(AttachmentStorage.Kind.JPEG), AttachmentStorage.detect(file("x", JPEG)));
        assertTrue(AttachmentStorage.detect(file("facture.pdf", "<script>".getBytes())).isEmpty());
        assertTrue(AttachmentStorage.detect(file("vide.pdf", new byte[0])).isEmpty());
    }

    @Test
    void fileIsCopiedUnderARandomNameInTheClaimFolder() throws Exception {
        AttachmentStorage storage = new AttachmentStorage(root);
        String relative = storage.store(12L, AttachmentStorage.Kind.PDF, file("../../constat.pdf", PDF));

        assertTrue(relative.matches("claims/12/[0-9a-f-]{36}\\.pdf"), relative);
        Path stored = storage.resolve(relative);
        assertTrue(stored.startsWith(root.toAbsolutePath().normalize()));
        assertArrayEquals(PDF, Files.readAllBytes(stored));

        storage.deleteQuietly(relative);
        assertFalse(Files.exists(stored));
    }

    @Test
    void pathOutsideTheStorageFolderIsRefused() {
        AttachmentStorage storage = new AttachmentStorage(root);
        assertThrows(IllegalArgumentException.class, () -> storage.resolve("../secret.txt"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolve("claims/../../etc/passwd"));
    }
}
