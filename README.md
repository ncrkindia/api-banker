# JAPI - Offline-First API Testing Client

JAPI is a lightweight, high-performance, and **completely offline** desktop API client built with Java 21 and Swing. Designed as a privacy-focused and modern alternative to cloud-dependent API testing tools, JAPI lets developers design, run, test, and manage REST requests locally on their machines without any registration, telemetry, or external network dependencies.

---

## 🚀 Key Features

### 🔒 Offline-First & Privacy-Focused
* **No Accounts Required**: Use the app instantly without any login, registration, or online verification.
* **Readable Filesystem Workspace**: All request collections, environments, scripts, and history items are stored as human-readable JSON files. This makes it trivial to place your workspace data under version control (e.g. Git) to collaborate with team members.
* **Local Security**: None of your variables, credentials, request payloads, or responses leave your machine.

### 🛠️ Core Client Capabilities
* **Robust HTTP Engine**: Powered by an asynchronous Java `HttpClient` wrapper running inside Swing background worker threads to ensure the UI remains fully responsive at all times.
* **Dynamic Parameter Grid**: Grid lists for query parameters, request headers, and request bodies that auto-expand as you type.
* **Authorization Support**: Native support for common auth methods, including **Bearer Token**, **Basic Auth**, and custom **API Key** headers.
* **Flexible SSL Management**: Enabled to trust local self-signed SSL certificates automatically, allowing seamless testing of local development setups (`http://localhost`, etc.).

### ⚙️ Environments & Variables
* **Dynamic Variable Interpolation**: Inject environment and collection variables in double braces (e.g. `{{host}}`) directly into URLs, request headers, query parameters, auth values, and body payloads.
* **Inline Syntax Highlighting**: Dynamic, live variable coloring and highlighting across inputs. 
* **Variable Tooltips**: Hover over variables to instantly view their current resolved values, types, and source (e.g. Current Environment name or Collection name).
* **Header-Aligned Environment Selector**: Instantly switch environments and access the environment manager via the dropdown and gear (`⚙`) button located at the top-right corner of the tab bar.

### 🧪 Automation & Scripting
* **Pre & Post Request Scripts**: Support for scripting to dynamically execute JavaScript code before a request is sent, or parse and assert responses (e.g. capturing tokens from auth responses to set environment variables dynamically).
* **Collection Runner**: Sequence executor to run all requests in a collection sequentially, featuring a progress dashboard and success/failure statistics reporting.
* **History Logging**: Chronological history of executed requests with filter searching.

### 🎨 Developer Experience (DX)
* **Pinning & Tab Management**: Standard tab controls including Pin/Unpin, Rename, Close others, Close to the left, Close to the right, and Close all.
* **State-Aware tab dirty tracking**: Prompts you to Save, Discard, or Cancel if you try to close tabs containing unsaved modifications.
* **Zoom Support**: Scale the UI, labels, editor font size, and text layouts globally using `Ctrl + +` / `Ctrl + =` (Zoom In) and `Ctrl + -` (Zoom Out).
* **Save Hotkey**: Save your current request or collection state instantly using `Ctrl + S`.
* **Postman Interoperability**: Built-in support to import and export collections and environments in standard Postman formats.

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

This outputs a shaded executable JAR under `target/japi-1.0.0.jar`.

### Run the Application

Execute the compiled shaded JAR:

```bash
java -jar target/japi-1.0.0.jar
```

Alternatively, run directly during development:

```bash
mvn compile exec:java -Dexec.mainClass="in.slpro.japi.App"
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
