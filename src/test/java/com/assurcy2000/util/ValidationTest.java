package com.assurcy2000.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Règles de saisie de l'inscription et du profil (classe Validation, appelée par les servlets). */
class ValidationTest {

    private Map<String, String> errors;

    @BeforeEach
    void setUp() { errors = new HashMap<>(); }

    // ---- Champs obligatoires ----

    @Test
    void emptyOrBlankNamesAreRejected() {
        Validation.firstName("", errors);
        Validation.lastName("   ", errors);
        assertTrue(errors.containsKey("firstName"));
        assertTrue(errors.containsKey("lastName"));
    }

    @Test
    void missingNamesAreRejected() {
        Validation.firstName(null, errors);
        assertTrue(errors.containsKey("firstName"));
    }

    @Test
    void namesAreTrimmed() {
        assertEquals("Alice", Validation.firstName("  Alice ", errors));
        assertTrue(errors.isEmpty());
    }

    @Test
    void tooLongNameIsRejected() {
        Validation.lastName("a".repeat(Validation.NAME_MAX + 1), errors);
        assertTrue(errors.containsKey("lastName"));
    }

    // ---- Email ----

    @Test
    void emailIsTrimmedAndLowercased() {
        assertEquals("alice@x.fr", Validation.email("  Alice@X.FR ", errors));
        assertTrue(errors.isEmpty());
    }

    @Test
    void emptyOrMalformedEmailIsRejected() {
        Validation.email("", errors);
        assertTrue(errors.containsKey("email"));

        for (String bad : new String[] {"alice", "alice@", "alice@x", "a b@x.fr", "a@@x.fr"}) {
            Map<String, String> e = new HashMap<>();
            Validation.email(bad, e);
            assertTrue(e.containsKey("email"), "devrait être refusé : " + bad);
        }
    }

    // ---- Date de naissance (18 ans minimum) ----

    @Test
    void emptyBirthDateIsRejected() {
        assertNull(Validation.birthDate("", errors));
        assertTrue(errors.containsKey("birthDate"));
    }

    @Test
    void under18IsRejected() {
        String yesterdayMinus18 = LocalDate.now().minusYears(18).plusDays(1).toString(); // 18 ans demain
        Validation.birthDate(yesterdayMinus18, errors);
        assertTrue(errors.get("birthDate").contains("18"));
    }

    @Test
    void exactly18TodayIsAccepted() {
        LocalDate birth = LocalDate.now().minusYears(18);
        assertEquals(birth, Validation.birthDate(birth.toString(), errors));
        assertTrue(errors.isEmpty());
    }

    @Test
    void unrealisticOrMalformedBirthDateIsRejected() {
        Validation.birthDate(LocalDate.now().minusYears(130).toString(), errors);
        assertTrue(errors.containsKey("birthDate"));

        Map<String, String> e = new HashMap<>();
        assertNull(Validation.birthDate("14/03/1995", e)); // le champ date envoie AAAA-MM-JJ
        assertTrue(e.containsKey("birthDate"));
    }

    // ---- Champs facultatifs ----

    @Test
    void emptyPhoneAndAddressBecomeNull() {
        assertNull(Validation.phone("  ", errors));
        assertNull(Validation.address("", errors));
        assertTrue(errors.isEmpty());
    }

    @Test
    void phoneFormatIsChecked() {
        assertEquals("06 12 34 56 78", Validation.phone("06 12 34 56 78", errors));
        assertEquals("+33612345678", Validation.phone("+33612345678", errors));
        assertTrue(errors.isEmpty());

        Validation.phone("abc", errors);
        assertTrue(errors.containsKey("phone"));
    }

    @Test
    void tooLongAddressIsRejected() {
        Validation.address("a".repeat(Validation.ADDRESS_MAX + 1), errors);
        assertTrue(errors.containsKey("address"));
    }

    // ---- Mot de passe ----

    @Test
    void validPasswordWithMatchingConfirmationPasses() {
        Validation.validatePassword("motdepasse1", "motdepasse1", "password", "confirmation", errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void tooShortOrWithoutDigitIsRejected() {
        Validation.validatePassword("abc1", "abc1", "password", "confirmation", errors);
        assertTrue(errors.containsKey("password"));

        Map<String, String> e = new HashMap<>();
        Validation.validatePassword("motdepasse", "motdepasse", "password", "confirmation", e);
        assertTrue(e.containsKey("password"));
    }

    @Test
    void tooLongPasswordIsRejected() {
        String p = "a1".repeat(Validation.PASSWORD_MAX); // 144 caractères
        Validation.validatePassword(p, p, "password", "confirmation", errors);
        assertTrue(errors.containsKey("password"));
    }

    @Test
    void differentConfirmationIsRejected() {
        Validation.validatePassword("motdepasse1", "motdepasse2", "password", "confirmation", errors);
        assertTrue(errors.containsKey("confirmation"));
        assertFalse(errors.containsKey("password"));
    }

    @Test
    void missingPasswordIsRejected() {
        Validation.validatePassword(null, null, "password", "confirmation", errors);
        assertTrue(errors.containsKey("password"));
    }
}
