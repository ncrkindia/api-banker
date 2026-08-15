# ✨ ApiBanker Features & Capabilities Guide

<div style="background-color: #f8f9fa; border: 1px solid #e0e0e0; border-radius: 8px; padding: 16px 20px; margin: 15px 0;">
  <b style="font-size: 15px; color: #1a73e8;">Table of Contents</b>
  <ul style="margin-top: 10px; padding-left: 20px; line-height: 1.8;">
    <li><a href="#feat-core">1. Offline-First Core & Privacy Architecture</a></li>
    <li><a href="#feat-http">2. HTTP Request Engine & Payload Formats</a></li>
    <li><a href="#feat-auth">3. Authentication & Inheritance System</a></li>
    <li><a href="#feat-vars">4. Recursive Environment & Variable Resolution</a></li>
    <li><a href="#feat-scripts">5. Rhino JS Scripting & Assertion Automation</a></li>
    <li><a href="#feat-openapi">6. OpenAPI 2.0 / 3.x / 3.1 Spec Import Engine</a></li>
    <li><a href="#feat-runner">7. Collection Runner & Load Simulation Engine</a></li>
    <li><a href="#feat-console">8. Standalone Persistent Console & Logging Window</a></li>
    <li><a href="#feat-tools">9. Comprehensive Utility Suite (JWT, Diff, Mock, Cookies)</a></li>
    <li><a href="#feat-ui">10. High-DPI UI Scaling, Stricter Editing & SSL Diagnostics</a></li>
  </ul>
</div>

---

<a name="feat-core"></a>
## 1. 🔒 100% Offline-First Core & Privacy Architecture

ApiBanker is engineered specifically for privacy-focused developers and enterprise security environments.

### Core Architecture Highlights
- **Zero Cloud Dependencies**: No user registration, external server tracking, telemetry, or analytics.
- **Readable Local Filesystem Storage**: All collections, environments, settings, and request histories are serialized into clean, human-readable `.json` files under `~/.apibanker` (or a custom workspace directory).
- **Daily Action Audit Logging**: Auto-rotating log system (`action_audit_<YYYY-MM-DD>.log` capped at 10MB) tracking application lifecycle, imports, exports, and workspace mutations.
- **Self-Signed SSL Support**: Seamless testing against local development servers with untrusted or self-signed certificates without external setup.

---

<a name="feat-http"></a>
## 2. 🌐 HTTP Request Engine & Payload Formats

Full-featured HTTP client powered by Java 21 `HttpClient` executing asynchronously on Swing background workers.

