package com.assurcy2000.model;

import jakarta.persistence.*;

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

    @Column(name = "actif", nullable = false)
    private boolean active = true;

    protected Guarantee() { }

    public Guarantee(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
