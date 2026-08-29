# ApiBanker Release Plan — v2.0.1

This document outlines the release plan for **ApiBanker (v2.0.1)**, the offline-first API client, detailing the beta entry criteria, verification methods, distribution strategy, and milestones for the first stable release following the massive project rebranding.

---

## 1. Release Goals

ApiBanker's transition to the `2.0.1` pre-release phase aims to achieve the following:
* **Brand Migration**: Safely migrate all legacy user configurations, folders, and preferences from `.japi` to `.apibanker` on first boot.
* **Feature Freeze**: Baseline core features including HTTP request builder, scripting, collection running, environments, zoom controls, and theme toggling.
* **Local Sandboxing Validation**: Verify that the application functions 100% offline without local network leaks or internet requirements.
* **Interoperability Check**: Ensure standard Postman Collection (v2.1) and Apache JMeter (.jmx) files import and export seamlessly.
* **Stabilization**: Collect community feedback and log reports to fix interface scaling issues, visual bugs, or script engine runtime errors.

---

### v2.0.1 Patch Summary

This patch resolves critical OutOfMemory bottlenecks in the Collection Runner and finalizes the JMeter export pipeline.

**Performance & Fixes:**
- **JMeter JMX Export**: Modernized the exporter to correctly resolve dynamic variables during JMX generation and intelligently format URLs into paths to bypass JMeter illegal host character crashes.
- **Collection Runner Thread Pool**: Replaced unbounded queue with `CallerRunsPolicy` bounded ExecutorService, preventing memory exhaustion when queueing millions of iterations.
- **Collection Runner Real-Time Metrics**: Relocated `RequestStats` storage from `CopyOnWriteArrayList` into real-time streaming temporary disk files, enabling perfectly flat memory usage during extreme load tests.
- **Results Table Pagination**: Added 2000-row automatic truncation to the GUI Results Table to prevent Swing thread hangs during fast iteration loops.
- **Runner Queue Multiplier**: Added advanced tuning configuration to `SettingsPanel` for customizing the background thread pool queue scale.

---

### v2.0.0 Release Summary

This release focuses on documentation accessibility and script automation enhancements.

**Features & Enhancements:**
- **Stricter Editing Safeguards**: Added a new mode (`Settings > Mode Stricter Editing`) that forces explicit user confirmation on destructive operations like deleting collections, requests, or environments, and undo/paste actions.
- **Hierarchical Connection Timeouts**: Implemented a cascading timeout architecture allowing timeouts to be defined globally (Default, Custom Optional, Custom Forced), at the Collection/Folder level, or natively at the Request level. Timeouts are resolved dynamically at runtime and applied to the HTTP client natively.
- **SSL Diagnostic Popups**: Overhauled the SSL validation response UI to display richly formatted, word-wrapped, and color-coded certificate details (Issuer, Subject, Validations status).
- **Recursive Variable Resolution**: Variable interpolation now fully supports recursive referencing across Environment, Collection, and Global scopes natively inside the request builder and Rhino JS scripts.
- **User Guide Contextual Search**: Introduced an embedded, responsive search bar inside the User Guide panel. It supports live highlighting, Exact Match, and regex-backed Fuzzy Search.
- **Search UI/UX Refinement**: Optimized search bar layout using `BorderLayout` to cleanly separate search controls and the dismiss button, supporting `Ctrl+F` activation and `Esc` dismissal.
- **Diff Automation Checklist**: Expanded `scripts/diff.sh` to explicitly include checks for all Java and resource file modifications during the release tagging process.
- **Shortcut Parity**: Synchronized and updated documentation text to correctly reflect the "Open Environment Manager" shortcut context.

**Fixes:**
- **UI Button Truncation**: Fixed an issue where the Request execution action button (e.g. `Send`, `Cancel`, `Sending...`) text was truncated when scaled up on high-DPI displays or user zoom by dynamically recalculating preferred dimensions.

---

### v1.5.0-beta Release Summary

