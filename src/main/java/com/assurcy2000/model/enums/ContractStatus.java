package com.assurcy2000.model.enums;

public enum ContractStatus {
    EN_ATTENTE("En attente"),
    ACTIF("Actif"),
    SUSPENDU("Suspendu"),
    EXPIRE("Expiré"),
    RESILIE("Résilié");

    /** Libellé affiché à l'utilisateur ; name() reste la valeur envoyée dans les formulaires et stockée en base. */
    private final String label;

    ContractStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
