package com.buscadocs.service;

/**
 * Proporciona métricas de uso de CPU y memoria RAM.
 * Los valores se actualizan en cada llamada.
 *
 * @author VJuan955
 * @version 1.0
 */
public interface MetricService {

    /**
     * Obtiene el porcentaje actual de utilización del procesador.
     *
     * @return porcentaje de uso de CPU en el rango de {@code 0.0} a {@code 100.0}.
     */
    double getCpuLoad();

    /**
     * Obtiene la cantidad de memoria utilizada actualmente por la JVM.
     *
     * @return memoria utilizada en megabytes (MB).
     */
    long getUsedMemoryMB();

    /**
     * Obtiene la cantidad total de memoria asignada actualmente a la JVM.
     *
     * @return memoria total disponible para la JVM en megabytes (MB).
     */
    long getTotalMemoryMB();
}