This release introduces comprehensive tracking and accountability via Action Audit Logging.

**Features & Enhancements:**
- **Action Audit Logger**: Implemented a core singleton audit logger (`ActionAuditLogger`) capable of securely tracking critical application lifecycle and user actions.
- **Auto-rotating Storage**: Audit logs are safely written to the user's workspace logs directory (`~/.apibanker/logs/`) with an automatic daily file rotation (e.g. `action_audit_2026-08-11.log`).
- **File Cap Security**: Implemented a hard 10MB size limit per log file to prevent disk exhaustion, securely appending incremental indices if the cap is breached in a single day.
- **Standardized Action Tracking**: The system now seamlessly tracks operations using standardized uppercase tokens:
  - `APP_START`, `APP_CLOSE`
  - `IMPORT_COLLECTION`, `IMPORT_ENVIRONMENT`, `IMPORT_OPENAPI`, `IMPORT_FAILED`
  - `EXPORT_COLLECTION`, `EXPORT_ENVIRONMENT`, `EXPORT_REPORT`, `EXPORT_LOGS`
  - `DELETE_COLLECTION`, `DELETE_ENVIRONMENT`
  - `COPY_COLLECTION`, `COPY_ENVIRONMENT`
  - `UNDO_COLLECTION_ACTION`, `UNDO_ENVIRONMENT_ACTION`
- **Granular Toggle Control**: Integrated a master switch within `SettingsPanel` -> `enableActionAuditLog`, providing users with complete opt-out capability.
- **Documentation Overhaul**: Generated detailed JavaDoc annotations for the logger and related panels (`ExportPanel`) to maintain rigorous code-level documentation.



---

### v1.4.0-beta Release Summary

This release focuses on massive improvements to OpenAPI specification support and distribution automation.

**Features & Enhancements:**
- **Universal OpenAPI/Swagger Import**: Completely overhauled the OpenAPI importer. Migrated from `OpenAPIV3Parser` to the generic `OpenAPIParser`, providing seamless, native support for legacy **Swagger 2.0** alongside **OpenAPI 3.0.x** and **OpenAPI 3.1.x** specifications.
- **Intelligent JSON Body Generation**: The importer now dynamically generates accurate sample JSON request bodies for `POST`, `PUT`, and `PATCH` methods by recursively traversing the OpenAPI schema components, resolving `$ref`s, arrays, and nested objects if explicit examples are missing.
- **Advanced Parameter Extraction**: The importer now robustly extracts `query`, `path`, and `header` parameters from both path-level and operation-level definitions, successfully loading them into the respective configuration tables. 
- **Query Parameter Synchronization**: During OpenAPI import, query parameters are now correctly appended to the `RequestModel`'s URL string automatically (`?key=value`), ensuring proper synchronization with the URL bar UI.
- **Dynamic Collection Variables**: If OpenAPI parameters lack default or example values, the system now automatically generates them as Collection Variables and injects them into the request using `{{variable}}` syntax for maximum flexibility.
- **Automated ZIP Distribution**: Enhanced the `build-installers.bat` Windows pipeline to automatically compress the generated app-image into a portable `.zip` bundle (`ApiBanker-jre21-winX64.zip`) immediately following `.msi` creation.
- **Artifact Cleanup**: The build scripts now proactively clean up intermediate app-image directories (`target/artifacts/ApiBanker`) after the `.zip` generation, preventing disk bloat.

---

### v1.3.0-beta Release Summary

This release focuses on automation and dependency security.

**Features & Enhancements:**
- **Automated Workflows**: Added `.github/workflows/maven.yml` for automated CI/CD builds and `.github/workflows/crda.yml` for Red Hat CodeReady Dependency Analytics.
- **Automated Releases**: The Maven workflow is now configured to create a GitHub Release automatically when a tag starting with `v` is pushed.

