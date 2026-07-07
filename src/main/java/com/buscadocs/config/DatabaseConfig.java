package com.buscadocs.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuración y administración de la conexión a la base de datos SQLite.
 * <p>
 * Proporciona un punto único de acceso a la base de datos de la aplicación,
 * asegurando que la estructura de tablas esté creada antes de su uso.
 * Implementa el patrón Singleton para mantener una única conexión durante toda la ejecución.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    /**
     * Ruta del archivo de base de datos. Puede sobrescribirse mediante la
     * propiedad de sistema {@code buscadocs.db.url} (por ejemplo, para
     * apuntar a una base de datos temporal durante las pruebas automatizadas),
     * evitando así modificar el archivo real de la aplicación.
     */
    private static final String DB_URL =
            System.getProperty("buscadocs.db.url", "jdbc:sqlite:buscadocs.db");

    private static DatabaseConfig instance;

    /**
     * Constructor privado. Inicializa la conexión y ejecuta el script de creación de tablas si es necesario.
     */
    private DatabaseConfig() {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            logger.info("Conexión a SQLite establecida: {}", DB_URL);
            initializeSchema(connection);
        } catch (SQLException e) {
            logger.error("Error al conectar con la base de datos SQLite", e);
            throw new RuntimeException("No se pudo inicializar la base de datos", e);
        }
    }

    /**
     * Obtiene la instancia única de {@code DatabaseConfig}.
     *
     * @return la instancia singleton.
     */
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }

    /**
     * Devuelve la conexión activa a la base de datos.
     *
     * @return conexión JDBC a SQLite.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Lee el archivo {@code db/schema.sql} desde el classpath y ejecuta las sentencias DDL
     * para crear las tablas necesarias si no existen.
     * <p>
     * Se utiliza un enfoque sencillo: cada sentencia termina en ';' y se ejecuta por separado.
     * </p>
     */
    private void initializeSchema(Connection connection) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró db/schema.sql");
            }
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = connection.createStatement()) {
                for (String sentence : sql.split("@@")) {
                    String trimmed = sentence.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    logger.debug("Ejecutando sentencia:\n{}", trimmed);
                    stmt.execute(trimmed);
                }
            }
            logger.info("Esquema de base de datos inicializado correctamente.");
        } catch (Exception e) {
            logger.error("Error al ejecutar el script de esquema de la base de datos", e);
            throw new RuntimeException("Falló la inicialización del esquema de BD", e);
        }
    }
}