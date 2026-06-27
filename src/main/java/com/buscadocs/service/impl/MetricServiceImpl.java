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
        return osBean.getCpuLoad() * 100.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsedMemoryMB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalMemoryMB() {
        return Runtime.getRuntime().totalMemory() / (1024 * 1024);
    }
}