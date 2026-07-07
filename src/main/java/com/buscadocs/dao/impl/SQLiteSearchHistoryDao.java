package com.buscadocs.dao.impl;

import com.buscadocs.config.DatabaseConfig;
import com.buscadocs.dao.SearchHistoryDao;
import com.buscadocs.model.SearchHistory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link SearchHistoryDao} para el motor de base de datos SQLite.
 * <p>
 * Maneja el almacenamiento estructurado de los criterios de búsqueda, incluyendo
 * filtros de extensión y rangos de fechas opcionales. Utiliza las funciones de
 * modificadores de tiempo de SQLite ({@code datetime}) para tareas de depuración automática.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class SQLiteSearchHistoryDao implements SearchHistoryDao {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteSearchHistoryDao.class);
    private static final DateTimeFormatter DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final String INSERT_SQL = "INSERT INTO search_history (query_text, extension_filter, date_from, date_to, result_count) VALUES (?,?,?,?,?)";
    private static final String SELECT_ALL = "SELECT * FROM search_history ORDER BY searched_at DESC";
    private static final String SELECT_RECENT = "SELECT * FROM search_history ORDER BY searched_at DESC LIMIT ?";
    private static final String DELETE_OLDER = "DELETE FROM search_history WHERE searched_at < datetime('now', ?)";
    private static final String DELETE_ALL = "DELETE FROM search_history";

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchHistory insert(SearchHistory entry) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, entry.getQueryText());
            pstmt.setString(2, entry.getExtensionFilter());
            pstmt.setString(3, entry.getDateFrom() != null ? entry.getDateFrom().format(DT) : null);
            pstmt.setString(4, entry.getDateTo() != null ? entry.getDateTo().format(DT) : null);
            pstmt.setInt(5, entry.getResultCount());

            if (pstmt.executeUpdate() == 1) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        entry.setId(keys.getInt(1));
                        entry.setSearchedAt(LocalDateTime.now());
                    }
                }
                logger.debug("Búsqueda registrada: id={}, query='{}'", entry.getId(), entry.getQueryText());
                return entry;
            }
        } catch (SQLException e) {
            logger.error("Error al insertar historial de búsqueda", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SearchHistory> findAll() {
        return findList(SELECT_ALL, 0, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SearchHistory> findRecent(int limit) {
        return findList(SELECT_RECENT, limit, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteOlderThan(long days) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_OLDER)) {
            pstmt.setString(1, "-" + days + " days");
            int deleted = pstmt.executeUpdate();
            logger.info("Limpieza de historial: {} registros eliminados (más de {} días)", deleted, days);
            return true;
        } catch (SQLException e) {
            logger.error("Error al limpiar historial", e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteAll() {
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(DELETE_ALL);
            logger.info("Historial de búsquedas limpiado: {} registros eliminados", deleted);
            return true;
        } catch (SQLException e) {
            logger.error("Error al limpiar historial de búsquedas", e);
            return false;
        }
    }

    /**
     * Método auxiliar genérico para la centralización y lectura de conjuntos de resultados
     * relacionados con el historial de consultas.
     *
     * @param sql La consulta SQL estructurada a ser ejecutada.
     * @param param El valor numérico de parametrización (ej. límites); ignorado si {@code hasParam} es falso.
     * @param hasParam Define si el {@link PreparedStatement} requiere inyectar el parámetro posicional.
     * @return Una {@link List} estructurada con los registros mapeados.
     */
    private List<SearchHistory> findList(String sql, int param, boolean hasParam) {
        List<SearchHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (hasParam) pstmt.setInt(1, param);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    SearchHistory sh = new SearchHistory();
                    sh.setId(rs.getInt("id"));
                    sh.setQueryText(rs.getString("query_text"));
                    sh.setExtensionFilter(rs.getString("extension_filter"));
                    String df = rs.getString("date_from");
                    sh.setDateFrom(df != null ? LocalDateTime.parse(df, DT) : null);
                    String dt = rs.getString("date_to");
                    sh.setDateTo(dt != null ? LocalDateTime.parse(dt, DT) : null);
                    sh.setResultCount(rs.getInt("result_count"));
                    String sa = rs.getString("searched_at");
                    sh.setSearchedAt(sa != null ? LocalDateTime.parse(sa, DT) : null);
                    list.add(sh);
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener historial de búsquedas", e);
        }
        return list;
    }
}