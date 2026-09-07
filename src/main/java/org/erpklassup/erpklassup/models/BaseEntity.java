package org.erpklassup.erpklassup.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe abstraite de base pour toutes les entités synchronisables.
 * Attributs : id, version, deleted_at, created_at, updated_at
 */
public abstract class BaseEntity {

    protected String id;
    protected Long version;
    protected LocalDateTime deletedAt;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public BaseEntity() {
        this.id = UUID.randomUUID().toString();
        this.version = 1L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ========== GETTERS & SETTERS ==========
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public void incrementerVersion() { this.version++; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public boolean isDeleted() { return deletedAt != null; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}