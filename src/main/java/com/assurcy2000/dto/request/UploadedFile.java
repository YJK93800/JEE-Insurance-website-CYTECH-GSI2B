package com.assurcy2000.dto.request;

import java.io.IOException;
import java.io.InputStream;

/**
 * Fichier envoyé par un formulaire multipart, sans dépendre de l'API Servlet :
 * la servlet le construit à partir d'un Part, les tests à partir d'un tableau d'octets.
 * name : nom donné par le navigateur (non fiable), size : taille en octets.
 */
public record UploadedFile(String name, long size, Opener opener) {

    @FunctionalInterface
    public interface Opener {
        InputStream open() throws IOException;
    }

    /** Nouveau flux sur le contenu ; peut être appelé plusieurs fois (vérification du type, puis copie). */
    public InputStream open() throws IOException {
        return opener.open();
    }
}
