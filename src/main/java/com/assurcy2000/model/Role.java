package com.assurcy2000.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, unique = true, length = 30)
    private String name;

    protected Role() { }

    public Role(String nom) { this.name = nom; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String nom) { this.name = nom; }

    @Override
    public String toString() { return name; }
}