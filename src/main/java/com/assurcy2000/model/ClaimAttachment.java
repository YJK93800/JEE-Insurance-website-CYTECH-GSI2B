package com.assurcy2000.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "piece_justificative")
public class ClaimAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_fichier", nullable = false)
    private String fileName;

    @Column(name = "chemin", nullable = false, length = 500)
    private String path;                 // emplacement du fichier sur le serveur

    @Column(name = "type_mime", length = 100)
    private String mimeType;

    @Column(name = "taille_octets")
    private Long sizeBytes;

    @Column(name = "date_ajout", insertable = false, updatable = false)
    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinistre_id", nullable = false)
    private Claim claim;

    protected ClaimAttachment() { }

    public ClaimAttachment(String fileName, String path, String mimeType, Long sizeBytes, Claim claim) {
        this.fileName = fileName;
        this.path = path;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.claim = claim;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getPath() { return path; }
    public String getMimeType() { return mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public Claim getClaim() { return claim; }
}