### Supported Features & Methods
- **HTTP Methods**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`, `HEAD`.
- **Dynamic Query Parameters & Headers**: Auto-managing parameter grids with key-value toggle checkboxes, URL encoding, and dynamic variable expansion.
- **Connection Timeouts**: Hierarchical timeout configurations (Global > Collection > Request) with support for forced overrides and granular endpoint-level control.
- **Payload Types**:
  - `none`: Empty payload body.
  - `raw`: Syntax-highlighted text editor for `JSON`, `XML`, `HTML`, and `Plain Text`.
  - `form-data`: Support for text fields and local file attachment uploads (`Type = file`).
  - `x-www-form-urlencoded`: Standard web form parameters.
  - `GraphQL`: Dedicated query and variables editor with full schema introspection.
- **Response Inspection**: Latency timing (ms), size (KB), HTTP status badges, formatted body view, and response header analysis.

---

<a name="feat-auth"></a>
## 3. 🔑 Authentication & Inheritance System

Configure authentication rules at any level in the workspace hierarchy.

### Supported Strategies
- **Inherit Auth**: Child requests and sub-folders automatically inherit auth credentials from their parent folder or collection.
- **Bearer Token**: Transmits `Authorization: Bearer <token>` header with instant connection to the built-in **JWT Decoder**.
- **Basic Auth**: Automates standard Base64 `Authorization: Basic ...` header encoding.
- **API Key**: Append custom keys as HTTP Headers or Query Parameters.
- **OAuth 2.0**: Full support for Authorization Code, Implicit, Password Credentials, and Client Credentials grants with browser token acquisition and auto-refresh.

---

<a name="feat-vars"></a>
## 4. 🌐 Recursive Environment & Variable Resolution

Parameterize base URLs, access tokens, and request payloads using double curly brace syntax (`{{variableName}}`).

### Variable Scope Precedence
1. **Environment Scope**: Overrides all other scopes. Switch active environments via top-right dropdown or `Ctrl + E`.
2. **Collection Scope**: Defined per collection in the **Collection Details → Variables** tab.
3. **Global Scope**: Global scope variables accessible workspace-wide via **Tools → Global Variables**.

### Recursive Interpolation & Tooltips
- Variables can reference other variables recursively across scopes (e.g. `baseUrl` = `{{host}}:8080/api` where `host` is defined in an Active Environment).
- Hover over any `{{variableName}}` in request inputs to view a popover showing its fully interpolated value and scope origin.

---

<a name="feat-scripts"></a>
## 5. ⚡ Rhino JS Scripting & Assertion Automation

Automate workflows, execute assertions, and chain request parameters using embedded Mozilla Rhino JavaScript engine.

### Script Execution Stages
- **Pre-request Script**: Calculate dynamic timestamps, HMAC signatures, or pre-process body inputs before sending HTTP request.
- **Tests Script**: Validate HTTP response status, assert body JSON/XML structure, and extract tokens for variable storage.
- **Chaining**: Extract data from responses and pass it into subsequent requests effortlessly.

### Snippets Library
- Pre-built code templates accessible via one-click insertion from the script editor sidebar.

---

<a name="feat-openapi"></a>
## 6. 📄 OpenAPI 2.0 / 3.x / 3.1 Spec Import Engine

Import REST API specifications in JSON or YAML formats and convert them instantly into complete test suites.

### Key Capabilities
- **Multi-Version Support**: Full compatibility with Swagger 2.0, OpenAPI 3.0, 3.1, and 3.2 specs.
- **Selective Endpoint Import**: Interactive matrix table with color-coded HTTP method badges to check or uncheck individual routes.
- **Base URL Strategy**: Option to bake server URLs directly into requests or centralize them into `{{baseUrl}}` Collection Variables.
- **Auto-Generated Documentation**: Auto-creates a comprehensive Markdown README in the generated collection detailing server environments, auth rules, query params, and body schemas.

---

<a name="feat-runner"></a>
## 7. 📊 Collection Runner & Load Simulation Engine

Batch execute entire collections with configurable execution bounds and real-time visual metrics.

### Execution & Load Modes
- **Fixed Iterations**: Execute the collection sequence a specific number of times.
- **Fixed Duration**: Run continuous test cycles for a set timeframe (Seconds, Minutes, Hours, Days).
- **Concurrency & Ramp-up**: Adjust **Virtual Users (VUsers)** and **Ramp-up (s)** to simulate concurrent server load.

### Export Capabilities & Analytics
- **Real-Time Scatter Plots**: Monitor live metrics (Pass/Fail, Response Times, APDEX) drawn on a live UI chart.
- **Native JMeter Export**: One-click export to Apache JMeter `.jmx` format with full thread group and schedule mappings.
- **Report Generation**: Export test results to interactive HTML dashboards, CSV, or formatted PDF reports.

---

<a name="feat-console"></a>
## 8. 🖥️ Standalone Persistent Console & Logging Window

Real-time diagnostic console operating independently of the primary application workspace.

### Key Capabilities
- **Independent Top-Level Window**: Minimize, maximize, move, or focus the log window separately from the main IDE window.
- **Single-Instance Enforcement**: Re-opening the console window brings the existing instance to focus.
- **Live Filtering**: Filter logs by level (`INFO`, `WARN`, `ERROR`, `DEBUG`) or keyword.
- **Timestamped Downloads**: Download filtered logs directly to disk using standard format `apibanker-live-log-yyyy-MM-dd-HH-mm-ss.log`.

---

<a name="feat-tools"></a>
## 9. ⚡ Comprehensive Utility Suite

Built-in developer tools available directly within the sidebar and workspace menus:

- **Postman v2.1 Support**: Native, lossless Import and Export to standard Postman Collections and Environments.
- **JWT Decoder**: Inspect JWT header claims, payload fields, and signature verification.
- **Data Comparator**: Side-by-side visual diffing of JSON and XML documents with change highlights.
- **JSON Formatter & Schema Validator**: Prettify, minify, and validate JSON payloads against draft schemas.
- **Mock Data Generator**: Bulk generate UUIDs, realistic names, emails, addresses, and dates.
- **Mock Server**: Run local HTTP mock servers to serve mock endpoints with custom latency and status codes.
- **Cookie Jar**: Inspect, add, edit, and clear domain cookies.

---

<a name="feat-ui"></a>
## 10. 🎨 High-DPI UI Scaling, Stricter Editing & SSL Diagnostics

- **Responsive High-DPI Zooming**: Real-time font scaling via `Ctrl + Scroll` or `Ctrl + = / -` with dynamic button width recalculation.
- **Dynamic Execution Flow**: Action buttons adapt seamlessly during execution states (e.g. `Sending...` / `Cancel`) without breaking visual layout.
- **Stricter Editing Safeguards**: Confirmation dialogs for destructive actions (deletions, undo, paste) to prevent accidental data loss.
- **SSL Certificate Diagnostics**: Color-coded popup inspector providing full SSL certificate chain details, trust status, and expiration validation.
