# ApiBanker - The Offline-First API Toolkit (v1.1.0-beta)

## 🚀 Getting Started

### 📋 Prerequisites
To build and run ApiBanker from source, you will need the following tools installed on your system:

1. **Java Development Kit (JDK) 21** (or higher).
   * Note: If you intend to build the *Standalone Native Executable*, you **MUST** install **GraalVM JDK 21+**.
2. **Apache Maven** (for building from source).
3. **WiX Toolset v3** (Required ONLY for Windows users building the `.msi` native installer via `jpackage`).
4. **C++ Build Tools** (Required ONLY for building the `.exe` via GraalVM Native Image).
   * *Windows*: Visual Studio C++ Build Tools.
   * *Linux*: `gcc` and `glibc-devel`.
   * *macOS*: Xcode Command Line Tools.

---

## 🛠️ Building & Packaging Options

ApiBanker supports three different distribution formats. Choose the one that fits your needs:

### 1. Standard Portable JAR / ZIP (Default)
This is the standard, cross-platform Java package. It compiles the source code into a standalone `.jar` file.

**Command:**
```bash
mvn clean package
```
**Output:**
* `target/apibanker-1.1.0-beta.jar` (Executable Fat JAR)
* `target/artifacts/apibanker.zip` & `target/artifacts/apibanker.tar.gz` (Portable distributions with launch scripts)

### 2. Professional Native Installers (MSI / PKG / DEB)
This builds a complete standalone setup wizard tailored to your operating system. It uses `jpackage` to bundle a custom, stripped-down JRE along with the application.

**Command:**
```bash
mvn clean verify -DbuildNative
```
*(This automatically activates the correct OS profile: `native-installer-windows`, `native-installer-mac`, or `native-installer-linux`)*

**Output:**
* `target/artifacts/ApiBanker-installer.msi` (Windows)
* `target/artifacts/apibanker.deb` (Linux)
* `target/artifacts/ApiBanker.pkg` (macOS)

### 3. Standalone Native Executable (GraalVM AOT)
This compiles the Java bytecode directly to native machine code for lightning-fast startup times with zero external dependencies.

**Command:**
```bash
mvn clean package -Pgraalvm-native-image
```
**Output:**
* `target/artifacts/ApiBanker.exe` (or `ApiBanker` on Unix)

---

## 💻 Running Locally during Development

To instantly launch the application directly from your IDE or terminal without packaging:

```bash
mvn compile exec:java -Dexec.mainClass="in.slpro.apibanker.App"
```

Alternatively, just run the compiled JAR directly:
```bash
java -jar target/apibanker-1.1.0-beta.jar
```

---

## 📂 Project Structure

```text
apibanker/
├── src/main/java/in/slpro/apibanker/
│   ├── App.java                   # Main entry point and initialization
│   ├── http/                      # Request dispatching, auth, Rhino script engine, JMX parsing
│   ├── model/                     # Data structures (RequestModel, CollectionModel, Settings)
│   ├── storage/                   # JSON persistence, auto-migrations, and environment state
│   └── ui/                        # Swing components, custom syntax highlighters, Collection Runner
├── src/main/resources/            # Application Icons, Fonts, properties, and Native Image configs
├── src/assembly/                  # Distribution script bundle configurations
└── pom.xml                        # Project dependencies (Gson, FlatLaf, RSyntaxTextArea)
```

## 📜 Version History

* **v1.1.0-beta (Current)** - First official release under the ApiBanker rebranding. Includes massive UI/UX improvements, advanced clipboard protection, OpenAPI/Swagger Import with auto-generated Collection README documentation, complete JavaDoc coverage, and Native Executable support.
* *(Legacy: v1.1.0-beta to v1.4.0-beta under JAPI)*.

## 🤝 Contributing
Contributions are highly welcome! Because this is a Swing application, we heavily emphasize keeping external UI libraries to a minimum. 
If you find a bug, visual glitch, or feature request, feel free to open an Issue or PR!
