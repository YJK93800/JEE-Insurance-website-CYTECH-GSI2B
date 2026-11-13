package com.assurcy2000.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "utilisateur")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prenom", nullable = false, length = 60)
    private String firstName;

    @Column(name = "nom", nullable = false, length = 60)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "telephone", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate birthDate;

    @Column(name = "adresse")
    private String address;

    @Column(name = "mot_de_passe", nullable = false, length = 60) // hash BCrypt = 60 caractères
    private String password;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    protected User() {

    }

    public User(String firstName, String name, String email, String phoneNumber,
                LocalDate birthDate, String address, String password) {
        this.firstName = firstName;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.address = address;
        this.password = password;
    }

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (email != null) email = email.trim().toLowerCase();
    }

    public Long getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPassword() { return password; }
    public void setPassword(String passwordHash) { this.password = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    @Override
    public String toString() {
        return "User{id=" + id + ", email='" + email + "', role=" + role + "}"; // pas le mot de passe
    }
}
