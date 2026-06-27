package com.buscadocs.model;

import java.time.LocalDateTime;

/**
 * Registro de apertura de un archivo desde los resultados de búsqueda.
 *
 * @author VJuan955
 * @version 1.0
 */
public class FileOpenHistory {
    private int id;
    private String filePath;
    private LocalDateTime openedAt;

    public FileOpenHistory() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(LocalDateTime openedAt) {
        this.openedAt = openedAt;
    }
}