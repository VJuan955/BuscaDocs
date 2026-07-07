package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.service.LogMonitoringService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controlador de la ventana principal de BuscaDocs.
 * <p>
 * Gestiona la navegación entre las diferentes vistas (búsqueda, dashboard, configuración)
 * mediante un {@link BorderPane} que actúa como contenedor. Inicia y detiene el servicio
 * de monitoreo de logs al abrir/cerrar la aplicación.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private BorderPane mainContainer;

    private final LogMonitoringService logService = AppContext.getInstance().getLogMonitoringService();

    /**
     * Referencia al controlador del dashboard actualmente cargado, si lo hay,
     * para poder detener su temporizador al navegar a otra vista y evitar que
     * siga actualizándose en segundo plano tras ser reemplazado.
     */
    private DashboardController activeDashboardController;

    /**
     * Inicializa el controlador después de cargar el FXML.
     * Carga la vista de búsqueda por defecto y arranca el monitoreo de logs.
     */
    @FXML
    public void initialize() {
        logger.info("Iniciando MainController");
        logService.start();
        loadView("search.fxml");
    }

    /**
     * Carga la vista de búsqueda en el centro del contenedor.
     * Se invoca desde el menú o botón correspondiente.
     */
    @FXML
    public void showSearchView() {
        loadView("search.fxml");
    }

    /**
     * Carga la vista del dashboard en el centro del contenedor.
     */
    @FXML
    public void showDashboardView() {
        loadView("dashboard.fxml");
    }

    /**
     * Carga la vista de configuración en el centro del contenedor.
     */
    @FXML
    public void showSettingsView() {
        loadView("settings.fxml");
    }

    /**
     * Carga la vista de historial (búsquedas y archivos abiertos recientes).
     */
    @FXML
    public void showHistoryView() {
        loadView("history.fxml");
    }

    /**
     * Método auxiliar para cargar un archivo FXML en el área central.
     *
     * @param fxmlFile nombre del archivo FXML (relativo a /com/buscadocs/view/).
     */
    private void loadView(String fxmlFile) {
        // Si veníamos del dashboard, detenemos su temporizador antes de
        // reemplazarlo: de lo contrario seguiría actualizándose en segundo
        // plano indefinidamente, aunque ya no sea visible.
        if (activeDashboardController != null) {
            activeDashboardController.stopUpdater();
            activeDashboardController = null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/buscadocs/view/" + fxmlFile));
            Parent view = loader.load();
            mainContainer.setCenter(view);
            if (loader.getController() instanceof DashboardController dashboardController) {
                activeDashboardController = dashboardController;
            }
            logger.debug("Vista cargada: {}", fxmlFile);
        } catch (IOException e) {
            logger.error("Error al cargar vista: {}", fxmlFile, e);
        }
    }

    /**
     * Cierra la aplicación deteniendo los servicios activos.
     */
    @FXML
    public void shutdown() {
        if (activeDashboardController != null) {
            activeDashboardController.stopUpdater();
        }
        logService.stop();
        logger.info("Aplicación cerrada");
        javafx.application.Platform.exit();
    }
}