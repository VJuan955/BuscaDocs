package com.buscadocs.service;

import com.buscadocs.dao.FileOpenHistoryDao;
import com.buscadocs.dao.SearchHistoryDao;
import com.buscadocs.model.FileOpenHistory;
import com.buscadocs.model.SearchHistory;
import com.buscadocs.service.impl.HistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link HistoryServiceImpl}.
 * <p>
 * Verifica que las listas devueltas sean copias defensivas inmutables
 * (implementadas con {@code com.google.common.collect.ImmutableList}) y que
 * las operaciones de limpieza deleguen correctamente en los DAOs.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
class HistoryServiceImplTest {

    private FakeSearchHistoryDao searchHistoryDao;
    private FakeFileOpenHistoryDao fileOpenHistoryDao;
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        searchHistoryDao = new FakeSearchHistoryDao();
        fileOpenHistoryDao = new FakeFileOpenHistoryDao();
        historyService = new HistoryServiceImpl(searchHistoryDao, fileOpenHistoryDao);
    }

    @Test
    void getRecentSearchesDevuelveLosDatosDelDao() {
        SearchHistory entry = new SearchHistory();
        entry.setQueryText("informe");
        searchHistoryDao.recent = List.of(entry);

        List<SearchHistory> resultado = historyService.getRecentSearches(10);

        assertEquals(1, resultado.size());
        assertEquals("informe", resultado.get(0).getQueryText());
    }

    @Test
    void getRecentSearchesDevuelveUnaListaInmutable() {
        searchHistoryDao.recent = List.of(new SearchHistory());

        List<SearchHistory> resultado = historyService.getRecentSearches(10);

        assertThrows(UnsupportedOperationException.class, () -> resultado.add(new SearchHistory()));
    }

    @Test
    void getRecentFileOpensDevuelveUnaListaInmutable() {
        fileOpenHistoryDao.recent = List.of(new FileOpenHistory());

        List<FileOpenHistory> resultado = historyService.getRecentFileOpens(10);

        assertThrows(UnsupportedOperationException.class, () -> resultado.add(new FileOpenHistory()));
    }

    @Test
    void clearSearchHistoryDelegaEnElDao() {
        searchHistoryDao.deleteAllCalled = false;

        boolean resultado = historyService.clearSearchHistory();

        assertTrue(resultado);
        assertTrue(searchHistoryDao.deleteAllCalled);
    }

    @Test
    void clearFileOpenHistoryDelegaEnElDao() {
        fileOpenHistoryDao.deleteAllCalled = false;

        boolean resultado = historyService.clearFileOpenHistory();

        assertTrue(resultado);
        assertTrue(fileOpenHistoryDao.deleteAllCalled);
    }

    /** Fake en memoria de {@link SearchHistoryDao}. */
    private static class FakeSearchHistoryDao implements SearchHistoryDao {
        List<SearchHistory> recent = List.of();
        boolean deleteAllCalled = false;

        @Override public SearchHistory insert(SearchHistory entry) { throw new UnsupportedOperationException(); }
        @Override public List<SearchHistory> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<SearchHistory> findRecent(int limit) { return new ArrayList<>(recent); }
        @Override public boolean deleteOlderThan(long days) { throw new UnsupportedOperationException(); }

        @Override
        public boolean deleteAll() {
            deleteAllCalled = true;
            return true;
        }
    }

    /** Fake en memoria de {@link FileOpenHistoryDao}. */
    private static class FakeFileOpenHistoryDao implements FileOpenHistoryDao {
        List<FileOpenHistory> recent = List.of();
        boolean deleteAllCalled = false;

        @Override public FileOpenHistory insert(FileOpenHistory entry) { throw new UnsupportedOperationException(); }
        @Override public List<FileOpenHistory> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<FileOpenHistory> findRecent(int limit) { return new ArrayList<>(recent); }

        @Override
        public boolean deleteAll() {
            deleteAllCalled = true;
            return true;
        }
    }
}
