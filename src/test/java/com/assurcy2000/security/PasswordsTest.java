package com.assurcy2000.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordsTest {

    @Test
    void hashThenMatch() {
        String hash = Passwords.hash("Secret123!");
        assertNotEquals("Secret123!", hash);
        assertTrue(Passwords.matches("Secret123!", hash));
        assertFalse(Passwords.matches("secret123!", hash));
    }

    @Test
    void samePasswordGivesDifferentHashes() { // sel aléatoire
        assertNotEquals(Passwords.hash("Secret123!"), Passwords.hash("Secret123!"));
    }

    @Test
    void missingOrInvalidValuesNeverMatch() {
        assertFalse(Passwords.matches("mot-de-passe-factice", null)); // compte introuvable, même avec le mot de passe factice
        assertFalse(Passwords.matches(null, Passwords.hash("x")));
        assertFalse(Passwords.matches("x", "pas-un-hash-bcrypt"));
    }
}
