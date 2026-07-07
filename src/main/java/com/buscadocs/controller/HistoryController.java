package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.model.FileOpenHistory;
import com.buscadocs.model.SearchHistory;
import com.buscadocs.service.HistoryService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;

/**
 * Controlador de la vista de historial.
 * <p>
 * Muestra las búsquedas más recientes y los archivos abiertos más
 * recientemente, y permite limpiar cada historial de forma independiente.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class HistoryController {

    private static final int RECENT_LIMIT = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ListView<SearchHistory> searchHistoryList;
    @FXML private ListView<FileOpenHistory> fileHistoryList;
    @FXML private Label statusLabel;

    private final HistoryService historyService = AppContext.getInstance().getHistoryService();

    @FXML
    public void initialize() {
        searchHistoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchHistory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String fecha = item.getSearchedAt() != null ? item.getSearchedAt().format(DATE_FORMAT) : "—";
                String ext = item.getExtensionFilter() != null && !item.getExtensionFilter().isBlank()
                        ? " [" + item.getExtensionFilter() + "]" : "";
                setText(String.format("%s%s — %d resultado(s) — %s",
                        item.getQueryText(), ext, item.getResultCount(), fecha));
            }
        });

        fileHistoryList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileOpenHistory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String fecha = item.getOpenedAt() != null ? item.getOpenedAt().format(DATE_FORMAT) : "—";
                setText(String.format("%s — %s", item.getFilePath(), fecha));
            }
        });

        loadHistory();
    }

    /**
     * Recarga ambas listas de historial desde el servicio.
     */
    private void loadHistory() {
        searchHistoryList.getItems().setAll(historyService.getRecentSearches(RECENT_LIMIT));
        fileHistoryList.getItems().setAll(historyService.getRecentFileOpens(RECENT_LIMIT));
        statusLabel.setText(String.format("%d búsquedas, %d archivos abiertos",
                searchHistoryList.getItems().size(), fileHistoryList.getItems().size()));
    }

    /**
     * Limpia por completo el historial de búsquedas, tras confirmación implícita
     * del botón, y recarga la vista.
     */
    @FXML
    public void clearSearchHistory() {
        if (historyService.clearSearchHistory()) {
            loadHistory();
            statusLabel.setText("Historial de búsquedas eliminado");
        } else {
            statusLabel.setText("No se pudo limpiar el historial de búsquedas");
        }
    }

    /**
     * Limpia por completo el historial de archivos abiertos y recarga la vista.
     */
    @FXML
    public void clearFileOpenHistory() {
        if (historyService.clearFileOpenHistory()) {
            loadHistory();
            statusLabel.setText("Historial de archivos abiertos eliminado");
        } else {
            statusLabel.setText("No se pudo limpiar el historial de archivos abiertos");
        }
    }
}
