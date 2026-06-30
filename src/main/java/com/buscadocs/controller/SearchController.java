package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.model.SearchResult;
import com.buscadocs.service.FileActionService;
import com.buscadocs.service.SearchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Controlador de la vista de búsqueda.
 * <p>
 * Gestiona la entrada de texto, filtros (extensión y fecha) y muestra
 * los resultados en un {@link ListView} de tarjetas.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 * @since 2026-06-28
 */
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @FXML private TextField queryField;
    @FXML private TextField extensionFilter;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private ListView<SearchResult> resultsList;
    @FXML private Label statusLabel;

    private final SearchService searchService = AppContext.getInstance().getSearchService();
    private final FileActionService fileActionService = AppContext.getInstance().getFileActionService();

    /**
     * Configura la lista de resultados con doble clic para abrir archivos.
     */
    @FXML
    public void initialize() {
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(SearchResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%s [%s] - %s",
                            item.getFileName(), item.getExtension(), item.getSnippet()));
                }
            }
        });

        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SearchResult selected = resultsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    fileActionService.openFile(selected.getFilePath());
                }
            }
        });
    }

    /**
     * Ejecuta la búsqueda con los filtros actuales.
     */
    @FXML
    public void performSearch() {
        String query = queryField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Ingrese un texto de búsqueda");
            return;
        }

        List<String> extensions = null;
        String extText = extensionFilter.getText().trim();
        if (!extText.isEmpty()) {
            extensions = List.of(extText.split(",\\s*"));
        }

        LocalDateTime from = dateFrom.getValue() != null
                ? LocalDateTime.of(dateFrom.getValue(), LocalTime.MIN) : null;
        LocalDateTime to = dateTo.getValue() != null
                ? LocalDateTime.of(dateTo.getValue(), LocalTime.MAX) : null;

        List<SearchResult> results = searchService.search(query, extensions, from, to, 50);
        resultsList.setItems(FXCollections.observableArrayList(results));
        statusLabel.setText(String.format("%d resultados encontrados", results.size()));
        logger.info("Búsqueda completada: {} resultados", results.size());
    }
}