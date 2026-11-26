package com.assurcy2000.dto.request;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Saisie du formulaire de type d'assurance, déjà nettoyée par la servlet (Validation).
 * guaranteeIds : garanties cochées (proposées à la souscription d'un contrat de ce type).
 */
public record InsuranceTypeRequest(String name, String description, BigDecimal basePrice, Set<Long> guaranteeIds) {
    public InsuranceTypeRequest {
        guaranteeIds = guaranteeIds == null ? Set.of() : Set.copyOf(guaranteeIds);
    }
}
