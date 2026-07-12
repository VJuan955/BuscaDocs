package com.buscadocs.service;

import com.buscadocs.model.SearchResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio encargado de realizar búsquedas sobre los archivos previamente indexados.
 * Permite buscar archivos utilizando una consulta de texto y
 * aplicar filtros por extensión y rango de fechas de modificación.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface SearchService {

    /**
     * Busca archivos indexados cuyo contenido coincida con la consulta
     * especificada y aplica los filtros indicados.
     *
     * @param query texto que se desea buscar.
     * @param extensions lista de extensiones permitidas; si es {@code null} o está vacía, no se aplica este filtro.
     * @param dateFrom fecha mínima de modificación; si es {@code null}, no se aplica límite inferior.
     * @param dateTo fecha máxima de modificación; si es {@code null}, no se aplica límite superior.
     * @param maxResults número máximo de resultados que se devolverán.
     * @return lista de resultados que cumplen los criterios de búsqueda.
     */
    List<SearchResult> search(String query, List<String> extensions,
                                     LocalDateTime dateFrom, LocalDateTime dateTo, int maxResults);

    /**
     * Sugiere nombres de archivo indexados que comienzan con el texto parcial
     * dado, para alimentar el autocompletado del campo de búsqueda mientras
     * el usuario escribe.
     *
     * @param prefix texto parcial escrito por el usuario.
     * @param limit número máximo de sugerencias a devolver.
     * @return lista de nombres de archivo sugeridos.
     */
    List<String> suggestFileNames(String prefix, int limit);
}