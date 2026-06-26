package com.buscadocs;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicación BuscaDocs.
 * <p>
 * Inicializa la interfaz gráfica JavaFX y carga la ventana principal definida en el archivo FXML.
 * Sigue el patrón MVC: la vista está en main.fxml, el controlador asociado es {@code MainController}
 * y los modelos se encuentran en el paquete {@code com.buscadocs.model}.
 * </p>
 *
 * @author VJuan955
 * @version 1.0
 */
public class App extends Application {

    /**
     * Método de entrada de JavaFX. Carga la escena principal y muestra la ventana.
     *
     * @param primaryStage el escenario principal proporcionado por el framework.
     * @throws Exception si ocurre un error al cargar el archivo FXML.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/buscadocs/view/main.fxml"));
        Scene scene = new Scene(loader.load(), 1024, 768);
        scene.getStylesheets().add(getClass().getResource("/com/buscadocs/css/styles.css").toExternalForm());
        primaryStage.setTitle("BuscaDocs - Búsqueda inteligente de documentos");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Lanza la aplicación.
     *
     * @param args argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        launch(args);
    }
}