package com.assurcy2000.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "garantie")
public class Guarantee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    /** Montant maximal remboursé par sinistre ; null = pas de plafond. */
    @Column(name = "plafond", precision = 12, scale = 2)
    private BigDecimal coverageLimit;

    /** Somme restant à la charge du client par sinistre ; null = pas de franchise. */
    @Column(name = "franchise", precision = 12, scale = 2)
    private BigDecimal deductible;

    @Column(name = "actif", nullable = false)
    private boolean active = true;

    protected Guarantee() { }

    public Guarantee(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Guarantee(String name, String description, BigDecimal coverageLimit, BigDecimal deductible) {
        this(name, description);
        this.coverageLimit = coverageLimit;
        this.deductible = deductible;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getCoverageLimit() { return coverageLimit; }
    public void setCoverageLimit(BigDecimal coverageLimit) { this.coverageLimit = coverageLimit; }

    public BigDecimal getDeductible() { return deductible; }
    public void setDeductible(BigDecimal deductible) { this.deductible = deductible; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guarantee other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return Guarantee.class.hashCode(); }

    @Override
    public String toString() { return name; }
}
