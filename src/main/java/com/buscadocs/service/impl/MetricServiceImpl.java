package com.buscadocs.service.impl;

import com.buscadocs.service.MetricService;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

/**
 * Implementación de {@link MetricService} que obtiene información sobre
 * el uso de recursos del sistema utilizando las API de administración de Java.
 * <p>
 * {@code com.sun.management.OperatingSystemMXBean#getCpuLoad()} y
 * {@code #getFreeMemorySize()} pueden devolver {@code -1} para indicar que
 * la lectura no está disponible en ese instante — esto no ocurre solo en la
 * primera llamada tras iniciar la JVM, sino que puede repetirse de forma
 * intermitente según la plataforma (máquinas virtuales, contenedores, WSL,
 * o permisos restringidos del sistema operativo). Esta implementación
 * conserva la última lectura válida en esos casos, en lugar de mostrar 0
 * (que haría parecer que el uso de recursos cayó a cero, o que el panel
 * dejó de actualizarse).
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class MetricServiceImpl implements MetricService {

    /**
     * Bean del sistema operativo utilizado para consultar métricas de CPU.
     */
    private final OperatingSystemMXBean osBean;

    /** Última lectura válida de carga de CPU (0-100), usada como respaldo si el SO devuelve -1. */
    private volatile double lastValidCpuLoad = 0.0;

    /** Última lectura válida de memoria libre en bytes, usada como respaldo si el SO devuelve -1. */
    private volatile long lastValidFreeMemoryBytes = -1;

    /**
     * Crea el servicio usando el {@link OperatingSystemMXBean} real de la JVM en ejecución.
     */
    public MetricServiceImpl() {
        this((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean());
    }

    /**
     * Crea el servicio a partir de un {@link OperatingSystemMXBean} específico.
     * <p>
     * Permite inyectar un doble de prueba en pruebas unitarias para simular
     * lecturas inválidas ({@code -1}) sin depender del hardware real.
     * </p>
     *
     * @param osBean bean de sistema operativo a utilizar.
     */
    public MetricServiceImpl(OperatingSystemMXBean osBean) {
        this.osBean = osBean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuLoad() {
        double load = osBean.getCpuLoad();
        if (load < 0) {
            return lastValidCpuLoad;
        }
        lastValidCpuLoad = load * 100.0;
        return lastValidCpuLoad;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUsedMemoryMB() {
        long freeBytes = osBean.getFreeMemorySize();
        if (freeBytes < 0) {
            if (lastValidFreeMemoryBytes < 0) {
                return 0;
            }
            freeBytes = lastValidFreeMemoryBytes;
        } else {
            lastValidFreeMemoryBytes = freeBytes;
        }
        return getTotalMemoryMB() - (freeBytes / (1024 * 1024));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalMemoryMB() {
        return osBean.getTotalMemorySize() / (1024 * 1024);
    }
}