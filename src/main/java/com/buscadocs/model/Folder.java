package com.buscadocs.model;

import java.time.LocalDateTime;

/**
 * Representa una carpeta a indexar.
 * Contiene la ruta, estado de indexación, fechas y política de archivos ocultos.
 *
 * @author VJuan955
 * @version 1.0
 */
public class Folder {
    private int id;
    private String path;
    private String status;
    private LocalDateTime lastIndexed;
    private boolean includeHidden;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Folder() {
    }

    public Folder(int id, String path, String status, LocalDateTime lastIndexed,
                  boolean includeHidden, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.path = path;
        this.status = status;
        this.lastIndexed = lastIndexed;
        this.includeHidden = includeHidden;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastIndexed() {
        return lastIndexed;
    }

    public void setLastIndexed(LocalDateTime lastIndexed) {
        this.lastIndexed = lastIndexed;
    }

    public boolean isIncludeHidden() {
        return includeHidden;
    }

    public void setIncludeHidden(boolean includeHidden) {
        this.includeHidden = includeHidden;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Folder{id=" + id + ", path='" + path + "', status='" + status + "'}";
    }
}