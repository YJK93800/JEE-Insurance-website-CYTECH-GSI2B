package com.assurcy2000.model.enums;

public enum PaymentMethod {
    CARTE("Carte bancaire"),
    VIREMENT("Virement"),
    PRELEVEMENT("Prélèvement"),
    CHEQUE("Chèque"),
    ESPECES("Espèces");

    /** Libellé affiché ; name() reste la valeur des formulaires et de la base. */
    private final String label;

    PaymentMethod(String label) { this.label = label; }

    public String getLabel() { return label; }
}
