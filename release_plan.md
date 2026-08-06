# ApiBanker Release Plan — v1.0.0-beta

This document outlines the release plan for **ApiBanker (v1.0.0-beta)**, the offline-first API client, detailing the beta entry criteria, verification methods, distribution strategy, and milestones for the first stable release following the massive project rebranding.

---

## 1. Release Goals

ApiBanker's transition to the `1.0.0-beta` pre-release phase aims to achieve the following:
* **Brand Migration**: Safely migrate all legacy user configurations, folders, and preferences from `.japi` to `.apibanker` on first boot.
* **Feature Freeze**: Baseline core features including HTTP request builder, scripting, collection running, environments, zoom controls, and theme toggling.
* **Local Sandboxing Validation**: Verify that the application functions 100% offline without local network leaks or internet requirements.
* **Interoperability Check**: Ensure standard Postman Collection (v2.1) and Apache JMeter (.jmx) files import and export seamlessly.
* **Stabilization**: Collect community feedback and log reports to fix interface scaling issues, visual bugs, or script engine runtime errors.

### v1.0.0-beta Release Summary

This release introduces comprehensive workflow enhancements and project stabilization leading up to v1.0.0-beta.

**Features & Enhancements:**
- **Rebranding Migration**: Full codebase shift from JAPI to ApiBanker. Implemented automatic logic in `StorageManager` to automatically port legacy `.japi` workspaces to `.apibanker` without data loss.
- **Scripting Expansions**: Added full support for `apibanker.collectionVariables` inside pre-request and test scripts, enabling dynamic scoping of variables with native snippet UI insertion.
- **Global Variables Management**: Added a dedicated `Global Variables` tab for easy management, modification, and copy-pasting of global state.
- **Authentication Inheritance**: Rewrote the Auth inheritance engine to recursively climb the hierarchical tree, allowing deep nested folders to inherit auth seamlessly from the root Collection.
- **OAuth 2.0 Integrations**: Complete serialization of all OAuth 2.0 configuration properties and automatic integration with native Postman Collections export.
- **Postman Interoperability**: Enhanced `ImportManager` and `ExportManager` to fully parse and persist Collection-level variables when loading/saving standard Postman v2.1 Collections.
- **Data Tools Suite**: Implemented built-in JSON schema validation, XML structure checks, mock data generation algorithms, and JWT decoding tools.
- **Collection Runner Logs**: Added a dedicated workspace tab to manage historical run logs by collection and date, enabling live generation of HTML and CSV metrics reports.

**Fixes:**
- Fixed recursive collection export visibility bug in MainFrame resolving static lint warnings.
- Fixed export Postman URL conversion issue when exporting between ApiBanker/Postman JSON standards.
- Fixed `App.java` compilation failures caused by missing UI imports after the rebranding migration.

---

## 2. Beta Feature Scope
 
Here is the current implementation status of features included in the **v1.0.0-beta** release:
 
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
| **UI/UX** | Dark Mode & Scaling | FlatLaf Dark/Light themes and Ctrl+Scroll dynamic font scaling. | ✅ Complete |
| **UI/UX** | Global Variables Tab | Full independent tab for managing workspace global states. | ✅ Complete |

---

## 3. Packaging & Distribution

The application is bundled into a standalone offline-distributable archive.

### Distribution Artifacts
* **Target executable**: `apibanker-1.0.0-beta.jar` (Shaded fat JAR containing all dependencies).
* **Package formats**: `.zip` and `.tar.gz` archive containing launch scripts.
* **Launch scripts**:
  - `apibanker.bat`: Script to run the application on Windows (`javaw -jar apibanker-1.0.0-beta.jar`).
  - `apibanker.sh`: Script to run the application on macOS/Linux (configured with executable permissions `0755`).

### Packaging Pipeline
The distribution uses the Maven Assembly Plugin via `src/assembly/bin.xml`:
```bash
mvn clean package
```
This produces `target/apibanker.zip` which unzips to:
```
apibanker/
├── apibanker-1.0.0-beta.jar
├── apibanker.bat
└── apibanker.sh
```

---

## 4. Release Timeline

| Milestone | Target Date | Status |
| :--- | :--- | :--- |
| **Alpha Freeze** | 2026-08-01 | Complete |
| **Rebranding Migration** | 2026-08-06 | Complete |
| **Beta Release (v1.0.0-beta)** | 2026-08-10 | Active |
| **RC1 Preparation** | 2026-08-25 | Pending |
| **Stable v1.0.0** | 2026-09-01 | Pending |

### Next Steps for RC1:
1. Conduct user testing on the new `StorageManager` automatic directory migration.
2. Verify all `apibanker.bat`/`apibanker.sh` execution paths on clean systems.
3. Address any performance bottlenecks found in the Collection Runner scatter plot rendering.
