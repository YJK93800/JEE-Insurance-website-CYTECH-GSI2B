package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimAttachmentDao;
import com.assurcy2000.dao.ClaimCommentDao;
import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ClaimHistoryDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.ClaimComment;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.AttachmentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Commentaires internes de l'agent sur un sinistre. */
class ClaimCommentTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private ClaimService service;
    private SessionUser client, agent, admin;
    private User agentUser;
    private Claim claim;
    private final List<ClaimComment> comments = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        User alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);
        User adminUser = withId(new User("Ada", "Admin", "admin@x.fr", "h", new Role(Role.ADMIN)), 3L);
        InsuranceType auto = withId(new InsuranceType("Assurance automobile", "", new BigDecimal("400.00")), 5L);
        LocalDate start = LocalDate.now().minusMonths(6);
        Contract contract = withId(new Contract("CTR-2026-0001", start, start.plusYears(1).minusDays(1),
                new BigDecimal("400.00"), alice, auto), 100L);
        claim = withId(new Claim("SIN-2026-000001", LocalDate.now().minusDays(3), "Vitre cassée sur le parking",
                ClaimType.BRIS, new BigDecimal("300.00"), contract), 50L);

        ClaimDao claimDao = new ClaimDao() {
            @Override public Optional<Claim> findById(Long id) { return Optional.of(claim).filter(c -> c.getId().equals(id)); }
        };
        ClaimCommentDao commentDao = new ClaimCommentDao() {
            @Override public ClaimComment save(ClaimComment c) { comments.add(c); return c; }
            @Override public List<ClaimComment> findByClaim(Long id) { return List.copyOf(comments); }
        };
        UserDao userDao = new UserDao() {
            @Override public User getReference(Long id) { return Map.of(2L, agentUser, 3L, adminUser).get(id); }
        };
        service = new ClaimService(claimDao, new ClaimHistoryDao(), commentDao, new ClaimAttachmentDao(), new ContractDao(),
                userDao, new AttachmentStorage(java.nio.file.Path.of("target", "test-uploads")), DIRECT);
        client = SessionUser.from(alice);
        agent = SessionUser.from(agentUser);
        admin = SessionUser.from(adminUser);
    }

    @Test
    void agentAddsTrimmedCommentSignedByHim() {
        ClaimComment c = service.addComment(agent, 50L, "  Photos reçues, devis du garagiste demandé.  ");
        assertEquals("Photos reçues, devis du garagiste demandé.", c.getContent());
        assertSame(agentUser, c.getAuthor());
        assertSame(claim, c.getClaim());
        assertEquals(1, comments.size());
    }

    @Test
    void commentIsAllowedOnClosedClaim() {
        claim.setStatus(ClaimStatus.REFUSE);
        service.addComment(agent, 50L, "Refus confirmé après appel du client.");
        assertEquals(1, comments.size());
    }

    @Test
    void emptyOrTooLongCommentIsRejected() {
        ValidationException empty = assertThrows(ValidationException.class, () -> service.addComment(agent, 50L, "   "));
        assertTrue(empty.getErrors().containsKey("content"));
        assertThrows(ValidationException.class, () -> service.addComment(agent, 50L, "x".repeat(2001)));
        assertTrue(comments.isEmpty());
    }

    @Test
    void onlyTheAgentCanWriteComments() {
        assertThrows(ForbiddenException.class, () -> service.addComment(client, 50L, "Je voudrais une réponse"));
        assertThrows(ForbiddenException.class, () -> service.addComment(admin, 50L, "Note de l'admin"));
        assertTrue(comments.isEmpty());
    }

    @Test
    void agentAndAdminReadCommentsButNotTheClient() {
        service.addComment(agent, 50L, "Dossier complet.");
        assertEquals(1, service.comments(agent, 50L).size());
        assertEquals(1, service.comments(admin, 50L).size());
        assertThrows(ForbiddenException.class, () -> service.comments(client, 50L));
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
