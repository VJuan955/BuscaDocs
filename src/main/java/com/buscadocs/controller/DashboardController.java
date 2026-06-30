package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.service.LogMonitoringService;
import com.buscadocs.service.MetricService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Controlador del dashboard de métricas de hardware y consola de logs.
 * <p>
 * Actualiza periódicamente las barras de uso de CPU y RAM, y muestra
 * las últimas líneas del archivo de log en tiempo real.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 * @since 2026-06-28
 */
public class DashboardController {

    @FXML private ProgressBar cpuBar;
    @FXML private Label cpuLabel;
    @FXML private ProgressBar ramBar;
    @FXML private Label ramLabel;
    @FXML private ListView<String> logConsole;

    private final MetricService metricService = AppContext.getInstance().getMetricService();
    private final LogMonitoringService logService = AppContext.getInstance().getLogMonitoringService();
    private Timeline updater;

    /**
     * Inicia el temporizador que actualiza las métricas y los logs cada segundo.
     */
    @FXML
    public void initialize() {
        updater = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        updater.setCycleCount(Timeline.INDEFINITE);
        updater.play();
        logConsole.setItems(javafx.collections.FXCollections.observableArrayList());
    }

    private void refresh() {
        double cpu = metricService.getCpuLoad();
        cpuBar.setProgress(cpu / 100.0);
        cpuLabel.setText(String.format("CPU: %.1f%%", cpu));

        long used = metricService.getUsedMemoryMB();
        long total = metricService.getTotalMemoryMB();
        ramBar.setProgress((double) used / total);
        ramLabel.setText(String.format("RAM: %d / %d MB", used, total));

        java.util.List<String> recentLogs = logService.getRecentLines(20);
        logConsole.getItems().setAll(recentLogs);

        if (!recentLogs.isEmpty()) {
            logConsole.scrollTo(recentLogs.size() - 1);
        }
    }

    /**
     * Detiene el temporizador cuando la vista se oculta (opcional, mejora rendimiento).
     */
    public void stopUpdater() {
        if (updater != null) {
            updater.stop();
        }
    }
}