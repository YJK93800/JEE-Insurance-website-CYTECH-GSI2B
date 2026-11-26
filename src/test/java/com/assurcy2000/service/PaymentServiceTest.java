package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.PaymentFilter;
import com.assurcy2000.dto.PaymentSummary;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.model.enums.PaymentMethod;
import com.assurcy2000.model.enums.PaymentStatus;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/** PaymentService sans base de données. « Aujourd'hui » est fixé au 15/06/2026. */
class PaymentServiceTest {

    static final TransactionRunner DIRECT = new TransactionRunner() {
        @Override public <R> R inTransaction(Supplier<R> work) { return work.get(); }
    };
    static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final FakePaymentDao payments = new FakePaymentDao();
    private PaymentService service;
    private SessionUser agent, admin, aliceSession, bobSession;
    private Contract aliceContract, bobContract;
    private User agentUser;

    @BeforeEach
    void setUp() throws Exception {
        User alice = withId(new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT)), 1L);
        User bob = withId(new User("Bob", "Durand", "bob@x.fr", "h", new Role(Role.CLIENT)), 3L);
        agentUser = withId(new User("Paul", "Agent", "agent@x.fr", "h", new Role(Role.AGENT)), 2L);
        User adminUser = withId(new User("Ada", "Admin", "admin@x.fr", "h", new Role(Role.ADMIN)), 4L);
        InsuranceType habitation = withId(new InsuranceType("Assurance habitation", "", new BigDecimal("20.00")), 5L);

        // contrats de l'année 2026, prime de 20 € par mois : 12 échéances chacun
        aliceContract = withId(new Contract("CTR-2026-0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("20.00"), alice, habitation), 10L);
        bobContract = withId(new Contract("CTR-2026-0002", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                new BigDecimal("20.00"), bob, habitation), 20L);
        PaymentSchedule schedule = new PaymentSchedule(payments);
        schedule.generate(aliceContract);
        schedule.generate(bobContract);

        Map<Long, Contract> contracts = Map.of(10L, aliceContract, 20L, bobContract);
        ContractDao contractDao = new ContractDao() {
            @Override public Optional<Contract> findById(Long id) { return Optional.ofNullable(contracts.get(id)); }
        };
        UserDao userDao = new UserDao() {
            @Override public User getReference(Long id) { return agentUser; }
        };
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        service = new PaymentService(payments, new ContractService(contractDao, DIRECT), userDao, DIRECT, clock);

        agent = SessionUser.from(agentUser);
        admin = SessionUser.from(adminUser);
        aliceSession = SessionUser.from(alice);
        bobSession = SessionUser.from(bob);
    }

    private Payment aliceDue(int month) {
        return payments.of(10L).get(month - 1);
    }

    // ---- consultation ----

    @Test
    void clientSeesOnlyHisPaymentsAgentAndAdminSeeAll() {
        assertEquals(12, service.visible(aliceSession, null).size());
        assertTrue(service.visible(aliceSession, null).stream().allMatch(p -> p.getContract() == aliceContract));
        assertEquals(24, service.visible(agent, null).size());
        assertEquals(24, service.visible(admin, null).size());
    }

    @Test
    void clientCannotListThePaymentsOfAnotherClientsContract() {
        assertThrows(NotFoundException.class, () -> service.visible(aliceSession, 20L));
        assertEquals(12, service.visible(bobSession, 20L).size());
    }

    @Test
    void tabsSplitPaidPendingAndUpcoming() {
        aliceDue(1).setStatus(PaymentStatus.EFFECTUE);
        aliceDue(1).setPaymentDate(LocalDate.of(2026, 1, 3));
        aliceDue(2).setStatus(PaymentStatus.EN_RETARD);
        List<Payment> all = service.visible(aliceSession, null);

        // payé : janvier ; en attente (dû au 15/06, non payé) : février à juin ; à venir : juillet à décembre
        assertEquals(List.of(aliceDue(1)), service.filter(all, PaymentFilter.PAID, null));
        List<Payment> pending = service.filter(all, PaymentFilter.PENDING, null);
        assertEquals(5, pending.size());
        assertEquals(aliceDue(2), pending.get(0)); // le plus ancien d'abord
        List<Payment> upcoming = service.filter(all, PaymentFilter.UPCOMING, null);
        assertEquals(6, upcoming.size());
        assertEquals(LocalDate.of(2026, 7, 1), upcoming.get(0).getDueDate());
        // historique : tout sauf les échéances futures, le plus récent d'abord
        List<Payment> history = service.filter(all, PaymentFilter.HISTORY, null);
        assertEquals(6, history.size());
        assertEquals(LocalDate.of(2026, 6, 1), history.get(0).getDueDate());
    }

    @Test
    void agentSearchesByContractNumberOrClientName() {
        List<Payment> all = service.visible(agent, null);
        assertEquals(12, service.filter(all, PaymentFilter.UPCOMING, "").size());
        assertEquals(6, service.filter(all, PaymentFilter.UPCOMING, "ctr-2026-0002").size());
        assertEquals(6, service.filter(all, PaymentFilter.UPCOMING, "  martin ").size());
        assertEquals(6, service.filter(all, PaymentFilter.UPCOMING, "alice martin").size());
        assertEquals(6, service.filter(all, PaymentFilter.UPCOMING, "bob@x").size());
        assertTrue(service.filter(all, PaymentFilter.UPCOMING, "inconnu").isEmpty());
    }

    @Test
    void summaryCountsPaidDueLateAndNextDueDate() {
        aliceDue(1).setStatus(PaymentStatus.EFFECTUE);
        aliceDue(2).setStatus(PaymentStatus.EN_RETARD);
        aliceDue(12).setStatus(PaymentStatus.ANNULE);
        PaymentSummary s = service.summary(service.visible(aliceSession, null));
        assertEquals(new BigDecimal("20.00"), s.totalPaid());
        assertEquals(new BigDecimal("100.00"), s.totalDue()); // février à juin
        assertEquals(1, s.lateCount());
        assertEquals(LocalDate.of(2026, 7, 1), s.nextDue().getDueDate());
    }

    // ---- enregistrement d'un paiement ----

    @Test
    void agentRecordsAPayment() {
        Payment p = service.markPaid(agent, aliceDue(2).getId(), PaymentMethod.VIREMENT, LocalDate.of(2026, 6, 10));
        assertEquals(PaymentStatus.EFFECTUE, p.getStatus());
        assertEquals(LocalDate.of(2026, 6, 10), p.getPaymentDate());
        assertEquals(PaymentMethod.VIREMENT, p.getMethod());
        assertSame(agentUser, p.getRecordedBy());
    }

    @Test
    void lateDueDateCanBePaidAndFutureOneInAdvance() {
        aliceDue(2).setStatus(PaymentStatus.EN_RETARD);
        assertEquals(PaymentStatus.EFFECTUE, service.markPaid(agent, aliceDue(2).getId(), PaymentMethod.CARTE, TODAY).getStatus());
        assertEquals(PaymentStatus.EFFECTUE, service.markPaid(agent, aliceDue(9).getId(), PaymentMethod.CARTE, TODAY).getStatus());
    }

    @Test
    void paidOrCancelledDueDateCannotBePaidAgain() {
        service.markPaid(agent, aliceDue(1).getId(), PaymentMethod.CHEQUE, TODAY);
        ValidationException paid = assertThrows(ValidationException.class,
                () -> service.markPaid(agent, aliceDue(1).getId(), PaymentMethod.CHEQUE, TODAY));
        assertTrue(paid.getErrors().get("paymentId").contains("déjà payée"));

        aliceDue(12).setStatus(PaymentStatus.ANNULE);
        assertThrows(ValidationException.class, () -> service.markPaid(agent, aliceDue(12).getId(), PaymentMethod.CHEQUE, TODAY));
    }

    @Test
    void paymentDateCannotBeInTheFuture() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.markPaid(agent, aliceDue(2).getId(), PaymentMethod.CARTE, TODAY.plusDays(1)));
        assertTrue(e.getErrors().containsKey("paymentDate"));
        assertEquals(PaymentStatus.EN_ATTENTE, aliceDue(2).getStatus());
    }

    @Test
    void unknownDueDateIsAFormError() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.markPaid(agent, 999L, PaymentMethod.CARTE, TODAY));
        assertTrue(e.getErrors().containsKey("paymentId"));
    }

    @Test
    void onlyAnAgentCanRecordAPayment() {
        assertThrows(ForbiddenException.class, () -> service.markPaid(aliceSession, aliceDue(2).getId(), PaymentMethod.CARTE, TODAY));
        assertThrows(ForbiddenException.class, () -> service.markPaid(admin, aliceDue(2).getId(), PaymentMethod.CARTE, TODAY));
        assertThrows(ForbiddenException.class, () -> service.payable(admin));
    }

    @Test
    void payableListsUnpaidDueDatesUntilNextMonth() {
        aliceDue(1).setStatus(PaymentStatus.EFFECTUE);
        // jusqu'au 15/07/2026 : février à juillet, pour les deux contrats
        List<Payment> payable = service.payable(agent);
        assertEquals(6 + 7, payable.size());
        assertTrue(payable.stream().noneMatch(p -> p.getDueDate().isAfter(TODAY.plusMonths(1))));
    }

    // ---- retards ----

    @Test
    void overdueUsesToday() {
        assertEquals(2 * 6, service.markOverdue()); // janvier à juin dépassés au 15/06 (le 01/06 < 15/06)
        assertEquals(PaymentStatus.EN_RETARD, aliceDue(6).getStatus());
        assertEquals(PaymentStatus.EN_ATTENTE, aliceDue(7).getStatus());
    }

    private static <T> T withId(T entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
        return entity;
    }
}
