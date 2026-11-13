package com.assurcy2000.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {
    public static final String CLIENT = "CLIENT";
    public static final String AGENT = "AGENT";
    public static final String ADMIN = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 30)
    private String name;

    protected Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String nom) { this.name = nom; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return name;
    }
}
