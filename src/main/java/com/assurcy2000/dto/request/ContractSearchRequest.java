package com.assurcy2000.dto.request;

import com.assurcy2000.model.enums.ContractStatus;

/**
 * Filtres de la recherche de contrats (agent/contrats.jsp).
 * Chaque critère vaut null quand il n'est pas utilisé : la recherche vide renvoie tous les contrats.
 *
 * @param keyword         numéro de contrat, nom, prénom ou email du client (partiel, sans casse)
 * @param status          statut exact
 * @param insuranceTypeId id du type d'assurance
 */
public record ContractSearchRequest(String keyword, ContractStatus status, Long insuranceTypeId) {

    public static ContractSearchRequest all() {
        return new ContractSearchRequest(null, null, null);
    }
}
