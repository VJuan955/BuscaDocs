package com.buscadocs.service.impl;

import com.buscadocs.dao.FileOpenHistoryDao;
import com.buscadocs.model.FileOpenHistory;
import com.buscadocs.service.FileActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * Implementación de {@link FileActionService} que utiliza la clase
 * {@link Desktop} para abrir archivos con la aplicación predeterminada
 * del sistema operativo.
 *
 * @author VJuan955
 * @version 1.0
 */
public class FileActionServiceImpl implements FileActionService {

    private static final Logger logger = LoggerFactory.getLogger(FileActionServiceImpl.class);
    private final FileOpenHistoryDao historyDao;

    /**
     * Crea una nueva instancia del servicio de acciones sobre archivos.
     *
     * @param historyDao DAO utilizado para almacenar el historial de archivos abiertos.
     */
    public FileActionServiceImpl(FileOpenHistoryDao historyDao) {
        this.historyDao = historyDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
                FileOpenHistory entry = new FileOpenHistory();
                entry.setFilePath(filePath);
                historyDao.insert(entry);
                logger.info("Archivo abierto: {}", filePath);
            } else {
                logger.warn("No se puede abrir el archivo: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("Error al abrir archivo: {}", filePath, e);
        }
    }
}