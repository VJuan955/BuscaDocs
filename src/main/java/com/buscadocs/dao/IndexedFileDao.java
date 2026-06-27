package com.buscadocs.dao;

import com.buscadocs.model.IndexedFile;
import java.util.Optional;
import java.util.List;

/**
 * Define las operaciones de acceso a datos para la entidad IndexedFile.
 * Incluye búsqueda por carpeta, eliminación en lote y búsqueda de texto completo (FTS).
 *
 * @author VJuan955
 * @version 1.0
 */
public interface IndexedFileDao {

    /**
     * Inserta un nuevo archivo indexado.
     *
     * @param file Archivo con los datos a insertar (sin ID).
     * @return El archivo con el ID autogenerado.
     */
    IndexedFile insert(IndexedFile file);

    /**
     * Actualiza los datos de un archivo existente.
     *
     * @param file Archivo con los nuevos datos (debe incluir ID).
     * @return El archivo actualizado.
     */
    IndexedFile update(IndexedFile file);

    /**
     * Elimina un archivo indexado por su ID.
     *
     * @param id Identificador del archivo.
     * @return {@code true} si se eliminó correctamente.
     */
    boolean delete(int id);

    /**
     * Elimina todos los archivos asociados a una carpeta.
     * Útil antes de reindexar una carpeta completa.
     *
     * @param folderId ID de la carpeta.
     * @return Número de archivos eliminados.
     */
    int deleteByFolder(int folderId);

    /**
     * Busca un archivo indexado por su ID.
     *
     * @param id Identificador del archivo.
     * @return Optional con el archivo si existe.
     */
    Optional<IndexedFile> findById(int id);

    /**
     * Obtiene todos los archivos indexados.
     *
     * @return Lista completa de archivos.
     */
    List<IndexedFile> findAll();

    /**
     * Obtiene los archivos pertenecientes a una carpeta específica.
     *
     * @param folderId ID de la carpeta.
     * @return Lista de archivos de esa carpeta.
     */
    List<IndexedFile> findByFolder(int folderId);

    /**
     * Realiza una búsqueda de texto completo utilizando la tabla virtual FTS5.
     * La consulta se formatea internamente para unir términos con AND.
     *
     * @param query Texto de búsqueda ingresado por el usuario.
     * @param limit Número máximo de resultados.
     * @return Lista de archivos que coinciden, ordenados por relevancia.
     */
    List<IndexedFile> searchFullText(String query, int limit);
}