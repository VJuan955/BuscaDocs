package com.buscadocs.service;

import com.buscadocs.model.Folder;

/**
 * Servicio para gestionar la indexación de carpetas.
 * Recorre el sistema de archivos, extrae contenido y actualiza la base de datos.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface IndexService {

    /**
     * Agrega una nueva carpeta al sistema y dispara su indexación.
     *
     * @param path Ruta absoluta de la carpeta.
     * @param includeHidden Si se deben incluir archivos ocultos.
     * @return La carpeta creada con su estado actual.
     */
    Folder addFolder(String path, boolean includeHidden);

    /**
     * Agrega una nueva carpeta al sistema, restringiendo la indexación a un
     * conjunto específico de extensiones, y dispara su indexación.
     *
     * @param path Ruta absoluta de la carpeta.
     * @param includeHidden Si se deben incluir archivos ocultos.
     * @param extensionFilter Extensiones separadas por coma (sin punto, ej. "pdf,docx")
     *                        que delimitan qué archivos se indexan, o {@code null}/vacío
     *                        para indexar todos los archivos sin restricción.
     * @return La carpeta creada con su estado actual.
     */
    Folder addFolder(String path, boolean includeHidden, String extensionFilter);

    /**
     * Reindexa una carpeta existente (borra sus archivos previos y vuelve a escanear).
     *
     * @param folderId ID de la carpeta.
     * @return Carpeta actualizada.
     */
    Folder reindexFolder(int folderId);

    /**
     * Elimina una carpeta y todos sus archivos indexados.
     *
     * @param folderId ID de la carpeta.
     */
    void removeFolder(int folderId);
}