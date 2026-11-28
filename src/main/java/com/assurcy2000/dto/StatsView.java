package com.assurcy2000.dto;

import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ContractStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record StatsView(
        long clientCount,
        long newClientCount,
        Map<String, Long> clientsByType,
        Map<ContractStatus, Long> contractsByStatus,
        Map<ClaimStatus, Long> claimsByStatus,
        BigDecimal activePremiums,
        BigDecimal paymentsReceived,
        BigDecimal indemnitiesPaid,
        BigDecimal averageProcessingDays,
        List<String> months,
        Map<String, BigDecimal> revenueByMonth,
        Map<String, BigDecimal> indemnitiesByMonth,
        Map<String, Long> contractsByMonth) {

    public long getClientCount() { return clientCount; }
    public long getNewClientCount() { return newClientCount; }
    public Map<String, Long> getClientsByType() { return clientsByType; }
    public Map<ContractStatus, Long> getContractsByStatus() { return contractsByStatus; }
    public Map<ClaimStatus, Long> getClaimsByStatus() { return claimsByStatus; }
    public BigDecimal getActivePremiums() { return activePremiums; }
    public BigDecimal getPaymentsReceived() { return paymentsReceived; }
    public BigDecimal getIndemnitiesPaid() { return indemnitiesPaid; }
    /** Délai moyen déclaration → indemnisation, en jours ; null si aucun sinistre indemnisé. */
    public BigDecimal getAverageProcessingDays() { return averageProcessingDays; }
    public List<String> getMonths() { return months; }
    public Map<String, BigDecimal> getRevenueByMonth() { return revenueByMonth; }
    public Map<String, BigDecimal> getIndemnitiesByMonth() { return indemnitiesByMonth; }
    public Map<String, Long> getContractsByMonth() { return contractsByMonth; }
}