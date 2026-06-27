package com.buscadocs.service;

import java.util.List;

/**
 * Servicio para monitorear el archivo de log en tiempo real.
 * Proporciona las últimas líneas del archivo para una consola visual.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface LogMonitoringService {
    /** Inicia la monitorización del archivo de log. */
    void start();

    /** Detiene la monitorización. */
    void stop();

    /**
     * Obtiene las líneas más recientes registradas en el archivo de log.
     *
     * @param maxLines número máximo de líneas que se desean recuperar.
     * @return lista con las últimas líneas registradas.
     */
    List<String> getRecentLines(int maxLines);
}