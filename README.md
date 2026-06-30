# BuscaDocs

**BuscaDocs** es una aplicación de escritorio para indexar carpetas locales y buscar archivos por nombre, extensión y contenido.  
Está construida con **Java 21**, **JavaFX** (FXML + CSS) y **SQLite**, siguiendo los patrones **MVC**, **DAO** y los principios **SOLID**.

![Java](https://img.shields.io/badge/Java-21-blue) ![JavaFX](https://img.shields.io/badge/JavaFX-21.0.11-orange) ![SQLite](https://img.shields.io/badge/SQLite-3.53-brightgreen) ![Maven](https://img.shields.io/badge/Maven-3.9+-red)

---

## Características principales

- **Indexación inteligente** de carpetas (incluye archivos ocultos opcionalmente).
- **Motor de búsqueda full‑text** sobre el contenido de los archivos (texto, Word, Excel).
- **Filtros avanzados**: por extensión, rango de fechas de modificación.
- **Tarjetas de resultado** con snippet del contenido encontrado.
- **Apertura de archivos** con doble clic (usando el programa predeterminado del sistema).
- **Dashboard de métricas** (CPU/RAM) y **consola de logs en tiempo real**.

---

## Arquitectura y patrones

- **MVC (Modelo‑Vista‑Controlador)**  
  Las vistas están definidas en archivos FXML, los controladores en el paquete `controller` y los modelos en `model`.

- **DAO (Data Access Object)**  
  La persistencia se abstrae mediante interfaces DAO, con implementaciones concretas para SQLite.

- **SOLID**  
  Cada clase tiene una única responsabilidad. Las dependencias se invierten (los servicios dependen de interfaces, no de implementaciones). Se usa inyección manual de dependencias desde `AppContext`.

- **Logging** con **SLF4J + Logback**: salida a consola y archivo rotativo (`logs/buscadocs.log`).

---

## Tecnologías utilizadas

| Tecnología            | Versión    | Uso                               |
|-----------------------|------------|-----------------------------------|
| Java                  | 21.0.11    | Lenguaje base                     |
| JavaFX                | 21.0.11    | Interfaz gráfica                  |
| SQLite (JDBC)         | 3.53.2.0   | Base de datos local               |
| Google Guava          | 33.6.0‑jre | Utilidades                        |
| Apache POI            | 5.5.1      | Lectura de archivos .docx y .xlsx |
| Apache Commons IO     | 2.22.0     | Tailer y utilidades de archivos   |
| Apache Commons Lang   | 3.20.0     | Utilidades de texto               |
| Logback               | 1.5.35     | Sistema de logging                |

---

## Requisitos previos

- **Java 21** (JDK) instalado y configurado en `JAVA_HOME`.
- **Maven 3.9+** (opcional si usas el wrapper incluido en el IDE).
- Sistema operativo compatible con JavaFX (Windows, Linux, macOS).

---

## Instalación y ejecución

### 1. Clonar el repositorio
```bash
git clone https://github.com/tuusuario/BuscaDocs.git
cd BuscaDocs
```
### 2. Compilar y ejecutar con Maven
```bash
mvn clean compile
mvn javafx:run
```
### 3. Ejecutar desde el IDE

Importa el proyecto como proyecto Maven en tu IDE (IntelliJ, Eclipse, VS Code) y ejecuta la clase `com.buscadocs.App`.