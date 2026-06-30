package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.model.Folder;
import com.buscadocs.service.IndexService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador de la vista de configuración de carpetas.
 * <p>
 * Permite agregar, reindexar y eliminar carpetas del sistema de indexación.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 * @since 2026-06-28
 */
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @FXML private TextField folderPathField;
    @FXML private CheckBox includeHiddenCheck;
    @FXML private ListView<Folder> foldersList;
    @FXML private Label messageLabel;

    private final IndexService indexService = AppContext.getInstance().getIndexService();

    @FXML
    public void initialize() {
        loadFolders();
        foldersList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Folder item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : String.format("[%s] %s", item.getStatus(), item.getPath()));
            }
        });
    }

    @FXML
    public void addFolder() {
        String path = folderPathField.getText().trim();
        if (path.isEmpty()) {
            messageLabel.setText("Seleccione una carpeta");
            return;
        }
        boolean includeHidden = includeHiddenCheck.isSelected();
        Folder folder = indexService.addFolder(path, includeHidden);
        messageLabel.setText("Carpeta agregada: " + folder.getPath());
        folderPathField.clear();
        loadFolders();
    }

    @FXML
    public void reindexFolder() {
        Folder selected = foldersList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            indexService.reindexFolder(selected.getId());
            messageLabel.setText("Reindexando: " + selected.getPath());
            loadFolders();
        }
    }

    @FXML
    public void removeFolder() {
        Folder selected = foldersList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            indexService.removeFolder(selected.getId());
            messageLabel.setText("Carpeta eliminada: " + selected.getPath());
            loadFolders();
        }
    }

    private void loadFolders() {
        var folders = AppContext.getInstance().getFolderDao().findAll();
        foldersList.getItems().setAll(folders);
    }
}