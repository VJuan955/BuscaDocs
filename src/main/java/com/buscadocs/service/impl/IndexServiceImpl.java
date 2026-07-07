package com.buscadocs.service.impl;

import com.buscadocs.dao.FolderDao;
import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.model.Folder;
import com.buscadocs.model.IndexedFile;
import com.buscadocs.service.IndexService;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación de {@link IndexService} que indexa archivos en segundo plano.
 * Utiliza java.nio.file para recorrer directorios y Apache POI para extraer texto
 * de documentos Office. Los archivos de texto plano se leen directamente.
 *
 * @author VJuan955
 * @version 1.0
 */
public class IndexServiceImpl implements IndexService {

    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);
    private final FolderDao folderDao;
    private final IndexedFileDao indexedFileDao;

    /**
     * Crea una nueva instancia del servicio de indexación.
     *
     * @param folderDao DAO utilizado para gestionar las carpetas.
     * @param indexedFileDao DAO utilizado para almacenar los archivos indexados.
     */
    public IndexServiceImpl(FolderDao folderDao, IndexedFileDao indexedFileDao) {
        this.folderDao = folderDao;
        this.indexedFileDao = indexedFileDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder addFolder(String path, boolean includeHidden) {
        Preconditions.checkArgument(path != null && !path.isBlank(),
                "La ruta de la carpeta no puede estar vacía");
        Folder folder = new Folder();
        folder.setPath(path);
        folder.setStatus("PENDING");
        folder.setIncludeHidden(includeHidden);
        Folder saved = folderDao.insert(folder);
        logger.info("Carpeta agregada: {} (id={})", path, saved.getId());
        new Thread(() -> performIndexing(saved)).start();
        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder reindexFolder(int folderId) {
        Folder folder = folderDao.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Carpeta no encontrada: " + folderId));
        folder.setStatus("PENDING");
        folderDao.update(folder);
        indexedFileDao.deleteByFolder(folderId);
        logger.info("Reindexando carpeta id={}", folderId);
        new Thread(() -> performIndexing(folder)).start();
        return folder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFolder(int folderId) {
        indexedFileDao.deleteByFolder(folderId);
        folderDao.delete(folderId);
        logger.info("Carpeta id={} eliminada", folderId);
    }

    /**
     * Ejecuta el proceso completo de indexación de una carpeta.
     *
     * El método recorre recursivamente el directorio indicado, procesa cada
     * archivo compatible y actualiza el estado de la carpeta durante el proceso.
     * Si ocurre algún error, el estado de la carpeta cambia a {@code ERROR}.
     *
     * @param folder carpeta que será indexada.
     */
    private void performIndexing(Folder folder) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        AtomicInteger filesIndexed = new AtomicInteger(0);
        try {
            folder.setStatus("INDEXING");
            folderDao.update(folder);
            Path root = Paths.get(folder.getPath());
            if (!Files.isDirectory(root)) {
                folder.setStatus("ERROR");
                folderDao.update(folder);
                logger.error("La ruta no es un directorio: {}", folder.getPath());
                return;
            }

            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!folder.isIncludeHidden() && file.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    processFile(file, folder.getId(), attrs);
                    filesIndexed.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (!folder.isIncludeHidden() && dir.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("No se pudo acceder al archivo: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });

            folder.setStatus("READY");
            folder.setLastIndexed(LocalDateTime.now());
            folderDao.update(folder);
            stopwatch.stop();
            logger.info("Indexación completada para carpeta id={}: {} archivos en {} ms",
                    folder.getId(), filesIndexed.get(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
            logger.error("Error durante la indexación de carpeta id={}", folder.getId(), e);
            folder.setStatus("ERROR");
            folderDao.update(folder);
        }
    }

    /**
     * Procesa un archivo individual para extraer su información y contenido.
     *
     * Dependiendo de su extensión, el archivo puede leerse como texto plano
     * o mediante Apache POI para documentos Office. Posteriormente se almacena
     * en la base de datos junto con sus metadatos.
     *
     * @param filePath ruta del archivo.
     * @param folderId identificador de la carpeta a la que pertenece.
     * @param attrs atributos básicos del archivo.
     */
    private void processFile(Path filePath, int folderId, BasicFileAttributes attrs) {
        try {
            String pathStr = filePath.toString();
            String fileName = filePath.getFileName().toString();
            String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            long size = attrs.size();
            LocalDateTime lastModified = LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());

            String fullContent = null;
            String snippet = null;

            if (isTextExtension(extension)) {
                fullContent = Files.readString(filePath);
            } else if ("docx".equals(extension)) {
                fullContent = extractDocx(filePath);
            } else if ("xlsx".equals(extension)) {
                fullContent = extractXlsx(filePath);
            }

            if (fullContent != null) {
                snippet = fullContent.length() > 500 ? fullContent.substring(0, 500) + "…" : fullContent;
            }

            IndexedFile indexed = new IndexedFile();
            indexed.setFolderId(folderId);
            indexed.setFilePath(pathStr);
            indexed.setFileName(fileName);
            indexed.setExtension(extension);
            indexed.setSizeBytes(size);
            indexed.setLastModified(lastModified);
            indexed.setFullContent(fullContent);
            indexed.setContentSnippet(snippet);

            indexedFileDao.insert(indexed);
            logger.debug("Archivo indexado: {}", fileName);
        } catch (IOException e) {
            logger.warn("Error al leer contenido de: {}", filePath, e);
        }
    }

    /**
     * Determina si una extensión corresponde a un archivo de texto plano.
     *
     * @param ext extensión del archivo sin el punto.
     * @return {@code true} si la extensión representa un archivo de texto; {@code false} en caso contrario.
     */
    private boolean isTextExtension(String ext) {
        return switch (ext) {
            case "txt", "log", "csv", "xml", "json", "html", "md", "java", "py", "properties" -> true;
            default -> false;
        };
    }

    /**
     * Extrae el contenido textual de un documento Microsoft Word (.docx).
     *
     * @param path ruta del archivo.
     * @return el texto extraído del documento.
     * @throws IOException si ocurre un error durante la lectura del archivo.
     */
    private String extractDocx(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /**
     * Extrae el contenido textual de un archivo Microsoft Excel (.xlsx).
     *
     * @param path ruta del archivo.
     * @return el texto extraído del libro de Excel.
     * @throws IOException si ocurre un error durante la lectura del archivo.
     */
    private String extractXlsx(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             XSSFWorkbook workbook = new XSSFWorkbook(is);
             XSSFExcelExtractor extractor = new XSSFExcelExtractor(workbook)) {
            return extractor.getText();
        }
    }
}