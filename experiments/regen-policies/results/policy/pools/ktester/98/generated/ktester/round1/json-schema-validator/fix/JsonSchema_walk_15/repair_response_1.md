```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean, ExecutionContextCustomizer)}.
 */
class JsonSchema_walk_15_Test {

    /**
     * Factory used to create {@link JsonSchema} instances for the tests.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a very small schema – an empty object schema – which does not impose any constraints.
        // This keeps the walk logic simple and deterministic.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
        factory = null;
    }

    /**
     * Simple reflection helper to read a private attribute from {@link ExecutionContext}.
     */
    private static Object getAttribute(ExecutionContext ctx, String name) throws Exception {
        java.lang.reflect.Method m = ExecutionContext.class.getDeclaredMethod("getAttribute", String.class);
        m.setAccessible(true);
        return m.invoke(ctx, name);
    }

    /**
     * Verifies that {@code walk} returns the value produced by the supplied {@link OutputFormat}.
     * The schema used is an empty object schema, therefore the deserialized node will be an
     * empty {@link com.fasterxml.jackson.databind.node.ObjectNode}. The {@code OutputFormat}
     * implementation simply returns the JSON string representation of that node.
     */
    @Test
    void walkReturnsOutputFormatResult() {
        // GIVEN
        String schemaData = "{ \"type\": \"object\" }";
        JsonSchema schema = factory.getSchema(schemaData);
        ExecutionContext executionContext = new ExecutionContext();
        String input = "{}";
        InputFormat inputFormat = InputFormat.JSON;

        // OutputFormat receives a {@link ValidationResult}; we simply return its string form.
        OutputFormat<String> outputFormat = new OutputFormat<String>() {
            @Override
            public String apply(ValidationResult result) {
                return result.toString();
            }
        };

        // WHEN
        String result = schema.walk(executionContext, input, inputFormat, outputFormat, false, null);

        // THEN
        assertEquals("{}", result,
                "The walk method should return the JSON string produced by the OutputFormat");
    }

    /**
     * When validation is requested (the {@code validate} flag is {@code true}) the walk
     * process still returns the value from the {@link OutputFormat}. This test uses a schema
     * that will produce a validation error and checks that the output format is still
     * honoured.
     */
    @Test
    void walkWithValidationReturnsOutputFormatResult() {
        // GIVEN
        String schemaData = "{\n" + "  \"type\": \"object\",\n"
                + "  \"properties\": { \"name\": { \"type\": \"string\" } },\n"
                + "  \"required\": [\"name\"]\n" + "}";
        JsonSchema schema = factory.getSchema(schemaData);
        ExecutionContext executionContext = new ExecutionContext();
        String input = "{}";
        InputFormat inputFormat = InputFormat.JSON;

        // OutputFormat that ignores the ValidationResult and returns a constant marker.
        OutputFormat<String> outputFormat = new OutputFormat<String>() {
            @Override
            public String apply(ValidationResult result) {
                return "output-called";
            }
        };

        // WHEN
        String result = schema.walk(executionContext, input, inputFormat, outputFormat, true, null);

        // THEN
        assertEquals("output-called",
                result,
                "Even when validation fails, the walk method must return the value from the OutputFormat");
    }

    @Test
    @DisplayName("Successful walk with validation enabled")
    void successfulWalkWithValidationEnabled() {
        // Schema requires a string property "name".
        String schemaJson = "{"
                + "\"type\":\"object\","
                + "\"properties\":{\"name\":{\"type\":\"string\"}},"
                + "\"required\":[\"name\"]"
                + "}";
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContext ctx = new ExecutionContext();
        String input = "{\"name\":\"John\"}";
        InputFormat format = InputFormat.JSON;

        // Collecting output format – simply returns the ValidationResult unchanged.
        OutputFormat<ValidationResult> out = result -> result;

        ValidationResult result = schema.walk(ctx, input, format, out, true, null);
        assertTrue(result.getMessages().isEmpty(),
                "Validation should succeed and therefore contain no messages");
    }

    @Test
    @DisplayName("Successful walk with validation disabled")
    void successfulWalkWithValidationDisabled() {
        // Schema does not matter because validation is disabled.
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContext ctx = new ExecutionContext();
        String input = "{\"age\":30}";
        OutputFormat<ValidationResult> out = result -> result;
        ValidationResult result = schema.walk(ctx, input, InputFormat.JSON, out, false, null);
        assertTrue(result.getMessages().isEmpty(),
                "When validation is disabled the result must contain no messages");
    }

    @Test
    @DisplayName("Walk with custom ExecutionContextCustomizer")
    void walkWithCustomExecutionContextCustomizer() throws Exception {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContext ctx = new ExecutionContext();

        // The customizer mutates the context – it does not return a value.
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {
            @Override
            public void customize(ExecutionContext c) {
                c.setAttribute("custom", "value");
            }
        };

        OutputFormat<ValidationResult> out = result -> result;

        ValidationResult result = schema.walk(ctx, "{\"id\":1}", InputFormat.JSON, out, true, customizer);
        // The result itself should be successful because the schema is empty.
        assertTrue(result.getMessages().isEmpty());

        // Verify that the customizer really mutated the context.
        Object attr = getAttribute(ctx, "custom");
        assertEquals("value", attr, "Custom attribute must be present after the customizer runs");
    }

    @Test
    @DisplayName("Walk with null ExecutionContext")
    void walkWithNullExecutionContext() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.walk(null, "{\"a\":true}", InputFormat.JSON,
                        result -> result, true, null),
                "A null ExecutionContext should cause NullPointerException");
    }

    @Test
    @DisplayName("Walk with unsupported InputFormat (YAML)")
    void walkWithUnsupportedInputFormat() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        // the library wraps the cause in a RuntimeException
        assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(), "some text", InputFormat.YAML,
                        result -> result, true, null),
                "Unsupported InputFormat should result in an exception");
    }

    @Test
    @DisplayName("Walk with malformed JSON input")
    void walkWithMalformedJsonInput() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        String malformed = "{\"unclosed\": [1,2,3\"}";
        // JsonProcessingException extends IOException
        assertThrows(IOException.class,
                () -> schema.walk(new ExecutionContext(), malformed, InputFormat.JSON,
                        result -> result, true, null),
                "Malformed JSON should cause an IOException during deserialization");
    }

    @Test
    @DisplayName("Walk with custom OutputFormat returning domain object")
    void walkWithCustomOutputFormatReturningString() {
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"type\":{\"type\":\"string\"}}}";
        JsonSchema schema = factory.getSchema(schemaJson);
        OutputFormat<String> fmt = new OutputFormat<String>() {
            @Override
            public String apply(ValidationResult result) {
                return result.getMessages().isEmpty() ? "OK" : "FAIL";
            }
        };
        String result = schema.walk(new ExecutionContext(), "{\"type\":\"string\"}",
                InputFormat.JSON, fmt, true, null);
        assertEquals("OK", result,
                "Custom OutputFormat should return \"OK\" when validation succeeds");
    }

    @Test
    @DisplayName("Validation failure returns messages")
    void validationFailureReturnsMessages() {
        String schemaJson = "{"
                + "\"type\":\"object\","
                + "\"properties\":{\"age\":{\"type\":\"number\"}}"
                + "}";
        JsonSchema schema = factory.getSchema(schemaJson);
        ValidationResult result = schema.walk(new ExecutionContext(),
                "{\"age\":\"twenty\"}", InputFormat.JSON,
                r -> r, true, null);
        Set<ValidationMessage> msgs = result.getMessages();
        assertFalse(msgs.isEmpty(),
                "Validation should fail and produce at least one message");
        // Simple sanity check – the message should mention the property "age".
        boolean mentionsAge = msgs.stream().anyMatch(m -> m.getPath().contains("age"));
        assertTrue(mentionsAge,
                "At least one validation message should refer to the 'age' property");
    }

    @Test
    @DisplayName("Null outputFormat causes NullPointerException")
    void nullOutputFormatCausesNPE() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"valid\":true}", InputFormat.JSON, null, true, null),
                "A null OutputFormat must trigger NullPointerException");
    }

    @Test
    @DisplayName("Null inputFormat causes NullPointerException")
    void nullInputFormatCausesNPE() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"x\":1}", null, result -> result, true, null),
                "A null InputFormat must trigger NullPointerException");
    }

    @Test
    @DisplayName("ExecutionCustomizer throws RuntimeException")
    void executionCustomizerThrowsRuntimeException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContextCustomizer badCustomizer = new ExecutionContextCustomizer() {
            @Override
            public void customize(ExecutionContext ctx) {
                throw new RuntimeException("customizer failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"a\":1}", InputFormat.JSON, result -> result, true, badCustomizer),
                "Customizer exception must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    @DisplayName("Empty input string causes JsonProcessingException")
    void emptyInputStringCausesException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IOException.class,
                () -> schema.walk(new ExecutionContext(), "", InputFormat.JSON,
                        result -> result, true, null),
                "Empty input should cause a parsing exception");
    }

    @Test
    @DisplayName("Whitespace‑only input string causes JsonProcessingException")
    void whitespaceOnlyInputStringCausesException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IOException.class,
                () -> schema.walk(new ExecutionContext(), "   \n\t  ", InputFormat.JSON,
                        result -> result, true, null),
                "Whitespace‑only input should cause a parsing exception");
    }

    @Test
    @DisplayName("Null input string causes NullPointerException")
    void nullInputStringCausesNPE() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class,
                () -> schema.walk(new ExecutionContext(), null, InputFormat.JSON,
                        result -> result, true, null),
                "Null input should trigger NullPointerException");
    }

    @Test
    @DisplayName("OutputFormat throws RuntimeException")
    void outputFormatThrowsRuntimeException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        OutputFormat<Object> badFmt = new OutputFormat<Object>() {
            @Override
            public Object apply(ValidationResult result) {
                throw new RuntimeException("output failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"key\":\"value\"}", InputFormat.JSON, badFmt, true, null),
                "Exception from OutputFormat must be propagated");
        assertEquals("output failure", ex.getMessage());
    }

    @Test
    @DisplayName("Deserialize throws UnsupportedEncodingException due to malformed URL encoding")
    void deserializeThrowsUnsupportedEncodingException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        // Using JSON format; malformed input leads to an IOException (which may be an
        // UnsupportedEncodingException depending on the implementation). We assert the
        // broader IOException to keep the test portable.
        assertThrows(IOException.class,
                () -> schema.walk(new ExecutionContext(), "%ZZ", InputFormat.JSON,
                        result -> result, true, null),
                "Malformed URL encoding (or similar malformed input) should cause an IOException");
    }

    @Test
    @DisplayName("ExecutionContextCustomizer throws checked exception wrapped in RuntimeException")
    void executionCustomizerCheckedExceptionWrapped() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {
            @Override
            public void customize(ExecutionContext ctx) {
                try {
                    throw new java.io.IOException("io failure");
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"a\":1}", InputFormat.JSON, result -> result, true, customizer),
                "Checked exception wrapped in RuntimeException must be propagated");
        assertTrue(ex.getCause() instanceof java.io.IOException);
        assertEquals("io failure", ex.getCause().getMessage());
    }
}
```