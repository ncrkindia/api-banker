<img src="src/main/resources/banner.png" alt="ApiBanker Banner" width="100%">

# ApiBanker - The Offline-First API Toolkit (v2.0.1)

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

ApiBanker supports three different distribution formats. Choose the one that fits your needs.

> **Recommended:** Use the provided `build.bat` (Windows) or `build.sh` (Linux/macOS) scripts at the project root for a guided, flag-driven build experience.

---

### ⚡ Quick Build (Recommended)

The `build.bat` / `build.sh` scripts at the project root perform a **complete, two-phase build** in one command:
1. **Phase 1** — Runs `mvn clean package` to produce the executable JAR and portable ZIP bundle.
2. **Phase 2** — Calls `scripts/build-installers.bat` (or `.sh`) to produce the platform-native installer (`.msi` on Windows, `.deb`/`.rpm` on Linux, `.dmg`/`.pkg` on macOS).

#### Windows

```bat
:: Full build: JAR + ZIP + .msi installer
build.bat

:: Full build, run with tests
build.bat --with-tests

:: Full build, use mvn install
build.bat --install
```

#### Linux / macOS

```bash
# Make executable on first use
chmod +x build.sh

# Full build: JAR + ZIP + platform native installer
./build.sh

# Full build, run with tests
./build.sh --with-tests
```

**Output location:** `target/artifacts/`

> **Note:** Building the `.msi` installer on Windows requires **WiX Toolset v3** and **jpackage** (JDK 14+) in your PATH.

### 1. Standard Portable JAR / ZIP (Manual Maven)
This is the standard, cross-platform Java package. It compiles the source code into a standalone `.jar` file.

**Command:**
```bash
mvn clean package
```
**Output:**
* `target/apibanker-2.0.1.jar` (Executable Fat JAR)
* `target/artifacts/apibanker.zip` & `target/artifacts/apibanker.tar.gz` (Portable distributions with launch scripts)

---

### 2. Professional Native Installers (MSI / PKG / DEB)
This builds a complete standalone setup wizard tailored to your operating system. It uses `jpackage` to bundle a custom, stripped-down JRE along with the application.

**Command (using build script — recommended):**
```bash
build.bat --native       # Windows
./build.sh --native      # Linux / macOS
```

**Command (manual Maven):**
```bash
# Windows
mvn clean package -P native-installer-windows

# Linux
mvn clean package -P native-installer-linux

# macOS
mvn clean package -P native-installer-mac
```

**Output:**
* `target/artifacts/ApiBanker-installer.msi` (Windows)
* `target/artifacts/apibanker.deb` (Linux)
* `target/artifacts/ApiBanker.pkg` (macOS)

---

### 3. Standalone Native Executable (GraalVM AOT)
This compiles the Java bytecode directly to native machine code for lightning-fast startup times with zero external dependencies.

**Command (using build script — recommended):**
```bash
build.bat --graal        # Windows
./build.sh --graal       # Linux / macOS
```

**Command (manual Maven):**
```bash
mvn clean package -P graalvm-native-image
```
**Output:**
* `target/artifacts/ApiBanker.exe` (or `ApiBanker` on Unix)

---

## 💻 Running Locally during Development

To instantly launch the application directly from your IDE or terminal without packaging:

```bash
mvn compile exec:java -Dexec.mainClass="in.slpro.apibanker.App"
```

Alternatively, run the compiled JAR directly:
```bash
java -jar target/apibanker-2.0.1.jar
```

---

## 📂 Project Structure

```text
apibanker/
├── build.bat                      # Windows build script (all profiles)
├── build.sh                       # Linux/macOS build script (all profiles)
├── pom.xml                        # Project dependencies (Gson, FlatLaf, RSyntaxTextArea)
├── docs/                          # Comprehensive documentation (UserGuide.md, Features.md, AboutUs.md, release_plan.md, init.md, PROJECT_STATUS.md)
├── scripts/
│   ├── apibanker.bat              # Windows launcher script (bundled in ZIP)
│   ├── apibanker.sh               # Linux/macOS launcher script (bundled in ZIP)
│   ├── build-installers.bat       # Legacy standalone Windows jpackage script
│   ├── build-installers.sh        # Legacy standalone Linux/macOS jpackage script
│   ├── release.sh                 # Git release/tag helper (auto-reads version from pom.xml)
│   └── diff.sh                    # Git diff helper between a version tag and develop (with reusable AI release prompt)
├── src/main/java/in/slpro/apibanker/
│   ├── App.java                   # Main entry point and initialization
│   ├── http/                      # Request dispatching, auth, Rhino script engine, JMX parsing
│   ├── model/                     # Data structures (RequestModel, CollectionModel, Settings)
│   ├── storage/                   # JSON persistence, auto-migrations, and environment state
│   └── ui/                        # Swing components, custom syntax highlighters, Collection Runner
├── src/main/resources/            # Application Icons, Fonts, properties, and Native Image configs
└── src/assembly/                  # Distribution script bundle configurations
```

---

## 📜 Version History

* **v2.0.1 (Current)** — First stable release. Professionalized branding, added dynamic console zoom synchronization, hardened network error handling, and robust unresolved variable execution safeties.
* **v1.5.0-beta** — Added GitHub actions workflows (`maven.yml` and `crda.yml`) for automated builds and security scanning. Updated internal dependencies to resolve potential vulnerabilities.
* **v1.2.0-beta** — Workspace data integrity release. Adds global unsaved changes guard (Settings, MockServer), disambiguates duplicate request names in Collection Runner metrics exports, overhauls Environment & Variable UI with auto-managing tables, and consolidates all utility scripts under `scripts/`.
* **v1.1.0-beta** - First official release under the ApiBanker rebranding. Includes massive UI/UX improvements, advanced clipboard protection, OpenAPI/Swagger Import with auto-generated Collection README documentation, complete JavaDoc coverage, and Native Executable support.
* *(Legacy: v1.0.0 to v1.4.0 under JAPI)*.

---

## 🤝 Contributing
Contributions are highly welcome! Because this is a Swing application, we heavily emphasize keeping external UI libraries to a minimum. 
If you find a bug, visual glitch, or feature request, feel free to open an Issue or PR!
