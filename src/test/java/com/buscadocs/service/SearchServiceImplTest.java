package com.buscadocs.service;

import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.dao.SearchHistoryDao;
import com.buscadocs.model.IndexedFile;
import com.buscadocs.model.SearchHistory;
import com.buscadocs.model.SearchResult;
import com.buscadocs.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link SearchServiceImpl}.
 * <p>
 * Utiliza implementaciones falsas ("fakes") de {@link IndexedFileDao} y
 * {@link SearchHistoryDao} en memoria para aislar la lógica de filtrado
 * (extensión y rango de fechas) y el registro de historial, sin depender
 * de una base de datos real.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
class SearchServiceImplTest {

    private FakeIndexedFileDao indexedFileDao;
    private FakeSearchHistoryDao searchHistoryDao;
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        indexedFileDao = new FakeIndexedFileDao();
        searchHistoryDao = new FakeSearchHistoryDao();
        searchService = new SearchServiceImpl(indexedFileDao, searchHistoryDao);
    }

    private IndexedFile archivo(String nombre, String extension, LocalDateTime fecha) {
        IndexedFile f = new IndexedFile();
        f.setFileName(nombre);
        f.setFilePath("/tmp/" + nombre);
        f.setExtension(extension);
        f.setLastModified(fecha);
        f.setContentSnippet("snippet de " + nombre);
        return f;
    }

    @Test
    void filtraResultadosPorExtension() {
        indexedFileDao.stub = List.of(
                archivo("a.pdf", "pdf", LocalDateTime.now()),
                archivo("b.docx", "docx", LocalDateTime.now())
        );

        List<SearchResult> resultados = searchService.search("contrato", List.of("pdf"), null, null, 10);

        assertEquals(1, resultados.size());
        assertEquals("a.pdf", resultados.get(0).getFileName());
    }

    @Test
    void filtraResultadosPorRangoDeFechas() {
        LocalDateTime viejo = LocalDateTime.now().minusYears(2);
        LocalDateTime reciente = LocalDateTime.now();
        indexedFileDao.stub = List.of(
                archivo("viejo.txt", "txt", viejo),
                archivo("reciente.txt", "txt", reciente)
        );

        List<SearchResult> resultados = searchService.search(
                "texto", null, LocalDateTime.now().minusDays(1), null, 10);

        assertEquals(1, resultados.size());
        assertEquals("reciente.txt", resultados.get(0).getFileName());
    }

    @Test
    void respetaElLimiteMaximoDeResultados() {
        indexedFileDao.stub = List.of(
                archivo("a.txt", "txt", LocalDateTime.now()),
                archivo("b.txt", "txt", LocalDateTime.now()),
                archivo("c.txt", "txt", LocalDateTime.now())
        );

        List<SearchResult> resultados = searchService.search("texto", null, null, null, 2);

        assertEquals(2, resultados.size());
    }

    @Test
    void registraLaBusquedaEnElHistorialConElConteoDeResultados() {
        indexedFileDao.stub = List.of(archivo("a.txt", "txt", LocalDateTime.now()));

        searchService.search("informe anual", List.of("txt"), null, null, 10);

        assertEquals(1, searchHistoryDao.insertados.size());
        SearchHistory registrada = searchHistoryDao.insertados.get(0);
        assertEquals("informe anual", registrada.getQueryText());
        assertEquals(1, registrada.getResultCount());
        assertEquals("txt", registrada.getExtensionFilter());
    }

    /** Fake en memoria de {@link IndexedFileDao}: solo implementa lo usado por el servicio bajo prueba. */
    private static class FakeIndexedFileDao implements IndexedFileDao {
        List<IndexedFile> stub = List.of();

        @Override
        public List<IndexedFile> searchFullText(String query, int limit) {
            return new ArrayList<>(stub);
        }

        @Override public IndexedFile insert(IndexedFile file) { throw new UnsupportedOperationException(); }
        @Override public IndexedFile update(IndexedFile file) { throw new UnsupportedOperationException(); }
        @Override public boolean delete(int id) { throw new UnsupportedOperationException(); }
        @Override public int deleteByFolder(int folderId) { throw new UnsupportedOperationException(); }
        @Override public Optional<IndexedFile> findById(int id) { throw new UnsupportedOperationException(); }
        @Override public List<IndexedFile> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<IndexedFile> findByFolder(int folderId) { throw new UnsupportedOperationException(); }
        @Override public List<String> suggestFileNames(String prefix, int limit) { throw new UnsupportedOperationException(); }
    }

    /** Fake en memoria de {@link SearchHistoryDao} que registra las inserciones para poder verificarlas. */
    private static class FakeSearchHistoryDao implements SearchHistoryDao {
        final List<SearchHistory> insertados = new ArrayList<>();

        @Override
        public SearchHistory insert(SearchHistory entry) {
            insertados.add(entry);
            return entry;
        }

        @Override public List<SearchHistory> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<SearchHistory> findRecent(int limit) { throw new UnsupportedOperationException(); }
        @Override public boolean deleteOlderThan(long days) { throw new UnsupportedOperationException(); }
        @Override public boolean deleteAll() { throw new UnsupportedOperationException(); }
    }
}
