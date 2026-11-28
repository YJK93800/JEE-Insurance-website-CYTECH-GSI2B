package com.assurcy2000.util;

import com.assurcy2000.dto.request.UploadedFile;
import com.assurcy2000.model.enums.ClaimType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Règles de saisie (classe Validation, appelée par les servlets) : inscription, profil, sinistres. */
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

    // ---- Sinistres : déclaration, commentaire, indemnisation, pièces ----

    private static final byte[] PDF = "%PDF-1.4 contenu".getBytes();

    private static UploadedFile file(String name, byte[] content) {
        return new UploadedFile(name, content.length, () -> new ByteArrayInputStream(content));
    }

    @Test
    void validClaimFieldsAreAccepted() {
        assertEquals(12L, Validation.requiredId(" 12 ", "contractId", "Choisissez un contrat.", errors));
        assertEquals(LocalDate.now(), Validation.claimDate(LocalDate.now().toString(), errors));
        assertEquals(ClaimType.VOL, Validation.claimType("VOL", errors));
        assertEquals("Vitre brisée par un ballon", Validation.claimDescription("  Vitre brisée par un ballon ", errors));
        assertEquals("Devis demandé", Validation.comment(" Devis demandé ", errors));
        assertEquals(2, Validation.attachments(List.of(file("a.pdf", PDF), file("b.pdf", PDF)), 0, errors).size());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void invalidClaimFieldsAreRejected() {
        assertNull(Validation.requiredId("", "contractId", "Choisissez un contrat.", errors));
        Validation.claimDate(LocalDate.now().plusDays(1).toString(), errors);
        Validation.claimType("INCONNU", errors);
        Validation.claimDescription("court", errors);
        assertEquals("Choisissez un contrat.", errors.get("contractId"));
        assertEquals("La date du sinistre ne peut pas être dans le futur.", errors.get("claimDate"));
        assertEquals("Type de sinistre invalide.", errors.get("type"));
        assertEquals("Décrivez le sinistre (10 caractères minimum).", errors.get("description"));
    }

    @Test
    void claimDateMustBePresentAndValid() {
        assertNull(Validation.claimDate("", errors));
        assertEquals("La date du sinistre est obligatoire.", errors.get("claimDate"));
        errors.clear();
        assertNull(Validation.claimDate("31/12/2025", errors));
        assertEquals("La date n'est pas valide.", errors.get("claimDate"));
    }

    @Test
    void tooLongTextsAreRejected() {
        String longText = "x".repeat(Validation.LONG_TEXT_MAX + 1);
        Validation.claimDescription(longText, errors);
        Validation.comment(longText, errors);
        assertTrue(errors.get("description").contains("trop longue"));
        assertTrue(errors.get("content").contains("trop long"));
        errors.clear();
        Validation.comment(" ", errors);
        assertEquals("Écrivez un commentaire.", errors.get("content"));
    }

    @Test
    void claimAmountsAreBounded() {
        assertNull(Validation.amount("0", "amount", "Le montant", Validation.CLAIM_AMOUNT_MAX, true, errors));
        assertTrue(errors.containsKey("amount"));
        errors.clear();
        assertNull(Validation.amount("10000000.01", "amount", "Le montant", Validation.CLAIM_AMOUNT_MAX, true, errors));
        assertTrue(errors.get("amount").contains("ne peut pas dépasser"));
        errors.clear();
        assertEquals(new BigDecimal("1250.50"), Validation.amount("1250,50", "amount", "Le montant", Validation.CLAIM_AMOUNT_MAX, true, errors));
    }

    @Test
    void attachmentsAreCheckedByCountSizeAndRealType() {
        Validation.attachments(List.of(file("1.pdf", PDF), file("2.pdf", PDF), file("3.pdf", PDF)), 3, errors);
        assertTrue(errors.get("attachments").contains("3 déjà jointe(s)"));
        errors.clear();
        Validation.attachments(List.of(new UploadedFile("gros.pdf", AttachmentStorage.MAX_SIZE + 1, () -> new ByteArrayInputStream(PDF))), 0, errors);
        assertEquals("« gros.pdf » dépasse 5 Mo.", errors.get("attachments"));
        errors.clear();
        Validation.attachments(List.of(file("C:\\Users\\virus.pdf", "MZ exécutable".getBytes())), 0, errors);
        assertEquals("« virus.pdf » n'est pas un PDF, un JPG ou un PNG.", errors.get("attachments"));
    }
}
