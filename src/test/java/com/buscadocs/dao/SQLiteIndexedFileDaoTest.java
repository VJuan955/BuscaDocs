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
 * búsqueda de texto completo (FTS5): tanto la sanitización de caracteres
 * especiales como la búsqueda por prefijo (que permite encontrar "informe"
 * al escribir solo "inform"), y las sugerencias de autocompletado de
 * nombres de archivo.
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

    /**
     * Regresión: antes de la corrección, cualquier término con caracteres
     * especiales de la sintaxis de FTS5 (comillas, guiones, dos puntos,
     * asteriscos) rompía la consulta con un {@code SQLException} y el método
     * devolvía silenciosamente una lista vacía. Ahora estos términos se tratan
     * como texto literal y no deben lanzar excepciones.
     */
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

    /**
     * Regresión: antes de este cambio, FTS5 exigía una coincidencia exacta
     * de token, por lo que buscar "inform" no encontraba archivos que
     * contenían "informe". Ahora la búsqueda es por prefijo.
     */
    @Test
    void searchFullTextEncuentraPorPrefijoDePalabra() {
        indexar("reporte.txt", "Este documento contiene información confidencial");

        List<IndexedFile> resultados = indexedFileDao.searchFullText("inform", 10);

        assertEquals(1, resultados.size());
        assertEquals("reporte.txt", resultados.get(0).getFileName());
    }

    @Test
    void suggestFileNamesEncuentraPorPrefijoDelNombre() {
        indexar("informe_anual.txt", "contenido");
        indexar("informe_mensual.txt", "contenido");
        indexar("presupuesto.txt", "contenido");

        List<String> sugerencias = indexedFileDao.suggestFileNames("info", 10);

        assertEquals(2, sugerencias.size());
        assertTrue(sugerencias.contains("informe_anual.txt"));
        assertTrue(sugerencias.contains("informe_mensual.txt"));
    }

    @Test
    void suggestFileNamesRespetaElLimite() {
        indexar("archivo1.txt", "contenido");
        indexar("archivo2.txt", "contenido");
        indexar("archivo3.txt", "contenido");

        List<String> sugerencias = indexedFileDao.suggestFileNames("archivo", 2);

        assertEquals(2, sugerencias.size());
    }

    @Test
    void suggestFileNamesDevuelveListaVaciaParaPrefijoVacio() {
        indexar("archivo.txt", "contenido");

        assertTrue(indexedFileDao.suggestFileNames("", 10).isEmpty());
        assertTrue(indexedFileDao.suggestFileNames(null, 10).isEmpty());
    }

    @Test
    void suggestFileNamesNoLanzaExcepcionConCaracteresEspeciales() {
        indexar("archivo.txt", "contenido");

        assertDoesNotThrow(() -> indexedFileDao.suggestFileNames("\"raro*: -", 10));
    }
}
