package com.buscadocs.dao.impl;

import com.buscadocs.config.DatabaseConfig;
import com.buscadocs.dao.FolderDao;
import com.buscadocs.model.Folder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link FolderDao} para base de datos SQLite.
 * <p>
 * Utiliza consultas parametrizadas y maneja la conversión de fechas.
 * Obtiene la conexión desde {@link DatabaseConfig#getInstance()}.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class SQLiteFolderDao implements FolderDao {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteFolderDao.class);
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_SQL = "INSERT INTO folders (path, status, last_indexed, include_hidden) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL = "UPDATE folders SET path=?, status=?, last_indexed=?, include_hidden=?, updated_at=strftime('%Y-%m-%dT%H:%M:%S','now') WHERE id=?";
    private static final String DELETE_SQL = "DELETE FROM folders WHERE id=?";
    private static final String SELECT_BY_ID = "SELECT * FROM folders WHERE id=?";
    private static final String SELECT_BY_PATH = "SELECT * FROM folders WHERE path=?";
    private static final String SELECT_ALL = "SELECT * FROM folders ORDER BY path";
    private static final String SELECT_BY_STATUS = "SELECT * FROM folders WHERE status=? ORDER BY path";

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder insert(Folder folder) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, folder.getPath());
            pstmt.setString(2, folder.getStatus());
            pstmt.setString(3, folder.getLastIndexed() != null ? folder.getLastIndexed().format(DT_FORMATTER) : null);
            pstmt.setInt(4, folder.isIncludeHidden() ? 1 : 0);

            int affected = pstmt.executeUpdate();
            if (affected == 1) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        folder.setId(generatedKeys.getInt(1));
                        // Recuperamos created_at y updated_at si se necesitan
                        folder.setCreatedAt(LocalDateTime.now());
                        folder.setUpdatedAt(LocalDateTime.now());
                    }
                }
                logger.debug("Carpeta insertada: id={}, path={}", folder.getId(), folder.getPath());
                return folder;
            }
        } catch (SQLException e) {
            logger.error("Error al insertar carpeta: {}", folder.getPath(), e);
            throw new RuntimeException("Error al insertar carpeta", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder update(Folder folder) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, folder.getPath());
            pstmt.setString(2, folder.getStatus());
            pstmt.setString(3, folder.getLastIndexed() != null ? folder.getLastIndexed().format(DT_FORMATTER) : null);
            pstmt.setInt(4, folder.isIncludeHidden() ? 1 : 0);
            pstmt.setInt(5, folder.getId());

            int affected = pstmt.executeUpdate();
            if (affected == 1) {
                folder.setUpdatedAt(LocalDateTime.now());
                logger.debug("Carpeta actualizada: id={}", folder.getId());
                return folder;
            }
        } catch (SQLException e) {
            logger.error("Error al actualizar carpeta con id={}", folder.getId(), e);
            throw new RuntimeException("Error al actualizar carpeta", e);
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
            int affected = pstmt.executeUpdate();
            if (affected == 1) {
                logger.debug("Carpeta eliminada: id={}", id);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error al eliminar carpeta con id={}", id, e);
            throw new RuntimeException("Error al eliminar carpeta", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Folder> findById(int id) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_ID)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFolder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar carpeta por id={}", id, e);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Folder> findByPath(String path) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_BY_PATH)) {
            pstmt.setString(1, path);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFolder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar carpeta por path={}", path, e);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Folder> findAll() {
        return findList(SELECT_ALL, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Folder> findByStatus(String status) {
        return findList(SELECT_BY_STATUS, status);
    }

    /**
     * Método auxiliar para ejecutar consultas que devuelven listas.
     *
     * @param sql Sentencia SQL.
     * @param param Parámetro único (puede ser null si no tiene parámetros).
     * @return Lista de carpetas.
     */
    private List<Folder> findList(String sql, String param) {
        List<Folder> folders = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (param != null) {
                pstmt.setString(1, param);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    folders.add(mapRowToFolder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener lista de carpetas", e);
        }
        return folders;
    }

    /**
     * Convierte una fila de ResultSet en un objeto Folder.
     *
     * @param rs ResultSet posicionado en una fila válida.
     * @return Objeto Folder poblado.
     * @throws SQLException Si hay error de acceso a columnas.
     */
    private Folder mapRowToFolder(ResultSet rs) throws SQLException {
        Folder f = new Folder();
        f.setId(rs.getInt("id"));
        f.setPath(rs.getString("path"));
        f.setStatus(rs.getString("status"));
        String lastIndexed = rs.getString("last_indexed");
        f.setLastIndexed(lastIndexed != null ? LocalDateTime.parse(lastIndexed, DT_FORMATTER) : null);
        f.setIncludeHidden(rs.getInt("include_hidden") == 1);
        String createdAt = rs.getString("created_at");
        f.setCreatedAt(createdAt != null ? LocalDateTime.parse(createdAt, DT_FORMATTER) : null);
        String updatedAt = rs.getString("updated_at");
        f.setUpdatedAt(updatedAt != null ? LocalDateTime.parse(updatedAt, DT_FORMATTER) : null);
        return f;
    }
}