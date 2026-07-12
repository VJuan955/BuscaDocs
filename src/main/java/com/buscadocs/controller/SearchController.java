package com.buscadocs.controller;

import com.buscadocs.config.AppContext;
import com.buscadocs.model.SearchResult;
import com.buscadocs.service.FileActionService;
import com.buscadocs.service.SearchService;
import com.google.common.base.Splitter;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
 */
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @FXML private TextField queryField;
    @FXML private TextField extensionFilter;
    @FXML private DatePicker dateFrom;
    @FXML private DatePicker dateTo;
    @FXML private ListView<SearchResult> resultsList;
    @FXML private Label statusLabel;
    @FXML private Label filtersSummaryLabel;
    @FXML private Button filterToggleBtn;
    @FXML private Popup filtersPopup;

    private final SearchService searchService = AppContext.getInstance().getSearchService();
    private final FileActionService fileActionService = AppContext.getInstance().getFileActionService();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Divide el texto del filtro de extensiones en tokens individuales,
     * ignorando espacios en blanco y tokens vacíos (por ejemplo, comas
     * repetidas o finales), a diferencia de un {@code String.split} manual.
     */
    private static final Splitter EXTENSION_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    /** Cantidad mínima de caracteres antes de disparar una consulta de sugerencias. */
    private static final int MIN_CHARS_FOR_SUGGESTIONS = 2;

    /** Menú desplegable con las sugerencias de autocompletado bajo el campo de búsqueda. */
    private final ContextMenu suggestionsMenu = new ContextMenu();

    /**
     * Retrasa la consulta de sugerencias hasta que el usuario deja de escribir
     * durante 250 ms ("debounce"), para no golpear la base de datos en cada
     * pulsación de tecla.
     */
    private final PauseTransition suggestionsDebounce = new PauseTransition(Duration.millis(250));

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

        suggestionsDebounce.setOnFinished(e -> updateSuggestions());
        queryField.textProperty().addListener((obs, oldText, newText) -> {
            suggestionsDebounce.stop();
            if (newText != null && newText.trim().length() >= MIN_CHARS_FOR_SUGGESTIONS) {
                suggestionsDebounce.playFromStart();
            } else {
                suggestionsMenu.hide();
            }
        });
        queryField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                suggestionsMenu.hide();
            }
        });

        updateFilterSummary();
    }

    /**
     * Muestra u oculta la ventana emergente de filtros (extensión y rango de
     * fechas), anclada justo debajo del botón "Filtros".
     */
    @FXML
    public void toggleFilters() {
        if (filtersPopup.isShowing()) {
            filtersPopup.hide();
            return;
        }
        Bounds bounds = filterToggleBtn.localToScreen(filterToggleBtn.getBoundsInLocal());
        filtersPopup.show(filterToggleBtn, bounds.getMinX(), bounds.getMaxY() + 6);
    }

    /**
     * Aplica los filtros seleccionados en la ventana emergente: la cierra,
     * actualiza el resumen visible junto al contador de resultados y
     * relanza la búsqueda con los nuevos criterios.
     */
    @FXML
    public void applyFilters() {
        filtersPopup.hide();
        updateFilterSummary();
        performSearch();
    }

    /**
     * Limpia los filtros de extensión y fecha, sin cerrar la ventana
     * emergente (para que el usuario pueda seguir ajustando antes de aplicar).
     */
    @FXML
    public void clearFilters() {
        extensionFilter.clear();
        dateFrom.setValue(null);
        dateTo.setValue(null);
        updateFilterSummary();
    }

    /**
     * Actualiza el texto de resumen de filtros activos junto al contador de
     * resultados, y resalta el botón "Filtros" cuando hay al menos uno activo.
     */
    private void updateFilterSummary() {
        List<String> parts = new ArrayList<>();
        String ext = extensionFilter.getText();
        if (ext != null && !ext.isBlank()) {
            parts.add(ext.trim().toUpperCase());
        }
        if (dateFrom.getValue() != null || dateTo.getValue() != null) {
            parts.add("rango de fechas");
        }
        boolean active = !parts.isEmpty();
        filtersSummaryLabel.setText(active ? "Filtros: " + String.join(" · ", parts) : "");
        filterToggleBtn.getStyleClass().remove("filter-toggle-active");
        if (active) {
            filterToggleBtn.getStyleClass().add("filter-toggle-active");
        }
    }

    /**
     * Consulta sugerencias de nombres de archivo para el texto actual del
     * campo de búsqueda y las muestra en un menú desplegable bajo el campo.
     * Seleccionar una sugerencia completa el campo con ese nombre y dispara
     * la búsqueda automáticamente.
     */
    private void updateSuggestions() {
        String prefix = queryField.getText() != null ? queryField.getText().trim() : "";
        if (prefix.length() < MIN_CHARS_FOR_SUGGESTIONS) {
            suggestionsMenu.hide();
            return;
        }

        List<String> suggestions;
        try {
            suggestions = searchService.suggestFileNames(prefix, 8);
        } catch (Exception e) {
            logger.warn("Error al obtener sugerencias para: {}", prefix, e);
            suggestionsMenu.hide();
            return;
        }

        if (suggestions.isEmpty()) {
            suggestionsMenu.hide();
            return;
        }

        suggestionsMenu.getItems().clear();
        for (String suggestion : suggestions) {
            MenuItem item = new MenuItem(suggestion);
            item.setOnAction(e -> {
                queryField.setText(suggestion);
                queryField.positionCaret(suggestion.length());
                suggestionsMenu.hide();
                performSearch();
            });
            suggestionsMenu.getItems().add(item);
        }

        if (!suggestionsMenu.isShowing()) {
            suggestionsMenu.show(queryField, Side.BOTTOM, 0, 0);
        }
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
        suggestionsMenu.hide();
        String query = queryField.getText().trim();
        if (query.isEmpty()) {
            statusLabel.setText("Ingrese un texto de búsqueda");
            return;
        }

        List<String> extensions = null;
        String extText = extensionFilter.getText().trim();
        if (!extText.isEmpty()) {
            extensions = EXTENSION_SPLITTER.splitToList(extText);
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