package com.buscadocs.service.impl;

import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.dao.SearchHistoryDao;
import com.buscadocs.model.IndexedFile;
import com.buscadocs.model.SearchHistory;
import com.buscadocs.model.SearchResult;
import com.buscadocs.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link SearchService} que realiza búsquedas sobre los
 * archivos previamente indexados.
 *
 * @author VJuan955
 * @version 1.0
 */
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
    private final IndexedFileDao indexedFileDao;
    private final SearchHistoryDao searchHistoryDao;

    /**
     * Crea una nueva instancia del servicio de búsqueda.
     *
     * @param indexedFileDao DAO utilizado para acceder a los archivos indexados.
     * @param searchHistoryDao DAO utilizado para registrar el historial de búsquedas realizadas.
     */
    public SearchServiceImpl(IndexedFileDao indexedFileDao, SearchHistoryDao searchHistoryDao) {
        this.indexedFileDao = indexedFileDao;
        this.searchHistoryDao = searchHistoryDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SearchResult> search(String query, List<String> extensions,
                                     LocalDateTime dateFrom, LocalDateTime dateTo, int maxResults) {
        logger.info("Búsqueda: query='{}', extensions={}, fechas=[{} - {}]", query, extensions, dateFrom, dateTo);

        List<IndexedFile> results = indexedFileDao.searchFullText(query, maxResults * 2);

        var filtered = results.stream()
                .filter(f -> {
                    if (extensions != null && !extensions.isEmpty() && !extensions.contains(f.getExtension()))
                        return false;
                    if (dateFrom != null && f.getLastModified() != null && f.getLastModified().isBefore(dateFrom))
                        return false;
                    if (dateTo != null && f.getLastModified() != null && f.getLastModified().isAfter(dateTo))
                        return false;
                    return true;
                })
                .limit(maxResults)
                .map(this::toSearchResult)
                .collect(Collectors.toList());

        SearchHistory history = new SearchHistory();
        history.setQueryText(query);
        history.setExtensionFilter(extensions != null ? String.join(",", extensions) : null);
        history.setDateFrom(dateFrom);
        history.setDateTo(dateTo);
        history.setResultCount(filtered.size());
        searchHistoryDao.insert(history);

        return filtered;
    }

    /**
     * Convierte un archivo indexado en un objeto de resultado de búsqueda.
     *
     * @param file archivo indexado que se convertirá.
     * @return un objeto {@link SearchResult} con la información relevante para mostrar al usuario.
     */
    private SearchResult toSearchResult(IndexedFile file) {
        SearchResult sr = new SearchResult();
        sr.setFileName(file.getFileName());
        sr.setFilePath(file.getFilePath());
        sr.setExtension(file.getExtension());
        sr.setSizeBytes(file.getSizeBytes());
        sr.setLastModified(file.getLastModified());
        sr.setSnippet(file.getContentSnippet());
        return sr;
    }
}