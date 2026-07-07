package com.buscadocs.service;

import com.buscadocs.service.impl.MetricServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link MetricServiceImpl}.
 * <p>
 * Cubren la corrección que evita valores de CPU negativos (posibles en la
 * primera lectura de {@code OperatingSystemMXBean#getCpuLoad()}) y el uso de
 * la memoria física real del sistema en lugar del heap de la JVM.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
class MetricServiceImplTest {

    private final MetricService metricService = new MetricServiceImpl();

    @Test
    void getCpuLoadNuncaDevuelveUnValorNegativo() {
        double cpu = metricService.getCpuLoad();
        assertTrue(cpu >= 0.0, "El uso de CPU no debe ser negativo, fue: " + cpu);
    }

    @Test
    void getCpuLoadNoSuperaCienPorciento() {
        double cpu = metricService.getCpuLoad();
        assertTrue(cpu <= 100.0, "El uso de CPU no debería superar 100%, fue: " + cpu);
    }

    @Test
    void getTotalMemoryMBEsMayorQueCero() {
        assertTrue(metricService.getTotalMemoryMB() > 0);
    }

    @Test
    void getUsedMemoryMBNoSuperaElTotal() {
        long usado = metricService.getUsedMemoryMB();
        long total = metricService.getTotalMemoryMB();
        assertTrue(usado >= 0, "La memoria usada no debe ser negativa");
        assertTrue(usado <= total, "La memoria usada no debería superar el total del sistema");
    }
}
