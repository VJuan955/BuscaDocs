package com.buscadocs.dao;

import com.buscadocs.model.FileOpenHistory;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para la entidad {@link FileOpenHistory}.
 * Permite auditar y rastrear las acciones del usuario cuando decide abrir o ejecutar
 * un archivo desde el panel de resultados de búsqueda.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface FileOpenHistoryDao {

    /**
     * Inserta un nuevo registro de apertura de archivo en la base de datos.
     *
     * @param entry Objeto con la información del archivo abierto (sin ID).
     * @return El objeto {@link FileOpenHistory} con su ID generado y marca de tiempo asignada.
     */
    FileOpenHistory insert(FileOpenHistory entry);

    /**
     * Recupera la totalidad de los registros de aperturas guardados en el sistema,
     * ordenados cronológicamente desde el más reciente.
     *
     * @return Lista exhaustiva del historial de archivos abiertos.
     */
    List<FileOpenHistory> findAll();

    /**
     * Obtiene una sublista limitada con los últimos archivos abiertos por el usuario.
     * Útil para implementar secciones de "Archivos recientes" en la interfaz gráfica.
     *
     * @param limit Capacidad máxima de registros que poblarán la lista.
     * @return Lista con los registros de apertura más recientes.
     */
    List<FileOpenHistory> findRecent(int limit);
}