package com.buscadocs.service.impl;

import com.buscadocs.service.LogMonitoringService;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementación de {@link LogMonitoringService} que monitorea el archivo
 * de log de la aplicación utilizando la clase {@link Tailer} de
 * Apache Commons IO.
 *
 * @author VJuan955
 * @version 1.0
 */
public class LogMonitoringServiceImpl implements LogMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(LogMonitoringServiceImpl.class);

    /**
     * Número máximo de líneas de log que se mantienen en memoria.
     */
    private static final int MAX_LINES = 500;

    /**
     * Almacena las líneas más recientes del archivo de log.
     */
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

    /**
     * Instancia encargada de monitorear el archivo de log en tiempo real.
     */
    private Tailer tailer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        File logFile = Paths.get("logs", "buscadocs.log").toFile();
        if (!logFile.exists()) {
            try {
                File parent = logFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                logFile.createNewFile();
                logger.info("Archivo de log no existía, se creó vacío: {}", logFile.getAbsolutePath());
            } catch (java.io.IOException e) {
                logger.warn("No se pudo crear el archivo de log: {}", logFile.getAbsolutePath(), e);
                return;
            }
        }
        tailer = Tailer.create(logFile, new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                if (lines.size() >= MAX_LINES) {
                    lines.remove(0);
                }
                lines.add(line);
            }
        }, 1000, true);
        logger.info("Monitoreo de logs iniciado");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (tailer != null) {
            tailer.stop();
            logger.info("Monitoreo de logs detenido");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getRecentLines(int maxLines) {
        synchronized (lines) {
            int fromIndex = Math.max(0, lines.size() - maxLines);
            return new ArrayList<>(lines.subList(fromIndex, lines.size()));
        }
    }
}