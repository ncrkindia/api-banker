# Japi: Project Status

This document tracks the goals, implemented components, and pending roadmap items for the **Japi-API Testing** desktop application.

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
* [x] **JSON Storage Layer**: `StorageManager` reads and writes `collections.json`, `environments.json`, and `history.json` under `~/.japi/data` or a user-selected path.
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
* [x] **Importer & Exporter Utility**: Parses and loads Postman Collections and Environments, and exports JAPI collections/environments to Postman-compatible JSON formats.
* [x] **Request Editor**: Authorization inputs (Bearer Token, Basic Auth credentials) and request body syntax editor.
* [x] **Response Panel**: Shows colored status tags (2xx green, 3xx blue, 4xx orange, 5xx red), latency durations, response size formatting, pretty-printed JSON/XML highlight viewers, and response header grids.
* [x] **Environment Dialog**: Full manager interface to add, delete, and modify variables.
* [x] **Collection Overview (Readme.md) Editor**: Premium dual-mode viewer supporting a Read mode (live-rendered Markdown to HTML using Commonmark) and an Edit mode (full Markdown editing toolbar with Font, Size, Headings, Bold, Italic, Strikethrough, Link, List, Quote, Table, and line helpers, plus keyboard shortcuts).

---

## 3. Pending/Future Scope

Here is a list of features queued for future development iterations:

### Protocol Enhancements
* [ ] **Multipart/Form-Data Support**: Add a file/text form-data selector inside the Request Body tab to upload binary files.
* [ ] **Cookie Jar Manager**: Capture response cookies and attach them to outgoing requests of matching domains.
* [ ] **WebSocket Client**: A dedicated tab style to establish connection, send frame payloads, and stream real-time events.
* [ ] **GraphQL Support**: An editor pane to write GraphQL queries, variables, and fetch introspection schemas.

### Automation & Scripting
* [x] **Pre-request & Test Scripts**: Embed a JavaScript engine (e.g., Nashorn or GraalVM JavaScript) to run pre-request setups and validation assertions (similar to Postman's `pm.test` API).
* [x] **Collection Runner**: Run all requests inside a collection in a sequence with statistics reporting (success/failure summaries).

### Developer Quality-of-Life
* [x] **UI Scaling**: Zoom in or out of the application interface and editors using `Ctrl + =/+` and `Ctrl + -`.
* [x] **Theme Settings**: Add a Settings option to toggle between Light Theme (`FlatLightLaf`) and Dark Theme.
* [x] **Response Search**: Add a search/filter bar inside the Response panel to find text substrings within JSON/HTML payloads.
* [x] **Response Exporter**: "Save Response to File" button to download response payloads directly.
* [x] **Code Snippet Generator**: Automatically generate fetch code snippets from the request in other languages (curl, JavaScript fetch, Python requests, Java HttpClient).