**Fixes:**
- Bumped `rhino` dependency from 1.7.14 to 1.7.14.1 to address security vulnerabilities and stability issues.
- Bumped `poi-ooxml` dependency from 5.2.3 to 5.4.0 for improved security and performance.

---

### v1.2.0-beta Release Summary

This release focuses on workspace data integrity, UI consistency, and developer workflow improvements.

**Features & Enhancements:**
- **Global Unsaved Changes Guard**: Implemented `hasUnsavedChanges()` deep-comparison checks in `SettingsPanel` and `MockServerPanel`. `MainFrame.closeTab()` now intercepts tab closures and prompts the user to save pending changes for Settings and Mock Server configurations, matching the existing guard behavior in `RequestPanel` and `CollectionPanel`.
- **Duplicate Request Disambiguation in Runner**: Modified `CollectionRunnerPanel` to compute a unique `displayName` for every request being executed by appending the first 4 characters of its internal UUID (e.g., `"My Request -a4b2"`). This prevents identically named requests from erroneously merging in the `aggregateStatsMap`, ensuring accurate CSV/PDF/Excel/HTML metric exports.
- **Overhauled Environment & Variable UI**: Replaced the manually managed add/delete button table in `EnvironmentManagerPanel` and `GlobalVariablesPanel` with a dynamic auto-managing table. Empty rows are automatically appended when the last row is filled, and empty rows are pruned on focus loss, with a persistent single empty row always available.
- **Script Consolidation**: Moved all root-level utility scripts (`apibanker.bat`, `apibanker.sh`, `build-installers.bat`, `build-installers.sh`, `release.sh`, `patch_jwt_btn.py`) into a dedicated `scripts/` directory. Updated `src/assembly/bin.xml` to reference the new paths for the Maven distribution bundle.
- **Automated Release Script**: Enhanced `scripts/release.sh` to automatically parse and extract the target version from `pom.xml` via `awk` if no version argument is provided. Each script now uses `cd "$(dirname "$0")/.."` to execute correctly against the project root regardless of the caller's working directory.
- **Tab Style Refinement**: Refined FlatLaf tab styling in `App.java` for both dark and light themes (`tabType=underlined`, suppressed focus indicators) for a cleaner IDE appearance.

**Fixes:**
- Fixed `MockServerPanel` save button visibility and zoom responsiveness by promoting `saveConfigBtn` to a class field and implementing dynamic size recalculation in `updateFontSize()`.
- Fixed premature "unsaved changes" prompts on `RequestPanel` and `CollectionPanel` caused by auto-appended blank rows in Variable, Param, and Header tables.

---

### v1.1.0-beta Release Summary

This release introduces comprehensive workflow enhancements and project stabilization leading up to v1.0.0-beta.

