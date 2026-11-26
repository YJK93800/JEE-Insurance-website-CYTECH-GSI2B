package com.assurcy2000.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Properties;

public final class DbConfig {

    private static final Properties PROPS = load();

    private DbConfig() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire db.properties", e);
        }
        return p;
    }

    private static String get(String key, String envVar) {
        String env = System.getenv(envVar);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configuration manquante : '" + key
                    + "' dans db.properties (ou variable d'environnement " + envVar + ")");
        }
        return value.trim();
    }

    public static String url()      { return get("db.url", "DB_URL"); }
    public static String user()     { return get("db.user", "DB_USER"); }
    public static String password() { return get("db.password", "DB_PASSWORD"); }

    /**
     * Dossier des pièces justificatives (hors du WAR) : variable UPLOADS_DIR, sinon uploads.dir
     * dans db.properties, sinon ~/assurcy2000-uploads. Facultatif, contrairement aux réglages de la base.
     */
    public static Path uploadsDir() {
        String env = System.getenv("UPLOADS_DIR");
        if (env != null && !env.isBlank()) return Path.of(env.trim());
        String value = PROPS.getProperty("uploads.dir");
        if (value != null && !value.isBlank()) return Path.of(value.trim());
        return Path.of(System.getProperty("user.home"), "assurcy2000-uploads");
    }
}