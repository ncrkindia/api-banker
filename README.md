# JAPI - Offline-First API Testing Client (v1.0.0-beta)

JAPI is a lightweight, high-performance, and **completely offline** desktop API client built with Java 21 and Swing. Designed as a privacy-focused and modern alternative to cloud-dependent API testing tools, JAPI lets developers design, run, test, and manage REST requests locally on their machines without any registration, telemetry, or external network dependencies.

> [!NOTE]
> **Pre-Release Beta Version**: This version (v1.0.0-beta) is a pre-release candidate. It features a complete offline toolset, environment resolution, and a JavaScript-based collection runner, and is currently open for public beta testing and feedback.

---

## 🚀 Key Features

### 🔒 Offline-First & Privacy-Focused
* **No Accounts Required**: Use the app instantly without any login, registration, or online verification.
* **Nested Collections & Folders**: Organize requests in unlimited hierarchical subfolders, matching Postman's folder structure.
* **Granular, Readable Filesystem Workspace**: Request collections and environments are saved as individual human-readable JSON files named after their sanitized names (e.g. `my_collection.json`, `local_env.json`) inside your data directory. This makes it trivial to place your workspace data under version control (e.g., Git) to collaborate with team members.
* **Auto-Migration & Compatibility**: JAPI handles legacy workspaces and automatically migrates monolithic `collections.json` and `environments.json` files to the individual file-per-entity scheme upon startup, clean and seamless.
* **Local Security**: None of your variables, credentials, request payloads, or responses leave your machine.

### 🛠️ Core Client Capabilities
* **Robust HTTP Engine**: Powered by an asynchronous Java `HttpClient` wrapper running inside Swing background worker threads to ensure the UI remains fully responsive at all times.
* **Dynamic Parameter Grid**: Grid lists for query parameters, request headers, and request bodies that auto-expand as you type.
* **Authorization Support**: Native support for common auth methods, including **Bearer Token**, **Basic Auth**, and custom **API Key** headers.
* **Per-Request SSL Verification**: Granular control via settings to enable or disable SSL certificate verification for individual requests to support testing environments with self-signed certificates.
* **Flexible SSL Management**: Enabled to trust local self-signed SSL certificates automatically, allowing seamless testing of local development setups (`http://localhost`, etc.).

### ⚙️ Environments & Variables
* **Dynamic Variable Interpolation**: Inject environment and collection variables in double braces (e.g. `{{host}}`) directly into URLs, request headers, query parameters, auth values, and body payloads.
* **Inline Syntax Highlighting**: Dynamic, live variable coloring and highlighting across inputs. 
* **Variable Tooltips**: Hover over variables to instantly view their current resolved values, types, and source (e.g. Current Environment name or Collection name).
* **Header-Aligned Environment Selector**: Instantly switch environments and access the environment manager via the dropdown and gear (`⚙`) button located at the top-right corner of the tab bar.
* **Auto-Applying Renames**: Modifying environment names in the Environment Manager updates the list and file names instantly upon saving without needing manual Rename actions.

### 🧪 Automation, Scripting & Mocking
* **Pre & Post Request Scripts**: Support for scripting to dynamically execute JavaScript code before a request is sent, or parse and assert responses (e.g. capturing tokens from auth responses to set environment variables dynamically).
* **Collection Runner**: Sequence executor to run all requests in a collection sequentially, featuring a progress dashboard and success/failure statistics reporting.
* **History Logging**: Chronological history of executed requests with filter searching.
* **Isolated Performance & Server Logging**: Separates logs for mock server runs and collection execution runs. Log outputs are stored in a configurable folder, populated with parent collection metadata (names, IDs, ports, timestamps) for robust post-execution audits.

### 🎨 Developer Experience (DX)
* **Pinning & Tab Management**: Standard tab controls including Pin/Unpin, Rename, Close others, Close to the left, Close to the right, and Close all.
* **State-Aware tab dirty tracking**: Prompts you to Save, Discard, or Cancel if you try to close tabs containing unsaved modifications.
* **Zoom Support**: Scale the UI, labels, editor font size, and text layouts globally using `Ctrl + +` / `Ctrl + =` (Zoom In) and `Ctrl + -` (Zoom Out).
* **Save Hotkey**: Save your current request, collection state, or mock server configuration instantly using `Ctrl + S`.
* **Postman Interoperability**: Built-in support to import and export collections and environments in standard Postman formats. The importer supports selecting multiple files simultaneously and automatically detects whether they are Collections or Environments to import them seamlessly in a single action.
* **Configurable Storage Directories**: Customize both your local workspace **Data Directory** and **Logs Directory** on-the-fly using the integrated Settings tab and built-in folder browser.

---

## 🛠️ Technology Stack

* **Core Language**: Java 21 (JDK 21+)
* **Build System**: Maven (3.x+)
* **GUI Toolkit**: Java Swing
* **Look and Feel**: FlatLaf (Modern Look and Feel library for Swing)
* **Code/Text Editors**: RSyntaxTextArea (rich editor panels)
* **Serialization/Data Binding**: Google Gson (JSON persistence)

---

## 📦 Getting Started

### Prerequisites

* **Java Development Kit (JDK)**: JDK 21 or higher installed on your system.
* **Apache Maven**: Installed and configured in your path.

### Build and Package

To compile, build, and package the application into a single executable shaded JAR, run:

```bash
mvn clean package -DskipTests
```

This outputs a shaded executable JAR under `target/japi-1.0.0-beta.jar`.

### Run the Application

#### During Development
You can run the application directly from the source directory using Maven:
```bash
mvn compile exec:java -Dexec.mainClass="in.slpro.japi.App"
```

#### Executing the JAR directly
Once built, you can launch the compiled shaded JAR file:
```bash
java -jar target/japi-1.0.0-beta.jar
```

#### Using Launch Scripts (Distribution Bundle)
After building the project, Maven packages a distribution zip at `target/japi.zip`. Unzipping this bundle yields a standalone directory containing the executable jar along with native launch scripts:

* **On Windows**: Double-click `japi.bat` or run:
  ```cmd
  japi.bat
  ```
* **On macOS / Linux**: Grant execution permissions and run `japi.sh`:
  ```bash
  chmod +x japi.sh
  ./japi.sh
  ```

---

## 📁 Project Structure

```
japi/
├── src/main/java/in/slpro/japi/
│   ├── App.java                   # Main application entry point
│   ├── model/                     # Data models (Collection, Request, Environment)
│   ├── storage/                   # StorageManager and AppSettings (JSON persistence)
│   └── ui/                        # Swing Panels, Frames, Editors, and dialogs
├── PROJECT_STATUS.md              # Feature status and roadmap documentation
├── pom.xml                        # Maven configuration dependencies
└── README.md                      # Developer guides and instructions (this file)
```

---

## 📄 License & Privacy

JAPI is completely local, open source, and offline.
* **No Data Collection**: JAPI does not track, collect, or store telemetry, usage logs, or request metrics.
* **Offline Operation**: Rest assured that your API testing data remains private and secure inside your workspace.
