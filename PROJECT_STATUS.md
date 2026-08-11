# ApiBanker: Project Status

This document tracks the goals, implemented components, and pending roadmap items for the **ApiBanker-API Testing** desktop application.

---

## 1. Project Goal
Build a high-fidelity, premium, and **completely offline** desktop API client (similar to Postman) using Java 21 and Swing, utilizing a modern dark-theme user interface (FlatLaf) with code editors (RSyntaxTextArea). 

### Key Design Pillars:
* **Offline-First Security**: Zero cloud dependencies or registration. All data is saved on the local disk.
* **Readable filesystem workspace**: All collections, requests, history logs, and environment variables are saved in human-readable JSON files. The user can switch data directories inside the UI to easily version-control directories under Git.
* **Self-Signed SSL Support**: Seamless testing of local servers (e.g. `localhost` running with self-signed SSL certificates) by default.
* **Premium Usability**: Tabbed request editing, auto-generating query parameter/header grids, automatic JSON response formatting, and Postman Collection imports.

---

## 2. Implemented Features

### Core Infrastructure
* [x] **Maven Build Configuration**: Configured `pom.xml` with dependencies for FlatLaf (UI L&F), RSyntaxTextArea (syntax editor), and Gson (JSON binding).
* [x] **JSON Storage Layer**: `StorageManager` reads and writes `collections.json`, `environments.json`, and `history.json` under `~/.apibanker/data` or a user-selected path.
* [x] **Asynchronous Client Engine**: `HttpClientWrapper` wraps Java 21's `HttpClient` inside a SwingWorker background thread to avoid freezing the GUI during network execution.
* [x] **Variable Resolution**: Supports injecting environment values into URLs, headers, and request bodies via `{{variable_name}}` syntax.
* [x] **Self-Signed Certificates**: Configured default trust managers allowing connections to unsafe/local SSL endpoints.

### UI & UX Elements
* [x] **Modern L&F**: Customized FlatLaf Dark theme with rounded components (`arc=8`), customized tab bars, and glowing branding.
* [x] **Dynamic Key-Value Tables**: KeyValue tables for params, headers, and environment variables that automatically insert new rows upon editing.
* [x] **Tabbed Request Workspace**: Supports opening multiple requests side-by-side with close buttons and method color badges (e.g. green for `GET`, orange for `POST`).
* [x] **Sidebar Tree & Lists**:
  - Search filter text field for searching collections and history.
  - Interactive collections tree with right-click options to add/delete/rename requests.
  - Double-click loading of items.
  - History viewer sorted chronologically.
* [x] **Importer & Exporter Utility**: Parses and loads Postman Collections and Environments, and exports ApiBanker collections/environments to Postman-compatible JSON formats.
* [x] **Request Editor**: Authorization inputs (Bearer Token, Basic Auth credentials) and request body syntax editor.
* [x] **Response Panel**: Shows colored status tags (2xx green, 3xx blue, 4xx orange, 5xx red), latency durations, response size formatting, pretty-printed JSON/XML highlight viewers, and response header grids.
* [x] **Environment Dialog**: Full manager interface to add, delete, and modify variables.
* [x] **Collection Overview (Readme.md) Editor**: Premium dual-mode viewer supporting a Read mode (live-rendered Markdown to HTML using Commonmark) and an Edit mode (full Markdown editing toolbar with Font, Size, Headings, Bold, Italic, Strikethrough, Link, List, Quote, Table, and line helpers, plus keyboard shortcuts).
* [x] **OpenAPI / Swagger Import**: A full workspace-tab importer for OpenAPI 3.x and Swagger 2.x specifications (JSON or YAML). Features include: selective endpoint picker with color-coded HTTP method badges, configurable Base URL strategy (inline in request URL or as `{{baseUrl}}` Collection Variable), multi-server support with indexed variables (`baseUrl_1`, `baseUrl_2`, ...), automatic `http://localhost` fallback when no server is defined, automatic security scheme mapping (Bearer, Basic Auth, API Key, OAuth2) to the Collection's authentication, and auto-generated professional API documentation placed into the Collection's README covering servers, auth schemes, per-endpoint summaries, path/query/header parameters, and request body content types.

---

## 3. Pending/Future Scope

Here is a list of features queued for future development iterations:

### Protocol Enhancements
* [x] **Multipart/Form-Data Support**: Add a file/text form-data selector inside the Request Body tab to upload binary files.
* [x] **Cookie Jar Manager**: Capture response cookies and attach them to outgoing requests of matching domains.
* [x] **WebSocket Client**: A dedicated tab style to establish connection, send frame payloads, and stream real-time events.
* [x] **GraphQL Support**: An editor pane to write GraphQL queries, variables, and fetch introspection schemas.

### Automation & Scripting
* [x] **Pre-request & Test Scripts**: Embed a JavaScript engine (e.g., Nashorn or GraalVM JavaScript) to run pre-request setups and validation assertions (similar to Postman's `pm.test` API).
* [x] **Collection Runner**: Run all requests inside a collection in a sequence with statistics reporting (success/failure summaries).

### Developer Quality-of-Life
* [x] **UI Scaling**: Zoom in or out of the application interface and editors using `Ctrl + =/+` and `Ctrl + -`.
* [x] **Theme Settings**: Add a Settings option to toggle between Light Theme (`FlatLightLaf`) and Dark Theme.
* [x] **Response Search**: Add a search/filter bar inside the Response panel to find text substrings within JSON/HTML payloads.
* [x] **Response Exporter**: "Save Response to File" button to download response payloads directly.
* [x] **Code Snippet Generator**: Automatically generate fetch code snippets from the request in other languages (curl, JavaScript fetch, Python requests, Java HttpClient).
* [x] **Comprehensive Keyboard Shortcuts**: Add tree navigation and manipulation shortcuts (`F2`, `Delete`, `Ctrl+C/V/D`, `Ctrl+O`) and Request Runner shortcuts (`Ctrl+R`).

---

## 4. Release Plan (v1.5.0-beta)

* **Current Status**: Pre-release Beta Testing.
* **Next Steps**: Code freeze, complete end-to-end user acceptance testing, resolve critical GitHub issues, and prepare for stable v1.0.0 public launch.

