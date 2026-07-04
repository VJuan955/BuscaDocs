package com.buscadocs.dao;

import com.buscadocs.AbstractDatabaseTest;
import com.buscadocs.dao.impl.SQLiteFolderDao;
import com.buscadocs.dao.impl.SQLiteIndexedFileDao;
import com.buscadocs.model.Folder;
import com.buscadocs.model.IndexedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link SQLiteIndexedFileDao}, con énfasis en la
 * búsqueda de texto completo (FTS5) tras corregir la construcción de la
 * consulta para que escape caracteres especiales en lugar de concatenarlos
 * directamente en la expresión MATCH.
 *
 * @author VJuan955
 * @version 1.0
 */
class SQLiteIndexedFileDaoTest extends AbstractDatabaseTest {

    private final FolderDao folderDao = new SQLiteFolderDao();
    private final IndexedFileDao indexedFileDao = new SQLiteIndexedFileDao();
    private int folderId;

    @BeforeEach
    void setUp() {
        for (IndexedFile f : indexedFileDao.findAll()) {
            indexedFileDao.delete(f.getId());
        }
        for (Folder f : folderDao.findAll()) {
            folderDao.delete(f.getId());
        }
        Folder folder = new Folder();
        folder.setPath("/tmp/fts-tests-" + System.nanoTime());
        folder.setStatus("READY");
        folderId = folderDao.insert(folder).getId();
    }

    private IndexedFile indexar(String fileName, String contenido) {
        IndexedFile file = new IndexedFile();
        file.setFolderId(folderId);
        file.setFilePath("/tmp/" + fileName);
        file.setFileName(fileName);
        file.setExtension("txt");
        file.setSizeBytes(contenido.length());
        file.setLastModified(LocalDateTime.now());
        file.setFullContent(contenido);
        file.setContentSnippet(contenido);
        return indexedFileDao.insert(file);
    }

    @Test
    void searchFullTextEncuentraPorPalabraSimple() {
        indexar("informe.txt", "Este es un informe financiero anual");

        List<IndexedFile> resultados = indexedFileDao.searchFullText("financiero", 10);

        assertEquals(1, resultados.size());
        assertEquals("informe.txt", resultados.get(0).getFileName());
    }

    @Test
    void searchFullTextCombinaVariosTerminosConAnd() {
        indexar("a.txt", "reporte de ventas mensuales");
        indexar("b.txt", "reporte de gastos mensuales");

        List<IndexedFile> resultados = indexedFileDao.searchFullText("reporte ventas", 10);

        assertEquals(1, resultados.size());
        assertEquals("a.txt", resultados.get(0).getFileName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"C++", "informe-final", "precio: 100", "2024*", "\"comillas\"", "AND OR NOT"})
    void searchFullTextNoLanzaExcepcionConCaracteresEspeciales(String query) {
        indexar("doc.txt", "contenido de prueba");

        assertDoesNotThrow(() -> indexedFileDao.searchFullText(query, 10));
    }

    @Test
    void searchFullTextDevuelveListaVaciaSiNoHayCoincidencias() {
        indexar("doc.txt", "contenido irrelevante");

        List<IndexedFile> resultados = indexedFileDao.searchFullText("palabraQueNoExiste", 10);

        assertTrue(resultados.isEmpty());
    }

    @Test
    void deleteByFolderEliminaSoloLosArchivosDeEsaCarpeta() {
        indexar("uno.txt", "contenido uno");
        indexar("dos.txt", "contenido dos");

        int eliminados = indexedFileDao.deleteByFolder(folderId);

        assertEquals(2, eliminados);
        assertTrue(indexedFileDao.findByFolder(folderId).isEmpty());
    }
}
