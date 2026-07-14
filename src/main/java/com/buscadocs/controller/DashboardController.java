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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del dashboard de métricas de hardware y consola de logs.
 * <p>
 * Actualiza periódicamente las barras de uso de CPU y RAM, y muestra
 * las últimas líneas del archivo de log en tiempo real.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private static final double SEVERITY_WARNING_THRESHOLD = 60.0;

    private static final double SEVERITY_CRITICAL_THRESHOLD = 85.0;

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
        try {
            double cpu = metricService.getCpuLoad();
            cpuBar.setProgress(clampProgress(cpu / 100.0));
            cpuLabel.setText(String.format("CPU: %.1f%%", cpu));
            applySeverityStyle(cpuBar, cpu);

            long used = metricService.getUsedMemoryMB();
            long total = metricService.getTotalMemoryMB();
            double ramProgress = total > 0 ? clampProgress((double) used / total) : 0.0;
            ramBar.setProgress(ramProgress);
            ramLabel.setText(String.format("RAM: %d / %d MB", used, total));
            applySeverityStyle(ramBar, ramProgress * 100.0);

            java.util.List<String> recentLogs = logService.getRecentLines(20);
            logConsole.getItems().setAll(recentLogs);

            if (!recentLogs.isEmpty()) {
                logConsole.scrollTo(recentLogs.size() - 1);
            }
        } catch (Exception e) {
            logger.warn("Error al actualizar el dashboard, se reintentará en el próximo ciclo", e);
        }
    }

    /**
     * Recolorea una barra de progreso según qué tan alto es el porcentaje que
     * representa: verde por debajo de {@value #SEVERITY_WARNING_THRESHOLD}%,
     * amarillo hasta {@value #SEVERITY_CRITICAL_THRESHOLD}%, y rojo por
     * encima — dando una señal visual inmediata de sobrecarga, en vez de un
     * único color estático sin importar el valor.
     *
     * @param bar barra de progreso a recolorear.
     * @param percentage porcentaje representado (0-100).
     */
    private void applySeverityStyle(ProgressBar bar, double percentage) {
        bar.getStyleClass().removeAll("metric-bar-ok", "metric-bar-warning", "metric-bar-critical");
        String styleClass;
        if (percentage >= SEVERITY_CRITICAL_THRESHOLD) {
            styleClass = "metric-bar-critical";
        } else if (percentage >= SEVERITY_WARNING_THRESHOLD) {
            styleClass = "metric-bar-warning";
        } else {
            styleClass = "metric-bar-ok";
        }
        bar.getStyleClass().add(styleClass);
    }

    /**
     * Acota un valor de progreso al rango [0, 1] que espera {@link ProgressBar},
     * evitando además valores {@code NaN} o infinitos.
     *
     * @param value valor de progreso calculado, potencialmente fuera de rango.
     * @return el valor acotado entre 0.0 y 1.0.
     */
    private double clampProgress(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
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