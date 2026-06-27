package com.buscadocs.dao;

import com.buscadocs.model.Folder;
import java.util.List;
import java.util.Optional;

/**
 * Define las operaciones de acceso a datos para la entidad Folder.
 * Sigue el patrón DAO para abstraer la fuente de datos (SQLite).
 *
 * @author VJuan955
 * @version 1.0
 */
public interface FolderDao {

    /**
     * Inserta una nueva carpeta en la base de datos.
     *
     * @param folder La carpeta a insertar (sin ID, se autogenera).
     * @return El objeto Folder con el ID asignado.
     */
    Folder insert(Folder folder);

    /**
     * Actualiza los datos de una carpeta existente.
     *
     * @param folder Carpeta con los datos a actualizar (debe tener ID).
     * @return La carpeta actualizada.
     */
    Folder update(Folder folder);

    /**
     * Elimina una carpeta por su ID.
     *
     * @param id Identificador de la carpeta.
     * @return true si se eliminó correctamente, false si no se encontró.
     */
    boolean delete(int id);

    /**
     * Busca una carpeta por su ID.
     *
     * @param id Identificador.
     * @return Un Optional con la carpeta si existe, vacío en caso contrario.
     */
    Optional<Folder> findById(int id);

    /**
     * Busca una carpeta por su ruta exacta.
     *
     * @param path Ruta absoluta normalizada.
     * @return Optional con la carpeta.
     */
    Optional<Folder> findByPath(String path);

    /**
     * Obtiene todas las carpetas registradas.
     *
     * @return Lista de carpetas.
     */
    List<Folder> findAll();

    /**
     * Obtiene las carpetas que tienen un estado específico.
     *
     * @param status Estado (PENDING, INDEXING, READY, ERROR).
     * @return Lista de carpetas con ese estado.
     */
    List<Folder> findByStatus(String status);
}