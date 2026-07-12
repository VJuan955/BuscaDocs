package com.buscadocs.service;

import com.sun.management.OperatingSystemMXBean;
import com.buscadocs.service.impl.MetricServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para {@link MetricServiceImpl}.
 * <p>
 * Incluye pruebas "de humo" contra el {@link OperatingSystemMXBean} real de
 * la JVM en ejecución, y pruebas dirigidas con un doble de prueba (Mockito)
 * que simulan lecturas inválidas ({@code -1}), tal como puede ocurrir de
 * forma intermitente en máquinas virtuales, contenedores o entornos con
 * permisos restringidos — el escenario que originaba que el dashboard
 * mostrara 0% o pareciera "congelado".
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
class MetricServiceImplTest {

    private final MetricService realMetricService = new MetricServiceImpl();

    @Test
    void getCpuLoadNuncaDevuelveUnValorNegativo() {
        double cpu = realMetricService.getCpuLoad();
        assertTrue(cpu >= 0.0, "El uso de CPU no debe ser negativo, fue: " + cpu);
    }

    @Test
    void getTotalMemoryMBEsMayorQueCero() {
        assertTrue(realMetricService.getTotalMemoryMB() > 0);
    }

    @Test
    void getUsedMemoryMBNoSuperaElTotal() {
        long usado = realMetricService.getUsedMemoryMB();
        long total = realMetricService.getTotalMemoryMB();
        assertTrue(usado >= 0, "La memoria usada no debe ser negativa");
        assertTrue(usado <= total, "La memoria usada no debería superar el total del sistema");
    }

    @Test
    void getCpuLoadDevuelveCeroSiNuncaHuboUnaLecturaValida() {
        OperatingSystemMXBean fakeBean = mock(OperatingSystemMXBean.class);
        when(fakeBean.getCpuLoad()).thenReturn(-1.0);

        MetricService service = new MetricServiceImpl(fakeBean);

        assertEquals(0.0, service.getCpuLoad());
    }

    @Test
    void getCpuLoadConservaElUltimoValorValidoCuandoElSoDevuelveMenosUno() {
        OperatingSystemMXBean fakeBean = mock(OperatingSystemMXBean.class);
        when(fakeBean.getCpuLoad()).thenReturn(0.42, -1.0, -1.0);

        MetricService service = new MetricServiceImpl(fakeBean);

        assertEquals(42.0, service.getCpuLoad(), 0.001, "Primera lectura válida");
        assertEquals(42.0, service.getCpuLoad(), 0.001,
                "Debe conservar el último valor válido en vez de mostrar 0");
        assertEquals(42.0, service.getCpuLoad(), 0.001,
                "Debe seguir conservando el último valor válido en lecturas repetidas inválidas");
    }

    @Test
    void getUsedMemoryMBDevuelveCeroSiNuncaHuboUnaLecturaValida() {
        OperatingSystemMXBean fakeBean = mock(OperatingSystemMXBean.class);
        when(fakeBean.getFreeMemorySize()).thenReturn(-1L);
        when(fakeBean.getTotalMemorySize()).thenReturn(16_000L * 1024 * 1024);

        MetricService service = new MetricServiceImpl(fakeBean);

        assertEquals(0, service.getUsedMemoryMB());
    }

    @Test
    void getUsedMemoryMBConservaLaUltimaLecturaValidaDeMemoriaLibre() {
        OperatingSystemMXBean fakeBean = mock(OperatingSystemMXBean.class);
        long totalBytes = 16_000L * 1024 * 1024;
        when(fakeBean.getTotalMemorySize()).thenReturn(totalBytes);
        when(fakeBean.getFreeMemorySize()).thenReturn(6_000L * 1024 * 1024, -1L, -1L);

        MetricService service = new MetricServiceImpl(fakeBean);

        long primeraLectura = service.getUsedMemoryMB();
        long segundaLectura = service.getUsedMemoryMB();
        long terceraLectura = service.getUsedMemoryMB();

        assertEquals(10_000, primeraLectura, "16000 - 6000 = 10000 MB usados");
        assertEquals(primeraLectura, segundaLectura,
                "Debe conservar la última lectura válida de memoria libre en vez de mostrar 100% de uso");
        assertEquals(primeraLectura, terceraLectura);
    }
}
