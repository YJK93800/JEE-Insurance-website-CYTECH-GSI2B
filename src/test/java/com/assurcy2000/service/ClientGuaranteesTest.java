package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** Page « Mes garanties » (ContractService.listOwnWithGuarantees), sans base de données. */
class ClientGuaranteesTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private final List<Contract> store = new ArrayList<>();
    private ContractService service;
    private final SessionUser alice = new SessionUser(1L, "Alice", "Martin", "alice@x.fr", Role.CLIENT);
    private final SessionUser agent = new SessionUser(3L, "Paul", "Agent", "agent@x.fr", Role.AGENT);
    private final SessionUser admin = new SessionUser(4L, "Ada", "Admin", "admin@x.fr", Role.ADMIN);

    @BeforeEach
    void setUp() throws Exception {
        User aliceUser = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        User bobUser = withId(new User("Bob", "Durand", "bob@x.fr", "h", new Role(Role.CLIENT)), 2L);
        InsuranceType auto = withId(new InsuranceType("Assurance automobile", "", new BigDecimal("40.00")), 5L);
        long id = 10;
        for (ContractStatus s : ContractStatus.values()) {
            store.add(contract(id++, aliceUser, auto, s));
        }
        store.add(contract(id, bobUser, auto, ContractStatus.ACTIF));

        ContractDao dao = new ContractDao() {
            @Override public List<Contract> findByClientWithGuarantees(Long clientId, Collection<ContractStatus> statuses) {
                return store.stream()
                        .filter(c -> c.getClient().getId().equals(clientId) && statuses.contains(c.getStatus()))
                        .toList();
            }
        };
        service = new ContractService(dao, DIRECT);
    }

    @Test
    void clientSeesOnlyHisContractsInForce() {
        List<Contract> mine = service.listOwnWithGuarantees(alice);
        assertEquals(List.of(ContractStatus.EN_ATTENTE, ContractStatus.ACTIF, ContractStatus.SUSPENDU),
                mine.stream().map(Contract::getStatus).toList()); // ni expiré ni résilié
        assertTrue(mine.stream().allMatch(c -> c.getClient().getId().equals(1L))); // pas le contrat de Bob
    }

    @Test
    void agentAndAdminDoNotUseThisPage() {
        assertThrows(ForbiddenException.class, () -> service.listOwnWithGuarantees(agent));
        assertThrows(ForbiddenException.class, () -> service.listOwnWithGuarantees(admin));
        assertThrows(ForbiddenException.class, () -> service.listOwnWithGuarantees(null));
    }

    private static Contract contract(long id, User client, InsuranceType type, ContractStatus status) throws Exception {
        Contract c = withId(new Contract("CTR-2026-00" + id, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("40.00"), client, type), id);
        c.setStatus(status);
        return c;
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