**Features & Enhancements:**
- **Rebranding Migration**: Full codebase shift from JAPI to ApiBanker. Implemented automatic logic in `StorageManager` to automatically port legacy `.japi` workspaces to `.apibanker` without data loss.
- **Advanced Native Packaging**: Fully integrated Maven pipelines for generating multiple professional distribution formats, including cross-platform portable bundles (with .bat/.sh scripts), WiX-powered standalone Windows `.msi` installers, and AOT-compiled GraalVM `.exe` standalone native executables.
- **Collection Tree Undo (Ctrl+Z)**: Implemented an `UndoManager` system for the Collection Tree. Mutating actions (creating, deleting, pasting, dragging/dropping, and renaming) push deep-cloned JSON state snapshots to a stack in `MainFrame.java`, enabling effortless reverting of structural changes.
- **Global Variables Management & Precedence**: Added a dedicated `Global Variables` tab. Updated variable resolution precedence across the app to: `Environment > Collection > Global`. Added distinct badge styling (purple) for Global variables to instantly differentiate them from environment/collection variables.
- **Collection Variables Copy/Paste**: Enabled standard Ctrl+C, Ctrl+V, and right-click functionality (including Row Deletion and Undo) for Collection Variables by refactoring table logic to dynamically adapt to any table's column structure.
- **Auth Inheritance Default**: Changed the default `authType` to "inherit". All new and imported requests automatically inherit from their parent collection or folder out of the box.
- **Mock Server Code Generator**: Added a "Code" submenu to the Mock Server Rules table. Instantly generates and copies client-side code snippets (Curl, Python, JavaScript, Java) for your mocked endpoints. Supports multi-select intelligent snippet merging!
- **Mock Server Enhancements**: Added custom HTTP Method color-coding in the Mock Rules table, multi-select robust Copy/Paste/Delete configuration serialization, and simulated processing time (delay in ms) to test client timeouts.
- **Comprehensive Traffic Logging**: Completely overhauled Mock Server live traffic logs to show detailed requests (parsed query params, raw headers) and detailed matched rule responses. Restored the native `ApiBanker Mock Server` fallback Server header logging.
- **Data Generator with Schema**: Implemented a dynamic JSON Schema data generator within `DataToolsPanel.java` capable of recursively parsing primitive arrays and nested objects.
- **JSON Tool Integration**: Migrated the standalone JSON Tool into a unified tab within `DataToolsPanel.java`.
- **Window Alignment Persistence**: Updated `App.java` to launch the application at the exact (x, y) coordinates of the last session, persisting these values inside `AppSettings`. Defaults cleanly to top-left (0, 0) on fresh boot.
- **Welcome Page Scroll**: Initialized the Welcome Dashboard perfectly scrolled to the top via `setCaretPosition(0)`.
- **View Menu Updates**: Added a logical separator below the Collection Runner Logs item in the View menu.
- **Comprehensive Documentation**: Synchronized the `@version 1.1.0-beta` tag and injected complete JavaDoc blocks across all 50 source files.

**Fixes:**
- **SSL Dialog & Copy**: Enhanced `ResponsePanel.java` so that clicking the SSL label pops up a copyable, word-wrapped JEditorPane dialog with full HTML-formatted certificate details, fixing previous text overflow issues.
- **Mock Server Headers UI**: Fixed the infinite blank rows recursion bug by adding an `isUpdatingHeaders` safety flag, ensuring only one clean blank row exists at the bottom. Tinted non-editable HTTP headers gray for instant visual distinction.
- **README Loading Issue**: Enhanced `MainFrame.java` to have a robust fallback, attempting to load `README.md` from the ClassLoader if standard resource stream loading fails.
- Fixed recursive collection export visibility bug in MainFrame resolving static lint warnings.
- Fixed export Postman URL conversion issue when exporting between ApiBanker/Postman JSON standards.
- Fixed `App.java` compilation failures caused by missing UI imports after the rebranding migration.

---

## 2. Beta Feature Scope
 
Here is the current implementation status of features included in the **v2.0.1** release:
 
| Category | Feature Name | Description | Status |
| :--- | :--- | :--- | :--- |
| **Core** | Rebrand Migration Engine | Zero-loss porting of `~/.japi` config files to `~/.apibanker`. | ✅ Complete |
| **Core** | Request Builder | HTTP methods, headers, dynamic bodies (JSON, form-data, GraphQL). | ✅ Complete |
| **Auth** | OAuth 2.0 Integrations | First-class UI, payload mapping, and extraction. | ✅ Complete |
| **Auth** | Folder Auth Inheritance | Nested folders properly inherit security from root Collections. | ✅ Complete |
| **Scripting** | Rhino JS Engine | Postman-compatible `pm.*` and `apibanker.*` JS objects. | ✅ Complete |
| **Tools** | Collection Runner | Batch request execution with real-time scatter plot charting. | ✅ Complete |
| **Tools** | Data/Mock Utilities | JSON Schema validators and mass data generation scripts. | ✅ Complete |
| **Interop** | Import/Export Managers | Support for Postman v2.1 (Collections, Env) and Apache JMeter (.jmx). | ✅ Complete |
| **Interop** | OpenAPI / Swagger Import | Import OpenAPI 3.x / Swagger 2.x (JSON & YAML). Selective endpoint picker, Base URL strategy (inline or `{{baseUrl}}` variable), multi-server indexed vars, security scheme mapping, and auto-generated Collection README documentation. | ✅ Complete |
| **UI/UX** | Dark Mode & Scaling | FlatLaf Dark/Light themes and Ctrl+Scroll dynamic font scaling. | ✅ Complete |
| **UI/UX** | Global Variables Tab | Full independent tab for managing workspace global states. | ✅ Complete |
| **UI/UX** | Unsaved Changes Guard | Confirmation dialogs on tab close for MockServer and Settings. | ✅ Complete |
| **Runner** | Metric Deduplication | Unique display names for identically named requests in exports. | ✅ Complete |

