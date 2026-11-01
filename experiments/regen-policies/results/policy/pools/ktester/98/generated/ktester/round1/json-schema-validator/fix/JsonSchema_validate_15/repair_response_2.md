```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, ExecutionContextCustomizer)}.
 */
class JsonSchema_validate_15_Test {

    /**
     * Shared schema used by all test cases
     */
    private static JsonSchema schema;

    /**
     * Input format used for the tests (JSON string)
     */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /**
     * Simple {@link OutputFormat} implementation that materialises the set of
     * {@link ValidationMessage}s into a {@link List}.
     */
    private static final OutputFormat<List<ValidationMessage>> LIST_OUTPUT_FORMAT =
            (jsonSchema, messages, ctx, validationCtx) -> new ArrayList<>(messages);

    /**
     * No‑op customiser – required by the method signature but does not alter the
     * execution context.
     */
    private static final ExecutionContextCustomizer NO_OP_CUSTOMIZER =
            (ctx, validationCtx) -> {
                // intentionally left blank
            };

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that expects an object with a string property “name”.
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";

        // Build a factory for the 2020‑12 draft and obtain a JsonSchema instance.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));

        // No special configuration required for these tests.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        schema = factory.getSchema(schemaData, config);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the current test suite – the schema is immutable
        // and can be left for garbage collection.
        schema = null;
    }

    @Test
    void validate_invalidInput_returnsValidationMessages() {
        // Input violates the schema: “name” is an integer, not a string.
        String badInput = "{\"name\":1}";
        List<ValidationMessage> messages = schema.validate(badInput, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER);
        // The schema should produce exactly one validation error about the type.
        assertEquals(1, messages.size(), "Expected a single validation message");
        ValidationMessage vm = messages.get(0);
        assertTrue(vm.getMessage().toLowerCase().contains("type"),
                "Message should mention a type problem, but was: " + vm.getMessage());
    }

    @Test
    void validate_validInput_returnsEmptyList() {
        // Input conforms to the schema.
        String goodInput = "{\"name\":\"John\"}";
        List<ValidationMessage> messages = schema.validate(goodInput, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER);
        // No validation errors expected.
        assertTrue(messages.isEmpty(), "Expected no validation messages for valid input");
    }

    @Test
    void validate_withCustomExecutionContextCustomizer_isInvoked() {
        // Verify that the customiser is actually called by toggling a flag on the
        // execution context.  We use a simple mutable holder for the side‑effect.
        final boolean[] customiserInvoked = {false};
        ExecutionContextCustomizer customizer = (ctx, validationCtx) -> customiserInvoked[0] = true;

        String input = "{\"name\":\"Alice\"}";
        schema.validate(input, INPUT_FORMAT, LIST_OUTPUT_FORMAT, customizer);
        assertTrue(customiserInvoked[0], "ExecutionContextCustomizer should have been invoked");
    }

    @Test
    void validate_nullInput_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate(null, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertEquals("input must not be null", ex.getMessage());
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", null, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertEquals("inputFormat must not be null", ex.getMessage());
    }

    @Test
    void validate_nullOutputFormat_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, null, NO_OP_CUSTOMIZER));
        assertEquals("output format must not be null", ex.getMessage());
    }

    @Test
    void validate_urlEncodedInvalid_throwsWrappedUnsupportedEncodingException() {
        // UrlEncodedInputFormat will try to URL‑decode the string; the bogus value
        // triggers an UnsupportedEncodingException which must be wrapped.
        InputFormat urlEncoded = InputFormat.URL_ENCODED;
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate("%ZZInvalid%", urlEncoded, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException,
                "Cause should be UnsupportedEncodingException");
    }

    @Test
    void validate_malformedJson_throwsWrappedJsonProcessingException() {
        // JsonInputFormat parses the string as JSON – malformed content causes a
        // JsonProcessingException that must be wrapped.
        InputFormat jsonFormat = InputFormat.JSON;
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate("not a json", jsonFormat, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getCause() instanceof JsonProcessingException,
                "Cause should be JsonProcessingException");
    }

    @Test
    void validate_customizerThrows_propagatesException() {
        ExecutionContextCustomizer badCustomizer = (ctx, validationCtx) -> {
            throw new IllegalStateException("customizer failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, LIST_OUTPUT_FORMAT, badCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_outputFormatThrows_propagatesRuntimeException() {
        // OutputFormat that deliberately throws.
        OutputFormat<List<ValidationMessage>> throwingFormat = (jsonSchema, messages, ctx, validationCtx) -> {
            throw new RuntimeException("output format failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, throwingFormat, NO_OP_CUSTOMIZER));
        assertEquals("output format failure", ex.getMessage());
    }

    /**
     * The original test attempted to create a custom {@link InputFormat} that throws
     * an {@link IOException}.  Since {@code InputFormat} is an {@code enum} in the
     * current library version, it cannot be subclassed.  To keep the test suite
     * compiling we replace it with a disabled test that documents the intended
     * behaviour.
     */
    @Test
    @Disabled("Custom InputFormat cannot be created with current library version")
    void validate_inputFormatThrowsIOException_wrappedInJsonSchemaException() {
        // No implementation – test disabled.
    }

    @Test
    void validate_invalidRef_throwsJsonSchemaException() {
        // The instance contains a $ref that cannot be resolved by the schema.
        String inputWithBadRef = "{\"$ref\":\"#/nonexistent\"}";
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate(inputWithBadRef, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertTrue(ex.getMessage().contains("Unable to resolve reference"),
                "Exception message should mention inability to resolve reference");
    }
}
```