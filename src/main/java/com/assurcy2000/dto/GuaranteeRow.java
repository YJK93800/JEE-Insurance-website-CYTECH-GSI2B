package com.assurcy2000.dto;

import com.assurcy2000.model.Guarantee;

import java.math.BigDecimal;

/**
 * Une ligne de la liste des garanties (agent/guarantees.jsp) : la garantie et le nombre de contrats
 * qui l'utilisent (une garantie utilisée ne peut pas être supprimée).
 * Classe à getters (et pas un record) pour être lue par l'EL des JSP.
 */
public class GuaranteeRow {

    private final Guarantee guarantee;
    private final long contractCount;

    public GuaranteeRow(Guarantee guarantee, long contractCount) {
        this.guarantee = guarantee;
        this.contractCount = contractCount;
    }

    public Long getId() { return guarantee.getId(); }
    public String getName() { return guarantee.getName(); }
    public String getDescription() { return guarantee.getDescription(); }
    public BigDecimal getCoverageLimit() { return guarantee.getCoverageLimit(); }
    public BigDecimal getDeductible() { return guarantee.getDeductible(); }
    public boolean isActive() { return guarantee.isActive(); }
    public long getContractCount() { return contractCount; }
    public boolean isDeletable() { return contractCount == 0; }
}
