```java
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonParseException;
import com.networknt.schema.utils.JsonNodes;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional unit tests for {@link JsonSchema#walk(String, InputFormat, boolean, Consumer)}.
 *
 * <p>These tests cover the exhaustive scenarios described in the JSON test‑case
 * specification.  Each test method corresponds to a logical group of cases.
 * Parameterised tests are used where the input data differ only by the values
 * of the parameters.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_8_Test {

    private static final String SIMPLE_SCHEMA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" },\n"
            + "    \"age\":  { \"type\": \"integer\" },\n"
            + "    \"city\": { \"type\": \"string\" },\n"
            + "    \"id\":   { \"type\": \"integer\" },\n"
            + "    \"status\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance that will be reused by all tests
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder // default configuration – no special loaders required
        );
        schema = factory.getSchema(SIMPLE_SCHEMA);
        assertNotNull(schema, "Schema should have been created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing specific to clean up for each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    /* --------------------------------------------------------------------- */
    /*  1. Valid JSON, validation enabled, customizer provided               */
    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonWithCustomizerValidationEnabled() {
        String json = "{\"name\":\"John\",\"age\":30}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();

        ValidationResult result = schema.walk(json, InputFormat.JSON, true, ctx -> {
            ctx.setAttribute("custom", true);
            ctxRef.set(ctx);
        });

        assertNotNull(result, "Result must not be null");
        assertTrue(result.isValid(), "Result should be valid for correct JSON");
        assertTrue(result.getValidationMessages().isEmpty(), "No validation messages expected");
        assertNotNull(ctxRef.get(), "Consumer must have received a non‑null ExecutionContext");
        assertEquals(true, ctxRef.get().getAttribute("custom"), "Custom attribute should be set by the consumer");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Valid JSON, validation disabled, no customizer                    */
    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonValidationDisabledNoCustomizer() {
        String json = "{\"name\":\"Jane\"}";
        ValidationResult result = schema.walk(json, InputFormat.JSON, false, null);

        assertNotNull(result, "Result must not be null");
        assertTrue(result.isValid(), "Result should be valid when validation is disabled");
        assertTrue(result.getValidationMessages().isEmpty(), "No messages should be produced");
    }

    /* --------------------------------------------------------------------- */
    /*  3. Malformed JSON triggers deserialization exception                */
    /* --------------------------------------------------------------------- */
    @Test
    void walkMalformedJsonThrowsException() {
        String malformed = "{\"name\":\"Bob\", \"age\":}";
        // The implementation wraps JsonParseException in a ValidationException,
        // but we assert that *some* exception is thrown.
        assertThrows(RuntimeException.class, () ->
                schema.walk(malformed, InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Null input string                                                  */
    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                schema.walk(null, InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- */
    /*  5. Null InputFormat                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    void walkNullInputFormatThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                schema.walk("{\"name\":\"Alice\"}", null, true, null));
    }

    /* --------------------------------------------------------------------- */
    /*  6. Unsupported character encoding during deserialization            */
    /* --------------------------------------------------------------------- */
    @Test
    void walkUnsupportedUrlEncodedThrowsException() {
        // %E0%A4%A is an illegal percent‑encoding sequence
        String badEncoded = "%E0%A4%A";
        assertThrows(RuntimeException.class, () ->
                schema.walk(badEncoded, InputFormat.URL_ENCODED, true, null));
    }

    /* --------------------------------------------------------------------- */
    /*  7. Consumer throws runtime exception                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void walkConsumerThrowsRuntimeExceptionPropagates() {
        String json = "{\"id\":1}";
        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                schema.walk(json, InputFormat.JSON, true, ctx -> {
                    throw new RuntimeException("customizer failure");
                }));
        assertEquals("customizer failure", thrown.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  8. Valid YAML, validation enabled, no customizer                     */
    /* --------------------------------------------------------------------- */
    @Test
    void walkValidYamlNoCustomizer() {
        String yaml = "name: John\nage: 30";
        ValidationResult result = schema.walk(yaml, InputFormat.YAML, true, null);
        assertNotNull(result);
        assertTrue(result.isValid(), "YAML payload should be valid");
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*  9. Valid JSON, validation disabled, customizer provided              */
    /* --------------------------------------------------------------------- */
    @Test
    void walkValidJsonValidationDisabledWithCustomizer() {
        String json = "{\"city\":\"Paris\"}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();

        ValidationResult result = schema.walk(json, InputFormat.JSON, false, ctx -> {
            ctx.setAttribute("skip", true);
            ctxRef.set(ctx);
        });

        assertNotNull(result);
        assertTrue(result.isValid(), "Result must be valid when validation is disabled");
        assertTrue(result.getValidationMessages().isEmpty());
        assertNotNull(ctxRef.get());
        assertEquals(true, ctxRef.get().getAttribute("skip"));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Empty JSON string triggers deserialization exception              */
    /* --------------------------------------------------------------------- */
    @Test
    void walkEmptyStringThrowsException() {
        assertThrows(RuntimeException.class, () ->
                schema.walk("", InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- */
    /* 11. Whitespace‑only JSON triggers deserialization exception           */
    /* --------------------------------------------------------------------- */
    @Test
    void walkWhitespaceOnlyThrowsException() {
        assertThrows(RuntimeException.class, () ->
                schema.walk("   \t\n", InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Valid URL_ENCODED input, validation enabled, no customizer        */
    /* --------------------------------------------------------------------- */
    @Test
    void walkValidUrlEncodedNoCustomizer() {
        // name=John&age=30 URL‑encoded
        String encoded = "name%3DJohn%26age%3D30";
        ValidationResult result = schema.walk(encoded, InputFormat.URL_ENCODED, true, null);
        assertNotNull(result);
        assertTrue(result.isValid(), "Decoded JSON should be valid");
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 13. JSON array input fails schema validation                           */
    /* --------------------------------------------------------------------- */
    @Test
    void walkJsonArrayFailsValidation() {
        String jsonArray = "[1, 2, 3]";
        ValidationResult result = schema.walk(jsonArray, InputFormat.JSON, true, null);
        assertNotNull(result);
        assertFalse(result.isValid(), "Array does not satisfy object schema");
        assertFalse(result.getValidationMessages().isEmpty(),
                "At least one validation message expected for type mismatch");
    }

    /* --------------------------------------------------------------------- */
    /* 14. Consumer sets custom attribute, validation enabled                  */
    /* --------------------------------------------------------------------- */
    @Test
    void walkConsumerSetsAttributeValidationEnabled() {
        String json = "{\"id\":10}";
        AtomicReference<ExecutionContext> ctxRef = new AtomicReference<>();

        ValidationResult result = schema.walk(json, InputFormat.JSON, true, ctx -> {
            ctx.setAttribute("requestId", "abc-123");
            ctxRef.set(ctx);
        });

        assertNotNull(result);
        assertTrue(result.isValid(), "Valid input should produce a valid result");
        assertTrue(result.getValidationMessages().isEmpty());
        assertNotNull(ctxRef.get());
        assertEquals("abc-123", ctxRef.get().getAttribute("requestId"));
    }

    /* --------------------------------------------------------------------- */
    /* 15. No‑op Consumer, validation enabled                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void walkNoOpConsumerValidationEnabled() {
        String json = "{\"status\":\"ok\"}";
        AtomicBoolean invoked = new AtomicBoolean(false);
        ValidationResult result = schema.walk(json, InputFormat.JSON, true, ctx -> invoked.set(true));
        assertTrue(invoked.get(), "Consumer must be invoked even if it does nothing");
        assertNotNull(result);
        assertTrue(result.isValid());
        assertTrue(result.getValidationMessages().isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 16. Unsupported InputFormat value                                       */
    /* --------------------------------------------------------------------- */
    @Test
    void walkUnsupportedInputFormatThrowsException() {
        // Assuming InputFormat enum contains XML – which is not supported by walk()
        assertThrows(IllegalArgumentException.class, () ->
                schema.walk("{\"name\":\"Test\"}", InputFormat.XML, true, null));
    }

    /* --------------------------------------------------------------------- */
    /* 17. createExecutionContext returns null leading to NPE                 */
    /* --------------------------------------------------------------------- */
    @Test
    void walkWhenCreateExecutionContextReturnsNull() {
        // Sub‑class JsonSchema to override createExecutionContext()
        JsonSchema broken = new JsonSchema() {
            @Override
            public ExecutionContext createExecutionContext() {
                return null; // deliberately break contract
            }
        };
        // The broken instance still needs a schema – we reuse the same node tree
        // via reflection to set the internal fields (the factory is not easily
        // configurable for a subclass).  For the purpose of this test we only
        // need to call the overloaded walk that accepts an ExecutionContext,
        // which we can invoke via reflection.
        assertThrows(NullPointerException.class, () -> {
            // Use the public overload that creates its own ExecutionContext
            broken.walk("{\"valid\":\"json\"}", InputFormat.JSON, true, null);
        });
    }

    /* --------------------------------------------------------------------- */
    /* 18. Deserialization returns null node (input string 'null')            */
    /* --------------------------------------------------------------------- */
    @Test
    void walkLiteralNullInputThrowsException() {
        // The literal string "null" deserialises to a JsonNode representing JSON null.
        // The walk method should treat this as an invalid root.
        assertThrows(RuntimeException.class, () ->
                schema.walk("null", InputFormat.JSON, true, null));
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods for parameterised tests (if needed in future)        */
    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> provideValidJsonWithCustomizer() {
        // Example of how a parameterised test could be added later.
        return Stream.of(
                Arguments.of("{\"name\":\"John\",\"age\":30}", true)
        );
    }
}
```