package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.service.LogMonitoringService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controlador de la ventana principal de BuscaDocs.
 * <p>
 * Gestiona la navegación entre las diferentes vistas (búsqueda, dashboard,
 * configuración, historial) mediante una barra lateral fija y un
 * {@link BorderPane} cuyo centro se reemplaza dinámicamente. Inicia y
 * detiene el servicio de monitoreo de logs al abrir/cerrar la aplicación.
 * </p>
 *
 * @author VJuan955
 * @version 1.1
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    /** Clase CSS aplicada al botón de navegación de la vista actualmente visible. */
    private static final String ACTIVE_NAV_CLASS = "active";

    @FXML
    private BorderPane mainContainer;

    @FXML private Button navSearchBtn;
    @FXML private Button navSettingsBtn;
    @FXML private Button navHistoryBtn;
    @FXML private Button navDashboardBtn;

    private final LogMonitoringService logService = AppContext.getInstance().getLogMonitoringService();

    /**
     * Referencia al controlador del dashboard actualmente cargado, si lo hay,
     * para poder detener su temporizador al navegar a otra vista y evitar que
     * siga actualizándose en segundo plano tras ser reemplazado.
     */
    private DashboardController activeDashboardController;

    /**
     * Referencia al controlador de configuración actualmente cargado, si lo
     * hay, para poder detener su temporizador de auto-refresco al navegar a
     * otra vista, por el mismo motivo que con el dashboard.
     */
    private SettingsController activeSettingsController;

    /**
     * Inicializa el controlador después de cargar el FXML.
     * Carga la vista de búsqueda por defecto y arranca el monitoreo de logs.
     */
    @FXML
    public void initialize() {
        logger.info("Iniciando MainController");
        logService.start();
        showSearchView();
    }

    /**
     * Carga la vista de búsqueda en el centro del contenedor.
     * Se invoca desde el menú o botón correspondiente.
     */
    @FXML
    public void showSearchView() {
        loadView("search.fxml");
        setActiveNav(navSearchBtn);
    }

    /**
     * Carga la vista del dashboard en el centro del contenedor.
     */
    @FXML
    public void showDashboardView() {
        loadView("dashboard.fxml");
        setActiveNav(navDashboardBtn);
    }

    /**
     * Carga la vista de configuración en el centro del contenedor.
     */
    @FXML
    public void showSettingsView() {
        loadView("settings.fxml");
        setActiveNav(navSettingsBtn);
    }

    /**
     * Carga la vista de historial (búsquedas y archivos abiertos recientes).
     */
    @FXML
    public void showHistoryView() {
        loadView("history.fxml");
        setActiveNav(navHistoryBtn);
    }

    /**
     * Resalta visualmente, en la barra lateral, el botón correspondiente a la
     * vista actualmente visible, y quita ese resaltado de los demás.
     *
     * @param active botón de navegación de la vista que se acaba de mostrar.
     */
    private void setActiveNav(Button active) {
        for (Button navButton : new Button[]{navSearchBtn, navSettingsBtn, navHistoryBtn, navDashboardBtn}) {
            if (navButton == null) {
                continue;
            }
            navButton.getStyleClass().remove(ACTIVE_NAV_CLASS);
        }
        if (active != null && !active.getStyleClass().contains(ACTIVE_NAV_CLASS)) {
            active.getStyleClass().add(ACTIVE_NAV_CLASS);
        }
    }

    /**
     * Método auxiliar para cargar un archivo FXML en el área central.
     *
     * @param fxmlFile nombre del archivo FXML (relativo a /com/buscadocs/view/).
     */
    private void loadView(String fxmlFile) {
        if (activeDashboardController != null) {
            activeDashboardController.stopUpdater();
            activeDashboardController = null;
        }
        if (activeSettingsController != null) {
            activeSettingsController.stopUpdater();
            activeSettingsController = null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/buscadocs/view/" + fxmlFile));
            Parent view = loader.load();
            mainContainer.setCenter(view);
            if (loader.getController() instanceof DashboardController dashboardController) {
                activeDashboardController = dashboardController;
            } else if (loader.getController() instanceof SettingsController settingsController) {
                activeSettingsController = settingsController;
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
        if (activeSettingsController != null) {
            activeSettingsController.stopUpdater();
        }
        logService.stop();
        logger.info("Aplicación cerrada");
        javafx.application.Platform.exit();
    }
}