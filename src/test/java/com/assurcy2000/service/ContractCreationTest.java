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
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** ContractService.create sans base de données : faux DAO en mémoire. */
class ContractCreationTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    /** Contrats enregistrés + plus grand numéro existant (pour la numérotation). */
    static class FakeContractDao extends ContractDao {
        final List<Contract> saved = new ArrayList<>();
        String maxNumber;
        @Override public Contract save(Contract c) { saved.add(c); return c; }
        @Override public Optional<String> findMaxNumberWithPrefix(String prefix) {
            return Optional.ofNullable(maxNumber).filter(n -> n.startsWith(prefix));
        }
    }

    private FakeContractDao contractDao;
    private ContractService service;
    private SessionUser agent, client;
    private Guarantee vol, incendie, voyage;
    private InsuranceType habitation;

    @BeforeEach
    void setUp() throws Exception {
        User clientUser = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        User agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);

        vol = withId(new Guarantee("Vol", ""), 10L);
        incendie = withId(new Guarantee("Incendie", ""), 11L);
        voyage = withId(new Guarantee("Assistance voyage", ""), 12L);   // pas proposée en habitation
        habitation = withId(new InsuranceType("Assurance habitation", "", new BigDecimal("220.00")), 5L);

        Map<Long, User> users = Map.of(1L, clientUser, 2L, agentUser);
        UserDao userDao = new UserDao() {
            @Override public Optional<User> findById(Long id) { return Optional.ofNullable(users.get(id)); }
        };
        InsuranceTypeDao typeDao = new InsuranceTypeDao() {
            @Override public Optional<InsuranceType> findById(Long id) { return Optional.of(habitation).filter(t -> t.getId().equals(id)); }
        };
        GuaranteeDao guaranteeDao = new GuaranteeDao() {
            @Override public List<Guarantee> findByInsuranceType(Long typeId) { return List.of(vol, incendie); }
        };

        contractDao = new FakeContractDao();
        service = new ContractService(contractDao, userDao, typeDao, guaranteeDao, DIRECT);
        agent = SessionUser.from(agentUser);
        client = SessionUser.from(clientUser);
    }

    private CreateContractRequest request(BigDecimal premium, Set<Long> guarantees) {
        return new CreateContractRequest(1L, 5L, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 11, 30), premium, guarantees);
    }

    @Test
    void createsPendingContractWithFirstNumberOfTheYear() {
        Contract c = service.create(agent, request(new BigDecimal("250.00"), Set.of(10L, 11L)));

        assertEquals("CTR-" + Year.now().getValue() + "-0001", c.getNumber());
        assertEquals(ContractStatus.EN_ATTENTE, c.getStatus());
        assertEquals(new BigDecimal("250.00"), c.getPremium());
        assertEquals(2, c.getGuarantees().size());
        assertEquals(Long.valueOf(2L), c.getAgent().getId());
        assertEquals(1, contractDao.saved.size());
    }

    @Test
    void numberFollowsTheLastOneOfTheYear() {
        contractDao.maxNumber = "CTR-" + Year.now().getValue() + "-0041";
        assertEquals("CTR-" + Year.now().getValue() + "-0042", service.create(agent, request(null, Set.of(10L))).getNumber());
    }

    @Test
    void emptyPremiumUsesBasePriceOfTheType() {
        assertEquals(new BigDecimal("220.00"), service.create(agent, request(null, Set.of(10L))).getPremium());
    }

    @Test
    void guaranteeNotOfferedByTheTypeIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(agent, request(null, Set.of(10L, 12L))));
        assertTrue(e.getErrors().containsKey("garanties"));
        assertTrue(contractDao.saved.isEmpty());
    }

    @Test
    void contractMustBelongToAClient() {
        CreateContractRequest forAgent = new CreateContractRequest(2L, 5L, LocalDate.of(2026, 12, 1),
                LocalDate.of(2027, 11, 30), null, Set.of(10L));
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(agent, forAgent));
        assertTrue(e.getErrors().containsKey("clientId"));
    }

    @Test
    void clientCannotCreateAContract() {
        assertThrows(ForbiddenException.class, () -> service.create(client, request(null, Set.of(10L))));
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
