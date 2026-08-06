# ApiBanker - The Offline-First API Toolkit (v1.0.0-beta)

**ApiBanker** (formerly JAPI) is a lightweight, high-performance, and **completely offline** desktop API client built with Java 21 and Swing. Designed as a privacy-focused and modern alternative to cloud-dependent API testing tools, ApiBanker lets developers design, run, test, and manage REST/GraphQL/WebSocket requests locally on their machines without any registration, telemetry, or external network dependencies.

## ✨ Features & Capabilities

ApiBanker brings enterprise-grade API tools to your local environment without the bloat.

### 🌐 Core Request Engine
- **Full Protocol Support**: Seamlessly build REST, GraphQL, and live WebSocket connections.
- **Advanced Authentication**: First-class support for OAuth 2.0 (with automatic token extraction), Bearer tokens, API Keys, and Basic Auth.
- **Auth Inheritance**: Define Authentication at the Collection or Folder level, and watch it recursively inherit down to all nested requests.
- **Dynamic Body Formats**: Build Payloads in raw JSON, XML, HTML, `x-www-form-urlencoded`, `form-data` (with file upload support), or native GraphQL (with Schema Introspection).

### ⚡ Scripting & Automation
- **JavaScript Engine**: Write Pre-Request and Test scripts using Rhino JS.
- **Dynamic Variables**: Manage State via `apibanker.globals`, `apibanker.environment`, and `apibanker.collectionVariables`. 
- **Chaining**: Extract data from responses and pass it into subsequent requests effortlessly.
- **Snippets**: Automatically insert scripts using the built-in UI Code Snippets sidebar.

### 📊 Performance & Load Testing
- **Collection Runner**: Execute batch API requests with configurable iterations, virtual users (VUsers), and delay strategies.
- **Real-Time Scatter Plots**: Monitor live metrics (Pass/Fail, Response Times, APDEX) drawn on a live UI chart.
- **Reporting**: Export massive execution runs instantly to HTML Dashboards, CSV, or PDF formats.

### 🔄 Data & Interoperability
- **Postman v2.1 Support**: Native, lossless Import and Export to standard Postman Collections and Environments. 
- **JMeter Support**: Export your ApiBanker Collections directly to Apache JMeter `.jmx` files for distributed load testing.
- **Data Generator Tools**: Generate thousands of rows of realistic Mock JSON Data for testing endpoints or validate existing responses against JSON Schema Drafts and XML XSDs.

### 🔒 100% Offline & Local Storage
- **File-System First**: All your workspaces, environments, and history logs are serialized into clean `.json` files inside `~/.apibanker`. 
- **No Cloud Required**: No accounts, no data syncing to external servers, and absolutely no telemetry.

---

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

* **v1.0.0-beta (Current)** - First official release under the ApiBanker rebranding. Includes massive UI/UX improvements to Request Headers (auto-calculated `Host`, `Content-Length`, `Content-Type`), advanced clipboard protection for read-only rows, reversed Log Consoles for immediate latest-entry visibility, and 100% complete JavaDoc coverage across the codebase.
* *(Legacy: v1.1.0-beta to v1.4.0-beta under JAPI)*.

## 🤝 Contributing
Contributions are highly welcome! Because this is a Swing application, we heavily emphasize keeping external UI libraries to a minimum. 
If you find a bug, visual glitch, or feature request, feel free to open an Issue or PR!
