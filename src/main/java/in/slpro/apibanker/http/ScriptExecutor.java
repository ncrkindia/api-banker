package in.slpro.apibanker.http;

import org.mozilla.javascript.*;

import in.slpro.apibanker.logger.LogEntry;
import in.slpro.apibanker.model.*;

import java.util.*;

/**
 * ScriptExecutor
 *
 * <p>
 * This class leverages the Mozilla Rhino JavaScript engine to execute custom JS
 * scripts
 * defined by the user in the Pre-Request or Post-Request (Test) tabs of the UI.
 * It builds a restricted JavaScript context and injects a Postman-compatible
 * `pm.*` API
 * so that standard scripts can manipulate variables, read responses, and
 * perform assertions
 * (e.g. `pm.expect(pm.response.code).to.equal(200)`).
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 1.0.0-beta
 * @since 1.0.0
 */
public class ScriptExecutor {
    private boolean silentMode = false;

    /**
     * Enables or disables silent mode. When silent mode is active, any
     * `console.log`
     * statements or syntax errors from the JS execution will not be forwarded to
     * the
     * central application {@link in.slpro.apibanker.logger.ConsoleLogger}.
     * 
     * @param silentMode true to suppress console logging, false otherwise.
     */
    public void setSilentMode(boolean silentMode) {
        this.silentMode = silentMode;
    }

