package com.buscadocs.model;

import java.time.LocalDateTime;

/**
 * Resultado de búsqueda listo para mostrar en la interfaz.
 * Contiene el snippet y metadatos del archivo.
 *
 * @author VJuan955
 * @version 1.0
 */
public class SearchResult {
    private String fileName;
    private String filePath;
    private String extension;
    private long sizeBytes;
    private LocalDateTime lastModified;
    private String snippet;

    public SearchResult() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}