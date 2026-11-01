```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean)}.
 *
 * The tests cover a wide range of scenarios described in the provided JSON test matrix,
 * including successful walks, validation toggles, malformed inputs, null arguments,
 * unsupported formats and exceptional {@link OutputFormat} implementations.
 *
 * The schema used in all tests is the most permissive one – an empty JSON object (“{}”)
 * – so that validation does not reject any well‑formed JSON payload.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaWalkTest {

    private static final String PERMISSIVE_SCHEMA = "{}";

    private JsonSchemaFactory factory;
    private JsonSchema schema;

    @BeforeEach
    void setUp() {
        // Use the latest draft supported by the library.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(PERMISSIVE_SCHEMA);
    }

    /* --------------------------------------------------------------
     * Helper OutputFormat implementations used throughout the suite.
     * -------------------------------------------------------------- */

    /** Simple {@link OutputFormat} that returns a pre‑configured result. */
    private static class SimpleStringOutputFormat implements OutputFormat<String> {
        private final String result;

        SimpleStringOutputFormat(String result) {
            this.result = result;
        }

        @Override
        public String apply(Set<ValidationMessage> messages) {
            return result;
        }
    }

    /** {@link OutputFormat} that throws a {@link RuntimeException} when applied. */
    private static class ThrowingOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            throw new RuntimeException("OutputFormat failure");
        }
    }

    /** {@link OutputFormat} that returns {@code null}. */
    private static class NullReturningOutputFormat implements OutputFormat<String> {
        @Override
        public String apply(Set<ValidationMessage> messages) {
            return null;
        }
    }

    /* --------------------------------------------------------------
     * 1. Valid Input – Validation Enabled
     * -------------------------------------------------------------- */
    @Test
    void walk_ValidInput_ValidationEnabled_ReturnsSuccessMessage() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation succeeded");

        String result = schema.walk(ctx,
                "{\"name\":\"John\"}",
                InputFormat.JSON,
                format,
                true);

        assertEquals("Validation succeeded", result);
    }

    /* --------------------------------------------------------------
     * 2. Valid Input – Validation Disabled
     * -------------------------------------------------------------- */
    @Test
    void walk_ValidInput_ValidationDisabled_ReturnsSkippedMessage() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation skipped");

        String result = schema.walk(ctx,
                "{\"age\":30}",
                InputFormat.JSON,
                format,
                false);

        assertEquals("Validation skipped", result);
    }

    /* --------------------------------------------------------------
     * 3. Invalid JSON Input
     * -------------------------------------------------------------- */
    @Test
    void walk_InvalidJsonInput_ThrowsJsonProcessingException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("won't be used");

        assertThrows(JsonProcessingException.class, () ->
                schema.walk(ctx,
                        "{invalid json}",
                        InputFormat.JSON,
                        format,
                        true));
    }

    /* --------------------------------------------------------------
     * 4. Null ExecutionContext – method should create a default one.
     * -------------------------------------------------------------- */
    @Test
    void walk_NullExecutionContext_CreatesDefaultAndReturnsSuccess() {
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation succeeded");

        String result = schema.walk(null,
                "{\"id\":1}",
                InputFormat.JSON,
                format,
                true);

        assertEquals("Validation succeeded", result);
    }

    /* --------------------------------------------------------------
     * 5. Unsupported InputFormat (e.g., YAML) – expect UnsupportedOperationException.
     * -------------------------------------------------------------- */
    @Test
    void walk_UnsupportedInputFormat_ThrowsUnsupportedOperationException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("won't be used");

        assertThrows(UnsupportedOperationException.class, () ->
                schema.walk(ctx,
                        "some text",
                        InputFormat.YAML,   // assume YAML is not supported by the current version
                        format,
                        true));
    }

    /* --------------------------------------------------------------
     * 6. OutputFormat that throws RuntimeException.
     * -------------------------------------------------------------- */
    @Test
    void walk_OutputFormatThrowsException_PropagatesRuntimeException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new ThrowingOutputFormat();

        assertThrows(RuntimeException.class, () ->
                schema.walk(ctx,
                        "{\"name\":\"Alice\"}",
                        InputFormat.JSON,
                        format,
                        true));
    }

    /* --------------------------------------------------------------
     * 7. Empty JSON Object – Validation Enabled
     * -------------------------------------------------------------- */
    @Test
    void walk_EmptyJsonObject_ValidationEnabled_ReturnsSuccess() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation succeeded");

        String result = schema.walk(ctx,
                "{}",
                InputFormat.JSON,
                format,
                true);

        assertEquals("Validation succeeded", result);
    }

    /* --------------------------------------------------------------
     * 8. JSON Literal Null – Validation Enabled
     * -------------------------------------------------------------- */
    @Test
    void walk_JsonLiteralNull_ValidationEnabled_ReturnsSuccess() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation succeeded");

        String result = schema.walk(ctx,
                "null",
                InputFormat.JSON,
                format,
                true);

        assertEquals("Validation succeeded", result);
    }

    /* --------------------------------------------------------------
     * 9. JSON Array Input – Validation Enabled
     * -------------------------------------------------------------- */
    @Test
    void walk_JsonArrayInput_ValidationEnabled_ReturnsSuccess() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation succeeded");

        String result = schema.walk(ctx,
                "[1,2,3]",
                InputFormat.JSON,
                format,
                true);

        assertEquals("Validation succeeded", result);
    }

    /* --------------------------------------------------------------
     * 10. Whitespace‑Only Input – should fail deserialization.
     * -------------------------------------------------------------- */
    @Test
    void walk_WhitespaceOnlyInput_ThrowsJsonProcessingException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("won't be used");

        assertThrows(JsonProcessingException.class, () ->
                schema.walk(ctx,
                        "   ",
                        InputFormat.JSON,
                        format,
                        true));
    }

    /* --------------------------------------------------------------
     * 11. Null Input String – expect NullPointerException (or IllegalArgumentException).
     * -------------------------------------------------------------- */
    @Test
    void walk_NullInputString_ThrowsNullPointerException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("won't be used");

        assertThrows(NullPointerException.class, () ->
                schema.walk(ctx,
                        null,
                        InputFormat.JSON,
                        format,
                        true));
    }

    /* --------------------------------------------------------------
     * 12. Null OutputFormat – should result in NullPointerException.
     * -------------------------------------------------------------- */
    @Test
    void walk_NullOutputFormat_ThrowsNullPointerException() {
        ExecutionContext ctx = new ExecutionContext();

        assertThrows(NullPointerException.class, () ->
                schema.walk(ctx,
                        "{\"key\":\"value\"}",
                        InputFormat.JSON,
                        null,
                        true));
    }

    /* --------------------------------------------------------------
     * 13. OutputFormat Returns Null – the walk method must propagate the null.
     * -------------------------------------------------------------- */
    @Test
    void walk_OutputFormatReturnsNull_PropagatesNullResult() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new NullReturningOutputFormat();

        String result = schema.walk(ctx,
                "{\"name\":\"Bob\"}",
                InputFormat.JSON,
                format,
                true);

        assertNull(result, "The walk method should return null when the OutputFormat returns null");
    }

    /* --------------------------------------------------------------
     * 14. ExecutionContext with Customizer (No Effect)
     * -------------------------------------------------------------- */
    @Test
    void walk_ExecutionContextWithCustomizer_NoEffectOnResult() {
        // Create a custom ExecutionContext (e.g., with a different schema version). For the
        // purpose of the test we just instantiate a fresh one – the library does not expose
        // a public API to add a customizer to the five‑parameter overload.
        ExecutionContext customCtx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("Validation succeeded");

        String result = schema.walk(customCtx,
                "{\"id\":123}",
                InputFormat.JSON,
                format,
                true);

        assertEquals("Validation succeeded", result);
    }

    /* --------------------------------------------------------------
     * 15. Null InputFormat – should cause deserialization to fail with NPE.
     * -------------------------------------------------------------- */
    @Test
    void walk_NullInputFormat_ThrowsNullPointerException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("won't be used");

        assertThrows(NullPointerException.class, () ->
                schema.walk(ctx,
                        "{\"key\":\"value\"}",
                        null,
                        format,
                        true));
    }

    /* --------------------------------------------------------------
     * 16. Empty String Input – not a valid JSON document.
     * -------------------------------------------------------------- */
    @Test
    void walk_EmptyStringInput_ThrowsJsonProcessingException() {
        ExecutionContext ctx = new ExecutionContext();
        OutputFormat<String> format = new SimpleStringOutputFormat("won't be used");

        assertThrows(JsonProcessingException.class, () ->
                schema.walk(ctx,
                        "",
                        InputFormat.JSON,
                        format,
                        true));
    }
}
```