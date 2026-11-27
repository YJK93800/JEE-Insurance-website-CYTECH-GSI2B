package com.assurcy2000.dto;

import com.assurcy2000.model.Claim;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Payment;

import java.math.BigDecimal;
import java.util.List;

public record ClientDashboard(List<Contract> recentContracts,
                              int contractCount, int activeContractCount, int pendingContractCount,
                              BigDecimal totalPremiums,
                              int openClaimCount, Claim lastClaim,
                              Payment nextPayment, List<Payment> recentPayments) {
}
