package com.assurcy2000.dto;

import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Une carte de la liste des types d'assurance (admin/types.jsp) : le type et le nombre de contrats qui l'utilisent
 * (un type utilisé ne peut pas être supprimé, seulement désactivé).
 * Classe à getters (et pas un record) pour être lue par l'EL des JSP.
 */
public class InsuranceTypeRow {

    private final InsuranceType type;
    private final long contractCount;

    public InsuranceTypeRow(InsuranceType type, long contractCount) {
        this.type = type;
        this.contractCount = contractCount;
    }

    public Long getId() { return type.getId(); }
    public String getName() { return type.getName(); }
    public String getDescription() { return type.getDescription(); }
    public BigDecimal getBasePrice() { return type.getBasePrice(); }
    public boolean isActive() { return type.isActive(); }
    public Set<Guarantee> getGuarantees() { return type.getGuarantees(); }
    public long getContractCount() { return contractCount; }
    public boolean isDeletable() { return contractCount == 0; }
}
