package com.buscadocs.dao.impl;

import com.buscadocs.config.DatabaseConfig;
import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.model.IndexedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link IndexedFileDao} para SQLite.
 * Administra tanto la tabla principal como la tabla virtual FTS5 para búsquedas full‑text.
 *
 * @author VJuan955
 * @version 1.0
 */
public class SQLiteIndexedFileDao implements IndexedFileDao {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteIndexedFileDao.class);
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_SQL = "INSERT INTO indexed_files (folder_id, file_path, file_name, extension, size_bytes, last_modified, content_snippet, full_content) VALUES (?,?,?,?,?,?,?,?)";
    private static final String UPDATE_SQL = "UPDATE indexed_files SET file_path=?, file_name=?, extension=?, size_bytes=?, last_modified=?, content_snippet=?, full_content=? WHERE id=?";
    private static final String DELETE_SQL = "DELETE FROM indexed_files WHERE id=?";
    private static final String SELECT_BY_ID = "SELECT * FROM indexed_files WHERE id=?";
    private static final String SELECT_ALL = "SELECT * FROM indexed_files ORDER BY file_name";
    private static final String SELECT_BY_FOLDER = "SELECT * FROM indexed_files WHERE folder_id=? ORDER BY file_name";
    private static final String DELETE_BY_FOLDER = "DELETE FROM indexed_files WHERE folder_id=?";

    private static final String SEARCH_FTS = "SELECT f.* FROM indexed_files f JOIN files_fts ft ON f.id = ft.rowid WHERE files_fts MATCH ? ORDER BY rank LIMIT ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexedFile insert(IndexedFile file) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, file.getFolderId());
            pstmt.setString(2, file.getFilePath());
            pstmt.setString(3, file.getFileName());
            pstmt.setString(4, file.getExtension());
            pstmt.setLong(5, file.getSizeBytes());
            pstmt.setString(6, file.getLastModified() != null ? file.getLastModified().format(DT_FORMATTER) : null);
            pstmt.setString(7, file.getContentSnippet());
            pstmt.setString(8, file.getFullContent());

            if (pstmt.executeUpdate() == 1) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        file.setId(keys.getInt(1));
                        file.setCreatedAt(LocalDateTime.now());
                    }
                }
                // El trigger FTS se encarga de la inserción en la tabla virtual
                logger.debug("Archivo indexado insertado: id={}, name={}", file.getId(), file.getFileName());
                return file;
            }
        } catch (SQLException e) {
            logger.error("Error al insertar archivo indexado: {}", file.getFileName(), e);
            throw new RuntimeException("Error al insertar archivo", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexedFile update(IndexedFile file) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, file.getFilePath());
            pstmt.setString(2, file.getFileName());
            pstmt.setString(3, file.getExtension());
            pstmt.setLong(4, file.getSizeBytes());
            pstmt.setString(5, file.getLastModified() != null ? file.getLastModified().format(DT_FORMATTER) : null);
            pstmt.setString(6, file.getContentSnippet());
            pstmt.setString(7, file.getFullContent());
            pstmt.setInt(8, file.getId());

            if (pstmt.executeUpdate() == 1) {
                logger.debug("Archivo actualizado: id={}", file.getId());
                return file;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar archivo id={}", file.getId(), e);
            throw new RuntimeException("Error al actualizar archivo", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(int id) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_SQL)) {
            pstmt.setInt(1, id);
            if (pstmt.executeUpdate() == 1) {
                logger.debug("Archivo eliminado: id={}", id);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar archivo id={}", id, e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteByFolder(int folderId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_BY_FOLDER)) {
            pstmt.setInt(1, folderId);
            int deleted = pstmt.executeUpdate();
            logger.debug("{} archivos eliminados de la carpeta id={}", deleted, folderId);
            return deleted;
        } catch (SQLException e) {
            logger.error("Error al eliminar archivos de carpeta id={}", folderId, e);
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<IndexedFile> findById(int id) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar archivo por id={}", id, e);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexedFile> findAll() {
        return findList(SELECT_ALL, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexedFile> findByFolder(int folderId) {
        return findList(SELECT_BY_FOLDER, folderId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IndexedFile> searchFullText(String query, int limit) {
        List<IndexedFile> results = new ArrayList<>();
        // Formatear consulta para FTS5: cada término se puede concatenar con AND/OR, aquí usamos término simple
        String ftsQuery = query.trim().replace(" ", " AND ");
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SEARCH_FTS)) {
            pstmt.setString(1, ftsQuery);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error en búsqueda full-text: {}", query, e);
        }
        return results;
    }

    /**
     * Método de utilidad reutilizable para ejecutar consultas preparadas estructuradas
     * que devuelven listas de entidades {@link IndexedFile}.
     *
     * @param sql      La sentencia SQL estructurada a ejecutar.
     * @param folderId El identificador de la carpeta si la consulta lo requiere; puede ser {@code null}.
     * @return Una {@link List} con los registros procesados; estará vacía si no hay coincidencias o si ocurre un error.
     */
    private List<IndexedFile> findList(String sql, Integer folderId) {
        List<IndexedFile> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (folderId != null) pstmt.setInt(1, folderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al obtener lista de archivos", e);
        }
        return list;
    }

    /**
     * Mapea de forma manual la fila actual de un cursor de resultados a un objeto de dominio.
     * Convierte los tipos nativos de texto ISO-8601 de SQLite de vuelta a objetos {@link LocalDateTime}.
     *
     * @param rs El {@link ResultSet} posicionado sobre una fila válida.
     * @return Un objeto {@link IndexedFile} con sus propiedades mapeadas.
     * @throws SQLException Si ocurre un error al intentar leer los nombres de columnas o tipos de datos del cursor.
     */
    private IndexedFile mapRow(ResultSet rs) throws SQLException {
        IndexedFile f = new IndexedFile();
        f.setId(rs.getInt("id"));
        f.setFolderId(rs.getInt("folder_id"));
        f.setFilePath(rs.getString("file_path"));
        f.setFileName(rs.getString("file_name"));
        f.setExtension(rs.getString("extension"));
        f.setSizeBytes(rs.getLong("size_bytes"));
        String mod = rs.getString("last_modified");
        f.setLastModified(mod != null ? LocalDateTime.parse(mod, DT_FORMATTER) : null);
        f.setContentSnippet(rs.getString("content_snippet"));
        f.setFullContent(rs.getString("full_content"));
        String created = rs.getString("created_at");
        f.setCreatedAt(created != null ? LocalDateTime.parse(created, DT_FORMATTER) : null);
        return f;
    }
}