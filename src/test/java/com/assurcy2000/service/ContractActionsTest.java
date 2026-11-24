package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dao.InsuranceTypeDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Actions de statut : activer, résilier, renouveler (suspendre est testé dans ContractServiceTest). */
class ContractActionsTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private ContractService service;
    private SessionUser agent;
    private Contract contract;
    private InsuranceType habitation;
    private Guarantee vol, incendie, ancienne;
    private final List<Contract> saved = new ArrayList<>();
    private Contract existingRenewal;
    private LocalDate expiredBefore;

    @BeforeEach
    void setUp() throws Exception {
        User alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        User agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);
        vol = withId(new Guarantee("Vol", ""), 10L);
        incendie = withId(new Guarantee("Incendie", ""), 11L);
        ancienne = withId(new Guarantee("Garantie retirée du type", ""), 13L);
        habitation = withId(new InsuranceType("Assurance habitation", "", new BigDecimal("220.00")), 5L);

        LocalDate start = LocalDate.now().minusMonths(6);
        contract = withId(new Contract("CTR-2026-0001", start, start.plusYears(1).minusDays(1),
                new BigDecimal("230.00"), alice, habitation), 100L);
        contract.setGuarantees(new HashSet<>(Set.of(vol, ancienne)));

        ContractDao contractDao = new ContractDao() {
            @Override public Optional<Contract> findById(Long id) { return Optional.of(contract).filter(c -> c.getId().equals(id)); }
            @Override public Contract update(Contract c) { return c; }
            @Override public Contract save(Contract c) { saved.add(c); return c; }
            @Override public Optional<String> findMaxNumberWithPrefix(String prefix) { return Optional.empty(); }
            @Override public Optional<Contract> findRenewalOf(Long id) { return Optional.ofNullable(existingRenewal); }
            @Override public int expireEndedBefore(LocalDate today) { expiredBefore = today; return 3; }
        };
        UserDao userDao = new UserDao() {
            @Override public Optional<User> findById(Long id) { return Optional.ofNullable(Map.of(1L, alice, 2L, agentUser).get(id)); }
            @Override public User getReference(Long id) { return agentUser; }
        };
        GuaranteeDao guaranteeDao = new GuaranteeDao() {
            @Override public List<Guarantee> findByInsuranceType(Long typeId) { return List.of(vol, incendie); }
        };
        service = new ContractService(contractDao, userDao, new InsuranceTypeDao(), guaranteeDao, DIRECT);
        agent = SessionUser.from(agentUser);
    }

    // ---- activer ----

    @Test
    void pendingOrSuspendedContractCanBeActivated() {
        contract.setStatus(ContractStatus.EN_ATTENTE);
        assertEquals(ContractStatus.ACTIF, service.activate(agent, 100L).getStatus());
        contract.setStatus(ContractStatus.SUSPENDU);
        assertEquals(ContractStatus.ACTIF, service.activate(agent, 100L).getStatus());
    }

    @Test
    void activeOrTerminatedContractCannotBeActivated() {
        contract.setStatus(ContractStatus.ACTIF);
        assertThrows(ValidationException.class, () -> service.activate(agent, 100L));
        contract.setStatus(ContractStatus.RESILIE);
        assertThrows(ValidationException.class, () -> service.activate(agent, 100L));
    }

    @Test
    void contractWhosePeriodIsOverCannotBeActivated() {
        contract.setStatus(ContractStatus.SUSPENDU);
        contract.setEndDate(LocalDate.now().minusDays(1));
        assertThrows(ValidationException.class, () -> service.activate(agent, 100L));
    }

    // ---- résilier ----

    @Test
    void terminationStoresDateAndReason() {
        contract.setStatus(ContractStatus.ACTIF);
        Contract c = service.terminate(agent, 100L, LocalDate.now(), "Vente du logement");
        assertEquals(ContractStatus.RESILIE, c.getStatus());
        assertEquals(LocalDate.now(), c.getTerminationDate());
        assertEquals("Vente du logement", c.getTerminationReason());
    }

    @Test
    void terminationDateMustBeInsideThePeriod() {
        contract.setStatus(ContractStatus.ACTIF);
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.terminate(agent, 100L, contract.getStartDate().minusDays(1), "x"));
        assertTrue(e.getErrors().containsKey("terminationDate"));
        assertEquals(ContractStatus.ACTIF, contract.getStatus());
    }

    @Test
    void terminatedContractCannotBeTerminatedAgain() {
        contract.setStatus(ContractStatus.RESILIE);
        assertThrows(ValidationException.class, () -> service.terminate(agent, 100L, LocalDate.now(), "x"));
    }

    // ---- renouveler ----

    @Test
    void renewalStartsTheDayAfterAndKeepsClientTypePremium() {
        contract.setStatus(ContractStatus.ACTIF);
        Contract r = service.renew(agent, 100L);

        assertEquals(contract.getEndDate().plusDays(1), r.getStartDate());
        assertEquals(r.getStartDate().plusYears(1).minusDays(1), r.getEndDate());
        assertEquals(ContractStatus.EN_ATTENTE, r.getStatus());
        assertSame(contract, r.getPreviousContract());
        assertSame(contract.getClient(), r.getClient());
        assertEquals(new BigDecimal("230.00"), r.getPremium());
        assertEquals(Set.of(vol), r.getGuarantees()); // la garantie retirée du type n'est pas reprise
        assertEquals(ContractStatus.ACTIF, contract.getStatus()); // l'ancien contrat ne change pas
        assertEquals(1, saved.size());
    }

    @Test
    void contractIsRenewedOnlyOnce() throws Exception {
        contract.setStatus(ContractStatus.ACTIF);
        existingRenewal = withId(new Contract("CTR-2027-0001", LocalDate.now(), LocalDate.now().plusYears(1),
                BigDecimal.TEN, contract.getClient(), habitation), 101L);
        ValidationException e = assertThrows(ValidationException.class, () -> service.renew(agent, 100L));
        assertTrue(e.getErrors().get("status").contains("CTR-2027-0001"));
        assertTrue(saved.isEmpty());
    }

    @Test
    void pendingContractCannotBeRenewed() {
        contract.setStatus(ContractStatus.EN_ATTENTE);
        assertThrows(ValidationException.class, () -> service.renew(agent, 100L));
    }

    // ---- expiration automatique ----

    @Test
    void expirationUsesTodayAndReturnsTheCount() {
        assertEquals(3, service.expireEndedContracts());
        assertEquals(LocalDate.now(), expiredBefore);
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