    /**
     * Executes a user-defined Pre-Request JavaScript string.
     * <p>
     * The script has access to `pm.environment`, `pm.variables`, `pm.request`,
     * and `console`. Because this runs before the HTTP request is dispatched,
     * `pm.response` and `pm.test` are undefined and inaccessible.
     * </p>
     * 
     * @param script      The raw JavaScript string to execute.
     * @param request     The current Request context (can be read/mutated by the
     *                    script).
     * @param environment The active Environment (can be read/mutated by the
     *                    script).
     * @return A {@link ScriptResult} containing console logs and potential
     *         execution errors.
     */
    public ScriptResult executePreRequestScript(String script, RequestModel request, EnvironmentModel environment) {
        ScriptResult result = new ScriptResult();
        if (script == null || script.trim().isEmpty())
            return result;

        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Build and inject pm object for pre-request context
            injectPmObject(cx, scope, request, null, environment, result, false);
            injectConsole(cx, scope, result, "Pre-request");

            cx.evaluateString(scope, script, "pre-request-script", 1, null);
        } catch (RhinoException e) {
            String err = "Pre-request Script Error (line " + e.lineNumber() + "): " + e.details();
            result.setError(err);
            if (!silentMode && in.slpro.apibanker.logger.ConsoleLogger.getInstance().isEnableLogging()) {
                in.slpro.apibanker.logger.ConsoleLogger.getInstance().logMessage(LogEntry.Level.ERROR, err,
                        "Pre-request");
            }
        } catch (Exception e) {
            String err = "Pre-request Script Error: " + e.getMessage();
            result.setError(err);
            if (!silentMode && in.slpro.apibanker.logger.ConsoleLogger.getInstance().isEnableLogging()) {
                in.slpro.apibanker.logger.ConsoleLogger.getInstance().logMessage(LogEntry.Level.ERROR, err,
                        "Pre-request");
            }
        } finally {
            Context.exit();
        }
        return result;
    }

    /**
     * Executes a user-defined Post-Request (Test) JavaScript string.
     * <p>
     * The script runs after the HTTP request completes. It has full access to
     * `pm.environment`, `pm.variables`, `pm.request`, `pm.response`, `pm.test`,
     * `pm.expect`, and `console`.
     * </p>
     * 
     * @param script      The raw JavaScript string to execute.
     * @param request     The original Request context.
     * @param response    The completed Response context (status, body, headers,
     *                    time).
     * @param environment The active Environment.
     * @return A {@link ScriptResult} containing console logs, test assertions, and
     *         potential errors.
     */
    public ScriptResult executeTestScript(String script, RequestModel request, ResponseModel response,
            EnvironmentModel environment) {
        ScriptResult result = new ScriptResult();
        if (script == null || script.trim().isEmpty())
            return result;

        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            Scriptable scope = cx.initStandardObjects();

            // Build and inject pm object for test context
            injectPmObject(cx, scope, request, response, environment, result, true);
            injectConsole(cx, scope, result, "Test");

            cx.evaluateString(scope, script, "test-script", 1, null);
        } catch (RhinoException e) {
            String err = "Test Script Error (line " + e.lineNumber() + "): " + e.details();
            result.setError(err);
            if (!silentMode && in.slpro.apibanker.logger.ConsoleLogger.getInstance().isEnableLogging()) {
                in.slpro.apibanker.logger.ConsoleLogger.getInstance().logMessage(LogEntry.Level.ERROR, err, "Test");
            }
        } catch (Exception e) {
            String err = "Test Script Error: " + e.getMessage();
            result.setError(err);
            if (!silentMode && in.slpro.apibanker.logger.ConsoleLogger.getInstance().isEnableLogging()) {
                in.slpro.apibanker.logger.ConsoleLogger.getInstance().logMessage(LogEntry.Level.ERROR, err, "Test");
            }
        } finally {
            Context.exit();
        }
        return result;
    }

    /**
     * Builds and injects the global `pm` API object tree into the Rhino JavaScript
     * scope.
     * This method wires up Java method callbacks for Javascript functions (e.g.,
     * mapping
     * JS `pm.environment.set()` to the Java EnvironmentModel's `set()` method).
     * 
     * @param cx            The active Rhino JavaScript Context.
     * @param scope         The active global JS Scope.
     * @param request       The Java RequestModel context to bind.
     * @param response      The Java ResponseModel context to bind (null if
     *                      pre-request).
     * @param environment   The Java EnvironmentModel context to bind.
     * @param result        The result object to collect assertions.
     * @param isTestContext True if the scope should include `pm.response` and
     *                      `pm.test`.
     */
    private void injectPmObject(Context cx, Scriptable scope, RequestModel request,
            ResponseModel response, EnvironmentModel environment,
            ScriptResult result, boolean isTestContext) {
        Scriptable pm = cx.newObject(scope);

        // --- pm.environment ---
        Scriptable envObj = cx.newObject(scope);
        envObj.put("get", envObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1 || environment == null)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                if (environment.getVariables() != null) {
                    for (KeyValueItem kv : environment.getVariables()) {
                        if (kv.isEnabled() && key.equals(kv.getKey())) {
                            String rawVal = kv.getValue() != null ? kv.getValue() : "";
                            return in.slpro.apibanker.model.VariableHelper.resolveVariables(rawVal, request, environment);
                        }
                    }
                }
                return Undefined.instance;
            }
        });
        envObj.put("set", envObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2 || environment == null)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                String value = Context.toString(args[1]);
                if (environment.getVariables() != null) {
                    for (KeyValueItem kv : environment.getVariables()) {
                        if (key.equals(kv.getKey())) {
                            kv.setValue(value);
                            kv.setEnabled(true);
                            return Undefined.instance;
                        }
                    }
                    // Variable doesn't exist yet — add it
                    KeyValueItem newKv = new KeyValueItem(key, value, true);
                    environment.getVariables().add(newKv);
                }
                return Undefined.instance;
            }
        });
        envObj.put("unset", envObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1 || environment == null || environment.getVariables() == null)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                environment.getVariables().removeIf(kv -> key.equals(kv.getKey()));
                return Undefined.instance;
            }
        });
        envObj.put("toObject", envObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                Scriptable obj = cx.newObject(scope);
                if (environment != null && environment.getVariables() != null) {
                    for (KeyValueItem kv : environment.getVariables()) {
                        if (kv.isEnabled()) {
                            obj.put(kv.getKey(), obj, kv.getValue());
                        }
                    }
                }
                return obj;
            }
        });
        pm.put("environment", pm, envObj);

        Scriptable globObj = cx.newObject(scope);
        globObj.put("get", globObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                java.util.List<KeyValueItem> globalVars = in.slpro.apibanker.storage.StorageManager.getInstance()
                        .getSettings().getGlobalVariables();
                if (globalVars != null) {
                    for (KeyValueItem kv : globalVars) {
                        if (kv.isEnabled() && key.equals(kv.getKey())) {
                            String rawVal = kv.getValue() != null ? kv.getValue() : "";
                            return in.slpro.apibanker.model.VariableHelper.resolveVariables(rawVal, request, environment);
                        }
                    }
                }
                return Undefined.instance;
            }
        });
        globObj.put("set", globObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                String value = Context.toString(args[1]);
                java.util.List<KeyValueItem> globalVars = in.slpro.apibanker.storage.StorageManager.getInstance()
                        .getSettings().getGlobalVariables();
                if (globalVars != null) {
                    for (KeyValueItem kv : globalVars) {
                        if (key.equals(kv.getKey())) {
                            kv.setValue(value);
                            kv.setEnabled(true);
                            in.slpro.apibanker.storage.StorageManager.getInstance().saveSettings();
                            return Undefined.instance;
                        }
                    }
                    KeyValueItem newKv = new KeyValueItem(key, value, true);
                    globalVars.add(newKv);
                    in.slpro.apibanker.storage.StorageManager.getInstance().saveSettings();
                }
                return Undefined.instance;
            }
        });
        globObj.put("unset", globObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                java.util.List<KeyValueItem> globalVars = in.slpro.apibanker.storage.StorageManager.getInstance()
                        .getSettings().getGlobalVariables();
                if (globalVars != null) {
                    globalVars.removeIf(kv -> key.equals(kv.getKey()));
                    in.slpro.apibanker.storage.StorageManager.getInstance().saveSettings();
                }
                return Undefined.instance;
            }
        });
        pm.put("globals", pm, globObj);

        // --- pm.collectionVariables ---
        Scriptable collVarsObj = cx.newObject(scope);
        collVarsObj.put("get", collVarsObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1 || request == null)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                CollectionModel parentCol = null;
                if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                    parentCol = in.slpro.apibanker.ui.MainFrame.findParentCollection(request);
                }
                if (parentCol != null && parentCol.getVariables() != null) {
                    for (KeyValueItem kv : parentCol.getVariables()) {
                        if (kv.isEnabled() && key.equals(kv.getKey())) {
                            String rawVal = kv.getValue() != null ? kv.getValue() : "";
                            return in.slpro.apibanker.model.VariableHelper.resolveVariables(rawVal, request, environment);
                        }
                    }
                }
                return Undefined.instance;
            }
        });
        collVarsObj.put("set", collVarsObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2 || request == null)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                String value = Context.toString(args[1]);
                CollectionModel parentCol = null;
                if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                    parentCol = in.slpro.apibanker.ui.MainFrame.findParentCollection(request);
                }
                if (parentCol != null) {
                    if (parentCol.getVariables() == null) {
                        parentCol.setVariables(new java.util.ArrayList<>());
                    }
                    for (KeyValueItem kv : parentCol.getVariables()) {
                        if (key.equals(kv.getKey())) {
                            kv.setValue(value);
                            kv.setEnabled(true);
                            if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                                in.slpro.apibanker.storage.StorageManager.getInstance().saveCollections(
                                        in.slpro.apibanker.ui.MainFrame.getInstance().getCollections());
                            }
                            return Undefined.instance;
                        }
                    }
                    parentCol.getVariables().add(new KeyValueItem(key, value, true));
                    if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                        in.slpro.apibanker.storage.StorageManager.getInstance()
                                .saveCollections(in.slpro.apibanker.ui.MainFrame.getInstance().getCollections());
                    }
                }
                return Undefined.instance;
            }
        });
        collVarsObj.put("unset", collVarsObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1 || request == null)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                CollectionModel parentCol = null;
                if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                    parentCol = in.slpro.apibanker.ui.MainFrame.findParentCollection(request);
                }
                if (parentCol != null && parentCol.getVariables() != null) {
                    parentCol.getVariables().removeIf(kv -> key.equals(kv.getKey()));
                    if (in.slpro.apibanker.ui.MainFrame.getInstance() != null) {
                        in.slpro.apibanker.storage.StorageManager.getInstance()
                                .saveCollections(in.slpro.apibanker.ui.MainFrame.getInstance().getCollections());
                    }
                }
                return Undefined.instance;
            }
        });
        pm.put("collectionVariables", pm, collVarsObj);

        // --- pm.variables (request-scoped transient variables) ---
        Map<String, String> transientVars = new HashMap<>();
        Scriptable varsObj = cx.newObject(scope);
        varsObj.put("get", varsObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return Undefined.instance;
                String key = Context.toString(args[0]);
                String val = transientVars.get(key);
                if (val != null) {
                    return in.slpro.apibanker.model.VariableHelper.resolveVariables(val, request, environment);
                }
                return Undefined.instance;
            }
        });
        varsObj.put("set", varsObj, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2)
                    return Undefined.instance;
                transientVars.put(Context.toString(args[0]), Context.toString(args[1]));
                return Undefined.instance;
            }
        });
        pm.put("variables", pm, varsObj);

        // --- pm.request ---
        Scriptable reqObj = cx.newObject(scope);
        if (request != null) {
            reqObj.put("url", reqObj, request.getUrl() != null ? request.getUrl() : "");
            reqObj.put("method", reqObj, request.getMethod() != null ? request.getMethod() : "GET");

            // pm.request.headers
            Scriptable reqHeaders = cx.newObject(scope);
            if (request.getHeaders() != null) {
                for (KeyValueItem kv : request.getHeaders()) {
                    if (kv.isEnabled() && kv.getKey() != null) {
                        reqHeaders.put(kv.getKey(), reqHeaders, kv.getValue() != null ? kv.getValue() : "");
                    }
                }
            }
            reqObj.put("headers", reqObj, reqHeaders);

            // pm.request.body
            reqObj.put("body", reqObj, request.getBodyRawContent() != null ? request.getBodyRawContent() : "");
        }
        pm.put("request", pm, reqObj);

        // --- pm.response (only in test scripts) ---
        if (isTestContext && response != null) {
            Scriptable resObj = cx.newObject(scope);
            resObj.put("code", resObj, response.getStatusCode());
            resObj.put("status", resObj, response.getStatusText() != null ? response.getStatusText() : "");
            resObj.put("responseTime", resObj, (double) response.getExecutionTimeMs());
            resObj.put("responseSize", resObj, (double) response.getSizeBytes());

            // pm.response.text()
            final String bodyText = response.getBody() != null ? response.getBody() : "";
            resObj.put("text", resObj, new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    return bodyText;
                }
            });

            // pm.response.json()
            resObj.put("json", resObj, new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    try {
                        return parseJsonToJs(cx, scope, bodyText);
                    } catch (Exception e) {
                        throw Context.reportRuntimeError("Failed to parse response body as JSON: " + e.getMessage());
                    }
                }
            });

            // pm.response.headers
            Scriptable resHeaders = cx.newObject(scope);
            resHeaders.put("get", resHeaders, new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    if (args.length < 1 || response.getHeaders() == null)
                        return Undefined.instance;
                    String headerName = Context.toString(args[0]).toLowerCase();
                    for (Map.Entry<String, java.util.List<String>> entry : response.getHeaders().entrySet()) {
                        if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
                            return String.join(", ", entry.getValue());
                        }
                    }
                    return Undefined.instance;
                }
            });
            resObj.put("headers", resObj, resHeaders);

            pm.put("response", pm, resObj);
        }

        // --- pm.test(name, fn) ---
        if (isTestContext) {
            pm.put("test", pm, new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    if (args.length < 2)
                        return Undefined.instance;
                    String testName = Context.toString(args[0]);
                    if (args[1] instanceof Function fn) {
                        try {
                            fn.call(cx, scope, thisObj, new Object[] {});
                            result.addAssertion(testName, true, null);
                        } catch (Exception e) {
                            String msg = e.getMessage();
                            if (e instanceof JavaScriptException jse) {
                                Object val = jse.getValue();
                                if (val instanceof Scriptable sObj) {
                                    Object errMsg = sObj.get("message", sObj);
                                    if (errMsg != null && !(errMsg instanceof Undefined)) {
                                        msg = errMsg.toString();
                                    }
                                } else if (val != null) {
                                    msg = val.toString();
                                }
                            }
                            result.addAssertion(testName, false, msg);
                        }
                    }
                    return Undefined.instance;
                }
            });
        }

        // --- pm.expect(value) → chainable assertion ---
        pm.put("expect", pm, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                Object actual = args.length > 0 ? args[0] : Undefined.instance;
                return buildExpectChain(cx, scope, actual);
            }
        });

        scope.put("apibanker", scope, pm);
    }

    /**
     * Builds a deeply-chainable assertion API for `pm.expect(value)`.
     * 
     * <p>
     * Constructs JS objects enabling syntax like:
     * {@code pm.expect(val).to.not.be.below(5)}
     * It dynamically resolves assertion logic via anonymous Java functions bound to
     * the Rhino Scope.
     * </p>
     * 
     * @param cx     The active Rhino JavaScript Context.
     * @param scope  The active global JS Scope.
     * @param actual The actual value passed into `pm.expect(actual)`.
     * @return A Scriptable proxy object representing the root of the `.to...`
     *         assertion chain.
     */
    private Scriptable buildExpectChain(Context cx, Scriptable scope, Object actual) {
        Scriptable chain = cx.newObject(scope);
        Scriptable to = cx.newObject(scope);
        Scriptable be = cx.newObject(scope);
        Scriptable have = cx.newObject(scope);
        Scriptable not = cx.newObject(scope);

        // Negation support: pm.expect(x).to.not.equal(y)
        final boolean[] negated = { false };

        // --- to.equal / to.eql ---
        BaseFunction equalFn = new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                Object expected = args[0];
                boolean equals = deepEquals(actual, expected);
                if (negated[0])
                    equals = !equals;
                if (!equals) {
                    throw Context.reportRuntimeError("Expected " + stringify(actual)
                            + (negated[0] ? " to not equal " : " to equal ") + stringify(expected));
                }
                return chain;
            }
        };
        to.put("equal", to, equalFn);
        to.put("eql", to, equalFn);
        to.put("equals", to, equalFn);
        not.put("equal", not, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                negated[0] = true;
                return equalFn.call(cx, scope, thisObj, args);
            }
        });

        // --- to.include ---
        BaseFunction includeFn = new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                String expected = Context.toString(args[0]);
                String actualStr = Context.toString(actual);
                boolean includes = actualStr.contains(expected);
                if (negated[0])
                    includes = !includes;
                if (!includes) {
                    throw Context.reportRuntimeError("Expected \"" + actualStr + "\""
                            + (negated[0] ? " to not include " : " to include ") + "\"" + expected + "\"");
                }
                return chain;
            }
        };
        to.put("include", to, includeFn);
        to.put("contain", to, includeFn);

        // --- be.a(type) ---
        be.put("a", be, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                String expectedType = Context.toString(args[0]).toLowerCase();
                String actualType = getJsType(actual).toLowerCase();
                boolean match = actualType.equals(expectedType);
                if (negated[0])
                    match = !match;
                if (!match) {
                    throw Context.reportRuntimeError("Expected " + stringify(actual)
                            + (negated[0] ? " to not be a " : " to be a ") + expectedType + " but got " + actualType);
                }
                return chain;
            }
        });

        // --- be.below / be.above ---
        be.put("below", be, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                double actualNum = Context.toNumber(actual);
                double expected = Context.toNumber(args[0]);
                boolean ok = actualNum < expected;
                if (negated[0])
                    ok = !ok;
                if (!ok) {
                    throw Context.reportRuntimeError(
                            "Expected " + actualNum + (negated[0] ? " to not be below " : " to be below ") + expected);
                }
                return chain;
            }
        });
        be.put("above", be, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                double actualNum = Context.toNumber(actual);
                double expected = Context.toNumber(args[0]);
                boolean ok = actualNum > expected;
                if (negated[0])
                    ok = !ok;
                if (!ok) {
                    throw Context.reportRuntimeError(
                            "Expected " + actualNum + (negated[0] ? " to not be above " : " to be above ") + expected);
                }
                return chain;
            }
        });

        // --- be.ok (truthy), be.true, be.false, be.null, be.undefined ---
        be.put("ok", be, isTruthy(actual) ? Boolean.TRUE : Boolean.FALSE);

        // Boolean property getters via defineProperty
        boolean isTrue = actual instanceof Boolean b && b;
        be.put("true", be, isTrue);
        boolean isFalse = actual instanceof Boolean b2 && !b2;
        be.put("false", be, isFalse);

        // have.property(name)
        have.put("property", have, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                String propName = Context.toString(args[0]);
                boolean hasProp = false;
                if (actual instanceof Scriptable s) {
                    hasProp = s.has(propName, s);
                }
                if (negated[0])
                    hasProp = !hasProp;
                if (!hasProp) {
                    throw Context.reportRuntimeError("Expected object"
                            + (negated[0] ? " to not have property " : " to have property ") + "'" + propName + "'");
                }
                return chain;
            }
        });

        // have.status(code) — for pm.expect(pm.response.code).to.have.status(200)
        have.put("status", have, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                int expectedCode = (int) Context.toNumber(args[0]);
                int actualCode = (int) Context.toNumber(actual);
                boolean ok = actualCode == expectedCode;
                if (negated[0])
                    ok = !ok;
                if (!ok) {
                    throw Context.reportRuntimeError("Expected status code " + actualCode
                            + (negated[0] ? " to not be " : " to be ") + expectedCode);
                }
                return chain;
            }
        });

        // have.lengthOf(n)
        have.put("lengthOf", have, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 1)
                    return chain;
                int expectedLen = (int) Context.toNumber(args[0]);
                int actualLen = 0;
                if (actual instanceof String s)
                    actualLen = s.length();
                else if (actual instanceof NativeArray arr)
                    actualLen = (int) arr.getLength();
                else if (actual instanceof Scriptable s) {
                    Object lenProp = s.get("length", s);
                    if (lenProp instanceof Number n)
                        actualLen = n.intValue();
                }
                boolean ok = actualLen == expectedLen;
                if (negated[0])
                    ok = !ok;
                if (!ok) {
                    throw Context.reportRuntimeError("Expected length " + actualLen
                            + (negated[0] ? " to not equal " : " to equal ") + expectedLen);
                }
                return chain;
            }
        });

        // Wire up the chain: chain.to -> to, to.be -> be, to.have -> have, to.not ->
        // not
        to.put("be", to, be);
        to.put("have", to, have);
        to.put("not", to, not);

        // Also wire not.be, not.have, not.include for chaining
        Scriptable notBe = cx.newObject(scope);
        notBe.put("a", notBe, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                negated[0] = true;
                return ((BaseFunction) be.get("a", be)).call(cx, scope, thisObj, args);
            }
        });
        notBe.put("below", notBe, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                negated[0] = true;
                return ((BaseFunction) be.get("below", be)).call(cx, scope, thisObj, args);
            }
        });
        notBe.put("above", notBe, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                negated[0] = true;
                return ((BaseFunction) be.get("above", be)).call(cx, scope, thisObj, args);
            }
        });
        not.put("be", not, notBe);
        not.put("have", not, have); // reuse have with negated flag
        not.put("include", not, new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                negated[0] = true;
                return includeFn.call(cx, scope, thisObj, args);
            }
        });

        chain.put("to", chain, to);
        return chain;
    }

    /**
     * Injects the global `console` object (`console.log`, `console.warn`, etc.)
     * into the JS scope.
     * 
     * @param cx     The active Rhino JavaScript Context.
     * @param scope  The active global JS Scope.
     * @param result The result object to store logs.
     * @param source The context identifier (e.g., "Pre-request" or "Test") for log
     *               tagging.
     */
    private void injectConsole(Context cx, Scriptable scope, ScriptResult result, String source) {
        Scriptable console = cx.newObject(scope);

        console.put("log", console, createConsoleLogFn(result, LogEntry.Level.INFO, source));
        console.put("info", console, createConsoleLogFn(result, LogEntry.Level.INFO, source));
        console.put("warn", console, createConsoleLogFn(result, LogEntry.Level.INFO, source));
        console.put("error", console, createConsoleLogFn(result, LogEntry.Level.ERROR, source));

        scope.put("console", scope, console);
    }

    private BaseFunction createConsoleLogFn(ScriptResult result, LogEntry.Level level, String source) {
        return new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < args.length; i++) {
                    if (i > 0)
                        sb.append(" ");
                    sb.append(Context.toString(args[i]));
                }
                String msg = sb.toString();
                result.addConsoleLog(msg);
                if (!silentMode && in.slpro.apibanker.logger.ConsoleLogger.getInstance().isEnableLogging()) {
                    in.slpro.apibanker.logger.ConsoleLogger.getInstance().logMessage(level, "[Console] " + msg, source);
                }
                return Undefined.instance;
            }
        };
    }

    /**
     * A utility bridge that safely parses a JSON string (using Google Gson) and
     * recursively
     * converts it into native Rhino JavaScript objects so the script can manipulate
     * it naturally.
     * 
     * @param cx    The active Rhino JavaScript Context.
     * @param scope The active global JS Scope.
     * @param json  The raw JSON string.
     * @return A native JavaScript Object, Array, or primitive representing the JSON
     *         tree.
     */
    private Object parseJsonToJs(Context cx, Scriptable scope, String json) {
        if (json == null || json.trim().isEmpty())
            return Undefined.instance;

        com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(json);
        return convertGsonToJs(cx, scope, element);
    }

    private Object convertGsonToJs(Context cx, Scriptable scope, com.google.gson.JsonElement element) {
        if (element.isJsonNull())
            return null;
        if (element.isJsonPrimitive()) {
            com.google.gson.JsonPrimitive prim = element.getAsJsonPrimitive();
            if (prim.isBoolean())
                return prim.getAsBoolean();
            if (prim.isNumber())
                return prim.getAsDouble();
            return prim.getAsString();
        }
        if (element.isJsonArray()) {
            com.google.gson.JsonArray arr = element.getAsJsonArray();
            Object[] items = new Object[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                items[i] = convertGsonToJs(cx, scope, arr.get(i));
            }
            return cx.newArray(scope, items);
        }
        if (element.isJsonObject()) {
            Scriptable obj = cx.newObject(scope);
            com.google.gson.JsonObject jsonObj = element.getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : jsonObj.entrySet()) {
                obj.put(entry.getKey(), obj, convertGsonToJs(cx, scope, entry.getValue()));
            }
            return obj;
        }
        return Undefined.instance;
    }

    private boolean deepEquals(Object a, Object b) {
        if (a == b)
            return true;
        if (a == null || b == null)
            return false;
        if (a instanceof Number na && b instanceof Number nb) {
            return na.doubleValue() == nb.doubleValue();
        }
        return a.toString().equals(b.toString());
    }

    private String stringify(Object o) {
        if (o == null || o instanceof Undefined)
            return "undefined";
        if (o instanceof String)
            return "\"" + o + "\"";
        return o.toString();
    }

    private String getJsType(Object o) {
        if (o == null)
            return "null";
        if (o instanceof Undefined)
            return "undefined";
        if (o instanceof Boolean)
            return "boolean";
        if (o instanceof Number)
            return "number";
        if (o instanceof String || o instanceof CharSequence)
            return "string";
        if (o instanceof NativeArray)
            return "array";
        if (o instanceof Function)
            return "function";
        if (o instanceof Scriptable)
            return "object";
        return "object";
    }

    private boolean isTruthy(Object o) {
        if (o == null || o instanceof Undefined)
            return false;
        if (o instanceof Boolean b)
            return b;
        if (o instanceof Number n)
            return n.doubleValue() != 0;
        if (o instanceof String s)
            return !s.isEmpty();
        return true;
    }
}


