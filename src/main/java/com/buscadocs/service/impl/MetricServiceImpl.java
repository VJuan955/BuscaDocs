package com.buscadocs.service.impl;

import com.buscadocs.service.MetricService;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

/**
 * Implementación de {@link MetricService} que obtiene información sobre
 * el uso de recursos del sistema utilizando las API de administración de Java.
 *
 * @author VJuan955
 * @version 1.0
 */
public class MetricServiceImpl implements MetricService {

    /**
     * Bean del sistema operativo utilizado para consultar métricas de CPU.
     */
    private final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuLoad() {
        // getCpuLoad() puede devolver -1 cuando el valor todavía no está disponible
        // (p. ej. en la primera lectura tras iniciar la JVM). Se acota a 0 para
        // evitar romper componentes visuales como ProgressBar, que no aceptan
        // progreso negativo.
        double load = osBean.getCpuLoad();
        return load < 0 ? 0.0 : load * 100.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsedMemoryMB() {
        return (getTotalMemoryMB() - (osBean.getFreeMemorySize() / (1024 * 1024)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalMemoryMB() {
        // Memoria física total del sistema operativo, no solo el heap de la JVM,
        // para que el dashboard refleje el uso real de RAM del equipo.
        return osBean.getTotalMemorySize() / (1024 * 1024);
    }
}