package com.assurcy2000.dto.request;

import java.math.BigDecimal;

/**
 * Saisie du formulaire de garantie, déjà nettoyée par la servlet (Validation).
 * coverageLimit (plafond) et deductible (franchise) : null si laissés vides.
 */
public record GuaranteeRequest(String name, String description, BigDecimal coverageLimit, BigDecimal deductible) { }
