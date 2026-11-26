package com.assurcy2000.service;

import com.assurcy2000.model.Contract;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Calcul des dates d'échéance (prime mensuelle). */
class PaymentScheduleTest {

    @Test
    void fullYearGivesTwelveMonthlyDueDates() {
        List<LocalDate> dates = PaymentSchedule.dueDates(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        assertEquals(12, dates.size());
        assertEquals(LocalDate.of(2026, 1, 1), dates.get(0));
        assertEquals(LocalDate.of(2026, 12, 1), dates.get(11));
    }

    @Test
    void renewalPeriodStartingMidMonthKeepsTheSameDay() {
        List<LocalDate> dates = PaymentSchedule.dueDates(LocalDate.of(2026, 3, 15), LocalDate.of(2027, 3, 14));
        assertEquals(12, dates.size());
        assertEquals(LocalDate.of(2027, 2, 15), dates.get(11));
    }

    @Test
    void contractStartingOnThe31stFallsBackToTheLastDayOfShortMonths() {
        List<LocalDate> dates = PaymentSchedule.dueDates(LocalDate.of(2026, 1, 31), LocalDate.of(2027, 1, 30));
        assertEquals(12, dates.size());
        assertEquals(LocalDate.of(2026, 2, 28), dates.get(1));
        assertEquals(LocalDate.of(2026, 3, 31), dates.get(2)); // repart du 31, pas du 28
        assertEquals(LocalDate.of(2026, 4, 30), dates.get(3));
    }

    @Test
    void oneDayContractHasOneDueDate() {
        LocalDate d = LocalDate.of(2026, 5, 10);
        assertEquals(List.of(d), PaymentSchedule.dueDates(d, d));
    }

    @Test
    void freeContractHasNoDueDate() {
        FakePaymentDao payments = new FakePaymentDao();
        User alice = new User("Alice", "Martin", "alice@x.fr", "h", new Role(Role.CLIENT));
        InsuranceType type = new InsuranceType("Assurance habitation", "", new BigDecimal("220.00"));
        Contract c = new Contract("CTR-2026-0001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                BigDecimal.ZERO, alice, type);

        new PaymentSchedule(payments).generate(c);

        assertTrue(payments.rows.isEmpty()); // contrainte chk_paiement_montant : montant > 0
    }
}
