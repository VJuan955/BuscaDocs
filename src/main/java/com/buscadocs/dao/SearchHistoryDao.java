package com.buscadocs.dao;

import com.buscadocs.model.SearchHistory;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para la entidad {@link SearchHistory}.
 * Permite el seguimiento de las consultas del usuario y la depuración del historial.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface SearchHistoryDao {

    /**
     * Inserta un nuevo registro de consulta en el historial.
     *
     * @param entry Objeto con los datos de la búsqueda realizada (sin ID).
     * @return El objeto {@link SearchHistory} con su ID y marca de tiempo asignados.
     */
    SearchHistory insert(SearchHistory entry);

    /**
     * Recupera el historial completo de búsquedas ordenado de forma cronológica descendente.
     *
     * @return Lista con todos los registros del historial.
     */
    List<SearchHistory> findAll();

    /**
     * Obtiene un número limitado de las búsquedas más recientes realizadas por el usuario.
     *
     * @param limit Cantidad máxima de registros a retornar.
     * @return Lista con las últimas búsquedas.
     */
    List<SearchHistory> findRecent(int limit);

    /**
     * Elimina de forma permanente los registros de búsqueda que superen una antigüedad en días.
     * Utiliza modificadores de fecha nativos del motor de persistencia.
     *
     * @param days Número de días de antigüedad límite para conservar.
     * @return {@code true} si la operación de limpieza se ejecutó con éxito.
     */
    boolean deleteOlderThan(long days);
}