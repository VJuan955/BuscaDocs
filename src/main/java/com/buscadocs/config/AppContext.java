package com.buscadocs.config;

import com.buscadocs.dao.FileOpenHistoryDao;
import com.buscadocs.dao.FolderDao;
import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.dao.SearchHistoryDao;
import com.buscadocs.dao.impl.SQLiteFileOpenHistoryDao;
import com.buscadocs.dao.impl.SQLiteFolderDao;
import com.buscadocs.dao.impl.SQLiteIndexedFileDao;
import com.buscadocs.dao.impl.SQLiteSearchHistoryDao;
import com.buscadocs.service.FileActionService;
import com.buscadocs.service.HistoryService;
import com.buscadocs.service.IndexService;
import com.buscadocs.service.LogMonitoringService;
import com.buscadocs.service.MetricService;
import com.buscadocs.service.SearchService;
import com.buscadocs.service.impl.FileActionServiceImpl;
import com.buscadocs.service.impl.HistoryServiceImpl;
import com.buscadocs.service.impl.IndexServiceImpl;
import com.buscadocs.service.impl.LogMonitoringServiceImpl;
import com.buscadocs.service.impl.MetricServiceImpl;
import com.buscadocs.service.impl.SearchServiceImpl;

/**
 * Contenedor de dependencias de la aplicación.
 * <p>
 * Centraliza la creación y provisión de todas las instancias de DAOs y Servicios,
 * implementando un patrón Singleton para garantizar una única instancia de cada
 * componente durante la ejecución. Facilita la inyección manual en los controladores
 * sin necesidad de un framework externo.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class AppContext {

    private final FolderDao folderDao;
    private final IndexedFileDao indexedFileDao;
    private final SearchHistoryDao searchHistoryDao;
    private final FileOpenHistoryDao fileOpenHistoryDao;

    private final IndexService indexService;
    private final SearchService searchService;
    private final MetricService metricService;
    private final LogMonitoringService logMonitoringService;
    private final FileActionService fileActionService;
    private final HistoryService historyService;

    private static volatile AppContext instance;

    /**
     * Constructor privado. Inicializa todas las dependencias en orden:
     * primero los DAOs, luego los servicios que dependen de ellos.
     */
    private AppContext() {

        this.folderDao = new SQLiteFolderDao();
        this.indexedFileDao = new SQLiteIndexedFileDao();
        this.searchHistoryDao = new SQLiteSearchHistoryDao();
        this.fileOpenHistoryDao = new SQLiteFileOpenHistoryDao();

        this.indexService = new IndexServiceImpl(folderDao, indexedFileDao);
        this.searchService = new SearchServiceImpl(indexedFileDao, searchHistoryDao);
        this.metricService = new MetricServiceImpl();
        this.logMonitoringService = new LogMonitoringServiceImpl();
        this.fileActionService = new FileActionServiceImpl(fileOpenHistoryDao);
        this.historyService = new HistoryServiceImpl(searchHistoryDao, fileOpenHistoryDao);
    }

    /**
     * Obtiene la instancia única del contexto de aplicación.
     * Implementa double-checked locking para thread-safety.
     *
     * @return instancia singleton de {@code AppContext}.
     */
    public static AppContext getInstance() {
        if (instance == null) {
            synchronized (AppContext.class) {
                if (instance == null) {
                    instance = new AppContext();
                }
            }
        }
        return instance;
    }

    /** @return instancia única de {@link FolderDao}. */
    public FolderDao getFolderDao() {
        return folderDao;
    }

    /** @return instancia única de {@link IndexedFileDao}. */
    public IndexedFileDao getIndexedFileDao() {
        return indexedFileDao;
    }

    /** @return instancia única de {@link SearchHistoryDao}. */
    public SearchHistoryDao getSearchHistoryDao() {
        return searchHistoryDao;
    }

    /** @return instancia única de {@link FileOpenHistoryDao}. */
    public FileOpenHistoryDao getFileOpenHistoryDao() {
        return fileOpenHistoryDao;
    }

    /** @return instancia única de {@link IndexService}. */
    public IndexService getIndexService() {
        return indexService;
    }

    /** @return instancia única de {@link SearchService}. */
    public SearchService getSearchService() {
        return searchService;
    }

    /** @return instancia única de {@link MetricService}. */
    public MetricService getMetricService() {
        return metricService;
    }

    /** @return instancia única de {@link LogMonitoringService}. */
    public LogMonitoringService getLogMonitoringService() {
        return logMonitoringService;
    }

    /** @return instancia única de {@link FileActionService}. */
    public FileActionService getFileActionService() {
        return fileActionService;
    }

    /** @return instancia única de {@link HistoryService}. */
    public HistoryService getHistoryService() {
        return historyService;
    }
}