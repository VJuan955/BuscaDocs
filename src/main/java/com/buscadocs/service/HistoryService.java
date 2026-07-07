package com.buscadocs.service;

import com.buscadocs.model.FileOpenHistory;
import com.buscadocs.model.SearchHistory;

import java.util.List;

/**
 * Servicio que centraliza el acceso al historial de actividad del usuario:
 * las búsquedas realizadas y los archivos abiertos desde los resultados.
 * <p>
 * Actúa como fachada sobre {@link com.buscadocs.dao.SearchHistoryDao} y
 * {@link com.buscadocs.dao.FileOpenHistoryDao}, exponiendo únicamente las
 * operaciones que necesita la capa de presentación.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public interface HistoryService {

    /**
     * Obtiene las búsquedas más recientes realizadas por el usuario.
     *
     * @param limit cantidad máxima de registros a retornar.
     * @return lista inmutable con las búsquedas más recientes, ordenadas de la más nueva a la más antigua.
     */
    List<SearchHistory> getRecentSearches(int limit);

    /**
     * Obtiene los archivos más recientemente abiertos desde los resultados de búsqueda.
     *
     * @param limit cantidad máxima de registros a retornar.
     * @return lista inmutable con las aperturas más recientes, ordenadas de la más nueva a la más antigua.
     */
    List<FileOpenHistory> getRecentFileOpens(int limit);

    /**
     * Elimina por completo el historial de búsquedas.
     *
     * @return {@code true} si la operación se completó correctamente.
     */
    boolean clearSearchHistory();

    /**
     * Elimina por completo el historial de archivos abiertos.
     *
     * @return {@code true} si la operación se completó correctamente.
     */
    boolean clearFileOpenHistory();
}
