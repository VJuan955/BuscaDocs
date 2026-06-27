package com.buscadocs.model;

import java.time.LocalDateTime;

/**
 * Registro de una búsqueda realizada por el usuario.
 *
 * @author VJuan955
 * @version 1.0
 */
public class SearchHistory {
    private int id;
    private String queryText;
    private String extensionFilter;  // JSON o lista separada por comas
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;
    private int resultCount;
    private LocalDateTime searchedAt;

    public SearchHistory() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getExtensionFilter() {
        return extensionFilter;
    }

    public void setExtensionFilter(String extensionFilter) {
        this.extensionFilter = extensionFilter;
    }

    public LocalDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDateTime getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDateTime dateTo) {
        this.dateTo = dateTo;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public LocalDateTime getSearchedAt() {
        return searchedAt;
    }

    public void setSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }
}