---

## 3. Packaging & Distribution

ApiBanker offers a professional, multi-tier distribution pipeline depending on user requirements and deployment environments. We provide three main distribution formats.

### Option 1: Classic Portable Bundle (.zip / .tar.gz)
The traditional portable archive containing a shaded fat JAR and cross-platform launcher scripts. Best for users who already have Java installed and prefer a portable folder.
* **Target executable**: `apibanker-2.0.1.jar`
* **Launch scripts**: `scripts/apibanker.bat` (Windows), `scripts/apibanker.sh` (macOS/Linux).
* **Generation Command**: `mvn clean package`
* **Download**: [apibanker.zip](https://github.com/ncrkindia/api-banker/releases/download/v2.0.1/apibanker.zip) / [apibanker.tar.gz](https://github.com/ncrkindia/api-banker/releases/download/v2.0.1/apibanker.tar.gz)

### Option 2: Professional Native Installer (.msi)
A complete standalone setup wizard for Windows. This format uses `jpackage` and the WiX toolset to bundle a custom, stripped-down JRE along with the application. Best for end-users who want a standard installation experience and do not have Java installed.
* **Target executable**: `ApiBanker-installer.msi`
* **Generation Command**: `mvn clean verify -DbuildNative`
* **Download**: [ApiBanker-installer.msi](https://github.com/ncrkindia/api-banker/releases/download/v2.0.1/ApiBanker-2.0.1.202608.msi)

### Option 3: Portable JRE-Bundled ZIP (Windows x64)
A self-contained portable archive that includes a stripped-down JRE 21 runtime alongside the application. No Java installation required — simply extract and run. Best for users who want portability without a system-level installer.
* **Target archive**: `ApiBanker-2.0.1-jre21-winX64.zip`
* **Generation Command**: `scripts\build-installers.bat` (auto-generates after MSI build)
* **Download**: [ApiBanker-2.0.1-jre21-winX64.zip](https://github.com/ncrkindia/api-banker/releases/download/v2.0.1/ApiBanker-2.0.1-jre21-winX64.zip)


---

## 4. Release Timeline

| Milestone | Target Date | Status |
| :--- | :--- | :--- |
| **Alpha Freeze** | 2026-08-01 | Complete |
| **Rebranding Migration** | 2026-08-06 | Complete |
| **Beta Release (v1.1.0-beta)** | 2026-08-10 | Complete |
| **Beta Release (v1.2.0-beta)** | 2026-08-09 | Complete |
| **Beta Release (v1.5.0-beta)** | 2026-08-09 | Complete |
| **Stable Release (v2.0.1)** | 2026-08-23 | Complete |
| **v2.0.1 Patch Preparation** | 2026-08-25 | Pending |
| **Feature Release v2.1.0** | 2026-09-01 | Pending |

### Next Steps for RC1:
1. Conduct user testing on the new `StorageManager` automatic directory migration.
2. Verify all `scripts/apibanker.bat`/`scripts/apibanker.sh` execution paths on clean systems.
3. Address any performance bottlenecks found in the Collection Runner scatter plot rendering.
4. Perform full regression test of unsaved changes guard across all panel types.
