package com.buscadocs.service;

import com.buscadocs.AbstractDatabaseTest;
import com.buscadocs.dao.FolderDao;
import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.dao.impl.SQLiteFolderDao;
import com.buscadocs.dao.impl.SQLiteIndexedFileDao;
import com.buscadocs.model.Folder;
import com.buscadocs.model.IndexedFile;
import com.buscadocs.service.impl.IndexServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias y de integración para {@link IndexServiceImpl}, con
 * énfasis en el filtro opcional de extensiones a indexar por carpeta: su
 * normalización al guardarse y su efecto real durante el recorrido de
 * archivos en segundo plano.
 *
 * @author VJuan955
 * @version 1.0
 */
class IndexServiceImplTest extends AbstractDatabaseTest {

    private final FolderDao folderDao = new SQLiteFolderDao();
    private final IndexedFileDao indexedFileDao = new SQLiteIndexedFileDao();
    private final IndexService indexService = new IndexServiceImpl(folderDao, indexedFileDao);

    @BeforeEach
    void cleanUp() {
        for (Folder f : folderDao.findAll()) {
            indexedFileDao.deleteByFolder(f.getId());
            folderDao.delete(f.getId());
        }
    }

    @Test
    void addFolderNormalizaElFiltroDeExtensiones() {
        Folder folder = indexService.addFolder("/tmp/no-existe-" + System.nanoTime(), false, " .PDF, docx ,PDF");

        assertEquals("docx,pdf", folder.getExtensionFilter());
    }

    @Test
    void addFolderSinFiltroGuardaNull() {
        Folder folder = indexService.addFolder("/tmp/no-existe-" + System.nanoTime(), false);

        assertNull(folder.getExtensionFilter());
    }

    @Test
    void addFolderConFiltroEnBlancoGuardaNull() {
        Folder folder = indexService.addFolder("/tmp/no-existe-" + System.nanoTime(), false, "   ");

        assertNull(folder.getExtensionFilter());
    }

    @Test
    void indexacionRespetaElFiltroDeExtensiones(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("incluido.txt"), "contenido de texto");
        Files.writeString(tempDir.resolve("excluido.md"), "contenido markdown");

        Folder folder = indexService.addFolder(tempDir.toString(), false, "txt");
        Folder finalState = waitUntilFinished(folder.getId());

        assertEquals("READY", finalState.getStatus());
        List<IndexedFile> indexed = indexedFileDao.findByFolder(folder.getId());
        assertEquals(1, indexed.size());
        assertEquals("incluido.txt", indexed.get(0).getFileName());
    }

    @Test
    void indexacionSinFiltroIndexaTodosLosArchivos(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "contenido");
        Files.writeString(tempDir.resolve("b.md"), "contenido");

        Folder folder = indexService.addFolder(tempDir.toString(), false, null);
        waitUntilFinished(folder.getId());

        List<IndexedFile> indexed = indexedFileDao.findByFolder(folder.getId());
        assertEquals(2, indexed.size());
    }

    /**
     * Espera, sondeando periódicamente, a que la indexación en segundo plano
     * termine (estado {@code READY} o {@code ERROR}), ya que {@code addFolder}
     * dispara el proceso en un hilo aparte y no bloquea la llamada.
     *
     * @param folderId id de la carpeta cuya indexación se espera.
     * @return el estado final de la carpeta.
     */
    private Folder waitUntilFinished(int folderId) {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            Optional<Folder> current = folderDao.findById(folderId);
            if (current.isPresent()
                    && ("READY".equals(current.get().getStatus()) || "ERROR".equals(current.get().getStatus()))) {
                return current.get();
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        fail("La indexación no terminó dentro del tiempo esperado (10s)");
        return null;
    }
}
