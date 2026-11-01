```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.*;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Additional unit tests for {@link JsonSchema#walk(ExecutionContext, String, InputFormat, OutputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * <p>The tests cover the scenarios described in the JSON test‑case specification.
 * They use the real {@link JsonSchemaFactory} implementation (no heavy mocking is required)
 * and focus on the behaviour of the {@code walk} method itself.</p>
 */
class JsonSchema_walk_Additional_Tests {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void beforeAll() {
        // Use the 2020‑12 version – it is the default in the original test class.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterAll
    static void afterAll() {
        factory = null;
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                    */
    /* --------------------------------------------------------------------- */

    /** Simple reflection helper to read a private attribute from {@link ExecutionContext}. */
    private static Object getAttribute(ExecutionContext ctx, String name) throws Exception {
        Method m = ExecutionContext.class.getDeclaredMethod("getAttribute", String.class);
        m.setAccessible(true);
        return m.invoke(ctx, name);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Successful walk with validation enabled                             */
    /* --------------------------------------------------------------------- */

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
        OutputFormat<ValidationResult> out = new CollectingOutputFormat<>();

        ValidationResult result = schema.walk(ctx, input, format, out, true, null);

        assertTrue(result.getMessages().isEmpty(),
                "Validation should succeed and therefore contain no messages");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Successful walk with validation disabled                            */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Successful walk with validation disabled")
    void successfulWalkWithValidationDisabled() {
        // Schema does not matter because validation is disabled.
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        ExecutionContext ctx = new ExecutionContext();
        String input = "{\"age\":30}";
        OutputFormat<ValidationResult> out = new CollectingOutputFormat<>();

        ValidationResult result = schema.walk(ctx, input, InputFormat.JSON, out, false, null);

        assertTrue(result.getMessages().isEmpty(),
                "When validation is disabled the result must contain no messages");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Walk with custom ExecutionContextCustomizer                         */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Walk with custom ExecutionContextCustomizer")
    void walkWithCustomExecutionContextCustomizer() throws Exception {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        ExecutionContext ctx = new ExecutionContext();
        ExecutionContextCustomizer customizer = c -> c.setAttribute("custom", "value");

        ValidationResult result = schema.walk(
                ctx,
                "{\"id\":1}",
                InputFormat.JSON,
                new CollectingOutputFormat<>(),
                true,
                customizer);

        // The result itself should be successful because the schema is empty.
        assertTrue(result.getMessages().isEmpty());

        // Verify that the customizer really mutated the context.
        Object attr = getAttribute(ctx, "custom");
        assertEquals("value", attr, "Custom attribute must be present after the customizer runs");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Walk with null ExecutionContext                                      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Walk with null ExecutionContext")
    void walkWithNullExecutionContext() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(NullPointerException.class,
                () -> schema.walk(null, "{\"a\":true}", InputFormat.JSON,
                        new CollectingOutputFormat<>(), true, null),
                "A null ExecutionContext should cause NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Walk with unsupported InputFormat (YAML)                           */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Walk with unsupported InputFormat (YAML)")
    void walkWithUnsupportedInputFormat() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(RuntimeException.class, // the library wraps the cause in a RuntimeException
                () -> schema.walk(new ExecutionContext(),
                        "some text",
                        InputFormat.YAML,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "Unsupported InputFormat should result in an exception");
    }

    /* --------------------------------------------------------------------- */
    /* 6. Walk with malformed JSON input                                      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Walk with malformed JSON input")
    void walkWithMalformedJsonInput() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        String malformed = "{\"unclosed\": [1,2,3\"}";
        assertThrows(IOException.class, // JsonProcessingException extends IOException
                () -> schema.walk(new ExecutionContext(),
                        malformed,
                        InputFormat.JSON,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "Malformed JSON should cause an IOException during deserialization");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Walk with custom OutputFormat returning a domain object              */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Walk with custom OutputFormat returning domain object")
    void walkWithCustomOutputFormatReturningString() {
        String schemaJson = "{\"type\":\"object\",\"properties\":{\"type\":{\"type\":\"string\"}}}";
        JsonSchema schema = factory.getSchema(schemaJson);

        OutputFormat<String> fmt = msgs -> msgs.isEmpty() ? "OK" : "FAIL";

        String result = schema.walk(new ExecutionContext(),
                "{\"type\":\"string\"}",
                InputFormat.JSON,
                fmt,
                true,
                null);

        assertEquals("OK", result, "Custom OutputFormat should return \"OK\" when validation succeeds");
    }

    /* --------------------------------------------------------------------- */
    /* 8. Validation failure returns messages                                 */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Validation failure returns messages")
    void validationFailureReturnsMessages() {
        String schemaJson = "{"
                + "\"type\":\"object\","
                + "\"properties\":{\"age\":{\"type\":\"number\"}}"
                + "}";
        JsonSchema schema = factory.getSchema(schemaJson);

        ValidationResult result = schema.walk(new ExecutionContext(),
                "{\"age\":\"twenty\"}",
                InputFormat.JSON,
                new CollectingOutputFormat<>(),
                true,
                null);

        Set<ValidationMessage> msgs = result.getMessages();
        assertFalse(msgs.isEmpty(), "Validation should fail and produce at least one message");
        // Simple sanity check – the message should mention the property "age".
        boolean mentionsAge = msgs.stream().anyMatch(m -> m.getPath().contains("age"));
        assertTrue(mentionsAge, "At least one validation message should refer to the 'age' property");
    }

    /* --------------------------------------------------------------------- */
    /* 9. Null outputFormat causes NullPointerException                      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Null outputFormat causes NullPointerException")
    void nullOutputFormatCausesNPE() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(NullPointerException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"valid\":true}",
                        InputFormat.JSON,
                        null,
                        true,
                        null),
                "A null OutputFormat must trigger NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 10. Null inputFormat causes NullPointerException                       */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Null inputFormat causes NullPointerException")
    void nullInputFormatCausesNPE() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(NullPointerException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"x\":1}",
                        null,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "A null InputFormat must trigger NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 11. ExecutionCustomizer throws RuntimeException                        */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("ExecutionCustomizer throws RuntimeException")
    void executionCustomizerThrowsRuntimeException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"a\":1}",
                        InputFormat.JSON,
                        new CollectingOutputFormat<>(),
                        true,
                        badCustomizer),
                "Customizer exception must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 12. Empty input string causes JsonProcessingException                    */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Empty input string causes JsonProcessingException")
    void emptyInputStringCausesException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(IOException.class,
                () -> schema.walk(new ExecutionContext(),
                        "",
                        InputFormat.JSON,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "Empty input should cause a parsing exception");
    }

    /* --------------------------------------------------------------------- */
    /* 13. Whitespace‑only input string causes JsonProcessingException        */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Whitespace‑only input string causes JsonProcessingException")
    void whitespaceOnlyInputStringCausesException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(IOException.class,
                () -> schema.walk(new ExecutionContext(),
                        "   \n\t  ",
                        InputFormat.JSON,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "Whitespace‑only input should cause a parsing exception");
    }

    /* --------------------------------------------------------------------- */
    /* 14. Null input string causes NullPointerException                      */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Null input string causes NullPointerException")
    void nullInputStringCausesNPE() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(NullPointerException.class,
                () -> schema.walk(new ExecutionContext(),
                        null,
                        InputFormat.JSON,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "Null input should trigger NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /* 15. OutputFormat throws RuntimeException                               */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("OutputFormat throws RuntimeException")
    void outputFormatThrowsRuntimeException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        OutputFormat<Object> badFmt = node -> {
            throw new RuntimeException("output failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"key\":\"value\"}",
                        InputFormat.JSON,
                        badFmt,
                        true,
                        null),
                "Exception from OutputFormat must be propagated");
        assertEquals("output failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 16. Deserialize throws UnsupportedEncodingException (malformed URL)   */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("Deserialize throws UnsupportedEncodingException due to malformed URL encoding")
    void deserializeThrowsUnsupportedEncodingException() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        assertThrows(UnsupportedEncodingException.class,
                () -> schema.walk(new ExecutionContext(),
                        "%ZZ",
                        InputFormat.URL,
                        new CollectingOutputFormat<>(),
                        true,
                        null),
                "Malformed URL encoding should cause UnsupportedEncodingException");
    }

    /* --------------------------------------------------------------------- */
    /* 17. ExecutionContextCustomizer throws checked exception wrapped in RuntimeException */
    /* --------------------------------------------------------------------- */

    @Test
    @DisplayName("ExecutionContextCustomizer throws checked exception wrapped in RuntimeException")
    void executionCustomizerCheckedExceptionWrapped() {
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchema schema = factory.getSchema(schemaJson);

        ExecutionContextCustomizer customizer = ctx -> {
            try {
                throw new java.io.IOException("io failure");
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(new ExecutionContext(),
                        "{\"a\":1}",
                        InputFormat.JSON,
                        new CollectingOutputFormat<>(),
                        true,
                        customizer),
                "Checked exception wrapped in RuntimeException must be propagated");
        assertTrue(ex.getCause() instanceof java.io.IOException);
        assertEquals("io failure", ex.getCause().getMessage());
    }
}
```