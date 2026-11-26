package com.assurcy2000.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "type_assurance")
public class InsuranceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "prix_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "actif", nullable = false)
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @OrderBy("name")
    @JoinTable(name = "type_assurance_garantie",
            joinColumns = @JoinColumn(name = "type_assurance_id"),
            inverseJoinColumns = @JoinColumn(name = "garantie_id"))
    private Set<Guarantee> guarantees = new HashSet<>();

    protected InsuranceType() { }

    public InsuranceType(String name, String description, BigDecimal basePrice) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Set<Guarantee> getGuarantees() { return guarantees; }
    public void setGuarantees(Set<Guarantee> guarantees) { this.guarantees = guarantees; }

    @Override
    public String toString() { return name; }
}
