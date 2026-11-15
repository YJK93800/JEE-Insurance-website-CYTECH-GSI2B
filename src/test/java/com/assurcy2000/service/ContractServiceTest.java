package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.security.SessionUser;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Prouve les restrictions SANS base de données : un faux DAO en mémoire remplace Hibernate. */
class ContractServiceTest {

    /** Faux DAO : surcharge uniquement les méthodes utilisées par le service. */
    static class FakeContractDao extends ContractDao {
        final Map<Long, Contract> store;
        FakeContractDao(Map<Long, Contract> store) { this.store = store; }
        @Override public Optional<Contract> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Contract> findAll() { return List.copyOf(store.values()); }
        @Override public List<Contract> findByClient(Long clientId) {
            return store.values().stream().filter(c -> c.getClient().getId().equals(clientId)).toList();
        }
        @Override public Contract update(Contract c) { return c; }
    }

    private SessionUser alice, bob, agent, admin;
    private ContractService service;
    private Contract aliceContract, bobContract;

    @BeforeEach
    void setUp() throws Exception {
        User aliceUser = user(1L, "alice"), bobUser = user(2L, "bob");
        aliceContract = contract(10L, aliceUser, ContractStatus.ACTIF);
        bobContract = contract(20L, bobUser, ContractStatus.ACTIF);
        service = new ContractService(new FakeContractDao(Map.of(10L, aliceContract, 20L, bobContract)));

        alice = new SessionUser(1L, "alice@x.fr", "Alice A", Role.CLIENT);
        bob = new SessionUser(2L, "bob@x.fr", "Bob B", Role.CLIENT);
        agent = new SessionUser(3L, "agent@x.fr", "Agent", Role.AGENT);
        admin = new SessionUser(4L, "admin@x.fr", "Admin", Role.ADMIN);
    }

    @Test
    void clientSeesOnlyOwnContracts() {
        assertEquals(List.of(aliceContract), service.listFor(alice));
        assertEquals(List.of(bobContract), service.listFor(bob));
    }

    @Test
    void clientCanOpenOwnContract() {
        assertSame(aliceContract, service.get(alice, 10L));
    }

    @Test
    void clientCannotOpenSomeoneElsesContractByChangingTheId() {
        assertThrows(NotFoundException.class, () -> service.get(alice, 20L));
        assertThrows(NotFoundException.class, () -> service.get(bob, 10L));
    }

    @Test
    void unknownContractIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.get(alice, 999L));
    }

    @Test
    void agentAndAdminSeeEverything() {
        assertEquals(2, service.listFor(agent).size());
        assertEquals(2, service.listFor(admin).size());
        assertSame(bobContract, service.get(agent, 20L));
    }

    @Test
    void agentCanListContractsOfAGivenClientButClientCannot() {
        assertEquals(List.of(aliceContract), service.listByClient(agent, 1L));
        assertThrows(ForbiddenException.class, () -> service.listByClient(alice, 2L));
        assertThrows(ForbiddenException.class, () -> service.search(alice, "bob"));
    }

    @Test
    void onlyAgentCanSuspend() {
        assertThrows(ForbiddenException.class, () -> service.suspend(alice, 10L));
        assertThrows(ForbiddenException.class, () -> service.suspend(admin, 10L));
        assertEquals(ContractStatus.SUSPENDU, service.suspend(agent, 10L).getStatus());
    }

    @Test
    void cannotSuspendTwice() {
        service.suspend(agent, 10L);
        assertThrows(ValidationException.class, () -> service.suspend(agent, 10L));
    }

    // --- aides : User#id et Contract#id sont générés par la base, on les fixe par réflexion ---

    private static User user(Long id, String name) throws Exception {
        User u = new User(name, "Test", name + "@x.fr", "hash", null);
        setId(u, id);
        return u;
    }

    private static Contract contract(Long id, User client, ContractStatus status) throws Exception {
        Contract c = new Contract("CTR-" + id, LocalDate.now().minusDays(10), LocalDate.now().plusYears(1),
                new BigDecimal("100.00"), client, null);
        c.setStatus(status);
        setId(c, id);
        return c;
    }

    private static void setId(Object entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }
}
