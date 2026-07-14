package com.buscadocs.service.impl;

import com.buscadocs.dao.FolderDao;
import com.buscadocs.dao.IndexedFileDao;
import com.buscadocs.model.Folder;
import com.buscadocs.model.IndexedFile;
import com.buscadocs.service.IndexService;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
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

    /** Divide el filtro de extensiones ingresado por el usuario en tokens individuales. */
    private static final Splitter EXTENSION_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder addFolder(String path, boolean includeHidden) {
        return addFolder(path, includeHidden, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Folder addFolder(String path, boolean includeHidden, String extensionFilter) {
        Preconditions.checkArgument(path != null && !path.isBlank(),
                "La ruta de la carpeta no puede estar vacía");
        Folder folder = new Folder();
        folder.setPath(path);
        folder.setStatus("PENDING");
        folder.setIncludeHidden(includeHidden);
        folder.setExtensionFilter(normalizeExtensionFilter(extensionFilter));
        Folder saved = folderDao.insert(folder);
        logger.info("Carpeta agregada: {} (id={}, filtro de extensiones={})",
                path, saved.getId(), saved.getExtensionFilter());
        startIndexingThread(saved);
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
        startIndexingThread(folder);
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
     * Lanza la indexación de una carpeta en un hilo en segundo plano.
     * <p>
     * Se instala un {@link Thread.UncaughtExceptionHandler} como última capa
     * de defensa: aunque {@link #performIndexing} y {@link #processFile} ya
     * capturan {@code Exception} internamente para no matar el hilo, este
     * manejador registra en el log (en vez de solo imprimir en la consola
     * del sistema, como ocurría antes) cualquier error verdaderamente
     * inesperado que igualmente lograra escapar, y deja la carpeta en estado
     * {@code ERROR} para que no quede atascada indefinidamente en "INDEXING".
     * </p>
     *
     * @param folder carpeta a indexar.
     */
    private void startIndexingThread(Folder folder) {
        Thread thread = new Thread(() -> performIndexing(folder), "indexing-folder-" + folder.getId());
        thread.setUncaughtExceptionHandler((t, e) -> {
            logger.error("Error no controlado durante la indexación de carpeta id={}", folder.getId(), e);
            try {
                folder.setStatus("ERROR");
                folderDao.update(folder);
            } catch (Exception updateError) {
                logger.error("Además, no se pudo actualizar el estado de la carpeta id={} a ERROR",
                        folder.getId(), updateError);
            }
        });
        thread.start();
    }

    /**
     * Normaliza el texto de filtro de extensiones ingresado por el usuario a
     * una forma canónica: minúsculas, sin puntos, sin duplicados y en orden
     * alfabético (ej. ".PDF, docx, PDF" → "docx,pdf"). Se guarda así en la
     * base de datos para que la interfaz siempre muestre un valor consistente.
     *
     * @param raw texto ingresado por el usuario, puede ser {@code null}.
     * @return cadena normalizada, o {@code null} si no queda ninguna extensión válida.
     */
    private String normalizeExtensionFilter(String raw) {
        Set<String> extensions = parseExtensionFilter(raw);
        return extensions.isEmpty() ? null : String.join(",", extensions);
    }

    /**
     * Convierte el filtro de extensiones almacenado (texto separado por comas)
     * en un conjunto de extensiones en minúsculas y sin punto, listo para
     * comparar contra la extensión de cada archivo durante el recorrido.
     *
     * @param raw texto de filtro, puede ser {@code null} o vacío.
     * @return conjunto de extensiones permitidas; vacío significa "sin restricción" (todas).
     */
    private Set<String> parseExtensionFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> result = new TreeSet<>();
        for (String token : EXTENSION_SPLITTER.split(raw)) {
            String normalized = token.startsWith(".") ? token.substring(1) : token;
            if (!normalized.isBlank()) {
                result.add(normalized.toLowerCase());
            }
        }
        return result;
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

            Set<String> allowedExtensions = parseExtensionFilter(folder.getExtensionFilter());
            if (!allowedExtensions.isEmpty()) {
                logger.info("Filtro de extensiones activo para carpeta id={}: {}", folder.getId(), allowedExtensions);
            }

            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!folder.isIncludeHidden() && file.getFileName().toString().startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!allowedExtensions.isEmpty()) {
                        String ext = FilenameUtils.getExtension(file.getFileName().toString()).toLowerCase();
                        if (!allowedExtensions.contains(ext)) {
                            return FileVisitResult.CONTINUE;
                        }
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
        } catch (Exception e) {
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
            } else if ("pptx".equals(extension)) {
                fullContent = extractPptx(filePath);
            } else if ("pdf".equals(extension)) {
                fullContent = extractPdf(filePath);
            } else if ("doc".equals(extension)) {
                fullContent = extractDoc(filePath);
            } else if ("xls".equals(extension)) {
                fullContent = extractXls(filePath);
            } else if ("rtf".equals(extension)) {
                fullContent = extractRtf(filePath);
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
        } catch (Exception e) {
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
            case "txt", "log", "csv", "xml", "json", "html", "htm", "md", "java", "py",
                    "properties", "yaml", "yml", "ini", "sql", "js", "ts", "css", "bat", "sh" -> true;
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

    /**
     * Extrae el texto de todas las diapositivas de una presentación de
     * Microsoft PowerPoint (.pptx), incluyendo notas del orador.
     *
     * @param path ruta del archivo.
     * @return el texto extraído de la presentación.
     * @throws IOException si ocurre un error durante la lectura del archivo.
     */
    private String extractPptx(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             XMLSlideShow slideShow = new XMLSlideShow(is);
             SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
            return extractor.getText();
        }
    }

    /**
     * Extrae el texto de un documento PDF, concatenando todas sus páginas.
     *
     * @param path ruta del archivo.
     * @return el texto extraído del PDF.
     * @throws IOException si ocurre un error durante la lectura o el documento está protegido.
     */
    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                logger.warn("PDF protegido, se omite su contenido: {}", path);
                return null;
            }
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * Extrae el contenido textual de un documento Word en el formato binario
     * antiguo (.doc, Word 97-2003).
     *
     * @param path ruta del archivo.
     * @return el texto extraído del documento.
     * @throws IOException si ocurre un error durante la lectura del archivo.
     */
    private String extractDoc(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /**
     * Extrae el contenido textual de un libro de Excel en el formato binario
     * antiguo (.xls, Excel 97-2003).
     *
     * @param path ruta del archivo.
     * @return el texto extraído del libro.
     * @throws IOException si ocurre un error durante la lectura del archivo.
     */
    private String extractXls(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             HSSFWorkbook workbook = new HSSFWorkbook(is);
             ExcelExtractor extractor = new ExcelExtractor(workbook)) {
            return extractor.getText();
        }
    }

    /**
     * Extrae el texto plano de un documento en formato RTF, usando el editor
     * RTF incluido en el módulo {@code java.desktop} del JDK (sin necesidad
     * de una librería externa adicional).
     *
     * @param path ruta del archivo.
     * @return el texto extraído del documento.
     * @throws IOException si ocurre un error durante la lectura o el archivo no es un RTF válido.
     */
    private String extractRtf(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            RTFEditorKit rtfEditorKit = new RTFEditorKit();
            Document document = rtfEditorKit.createDefaultDocument();
            rtfEditorKit.read(is, document, 0);
            return document.getText(0, document.getLength());
        } catch (BadLocationException e) {
            throw new IOException("Error al extraer texto de archivo RTF: " + path, e);
        }
    }
}