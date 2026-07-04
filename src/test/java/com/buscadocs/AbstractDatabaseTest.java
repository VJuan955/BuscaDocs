package com.buscadocs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Clase base para pruebas que necesitan acceso a la base de datos SQLite.
 * <p>
 * Configura, una única vez por ejecución de la suite, una base de datos
 * temporal aislada (mediante la propiedad de sistema {@code buscadocs.db.url}
 * leída por {@link com.buscadocs.config.DatabaseConfig}) para que las pruebas
 * nunca lean ni modifiquen el archivo {@code buscadocs.db} real usado por la
 * aplicación en ejecución.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public abstract class AbstractDatabaseTest {

    static {
        try {
            Path tempDb = Files.createTempFile("buscadocs-test", ".db");
            tempDb.toFile().deleteOnExit();
            System.setProperty("buscadocs.db.url", "jdbc:sqlite:" + tempDb.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la base de datos temporal de pruebas", e);
        }
    }
}
