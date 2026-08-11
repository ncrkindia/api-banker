# ApiBanker - The Offline-First API Toolkit (v1.5.0-beta)

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
- **Dynamic Variables**: Manage State via `apibanker.globals`, `apibanker.environment`, and `apibanker.collectionVariables`. When using `{{variableName}}` syntax, the resolution precedence is: **Environment > Collection > Global**. 
- **Chaining**: Extract data from responses and pass it into subsequent requests effortlessly.
- **Snippets**: Automatically insert scripts using the built-in UI Code Snippets sidebar.

### 📊 Performance & Load Testing
- **Collection Runner**: Execute batch API requests with either **Fixed Iterations** or **Fixed Duration** (Sec/Min/Hours/Days) limiters. 
- **Concurrency Control**: Adjust **Virtual Users (VUsers)** and **Ramp-up** timers for realistic load simulation.
- **Real-Time Scatter Plots**: Monitor live metrics (Pass/Fail, Response Times, APDEX) drawn on a live UI chart.
- **Reporting**: Export massive execution runs instantly to HTML Dashboards, CSV, or PDF formats.

### 🔄 Data & Interoperability
- **Postman v2.1 Support**: Native, lossless Import and Export to standard Postman Collections and Environments. 
- **JMeter Support**: Export your ApiBanker Collections directly to Apache JMeter `.jmx` files. Load settings mapping directly over: **VUsers** $\rightarrow$ `ThreadGroup.num_threads`, **Ramp-up** $\rightarrow$ `ThreadGroup.ramp_time`, and **Duration** $\rightarrow$ `ThreadGroup.scheduler` & `duration`.
- **OpenAPI / Swagger Import**: Import any OpenAPI 3.x or Swagger 2.x specification (JSON or YAML) directly as a fully structured Collection. Selectively pick which endpoints to import, choose how the Base URL is stored (inline in request URL or as a `{{baseUrl}}` Collection Variable), and automatically generate professional API documentation in the Collection's README from the spec's servers, security schemes, endpoint summaries, parameters, and request bodies.
- **Data Generator Tools**: Generate thousands of rows of realistic Mock JSON Data for testing endpoints or validate existing responses against JSON Schema Drafts and XML XSDs.

### 🔒 100% Offline & Local Storage
- **File-System First**: All your workspaces, environments, and history logs are serialized into clean `.json` files inside `~/.apibanker`. 
- **Action Audit Logging**: Keeps a meticulous, auto-rotating daily audit log of critical app operations (saved as `action_audit_<YYYY-MM-DD>.log` limited to 10MB). Toggleable in settings. Tracks comprehensive source, status, and destination metadata for the following standard actions:
  - `APP_START` / `APP_CLOSE`
  - `IMPORT_COLLECTION` / `IMPORT_ENVIRONMENT` / `IMPORT_OPENAPI` / `IMPORT_FAILED`
  - `EXPORT_COLLECTION` / `EXPORT_ENVIRONMENT` / `EXPORT_REPORT` / `EXPORT_LOGS`
  - `DELETE_COLLECTION` / `DELETE_ENVIRONMENT`
  - `COPY_COLLECTION` / `COPY_ENVIRONMENT`
- **No Cloud Required**: No accounts, no data syncing to external servers, and absolutely no telemetry.

---


## 📦 Installation & Distribution

ApiBanker is distributed in three distinct formats depending on your needs. Download the latest version from our [Releases Page](https://github.com/ncrkindia/api-banker/releases).

1. **Classic Portable Bundle (`.zip` / `.tar.gz`)**
   - **Description**: A highly portable archive containing the main executable JAR and simple `.bat`/`.sh` launcher scripts.
   - **Best For**: Users who already have Java 21+ installed and want to run the app directly from any folder or USB drive.
   - **Download**: [apibanker.zip](https://github.com/ncrkindia/api-banker/releases/download/v1.5.0-beta/apibanker.zip)

2. **Professional Native Installer (`.msi`)**
   - **Description**: A full standalone Windows installation wizard that bundles a stripped-down JRE along with the application.
   - **Best For**: End-users who want a standard setup experience (installing to `Program Files`, creating shortcuts) and do NOT have Java installed.
   - **Download**: [ApiBanker-installer.msi](https://github.com/ncrkindia/api-banker/releases/download/v1.5.0-beta/ApiBanker-installer-1.0.msi)

---

## 🤝 Contributing
Contributions are highly welcome! Because this is a Swing application, we heavily emphasize keeping external UI libraries to a minimum. 
If you find a bug, visual glitch, or feature request, feel free to open an Issue or PR!
