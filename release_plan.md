# ApiBanker Release Plan — v1.1.0-beta

This document outlines the release plan for **ApiBanker (v1.1.0-beta)**, the offline-first API client, detailing the beta entry criteria, verification methods, distribution strategy, and milestones for the first stable release following the massive project rebranding.

---

## 1. Release Goals

ApiBanker's transition to the `1.1.0-beta` pre-release phase aims to achieve the following:
* **Brand Migration**: Safely migrate all legacy user configurations, folders, and preferences from `.japi` to `.apibanker` on first boot.
* **Feature Freeze**: Baseline core features including HTTP request builder, scripting, collection running, environments, zoom controls, and theme toggling.
* **Local Sandboxing Validation**: Verify that the application functions 100% offline without local network leaks or internet requirements.
* **Interoperability Check**: Ensure standard Postman Collection (v2.1) and Apache JMeter (.jmx) files import and export seamlessly.
* **Stabilization**: Collect community feedback and log reports to fix interface scaling issues, visual bugs, or script engine runtime errors.

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
 
Here is the current implementation status of features included in the **v1.1.0-beta** release:
 
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

---

## 3. Packaging & Distribution

ApiBanker offers a professional, multi-tier distribution pipeline depending on user requirements and deployment environments. We provide three main distribution formats.

### Option 1: Classic Portable Bundle (.zip / .tar.gz)
The traditional portable archive containing a shaded fat JAR and cross-platform launcher scripts. Best for users who already have Java installed and prefer a portable folder.
* **Target executable**: `apibanker-1.1.0-beta.jar`
* **Launch scripts**: `apibanker.bat` (Windows), `apibanker.sh` (macOS/Linux).
* **Generation Command**: `mvn clean package`
* **Download**: [apibanker.zip](https://github.com/ncrkindia/api-banker/releases/download/v1.1.0-beta/apibanker.zip) / [apibanker.tar.gz](https://github.com/ncrkindia/api-banker/releases/download/v1.1.0-beta/apibanker.tar.gz)

### Option 2: Professional Native Installer (.msi)
A complete standalone setup wizard for Windows. This format uses `jpackage` and the WiX toolset to bundle a custom, stripped-down JRE along with the application. Best for end-users who want a standard installation experience and do not have Java installed.
* **Target executable**: `ApiBanker-installer.msi`
* **Generation Command**: `mvn clean verify -DbuildNative`
* **Download**: [ApiBanker-installer.msi](https://github.com/ncrkindia/api-banker/releases/download/v1.1.0-beta/ApiBanker-installer.msi)


---

## 4. Release Timeline

| Milestone | Target Date | Status |
| :--- | :--- | :--- |
| **Alpha Freeze** | 2026-08-01 | Complete |
| **Rebranding Migration** | 2026-08-06 | Complete |
| **Beta Release (v1.1.0-beta)** | 2026-08-10 | Active |
| **RC1 Preparation** | 2026-08-25 | Pending |
| **Stable v1.1.0** | 2026-09-01 | Pending |

### Next Steps for RC1:
1. Conduct user testing on the new `StorageManager` automatic directory migration.
2. Verify all `apibanker.bat`/`apibanker.sh` execution paths on clean systems.
3. Address any performance bottlenecks found in the Collection Runner scatter plot rendering.
