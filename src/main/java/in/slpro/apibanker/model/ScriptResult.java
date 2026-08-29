package in.slpro.apibanker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the results of executing a pre-request or test script,
 * including test assertions (pass/fail), console log output, and any errors.
 */
/**
 * ScriptResult
 *
 * <p>
 * Core functionality and implementation logic for ScriptResult.
 * </p>
 *
 * @author Naveen Chauhan (https://github.com/ncrkindia)
 * @version 2.0.1
 * @since 1.0.0
 */
public class ScriptResult {
    private final List<TestAssertion> assertions = new ArrayList<>();
    private final List<String> consoleLogs = new ArrayList<>();
    private String error;

    public void addAssertion(String name, boolean passed, String failureMessage) {
        assertions.add(new TestAssertion(name, passed, failureMessage));
    }

    public void addConsoleLog(String message) {
        consoleLogs.add(message);
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<TestAssertion> getAssertions() {
        return assertions;
    }

    public List<String> getConsoleLogs() {
        return consoleLogs;
    }

    public String getError() {
        return error;
    }

    public int getPassedCount() {
        return (int) assertions.stream().filter(TestAssertion::isPassed).count();
    }

    public int getFailedCount() {
        return (int) assertions.stream().filter(a -> !a.isPassed()).count();
    }

    public int getTotalCount() {
        return assertions.size();
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    /**
     * Represents a single test assertion with name, pass/fail status, and optional
     * failure message.
     */
    public static class TestAssertion {
        private final String name;
        private final boolean passed;
        private final String failureMessage;

        public TestAssertion(String name, boolean passed, String failureMessage) {
            this.name = name;
            this.passed = passed;
            this.failureMessage = failureMessage;
        }

        public String getName() {
            return name;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getFailureMessage() {
            return failureMessage;
        }
    }
}

