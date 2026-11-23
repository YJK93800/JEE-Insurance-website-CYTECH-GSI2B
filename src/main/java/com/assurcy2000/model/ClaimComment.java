package com.assurcy2000.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commentaire_sinistre")
public class ClaimComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contenu", nullable = false)
    private String content;

    @Column(name = "date_creation", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinistre_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User author;

    protected ClaimComment() { }

    public ClaimComment(String content, Claim claim, User author) {
        this.content = content;
        this.claim = claim;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Claim getClaim() { return claim; }
    public User getAuthor() { return author; }
}
