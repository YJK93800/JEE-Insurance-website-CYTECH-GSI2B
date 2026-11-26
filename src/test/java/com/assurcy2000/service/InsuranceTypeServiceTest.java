package com.assurcy2000.service;

import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dao.InsuranceTypeDao;
import com.assurcy2000.dto.InsuranceTypeRow;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.InsuranceTypeRequest;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** InsuranceTypeService sans base de données : DAO en mémoire. */
class InsuranceTypeServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private final List<InsuranceType> types = new ArrayList<>();
    private final Map<Long, Long> contractCounts = new HashMap<>();
    private final GuaranteeServiceTest.FakeGuaranteeDao guarantees = new GuaranteeServiceTest.FakeGuaranteeDao();
    private InsuranceTypeService service;

    private final SessionUser admin = new SessionUser(4L, "Ada", "Admin", "admin@x.fr", Role.ADMIN);
    private final SessionUser agent = new SessionUser(2L, "Paul", "Agent", "agent@x.fr", Role.AGENT);
    private final SessionUser client = new SessionUser(1L, "Alice", "Martin", "alice@x.fr", Role.CLIENT);

    private Guarantee vol, incendie, rc;
    private InsuranceType auto, habitation;
    private long nextId = 1;

    @BeforeEach
    void setUp() throws Exception {
        vol = guarantees.save(new Guarantee("Vol", ""));
        incendie = guarantees.save(new Guarantee("Incendie", ""));
        rc = guarantees.save(new Guarantee("Responsabilité civile", ""));

        InsuranceTypeDao dao = new InsuranceTypeDao() {
            @Override public InsuranceType save(InsuranceType t) { setId(t, nextId++); types.add(t); return t; }
            @Override public InsuranceType update(InsuranceType t) { return t; }
            @Override public void delete(InsuranceType t) { types.remove(t); }
            @Override public Optional<InsuranceType> findById(Long id) {
                return types.stream().filter(t -> t.getId().equals(id)).findFirst();
            }
            @Override public Optional<InsuranceType> findByName(String name) {
                return types.stream().filter(t -> t.getName().equals(name)).findFirst();
            }
            @Override public List<InsuranceType> findAll() {
                return types.stream().sorted(Comparator.comparing(InsuranceType::getName)).toList();
            }
            @Override public List<InsuranceType> findAllActive() {
                return findAll().stream().filter(InsuranceType::isActive).toList();
            }
            @Override public Map<Long, Long> countContractsByType() { return Map.copyOf(contractCounts); }
            @Override public boolean isUsedByContract(Long id) { return contractCounts.getOrDefault(id, 0L) > 0; }
        };
        service = new InsuranceTypeService(dao, guarantees, DIRECT);

        habitation = service.create(admin, req("Assurance habitation", "20", vol, incendie));
        auto = service.create(admin, req("Assurance automobile", "40", vol, rc));
        contractCounts.put(auto.getId(), 2L);
    }

    private static InsuranceTypeRequest req(String name, String price, Guarantee... gs) {
        Set<Long> ids = new java.util.HashSet<>();
        for (Guarantee g : gs) ids.add(g.getId());
        return new InsuranceTypeRequest(name, "Description", price == null ? null : new BigDecimal(price), ids);
    }

    private static void setId(InsuranceType t, long id) {
        try {
            Field f = InsuranceType.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(t, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- droits ----

    @Test
    void onlyTheAdminManagesTypes() {
        assertThrows(ForbiddenException.class, () -> service.listForAdmin(agent));
        assertThrows(ForbiddenException.class, () -> service.create(agent, req("X", "10", vol)));
        assertThrows(ForbiddenException.class, () -> service.update(client, auto.getId(), req("X", "10", vol)));
        assertThrows(ForbiddenException.class, () -> service.setActive(agent, auto.getId(), false));
        assertThrows(ForbiddenException.class, () -> service.delete(client, habitation.getId()));
        assertThrows(ForbiddenException.class, () -> service.guaranteeChoices(agent));
        assertEquals(2, service.listActive(client).size()); // catalogue lisible par tout utilisateur connecté
    }

    // ---- liste ----

    @Test
    void listForAdminIsSortedByNameWithContractCounts() {
        List<InsuranceTypeRow> rows = service.listForAdmin(admin);
        assertEquals(List.of("Assurance automobile", "Assurance habitation"), rows.stream().map(InsuranceTypeRow::getName).toList());
        assertEquals(2, rows.get(0).getContractCount());
        assertFalse(rows.get(0).isDeletable());
        assertEquals(0, rows.get(1).getContractCount());
        assertTrue(rows.get(1).isDeletable());
        assertEquals(Set.of(vol, rc), rows.get(0).getGuarantees());
    }

    // ---- création ----

    @Test
    void createSavesTheTypeWithItsGuarantees() {
        InsuranceType t = service.create(admin, new InsuranceTypeRequest("  Assurance animaux ", " ", new BigDecimal("12.50"), Set.of(rc.getId())));
        assertNotNull(t.getId());
        assertEquals("Assurance animaux", t.getName());
        assertNull(t.getDescription());
        assertEquals(new BigDecimal("12.50"), t.getBasePrice());
        assertEquals(Set.of(rc), t.getGuarantees());
        assertTrue(t.isActive());
        assertEquals(3, service.listActive(admin).size()); // nouveau type proposé sans changer le code
    }

    @Test
    void createRefusesMissingFieldsAndDuplicateName() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(admin, new InsuranceTypeRequest(" ", null, null, Set.of())));
        assertTrue(e.getErrors().containsKey("name"));
        assertTrue(e.getErrors().containsKey("basePrice"));
        assertEquals("Cochez au moins une garantie.", e.getErrors().get("guaranteeIds"));

        ValidationException dup = assertThrows(ValidationException.class,
                () -> service.create(admin, req("Assurance automobile", "10", vol)));
        assertEquals("Un type d'assurance porte déjà ce nom.", dup.getErrors().get("name"));
        assertEquals(2, types.size());
    }

    @Test
    void createRefusesAnUnknownGuarantee() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(admin, new InsuranceTypeRequest("Assurance animaux", "", BigDecimal.TEN, Set.of(999L))));
        assertTrue(e.getErrors().containsKey("guaranteeIds"));
        assertEquals(2, types.size());
    }

    // ---- modification ----

    @Test
    void updateChangesFieldsAndGuarantees() {
        service.update(admin, auto.getId(), req("Assurance auto", "45", rc, incendie));
        assertEquals("Assurance auto", auto.getName());
        assertEquals(new BigDecimal("45"), auto.getBasePrice());
        assertEquals(Set.of(rc, incendie), auto.getGuarantees()); // vol retiré, incendie ajouté
    }

    @Test
    void updateKeepingItsOwnNameIsAllowedButNotAnotherOne() {
        assertDoesNotThrow(() -> service.update(admin, auto.getId(), req("Assurance automobile", "40", vol)));
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(admin, auto.getId(), req("Assurance habitation", "40", vol)));
        assertTrue(e.getErrors().containsKey("name"));
        assertEquals("Assurance automobile", auto.getName());
    }

    @Test
    void unknownTypeIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.update(admin, 999L, req("X", "10", vol)));
        assertThrows(NotFoundException.class, () -> service.get(admin, 999L));
        assertThrows(NotFoundException.class, () -> service.setActive(admin, 999L, true));
        assertThrows(NotFoundException.class, () -> service.delete(admin, 999L));
    }

    // ---- activation et suppression ----

    @Test
    void deactivatedTypeIsNoLongerOffered() {
        service.setActive(admin, auto.getId(), false);
        assertEquals(List.of(habitation), service.listActive(admin));
        assertEquals(2, service.listAll(admin).size()); // toujours visible pour les anciens contrats
        service.setActive(admin, auto.getId(), true);
        assertEquals(2, service.listActive(admin).size());
    }

    @Test
    void deleteRemovesAnUnusedTypeOnly() {
        assertEquals("Assurance habitation", service.delete(admin, habitation.getId()));
        ValidationException e = assertThrows(ValidationException.class, () -> service.delete(admin, auto.getId()));
        assertTrue(e.getErrors().get("id").contains("désactivez-le"));
        assertEquals(List.of(auto), types);
        assertEquals(3, guarantees.rows.size()); // les garanties ne sont pas supprimées
    }
}
