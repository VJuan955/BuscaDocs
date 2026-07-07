package com.buscadocs.dao;

import com.buscadocs.AbstractDatabaseTest;
import com.buscadocs.dao.impl.SQLiteFileOpenHistoryDao;
import com.buscadocs.dao.impl.SQLiteSearchHistoryDao;
import com.buscadocs.model.FileOpenHistory;
import com.buscadocs.model.SearchHistory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración para las operaciones de limpieza ({@code deleteAll})
 * agregadas a {@link SearchHistoryDao} y {@link FileOpenHistoryDao}, usadas
 * por la vista de Historial para el botón "Limpiar".
 *
 * @author VJuan955
 * @version 1.0
 */
class HistoryDaoDeleteAllTest extends AbstractDatabaseTest {

    private final SearchHistoryDao searchHistoryDao = new SQLiteSearchHistoryDao();
    private final FileOpenHistoryDao fileOpenHistoryDao = new SQLiteFileOpenHistoryDao();

    @Test
    void deleteAllVaciaElHistorialDeBusquedas() {
        SearchHistory entry = new SearchHistory();
        entry.setQueryText("consulta-" + System.nanoTime());
        entry.setResultCount(3);
        searchHistoryDao.insert(entry);

        boolean resultado = searchHistoryDao.deleteAll();

        assertTrue(resultado);
        assertTrue(searchHistoryDao.findAll().isEmpty());
    }

    @Test
    void deleteAllVaciaElHistorialDeAperturas() {
        FileOpenHistory entry = new FileOpenHistory();
        entry.setFilePath("/tmp/archivo-" + System.nanoTime());
        fileOpenHistoryDao.insert(entry);

        boolean resultado = fileOpenHistoryDao.deleteAll();

        assertTrue(resultado);
        assertTrue(fileOpenHistoryDao.findAll().isEmpty());
    }
}
