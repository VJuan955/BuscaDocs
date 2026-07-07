package com.buscadocs.service.impl;

import com.buscadocs.dao.FileOpenHistoryDao;
import com.buscadocs.dao.SearchHistoryDao;
import com.buscadocs.model.FileOpenHistory;
import com.buscadocs.model.SearchHistory;
import com.buscadocs.service.HistoryService;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementación de {@link HistoryService} que delega en {@link SearchHistoryDao}
 * y {@link FileOpenHistoryDao}.
 * <p>
 * Las listas devueltas se envuelven con {@link ImmutableList} (Guava) para que
 * la capa de presentación no pueda mutar accidentalmente el historial recibido;
 * cualquier intento de modificarlas lanza {@link UnsupportedOperationException}
 * en lugar de corromper de forma silenciosa el estado interno del servicio.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class HistoryServiceImpl implements HistoryService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryServiceImpl.class);

    private final SearchHistoryDao searchHistoryDao;
    private final FileOpenHistoryDao fileOpenHistoryDao;

    /**
     * Crea una nueva instancia del servicio de historial.
     *
     * @param searchHistoryDao DAO utilizado para el historial de búsquedas.
     * @param fileOpenHistoryDao DAO utilizado para el historial de archivos abiertos.
     */
    public HistoryServiceImpl(SearchHistoryDao searchHistoryDao, FileOpenHistoryDao fileOpenHistoryDao) {
        this.searchHistoryDao = searchHistoryDao;
        this.fileOpenHistoryDao = fileOpenHistoryDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SearchHistory> getRecentSearches(int limit) {
        return ImmutableList.copyOf(searchHistoryDao.findRecent(limit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileOpenHistory> getRecentFileOpens(int limit) {
        return ImmutableList.copyOf(fileOpenHistoryDao.findRecent(limit));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean clearSearchHistory() {
        boolean cleared = searchHistoryDao.deleteAll();
        if (cleared) {
            logger.info("Historial de búsquedas limpiado por el usuario");
        }
        return cleared;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean clearFileOpenHistory() {
        boolean cleared = fileOpenHistoryDao.deleteAll();
        if (cleared) {
            logger.info("Historial de archivos abiertos limpiado por el usuario");
        }
        return cleared;
    }
}
