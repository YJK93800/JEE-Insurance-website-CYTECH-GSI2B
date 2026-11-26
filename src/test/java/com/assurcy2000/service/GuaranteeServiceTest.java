package com.assurcy2000.service;

import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dto.GuaranteeRow;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.GuaranteeRequest;
import com.assurcy2000.model.Guarantee;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** GuaranteeService sans base de données : DAO en mémoire. */
class GuaranteeServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };

    private final FakeGuaranteeDao dao = new FakeGuaranteeDao();
    private final GuaranteeService service = new GuaranteeService(dao, DIRECT);

    private final SessionUser agent = new SessionUser(2L, "Paul", "Agent", "agent@x.fr", Role.AGENT);
    private final SessionUser admin = new SessionUser(4L, "Ada", "Admin", "admin@x.fr", Role.ADMIN);
    private final SessionUser client = new SessionUser(1L, "Alice", "Martin", "alice@x.fr", Role.CLIENT);

    private Guarantee vol, incendie;

    @BeforeEach
    void setUp() {
        vol = dao.save(new Guarantee("Vol", "Vol ou tentative de vol", eur("15000"), eur("150")));
        incendie = dao.save(new Guarantee("Incendie", "Dommages causés par un incendie", eur("300000"), eur("300")));
        dao.contractCounts.put(vol.getId(), 3L); // le vol est souscrit dans 3 contrats
    }

    private static BigDecimal eur(String v) { return new BigDecimal(v); }

    private static GuaranteeRequest req(String name, String limit, String deductible) {
        return new GuaranteeRequest(name, "Description",
                limit == null ? null : eur(limit), deductible == null ? null : eur(deductible));
    }

    // ---- droits ----

    @Test
    void onlyTheAgentManagesGuarantees() {
        assertThrows(ForbiddenException.class, () -> service.listForAgent(client));
        assertThrows(ForbiddenException.class, () -> service.listForAgent(admin));
        assertThrows(ForbiddenException.class, () -> service.create(client, req("Bris de glace", null, null)));
        assertThrows(ForbiddenException.class, () -> service.update(admin, vol.getId(), req("Vol", null, null)));
        assertThrows(ForbiddenException.class, () -> service.delete(client, incendie.getId()));
        assertThrows(ForbiddenException.class, () -> service.listActive(null));
        assertEquals(2, service.listActive(client).size()); // catalogue lisible par tout utilisateur connecté
    }

    // ---- liste ----

    @Test
    void listForAgentIsSortedByNameWithContractCounts() {
        List<GuaranteeRow> rows = service.listForAgent(agent);
        assertEquals(List.of("Incendie", "Vol"), rows.stream().map(GuaranteeRow::getName).toList());
        assertEquals(0, rows.get(0).getContractCount());
        assertTrue(rows.get(0).isDeletable());
        assertEquals(3, rows.get(1).getContractCount());
        assertFalse(rows.get(1).isDeletable());
        assertEquals(eur("15000"), rows.get(1).getCoverageLimit());
        assertEquals(eur("150"), rows.get(1).getDeductible());
    }

    // ---- création ----

    @Test
    void createSavesTheGuarantee() {
        Guarantee g = service.create(agent, new GuaranteeRequest("  Bris de glace ", "  ", eur("2000"), null));
        assertNotNull(g.getId());
        assertEquals("Bris de glace", g.getName());   // espaces retirés
        assertNull(g.getDescription());               // description vide -> null
        assertEquals(eur("2000"), g.getCoverageLimit());
        assertNull(g.getDeductible());
        assertTrue(g.isActive());
        assertEquals(3, dao.rows.size());
    }

    @Test
    void createWithoutAmountsIsAllowed() {
        Guarantee g = service.create(agent, req("Assistance", null, null));
        assertNull(g.getCoverageLimit());
        assertNull(g.getDeductible());
    }

    @Test
    void createRefusesAnEmptyOrDuplicateName() {
        ValidationException empty = assertThrows(ValidationException.class, () -> service.create(agent, req("  ", null, null)));
        assertTrue(empty.getErrors().containsKey("name"));
        ValidationException dup = assertThrows(ValidationException.class, () -> service.create(agent, req("Vol", null, null)));
        assertEquals("Une garantie porte déjà ce nom.", dup.getErrors().get("name"));
        assertEquals(2, dao.rows.size());
    }

    @Test
    void createRefusesNegativeAmountsAndDeductibleAboveLimit() {
        ValidationException neg = assertThrows(ValidationException.class, () -> service.create(agent, req("A", "-1", "-5")));
        assertTrue(neg.getErrors().containsKey("coverageLimit"));
        assertTrue(neg.getErrors().containsKey("deductible"));
        ValidationException above = assertThrows(ValidationException.class, () -> service.create(agent, req("A", "100", "200")));
        assertEquals("La franchise ne peut pas dépasser le plafond.", above.getErrors().get("deductible"));
    }

    // ---- modification ----

    @Test
    void updateChangesAllFields() {
        service.update(agent, vol.getId(), new GuaranteeRequest("Vol et vandalisme", "Nouveau texte", eur("20000"), eur("200")));
        assertEquals("Vol et vandalisme", vol.getName());
        assertEquals("Nouveau texte", vol.getDescription());
        assertEquals(eur("20000"), vol.getCoverageLimit());
        assertEquals(eur("200"), vol.getDeductible());
    }

    @Test
    void updateKeepingItsOwnNameIsAllowedButNotAnotherOne() {
        assertDoesNotThrow(() -> service.update(agent, vol.getId(), req("Vol", "1000", null)));
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(agent, vol.getId(), req("Incendie", null, null)));
        assertTrue(e.getErrors().containsKey("name"));
        assertEquals("Vol", vol.getName());
    }

    @Test
    void updateOfAnUnknownGuaranteeIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.update(agent, 999L, req("X", null, null)));
        assertThrows(NotFoundException.class, () -> service.get(agent, 999L));
        assertThrows(NotFoundException.class, () -> service.get(agent, null));
    }

    // ---- suppression ----

    @Test
    void deleteRemovesAnUnusedGuarantee() {
        assertEquals("Incendie", service.delete(agent, incendie.getId()));
        assertEquals(List.of(vol), dao.rows);
    }

    @Test
    void deleteRefusesAGuaranteeUsedByContracts() {
        ValidationException e = assertThrows(ValidationException.class, () -> service.delete(agent, vol.getId()));
        assertTrue(e.getErrors().get("id").contains("utilisée par des contrats"));
        assertEquals(2, dao.rows.size());
    }

    @Test
    void deleteOfAnUnknownGuaranteeIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.delete(agent, 999L));
    }

    /** GuaranteeDao en mémoire : ids attribués à l'enregistrement, nombre de contrats fixé par le test. */
    static class FakeGuaranteeDao extends GuaranteeDao {
        final List<Guarantee> rows = new ArrayList<>();
        final Map<Long, Long> contractCounts = new HashMap<>();
        private long nextId = 1;

        @Override public Guarantee save(Guarantee g) {
            setId(g, nextId++);
            rows.add(g);
            return g;
        }
        @Override public Guarantee update(Guarantee g) { return g; }
        @Override public void delete(Guarantee g) { rows.remove(g); }
        @Override public Optional<Guarantee> findById(Long id) {
            return rows.stream().filter(g -> g.getId().equals(id)).findFirst();
        }
        @Override public Optional<Guarantee> findByName(String name) {
            return rows.stream().filter(g -> g.getName().equals(name)).findFirst();
        }
        @Override public List<Guarantee> findAllOrderByName() {
            return rows.stream().sorted(Comparator.comparing(Guarantee::getName)).toList();
        }
        @Override public List<Guarantee> findAllActive() {
            return findAllOrderByName().stream().filter(Guarantee::isActive).toList();
        }
        @Override public boolean isUsedInContract(Long id) { return contractCounts.getOrDefault(id, 0L) > 0; }
        @Override public Map<Long, Long> countContractsByGuarantee() { return Map.copyOf(contractCounts); }

        private static void setId(Guarantee g, long id) {
            try {
                Field f = Guarantee.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(g, id);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
