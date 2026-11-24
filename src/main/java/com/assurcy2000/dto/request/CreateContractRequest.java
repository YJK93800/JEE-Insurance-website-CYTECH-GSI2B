package com.assurcy2000.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Formulaire de contrat (agent/contract-form.jsp), déjà vérifié par la servlet.
 * Sert à la création (ContractService.create) et à la modification (ContractService.update).
 * Le numéro et le statut (EN_ATTENTE) sont décidés par le serveur, pas par le formulaire.
 *
 * @param premium prime saisie ; null = prix de base du type d'assurance
 */
public record CreateContractRequest(
        Long clientId,
        Long insuranceTypeId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal premium,
        Set<Long> guaranteeIds) {
}
