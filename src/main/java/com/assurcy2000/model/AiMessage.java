package com.assurcy2000.model;

import com.assurcy2000.model.enums.MessageRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Un message de la conversation entre un client et l'assistant IA (table message_ia). */
@Entity
@Table(name = "message_ia")
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_message", nullable = false, length = 10)
    private MessageRole role;               // USER ou ASSISTANT

    @Column(name = "contenu", nullable = false)
    private String content;

    @Column(name = "date_message", insertable = false, updatable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    protected AiMessage() { }

    public AiMessage(User client, MessageRole role, String content) {
        this.client = client;
        this.role = role;
        this.content = content;
    }

    public Long getId() { return id; }
    public MessageRole getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public User getClient() { return client; }
}
