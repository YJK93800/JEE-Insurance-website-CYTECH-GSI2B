package com.assurcy2000.util;

import com.assurcy2000.dto.request.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Stockage des pièces justificatives sur le disque, hors du WAR (dossier uploads.dir de db.properties),
 * pour qu'elles survivent aux redéploiements.
 * Chaque fichier est enregistré sous un nom aléatoire : claims/{idSinistre}/{uuid}.{ext}.
 * La base ne garde que ce chemin relatif ; le nom d'origine est seulement affiché.
 */
public class AttachmentStorage {

    public static final long MAX_SIZE = 5L * 1024 * 1024;   // 5 Mo par fichier
    public static final int MAX_FILES = 5;                   // par sinistre

    /** Types acceptés, reconnus à leurs premiers octets (pas au nom ni au Content-Type du navigateur). */
    public enum Kind {
        PDF("application/pdf", "pdf", new byte[] {'%', 'P', 'D', 'F', '-'}),
        JPEG("image/jpeg", "jpg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
        PNG("image/png", "png", new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

        private final String mimeType;
        private final String extension;
        private final byte[] signature;

        Kind(String mimeType, String extension, byte[] signature) {
            this.mimeType = mimeType;
            this.extension = extension;
            this.signature = signature;
        }

        public String mimeType() { return mimeType; }
        public String extension() { return extension; }
    }

    private final Path root;

    public AttachmentStorage() { this(DbConfig.uploadsDir()); }

    public AttachmentStorage(Path root) { this.root = root.toAbsolutePath().normalize(); }

    /** Type réel du fichier d'après sa signature, vide s'il n'est ni PDF, ni JPEG, ni PNG. */
    public static Optional<Kind> detect(UploadedFile file) {
        try (InputStream in = file.open()) {
            byte[] head = in.readNBytes(8);
            for (Kind kind : Kind.values()) {
                if (head.length >= kind.signature.length
                        && Arrays.equals(Arrays.copyOf(head, kind.signature.length), kind.signature)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier envoyé impossible", e);
        }
    }

    /** Copie le fichier dans le dossier du sinistre et renvoie son chemin relatif (à enregistrer en base). */
    public String store(Long claimId, Kind kind, UploadedFile file) {
        String relative = "claims/" + claimId + "/" + UUID.randomUUID() + "." + kind.extension();
        Path target = resolve(relative);
        try (InputStream in = file.open()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target);
            return relative;
        } catch (IOException e) {
            throw new UncheckedIOException("Enregistrement de la pièce justificative impossible", e);
        }
    }

    /** Chemin absolu d'un fichier stocké ; refuse tout chemin qui sortirait du dossier de stockage. */
    public Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Chemin de pièce justificative invalide : " + relative);
        }
        return path;
    }

    /** Supprime un fichier stocké sans lever d'erreur (nettoyage après une transaction annulée). */
    public void deleteQuietly(String relative) {
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (IOException | IllegalArgumentException ignored) {
            // fichier orphelin : sans conséquence pour les données
        }
    }
}
