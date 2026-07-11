package com.buscadocs;

/**
 * Punto de entrada alternativo para las distribuciones empaquetadas (JAR
 * "fat" e instaladores nativos generados con {@code jpackage}).
 * <p>
 * Cuando una clase que extiende {@link javafx.application.Application} es
 * declarada directamente como {@code Main-Class} de un JAR con todas las
 * dependencias incluidas, la JVM falla con el error
 * <em>"Error: JavaFX runtime components are missing"</em>, porque el
 * lanzador de JavaFX espera un JAR modular en el module-path, no en el
 * classpath plano de un JAR sombreado ("shaded").
 * </p>
 * <p>
 * La solución estándar es delegar el arranque a una clase que <b>no</b>
 * extienda {@code Application}: esta clase cumple ese propósito, invocando
 * a {@link App#main(String[])} sin ser ella misma una aplicación JavaFX.
 * Para desarrollo local con {@code mvn javafx:run} se sigue usando
 * {@link App} directamente, ya que ese plugin sí gestiona el module-path.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Delega el arranque de la aplicación a {@link App#main(String[])}.
     *
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        App.main(args);
    }
}
