package com.buscadocs.service;

/**
 * Servicio para abrir archivos con el programa predeterminado del sistema
 * y registrar la acción en el historial.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface FileActionService {

    /**
     * Abre un archivo con la aplicación predeterminada del sistema
     * operativo y registra la operación en el historial.
     *
     * @param filePath ruta absoluta del archivo que se desea abrir.
     */
    void openFile(String filePath);
}