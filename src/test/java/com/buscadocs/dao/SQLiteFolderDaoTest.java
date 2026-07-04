package com.buscadocs.dao;

import com.buscadocs.AbstractDatabaseTest;
import com.buscadocs.dao.impl.SQLiteFolderDao;
import com.buscadocs.model.Folder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link SQLiteFolderDao}, verificando las
 * operaciones básicas de creación, actualización, búsqueda y eliminación
 * de carpetas sobre una base de datos SQLite temporal de pruebas.
 *
 * @author VJuan955
 * @version 1.0
 */
class SQLiteFolderDaoTest extends AbstractDatabaseTest {

    private final FolderDao folderDao = new SQLiteFolderDao();

    @BeforeEach
    void cleanUp() {
        for (Folder f : folderDao.findAll()) {
            folderDao.delete(f.getId());
        }
    }

    @Test
    void insertAsignaIdYPersisteLosDatos() {
        Folder folder = new Folder();
        folder.setPath("/tmp/carpeta-prueba-" + System.nanoTime());
        folder.setStatus("PENDING");
        folder.setIncludeHidden(true);

        Folder guardada = folderDao.insert(folder);

        assertTrue(guardada.getId() > 0, "Se debe asignar un ID autogenerado");
        Optional<Folder> encontrada = folderDao.findById(guardada.getId());
        assertTrue(encontrada.isPresent());
        assertEquals(folder.getPath(), encontrada.get().getPath());
        assertEquals("PENDING", encontrada.get().getStatus());
        assertTrue(encontrada.get().isIncludeHidden());
    }

    @Test
    void updateModificaElEstadoDeLaCarpeta() {
        Folder folder = new Folder();
        folder.setPath("/tmp/carpeta-update-" + System.nanoTime());
        folder.setStatus("PENDING");
        Folder guardada = folderDao.insert(folder);

        guardada.setStatus("READY");
        folderDao.update(guardada);

        Optional<Folder> actualizada = folderDao.findById(guardada.getId());
        assertTrue(actualizada.isPresent());
        assertEquals("READY", actualizada.get().getStatus());
    }

    @Test
    void deleteEliminaLaCarpeta() {
        Folder folder = new Folder();
        folder.setPath("/tmp/carpeta-delete-" + System.nanoTime());
        folder.setStatus("PENDING");
        Folder guardada = folderDao.insert(folder);

        boolean eliminada = folderDao.delete(guardada.getId());

        assertTrue(eliminada);
        assertTrue(folderDao.findById(guardada.getId()).isEmpty());
    }

    @Test
    void findByPathEncuentraLaCarpetaCorrecta() {
        String path = "/tmp/carpeta-buscar-" + System.nanoTime();
        Folder folder = new Folder();
        folder.setPath(path);
        folder.setStatus("PENDING");
        folderDao.insert(folder);

        Optional<Folder> encontrada = folderDao.findByPath(path);

        assertTrue(encontrada.isPresent());
        assertEquals(path, encontrada.get().getPath());
    }

    @Test
    void findByPathDevuelveVacioSiNoExiste() {
        Optional<Folder> encontrada = folderDao.findByPath("/ruta/que/no/existe/" + System.nanoTime());
        assertTrue(encontrada.isEmpty());
    }
}
