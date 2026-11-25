package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dao.InsuranceTypeDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.CreateContractRequest;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** ContractService.update sans base de données : règles de modification d'un contrat. */
class ContractUpdateTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private ContractService service;
    private SessionUser agent, clientSession;
    private Contract contract;
    private User alice, bob;
    private InsuranceType habitation, auto;
    private Guarantee vol, incendie, rc;

    @BeforeEach
    void setUp() throws Exception {
        alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        bob = withId(new User("Bob", "Durand", "bob@x.fr", "h", new Role(Role.CLIENT)), 3L);
        User agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);

        vol = withId(new Guarantee("Vol", ""), 10L);
        incendie = withId(new Guarantee("Incendie", ""), 11L);
        rc = withId(new Guarantee("Responsabilité civile", ""), 12L);
        habitation = withId(new InsuranceType("Assurance habitation", "", new BigDecimal("220.00")), 5L);
        auto = withId(new InsuranceType("Assurance automobile", "", new BigDecimal("450.00")), 6L);

        contract = withId(new Contract("CTR-2026-0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("220.00"), alice, habitation), 100L);
        contract.setGuarantees(new HashSet<>(Set.of(vol)));

        Map<Long, User> users = Map.of(1L, alice, 2L, agentUser, 3L, bob);
        Map<Long, InsuranceType> types = Map.of(5L, habitation, 6L, auto);
        Map<Long, List<Guarantee>> guaranteesByType = Map.of(5L, List.of(vol, incendie), 6L, List.of(rc));

        ContractDao contractDao = new ContractDao() {
            @Override public Optional<Contract> findById(Long id) { return Optional.of(contract).filter(c -> c.getId().equals(id)); }
            @Override public Contract update(Contract c) { return c; }
        };
        UserDao userDao = new UserDao() {
            @Override public Optional<User> findById(Long id) { return Optional.ofNullable(users.get(id)); }
        };
        InsuranceTypeDao typeDao = new InsuranceTypeDao() {
            @Override public Optional<InsuranceType> findById(Long id) { return Optional.ofNullable(types.get(id)); }
        };
        GuaranteeDao guaranteeDao = new GuaranteeDao() {
            @Override public List<Guarantee> findByInsuranceType(Long typeId) { return guaranteesByType.get(typeId); }
        };

        service = new ContractService(contractDao, userDao, typeDao, guaranteeDao, DIRECT);
        agent = SessionUser.from(agentUser);
        clientSession = SessionUser.from(alice);
    }

    private CreateContractRequest form(Long clientId, Long typeId, BigDecimal premium, Set<Long> guarantees) {
        return new CreateContractRequest(clientId, typeId, LocalDate.of(2026, 2, 1), LocalDate.of(2027, 1, 31), premium, guarantees);
    }

    @Test
    void pendingContractCanChangeEverythingButNumberAndStatus() {
        contract.setStatus(ContractStatus.EN_ATTENTE);
        Contract c = service.update(agent, 100L, form(3L, 6L, new BigDecimal("500.00"), Set.of(12L)));

        assertSame(bob, c.getClient());
        assertSame(auto, c.getInsuranceType());
        assertEquals(Set.of(rc), c.getGuarantees());
        assertEquals(new BigDecimal("500.00"), c.getPremium());
        assertEquals(LocalDate.of(2026, 2, 1), c.getStartDate());
        assertEquals("CTR-2026-0001", c.getNumber());
        assertEquals(ContractStatus.EN_ATTENTE, c.getStatus());
    }

    @Test
    void activeContractKeepsClientAndType() {
        contract.setStatus(ContractStatus.ACTIF);
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(agent, 100L, form(3L, 6L, null, Set.of(12L))));
        assertTrue(e.getErrors().containsKey("clientId"));
        assertTrue(e.getErrors().containsKey("insuranceTypeId"));
        assertSame(alice, contract.getClient());
    }

    @Test
    void activeContractCanChangeDatesPremiumAndGuarantees() {
        contract.setStatus(ContractStatus.ACTIF);
        Contract c = service.update(agent, 100L, form(1L, 5L, new BigDecimal("240.00"), Set.of(10L, 11L)));
        assertEquals(new BigDecimal("240.00"), c.getPremium());
        assertEquals(Set.of(vol, incendie), c.getGuarantees());
        assertEquals(ContractStatus.ACTIF, c.getStatus());
    }

    @Test
    void emptyPremiumKeepsCurrentPremium() {
        contract.setStatus(ContractStatus.ACTIF);
        assertEquals(new BigDecimal("220.00"), service.update(agent, 100L, form(1L, 5L, null, Set.of(10L))).getPremium());
    }

    @Test
    void terminatedContractCannotBeModified() {
        contract.setStatus(ContractStatus.RESILIE);
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(agent, 100L, form(1L, 5L, null, Set.of(10L))));
        assertTrue(e.getErrors().containsKey("status"));
    }

    @Test
    void guaranteeOfAnotherTypeIsRejected() {
        contract.setStatus(ContractStatus.ACTIF);
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(agent, 100L, form(1L, 5L, null, Set.of(12L))));
        assertTrue(e.getErrors().containsKey("guaranteeIds"));
    }

    @Test
    void unknownContractIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.update(agent, 999L, form(1L, 5L, null, Set.of(10L))));
    }

    @Test
    void clientCannotModifyAContract() {
        assertThrows(ForbiddenException.class, () -> service.update(clientSession, 100L, form(1L, 5L, null, Set.of(10L))));
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
