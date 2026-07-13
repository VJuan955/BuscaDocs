package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.model.Folder;
import com.buscadocs.service.IndexService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador de la vista de configuración de carpetas.
 * <p>
 * Permite agregar (con un filtro opcional de extensiones a indexar),
 * reindexar y eliminar carpetas del sistema de indexación. La lista se
 * refresca automáticamente cada 1.5 segundos mientras la vista está activa,
 * para reflejar en vivo el progreso de la indexación en segundo plano sin
 * que el usuario tenga que cambiar de pestaña y volver. Cada carpeta se
 * muestra con un indicador de color según su estado: verde (lista), amarillo
 * (indexando) o rojo (pendiente/error).
 * </p>
 *
 * @author VJuan955
 * @version 1.1
 */
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Intervalo de refresco automático de la lista de carpetas. */
    private static final Duration REFRESH_INTERVAL = Duration.seconds(1.5);

    @FXML private TextField folderPathField;
    @FXML private TextField extensionFilterField;
    @FXML private CheckBox includeHiddenCheck;
    @FXML private ListView<Folder> foldersList;
    @FXML private Label messageLabel;

    private final IndexService indexService = AppContext.getInstance().getIndexService();

    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        foldersList.setCellFactory(lv -> new FolderCell());
        loadFolders();

        refreshTimeline = new Timeline(new KeyFrame(REFRESH_INTERVAL, e -> loadFolders()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    /**
     * Detiene el refresco automático. Debe invocarse al navegar a otra vista
     * para no seguir consultando la base de datos en segundo plano una vez
     * que esta vista ya no es visible.
     */
    public void stopUpdater() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    /**
     * Abre un selector de carpetas del sistema operativo y coloca la ruta
     * elegida en el campo de texto correspondiente.
     */
    @FXML
    public void browseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Selecciona una carpeta para indexar");
        File selected = chooser.showDialog(foldersList.getScene().getWindow());
        if (selected != null) {
            folderPathField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    public void addFolder() {
        String path = folderPathField.getText().trim();
        if (path.isEmpty()) {
            messageLabel.setText("Seleccione una carpeta");
            return;
        }
        boolean includeHidden = includeHiddenCheck.isSelected();
        String extensionFilter = extensionFilterField.getText();
        try {
            Folder folder = indexService.addFolder(path, includeHidden, extensionFilter);
            messageLabel.setText("Carpeta agregada: " + folder.getPath());
            folderPathField.clear();
            extensionFilterField.clear();
            loadFolders();
        } catch (Exception e) {
            logger.error("Error al agregar carpeta: {}", path, e);
            messageLabel.setText("No se pudo agregar la carpeta (¿ya existe o la ruta es inválida?)");
        }
    }

    @FXML
    public void reindexFolder() {
        Folder selected = foldersList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Selecciona una carpeta de la lista para reindexar");
            return;
        }
        indexService.reindexFolder(selected.getId());
        messageLabel.setText("Reindexando: " + selected.getPath());
        loadFolders();
    }

    @FXML
    public void removeFolder() {
        Folder selected = foldersList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Selecciona una carpeta de la lista para eliminar");
            return;
        }
        indexService.removeFolder(selected.getId());
        messageLabel.setText("Carpeta eliminada: " + selected.getPath());
        loadFolders();
    }

    /**
     * Recarga la lista de carpetas desde la base de datos, preservando la
     * selección actual (si sigue existiendo tras la recarga) para que el
     * refresco automático no interrumpa una selección en curso.
     */
    private void loadFolders() {
        Folder previouslySelected = foldersList.getSelectionModel().getSelectedItem();
        List<Folder> folders = AppContext.getInstance().getFolderDao().findAll();
        foldersList.getItems().setAll(folders);

        if (previouslySelected != null) {
            folders.stream()
                    .filter(f -> f.getId() == previouslySelected.getId())
                    .findFirst()
                    .ifPresent(f -> foldersList.getSelectionModel().select(f));
        }
    }

    /**
     * Celda visual para cada carpeta: muestra un punto de color según el
     * estado (verde = lista, amarillo = indexando, rojo = pendiente/error),
     * la ruta, y una línea de metadatos (estado en texto, última indexación,
     * archivos ocultos y filtro de extensiones si corresponde).
     */
    private static class FolderCell extends ListCell<Folder> {
        private final Circle statusDot = new Circle(5);
        private final Label pathLabel = new Label();
        private final Label metaLabel = new Label();
        private final VBox textBox = new VBox(3, pathLabel, metaLabel);
        private final HBox card = new HBox(10, statusDot, textBox);

        FolderCell() {
            pathLabel.getStyleClass().add("result-card-title");
            metaLabel.getStyleClass().add("result-card-meta");
            card.getStyleClass().add("result-card");
            card.setPadding(new Insets(4));
            card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(Folder folder, boolean empty) {
            super.updateItem(folder, empty);
            if (empty || folder == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            pathLabel.setText(folder.getPath());

            String status = folder.getStatus() != null ? folder.getStatus() : "PENDING";
            statusDot.getStyleClass().removeAll(
                    "status-dot-ready", "status-dot-indexing", "status-dot-error");
            String statusText;
            switch (status) {
                case "READY" -> {
                    statusDot.getStyleClass().add("status-dot-ready");
                    statusText = "Lista";
                }
                case "INDEXING" -> {
                    statusDot.getStyleClass().add("status-dot-indexing");
                    statusText = "Indexando...";
                }
                default -> {
                    statusDot.getStyleClass().add("status-dot-error");
                    statusText = "ERROR".equals(status) ? "Error" : "Pendiente";
                }
            }

            StringBuilder meta = new StringBuilder(statusText);
            if (folder.getLastIndexed() != null) {
                meta.append("  ·  Última indexación: ").append(folder.getLastIndexed().format(DATE_FORMAT));
            }
            if (folder.isIncludeHidden()) {
                meta.append("  ·  incluye ocultos");
            }
            if (folder.getExtensionFilter() != null && !folder.getExtensionFilter().isBlank()) {
                meta.append("  ·  solo: ").append(folder.getExtensionFilter());
            }
            metaLabel.setText(meta.toString());

            setGraphic(card);
            setText(null);
        }
    }
}
