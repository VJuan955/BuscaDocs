package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.model.SearchResult;
import com.buscadocs.service.FileActionService;
import com.buscadocs.service.SearchService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador de la vista de búsqueda.
 * <p>
 * Gestiona la entrada de texto, filtros (extensión y fecha) y muestra
 * los resultados en un {@link ListView} de tarjetas.
 * </p>
 *
 * @author VJuan955
 * @version 2.0
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

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Configura la lista de resultados con doble clic para abrir archivos.
     */
    @FXML
    public void initialize() {
        resultsList.setCellFactory(lv -> new ListCell<>() {
            private final Label nameLabel = new Label();
            private final Label pathLabel = new Label();
            private final Label snippetLabel = new Label();
            private final Label metaLabel = new Label();
            private final VBox card = new VBox(4, nameLabel, pathLabel, snippetLabel, metaLabel);

            {
                nameLabel.getStyleClass().add("result-card-title");
                pathLabel.getStyleClass().add("result-card-path");
                snippetLabel.getStyleClass().add("result-card-snippet");
                snippetLabel.setWrapText(true);
                metaLabel.getStyleClass().add("result-card-meta");
                card.getStyleClass().add("result-card");
                card.setPadding(new Insets(4));
            }

            @Override
            protected void updateItem(SearchResult item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                nameLabel.setText(item.getFileName());
                pathLabel.setText(item.getFilePath());
                String snippet = item.getSnippet();
                snippetLabel.setText(snippet == null || snippet.isBlank() ? "(sin vista previa de contenido)" : snippet);
                String ext = item.getExtension() == null || item.getExtension().isBlank()
                        ? "SIN EXT" : item.getExtension().toUpperCase();
                String size = formatSize(item.getSizeBytes());
                String date = item.getLastModified() != null ? item.getLastModified().format(DATE_FORMAT) : "—";
                metaLabel.setText(String.format("%s  ·  %s  ·  %s", ext, size, date));
                setGraphic(card);
                setText(null);
            }
        });

        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                SearchResult selected = resultsList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    boolean opened = fileActionService.openFile(selected.getFilePath());
                    if (!opened) {
                        statusLabel.setText("No se pudo abrir: " + selected.getFileName());
                    }
                }
            }
        });
    }

    /**
     * Formatea un tamaño en bytes a una representación legible (KB, MB, GB).
     *
     * @param bytes tamaño en bytes.
     * @return cadena formateada, por ejemplo "1.4 MB".
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.1f GB", mb / 1024.0);
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

        try {
            List<SearchResult> results = searchService.search(query, extensions, from, to, 50);
            resultsList.setItems(FXCollections.observableArrayList(results));
            statusLabel.setText(String.format("%d resultados encontrados", results.size()));
            logger.info("Búsqueda completada: {} resultados", results.size());
        } catch (Exception e) {
            logger.error("Error al ejecutar la búsqueda: {}", query, e);
            statusLabel.setText("Ocurrió un error al buscar. Intenta nuevamente.");
        }
    }
}