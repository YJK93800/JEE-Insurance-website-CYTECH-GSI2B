package com.assurcy2000.model.enums;

public enum PaymentStatus {
    EN_ATTENTE("En attente"),
    EFFECTUE("Effectué"),
    EN_RETARD("En retard"),
    ANNULE("Annulé");

    /** Libellé affiché ; name() reste la valeur des formulaires et de la base. */
    private final String label;

    PaymentStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
