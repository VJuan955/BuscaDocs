package com.buscadocs.dao.impl;

import com.buscadocs.config.DatabaseConfig;
import com.buscadocs.dao.FileOpenHistoryDao;
import com.buscadocs.model.FileOpenHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link FileOpenHistoryDao} para la base de datos SQLite.
 * <p>
 * Centraliza las transacciones relacionales referentes a las interacciones del usuario
 * con los archivos encontrados. Al igual que el resto de los DAOs del ecosistema,
 * gestiona de manera transparente el parseo bidireccional entre {@link LocalDateTime}
 * de Java y cadenas formateadas en ISO-8601 exigidas por el motor SQLite.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class SQLiteFileOpenHistoryDao implements FileOpenHistoryDao {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteFileOpenHistoryDao.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_SQL = "INSERT INTO file_open_history (file_path) VALUES (?)";
    private static final String SELECT_ALL = "SELECT * FROM file_open_history ORDER BY opened_at DESC";
    private static final String SELECT_RECENT = "SELECT * FROM file_open_history ORDER BY opened_at DESC LIMIT ?";
    private static final String DELETE_ALL = "DELETE FROM file_open_history";

    /**
     * {@inheritDoc}
     */
    @Override
    public FileOpenHistory insert(FileOpenHistory entry) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, entry.getFilePath());
            if (pstmt.executeUpdate() == 1) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        entry.setId(keys.getInt(1));
                        entry.setOpenedAt(LocalDateTime.now());
                    }
                }
                logger.debug("Apertura registrada: {}", entry.getFilePath());
                return entry;
            }
        } catch (SQLException e) {
            logger.error("Error al registrar apertura de archivo", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileOpenHistory> findAll() {
        return findList(SELECT_ALL, 0, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FileOpenHistory> findRecent(int limit) {
        return findList(SELECT_RECENT, limit, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteAll() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(DELETE_ALL);
            logger.info("Historial de aperturas limpiado: {} registros eliminados", deleted);
            return true;
        } catch (SQLException e) {
            logger.error("Error al limpiar historial de aperturas", e);
            return false;
        }
    }

    /**
     * Método auxiliar unificado para realizar la lectura de conjuntos de registros
     * mapeados a partir de sentencias preparadas de historial de aperturas.
     *
     * @param sql Consulta SQL estructurada de selección.
     * @param param Número entero utilizado para limitar la consulta; ignorado si {@code hasParam} es falso.
     * @param hasParam Determina si la consulta requiere inyectar dinámicamente un parámetro de control posicional.
     * @return Una {@link List} mutable con las entidades {@link FileOpenHistory} procesadas.
     */
    private List<FileOpenHistory> findList(String sql, int param, boolean hasParam) {
        List<FileOpenHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasParam) pstmt.setInt(1, param);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FileOpenHistory fh = new FileOpenHistory();
                    fh.setId(rs.getInt("id"));
                    fh.setFilePath(rs.getString("file_path"));
                    String opened = rs.getString("opened_at");
                    fh.setOpenedAt(opened != null ? LocalDateTime.parse(opened, DT) : null);
                    list.add(fh);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener historial de aperturas", e);
        }
        return list;
    }
}