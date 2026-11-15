package com.assurcy2000.dto;


public record DeclareClaimRequest(
        String contractId,
        String claimDate,
        String type,
        String description,
        String estimatedAmount) {
}
