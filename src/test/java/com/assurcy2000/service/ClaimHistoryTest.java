package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimCommentDao;
import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ClaimHistoryDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.DeclareClaimRequest;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.ClaimHistory;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ValidationException;
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

/** Chaque changement de statut d'un sinistre est tracé dans historique_sinistre. */
class ClaimHistoryTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private ClaimService service;
    private SessionUser client, agent;
    private User alice, agentUser;
    private Contract contract;
    private Claim claim;
    private final List<ClaimHistory> history = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);
        InsuranceType auto = withId(new InsuranceType("Assurance automobile", "", new BigDecimal("400.00")), 5L);
        LocalDate start = LocalDate.now().minusMonths(6);
        contract = withId(new Contract("CTR-2026-0001", start, start.plusYears(1).minusDays(1),
                new BigDecimal("400.00"), alice, auto), 100L);
        contract.setStatus(ContractStatus.ACTIF);
        claim = withId(new Claim("SIN-2026-000001", LocalDate.now().minusDays(3), "Vitre cassée sur le parking",
                ClaimType.BRIS, new BigDecimal("300.00"), contract), 50L);

        ClaimDao claimDao = new ClaimDao() {
            @Override public Optional<Claim> findById(Long id) { return Optional.of(claim).filter(c -> c.getId().equals(id)); }
            @Override public Claim save(Claim c) { claim = c; return c; }
            @Override public Claim update(Claim c) { return c; }
            @Override public boolean existsByNumber(String n) { return false; }
        };
        ClaimHistoryDao historyDao = new ClaimHistoryDao() {
            @Override public ClaimHistory save(ClaimHistory h) { history.add(h); return h; }
            @Override public List<ClaimHistory> findByClaim(Long id) { return List.copyOf(history); }
        };
        ContractDao contractDao = new ContractDao() {
            @Override public Optional<Contract> findById(Long id) { return Optional.of(contract).filter(c -> c.getId().equals(id)); }
        };
        UserDao userDao = new UserDao() {
            @Override public User getReference(Long id) { return Map.of(1L, alice, 2L, agentUser).get(id); }
        };
        service = new ClaimService(claimDao, historyDao, new ClaimCommentDao(), contractDao, userDao, DIRECT);
        client = SessionUser.from(alice);
        agent = SessionUser.from(agentUser);
    }

    @Test
    void declarationIsRecordedWithoutPreviousStatus() {
        service.declare(client, new DeclareClaimRequest("100", LocalDate.now().minusDays(1).toString(),
                "VOL", "Vélo volé devant la maison", "500"));
        assertEquals(1, history.size());
        ClaimHistory h = history.get(0);
        assertNull(h.getOldStatus());
        assertEquals(ClaimStatus.DECLARE, h.getNewStatus());
        assertSame(alice, h.getChangedBy());
        assertSame(claim, h.getClaim());
    }

    @Test
    void invalidDeclarationRecordsNothing() {
        assertThrows(ValidationException.class, () -> service.declare(client,
                new DeclareClaimRequest("100", "pas une date", "VOL", "court", "-1")));
        assertTrue(history.isEmpty());
    }

    @Test
    void eachAgentStepIsRecordedInOrder() {
        service.changeStatus(agent, 50L, ClaimStatus.EN_ANALYSE);
        service.changeStatus(agent, 50L, ClaimStatus.ACCEPTE);
        service.indemnify(agent, 50L, "250");

        assertEquals(3, history.size());
        assertEquals(ClaimStatus.DECLARE, history.get(0).getOldStatus());
        assertEquals(ClaimStatus.EN_ANALYSE, history.get(0).getNewStatus());
        assertEquals(ClaimStatus.EN_ANALYSE, history.get(1).getOldStatus());
        assertEquals(ClaimStatus.ACCEPTE, history.get(1).getNewStatus());
        assertEquals(ClaimStatus.ACCEPTE, history.get(2).getOldStatus());
        assertEquals(ClaimStatus.INDEMNISE, history.get(2).getNewStatus());
        history.forEach(h -> assertSame(agentUser, h.getChangedBy()));
    }

    @Test
    void refusedTransitionRecordsNothing() {
        assertThrows(ValidationException.class, () -> service.changeStatus(agent, 50L, ClaimStatus.ACCEPTE));
        assertTrue(history.isEmpty());
    }

    @Test
    void clientCanReadHistoryOfOwnClaimOnly() {
        service.changeStatus(agent, 50L, ClaimStatus.EN_ANALYSE);
        assertEquals(1, service.history(client, 50L).size());

        User bob = withIdUnchecked(new User("Bob", "Durand", "bob@x.fr", "h", new Role(Role.CLIENT)), 3L);
        assertThrows(com.assurcy2000.service.exceptions.NotFoundException.class,
                () -> service.history(SessionUser.from(bob), 50L));
    }

    private static <T> T withIdUnchecked(T entity, Long id) {
        try { return withId(entity, id); } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
