package com.kraft.saved;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "saved_numbers")
public class SavedNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_token_hash", nullable = false, length = 64)
    private String clientTokenHash;

    @Column(name = "numbers", nullable = false, length = 32)
    private String numbers;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SavedNumber() {
    }

    public SavedNumber(String clientTokenHash, String numbers, String label, String source, OffsetDateTime createdAt) {
        this.clientTokenHash = clientTokenHash;
        this.numbers = numbers;
        this.label = label;
        this.source = source;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getClientTokenHash() {
        return clientTokenHash;
    }

    public String getNumbers() {
        return numbers;
    }

    public String getLabel() {
        return label;
    }

    public String getSource() {
        return source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
