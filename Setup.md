# ApiBanker - The Offline-First API Toolkit (v1.0.0-beta)

## 🚀 Getting Started

### Prerequisites
- **Java 21** (or higher) is required.
- Apache Maven (for building from source).

### Building from Source

To compile and package ApiBanker into a standalone, distributable offline bundle:

```bash
mvn clean package
```

This outputs a shaded executable JAR under `target/apibanker-1.0.0-beta.jar`.

### Running Locally during Development

To instantly launch the application directly from your IDE or terminal without packaging:

```bash
mvn compile exec:java -Dexec.mainClass="in.slpro.apibanker.App"
```

Alternatively, just run the compiled JAR directly:
```bash
java -jar target/apibanker-1.0.0-beta.jar
```

### 📦 Distribution Artifacts
After building the project, Maven packages a distribution zip at `target/apibanker.zip`. Unzipping this bundle yields a standalone directory containing the executable jar along with native launch scripts:

* **On Windows**: Double-click `apibanker.bat` or run:
  ```cmd
  apibanker.bat
  ```
* **On macOS / Linux**: Grant execution permissions and run `apibanker.sh`:
  ```bash
  chmod +x apibanker.sh
  ./apibanker.sh
  ```

## 📂 Project Structure

```text
apibanker/
├── src/main/java/in/slpro/apibanker/
│   ├── App.java                   # Main entry point and initialization
│   ├── http/                      # Request dispatching, auth, Rhino script engine, JMX parsing
│   ├── model/                     # Data structures (RequestModel, CollectionModel, Settings)
│   ├── storage/                   # JSON persistence, auto-migrations, and environment state
│   └── ui/                        # Swing components, custom syntax highlighters, Collection Runner
├── src/main/resources/            # Application Icons, Fonts, properties
├── src/assembly/                  # Distribution script bundle configurations
└── pom.xml                        # Project dependencies (Gson, FlatLaf, RSyntaxTextArea)
```

## 📜 Version History

* **v1.0.0-beta (Current)** - First official release under the ApiBanker rebranding. Includes massive UI/UX improvements to Request Headers (auto-calculated `Host`, `Content-Length`, `Content-Type`), advanced clipboard protection for read-only rows, reversed Log Consoles for immediate latest-entry visibility, OpenAPI/Swagger Import with auto-generated Collection README documentation, and 100% complete JavaDoc coverage across the codebase.
* *(Legacy: v1.1.0-beta to v1.4.0-beta under JAPI)*.

## 🤝 Contributing
Contributions are highly welcome! Because this is a Swing application, we heavily emphasize keeping external UI libraries to a minimum. 
If you find a bug, visual glitch, or feature request, feel free to open an Issue or PR!
