package com.sebbcontt.subscriptions.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "imports")
public class ImportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ImportEntity() {
    }

    public ImportEntity(String filename) {
        this.filename = filename;
    }

    public Long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
