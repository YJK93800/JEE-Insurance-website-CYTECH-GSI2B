package com.assurcy2000.dto;

import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ContractStatus;

import java.math.BigDecimal;
import java.util.Map;

public record StatsView(
        long clientCount,
        Map<ContractStatus, Long> contractsByStatus,
        Map<ClaimStatus, Long> claimsByStatus,
        BigDecimal activePremiums,
        BigDecimal paymentsReceived,
        BigDecimal indemnitiesPaid,
        Map<String, BigDecimal> revenueByMonth) {
}
