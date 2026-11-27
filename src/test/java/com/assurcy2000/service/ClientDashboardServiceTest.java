package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dto.ClientDashboard;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.*;
import com.assurcy2000.model.enums.*;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** ClientDashboardService sans base de données. « Aujourd'hui » : 15/06/2026. */
class ClientDashboardServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };
    static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final FakePaymentDao payments = new FakePaymentDao();
    private User alice;
    private Contract active, pending, expired;
    private List<Claim> claims;
    private ClientDashboardService service;
    private SessionUser aliceSession;

    @BeforeEach
    void setUp() throws Exception {
        alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        InsuranceType type = withId(new InsuranceType("Assurance habitation", "", new BigDecimal("20.00")), 5L);
        active = withId(new Contract("C1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), new BigDecimal("20.00"), alice, type), 10L);
        active.setStatus(ContractStatus.ACTIF);
        pending = withId(new Contract("C2", LocalDate.of(2026, 6, 1), LocalDate.of(2027, 5, 31), new BigDecimal("30.00"), alice, type), 11L);
        expired = withId(new Contract("C3", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), new BigDecimal("15.00"), alice, type), 12L);
        expired.setStatus(ContractStatus.EXPIRE);

        Claim open = new Claim("S1", LocalDate.of(2026, 3, 1), "x", ClaimType.VOL, new BigDecimal("100"), active);
        Claim closed = new Claim("S2", LocalDate.of(2026, 2, 1), "x", ClaimType.VOL, new BigDecimal("100"), active);
        closed.setStatus(ClaimStatus.INDEMNISE);
        setField(open, "declaredAt", java.time.LocalDateTime.of(2026, 3, 1, 10, 0));
        setField(closed, "declaredAt", java.time.LocalDateTime.of(2026, 2, 1, 10, 0));
        claims = List.of(closed, open);

        new PaymentSchedule(payments).generate(active); // 12 échéances de 20 € en 2026

        ContractDao contractDao = new ContractDao() {
            @Override public List<Contract> findByClientWithGuarantees(Long id, Collection<ContractStatus> s) {
                return List.of(pending, active, expired);
            }
        };
        ClaimDao claimDao = new ClaimDao() {
            @Override public List<Claim> findByClient(Long id) { return claims; }
        };
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new ClientDashboardService(contractDao, claimDao, payments, DIRECT, clock);
        aliceSession = SessionUser.from(alice);
    }

    @Test
    void countsContractsAndSumsOnlyActivePremiums() {
        ClientDashboard d = service.forClient(aliceSession);
        assertEquals(3, d.contractCount());
        assertEquals(1, d.activeContractCount());
        assertEquals(1, d.pendingContractCount());
        assertEquals(0, new BigDecimal("20.00").compareTo(d.totalPremiums()));
        assertEquals(List.of(pending, active, expired), d.recentContracts());
    }

    @Test
    void countsOnlyOpenClaimsAndFindsTheLatest() {
        ClientDashboard d = service.forClient(aliceSession);
        assertEquals(1, d.openClaimCount());
        assertEquals("S1", d.lastClaim().getNumber());
    }

    @Test
    void nextPaymentIsTheOldestUnpaidAndRecentPaymentsAreAlreadyDue() {
        payments.rows.get(0).setStatus(PaymentStatus.EFFECTUE); // janvier payé
        ClientDashboard d = service.forClient(aliceSession);
        assertEquals(LocalDate.of(2026, 2, 1), d.nextPayment().getDueDate());
        assertEquals(5, d.recentPayments().size());
        assertEquals(LocalDate.of(2026, 6, 1), d.recentPayments().get(0).getDueDate()); // pas d'échéance future
    }

    @Test
    void noDataGivesEmptyDashboard() {
        payments.rows.clear();
        claims = List.of();
        ClientDashboard d = service.forClient(aliceSession);
        assertNull(d.lastClaim());
        assertNull(d.nextPayment());
        assertTrue(d.recentPayments().isEmpty());
        assertEquals(0, d.openClaimCount());
    }

    @Test
    void onlyClientsHaveADashboard() {
        User agent = new User("Paul", "Agent", "a@x.fr", "h", new Role(Role.AGENT));
        assertThrows(ForbiddenException.class, () -> service.forClient(SessionUser.from(agent)));
    }

    private static <T> T withId(T o, Long id) throws Exception { setField(o, "id", id); return o; }

    private static void setField(Object o, String name, Object v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, v);
    }
}
