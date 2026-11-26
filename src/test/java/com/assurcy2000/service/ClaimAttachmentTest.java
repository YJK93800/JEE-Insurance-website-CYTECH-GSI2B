package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimAttachmentDao;
import com.assurcy2000.dao.ClaimCommentDao;
import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ClaimHistoryDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.DeclareClaimRequest;
import com.assurcy2000.dto.request.UploadedFile;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.ClaimAttachment;
import com.assurcy2000.model.ClaimHistory;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.AttachmentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Pièces justificatives : à la déclaration, ajout ultérieur, limites, droits et nettoyage des fichiers. */
class ClaimAttachmentTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };
    static final byte[] PDF = "%PDF-1.7 constat".getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

    @TempDir Path root;

    private ClaimService service;
    private SessionUser client, otherClient, agent;
    private Contract contract;
    private Claim claim;
    private final List<ClaimAttachment> saved = new ArrayList<>();
    private boolean failOnSave;

    @BeforeEach
    void setUp() throws Exception {
        User alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        User bob = withId(new User("Bob", "Durand", "bob@x.fr", "h", new Role(Role.CLIENT)), 4L);
        User agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);
        InsuranceType auto = withId(new InsuranceType("Assurance automobile", "", new BigDecimal("400.00")), 5L);
        LocalDate start = LocalDate.now().minusMonths(6);
        contract = withId(new Contract("CTR-2026-0001", start, start.plusYears(1).minusDays(1),
                new BigDecimal("400.00"), alice, auto), 100L);
        contract.setStatus(ContractStatus.ACTIF);
        claim = withId(new Claim("SIN-2026-000001", LocalDate.now().minusDays(3), "Vitre cassée sur le parking",
                ClaimType.BRIS, new BigDecimal("300.00"), contract), 50L);

        ClaimDao claimDao = new ClaimDao() {
            @Override public Optional<Claim> findById(Long id) { return Optional.of(claim).filter(c -> c.getId().equals(id)); }
            @Override public Claim save(Claim c) {
                try { claim = withId(c, 51L); } catch (Exception e) { throw new RuntimeException(e); }
                return claim;
            }
            @Override public boolean existsByNumber(String n) { return false; }
        };
        ClaimAttachmentDao attachmentDao = new ClaimAttachmentDao() {
            @Override public ClaimAttachment save(ClaimAttachment a) {
                if (failOnSave) throw new IllegalStateException("base indisponible");
                try { saved.add(withId(a, 700L + saved.size())); } catch (Exception e) { throw new RuntimeException(e); }
                return a;
            }
            @Override public long countByClaim(Long id) { return saved.stream().filter(a -> a.getClaim().getId().equals(id)).count(); }
            @Override public List<ClaimAttachment> findByClaim(Long id) { return List.copyOf(saved); }
            @Override public Optional<ClaimAttachment> findById(Long id) {
                return saved.stream().filter(a -> a.getId().equals(id)).findFirst();
            }
        };
        ClaimHistoryDao historyDao = new ClaimHistoryDao() {
            @Override public ClaimHistory save(ClaimHistory h) { return h; }
        };
        ContractDao contractDao = new ContractDao() {
            @Override public Optional<Contract> findById(Long id) { return Optional.of(contract).filter(c -> c.getId().equals(id)); }
        };
        UserDao userDao = new UserDao() {
            @Override public User getReference(Long id) { return Map.of(1L, alice, 2L, agentUser, 4L, bob).get(id); }
        };
        service = new ClaimService(claimDao, historyDao, new ClaimCommentDao(), attachmentDao, contractDao, userDao,
                new AttachmentStorage(root), DIRECT);
        client = SessionUser.from(alice);
        otherClient = SessionUser.from(bob);
        agent = SessionUser.from(agentUser);
    }

    // ---- à la déclaration ----

    @Test
    void declarationStoresFilesAndRecordsThem() throws Exception {
        Claim declared = service.declare(client, validRequest(),
                List.of(file("C:\\Users\\alice\\constat.pdf", PDF), file("photo.png", PNG)));

        assertEquals(2, saved.size());
        ClaimAttachment first = saved.get(0);
        assertEquals("constat.pdf", first.getFileName());          // chemin du poste client retiré
        assertEquals("application/pdf", first.getMimeType());
        assertEquals((long) PDF.length, first.getSizeBytes());
        assertSame(declared, first.getClaim());
        assertEquals("image/png", saved.get(1).getMimeType());
        assertArrayEquals(PDF, Files.readAllBytes(service.fileOf(first)));
        assertEquals(2, storedFiles());
    }

    @Test
    void wrongFileRejectsTheWholeDeclarationAndStoresNothing() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.declare(client, validRequest(),
                List.of(file("constat.pdf", PDF), file("virus.pdf", "MZ exécutable".getBytes()))));
        assertEquals("« virus.pdf » n'est pas un PDF, un JPG ou un PNG.", e.getErrors().get("attachments"));
        assertTrue(saved.isEmpty());
        assertEquals(0, storedFiles());
    }

    @Test
    void tooBigEmptyOrTooManyFilesAreRejected() {
        UploadedFile big = new UploadedFile("gros.pdf", AttachmentStorage.MAX_SIZE + 1, () -> new ByteArrayInputStream(PDF));
        assertTrue(assertThrows(ValidationException.class, () -> service.declare(client, validRequest(), List.of(big)))
                .getErrors().get("attachments").contains("dépasse 5 Mo"));

        assertTrue(assertThrows(ValidationException.class,
                () -> service.declare(client, validRequest(), List.of(file("vide.pdf", new byte[0]))))
                .getErrors().get("attachments").contains("est vide"));

        List<UploadedFile> six = Collections.nCopies(6, file("p.pdf", PDF));
        assertTrue(assertThrows(ValidationException.class, () -> service.declare(client, validRequest(), six))
                .getErrors().get("attachments").startsWith("5 pièces justificatives maximum"));
        assertEquals(0, storedFiles());
    }

    @Test
    void filesAreDeletedWhenTheDatabaseFails() {
        failOnSave = true;
        assertThrows(IllegalStateException.class,
                () -> service.declare(client, validRequest(), List.of(file("constat.pdf", PDF))));
        assertEquals(0, storedFiles());
    }

    // ---- ajout après la déclaration ----

    @Test
    void clientAddsFilesWhileClaimIsOpenWithinTheLimit() {
        assertEquals(3, service.addAttachments(client, 50L, Collections.nCopies(3, file("p.pdf", PDF))).size());
        claim.setStatus(ClaimStatus.EN_ANALYSE);
        assertEquals(2, service.addAttachments(client, 50L, Collections.nCopies(2, file("p.png", PNG))).size());

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.addAttachments(client, 50L, List.of(file("p.pdf", PDF))));
        assertTrue(e.getErrors().get("attachments").contains("5 déjà jointe(s)"));
        assertEquals(5, storedFiles());
    }

    @Test
    void noFileCanBeAddedOnceTheClaimIsDecided() {
        claim.setStatus(ClaimStatus.ACCEPTE);
        assertThrows(ValidationException.class, () -> service.addAttachments(client, 50L, List.of(file("p.pdf", PDF))));
        assertThrows(ValidationException.class, () -> service.addAttachments(client, 50L, List.of()));
        assertEquals(0, storedFiles());
    }

    @Test
    void onlyTheOwnerCanAddFiles() {
        assertThrows(NotFoundException.class, () -> service.addAttachments(otherClient, 50L, List.of(file("p.pdf", PDF))));
        assertThrows(ForbiddenException.class, () -> service.addAttachments(agent, 50L, List.of(file("p.pdf", PDF))));
        assertEquals(0, storedFiles());
    }

    // ---- consultation ----

    @Test
    void ownerAndAgentCanDownloadOtherClientsCannot() {
        service.addAttachments(client, 50L, List.of(file("constat.pdf", PDF)));
        Long id = saved.get(0).getId();

        assertEquals("constat.pdf", service.attachment(client, id).getFileName());
        assertEquals("constat.pdf", service.attachment(agent, id).getFileName());
        assertThrows(NotFoundException.class, () -> service.attachment(otherClient, id));
        assertThrows(NotFoundException.class, () -> service.attachment(client, 999L));
        assertEquals(1, service.attachments(agent, 50L).size());
    }

    @Test
    void displayedNameIsCleaned() {
        assertEquals("constat.pdf", ClaimService.cleanFileName("/home/alice/constat.pdf"));
        assertEquals("ab.pdf", ClaimService.cleanFileName("a\u0000b.pdf"));
        assertEquals("fichier", ClaimService.cleanFileName("   "));
        assertEquals(255, ClaimService.cleanFileName("x".repeat(300) + ".pdf").length());
    }

    // ---- outils ----

    private DeclareClaimRequest validRequest() {
        return new DeclareClaimRequest("100", LocalDate.now().minusDays(1).toString(), "VOL",
                "Vélo volé devant la maison", "500");
    }

    private static UploadedFile file(String name, byte[] content) {
        return new UploadedFile(name, content.length, () -> new ByteArrayInputStream(content));
    }

    private long storedFiles() {
        if (!Files.exists(root)) return 0;
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).count();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
