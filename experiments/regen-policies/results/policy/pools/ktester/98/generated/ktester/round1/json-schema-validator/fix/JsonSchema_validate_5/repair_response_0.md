```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat)} and the
 * three‑argument overload that accepts an {@link ExecutionContextCustomizer}.
 *
 * <p>The original version of this test used Mockito for creating mock
 * {@link OutputFormat}s. The execution environment for these kata exercises does
 * not provide Mockito, therefore the tests have been rewritten to use simple
 * hand‑crafted implementations of {@link OutputFormat} that record the argument
 * they receive. This keeps the behaviour under test identical while removing
 * the external dependency.</p>
 */
class JsonSchema_validate_5_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;
    private JsonSchema emptySchema;
    private JsonSchema objectSchema;
    private JsonSchema refSchema;
    private JsonSchema recursiveSchema;

    /** Simple DTO used in one of the generic tests. */
    static final class CustomResult {
        final String payload;

        CustomResult(String payload) {
            this.payload = payload;
        }
    }

    /** Minimal {@link OutputFormat} implementation that returns a predefined value
     *  and stores the set of {@link ValidationMessage}s it received. */
    private static class RecordingOutputFormat<T> implements OutputFormat<T> {
        private final T result;
        private Set<ValidationMessage> capturedMessages;

        RecordingOutputFormat(T result) {
            this.result = result;
        }

        @Override
        public T apply(Set<ValidationMessage> messages) {
            this.capturedMessages = messages;
            return result;
        }

        Set<ValidationMessage> getCapturedMessages() {
            return capturedMessages;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1️⃣  Simple schema that requires a numeric "age" property
        // -----------------------------------------------------------------
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": [ "age" ]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Base schema should be created");

        // -----------------------------------------------------------------
        // 2️⃣  Empty schema – validates everything (used for MissingNode case)
        // -----------------------------------------------------------------
        emptySchema = factory.getSchema("{}");
        assertNotNull(emptySchema, "Empty schema should be created");

        // -----------------------------------------------------------------
        // 3️⃣  Object‑type schema – used to verify that a JSON null node fails
        // -----------------------------------------------------------------
        String objectSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object"
            }
            """;
        objectSchema = factory.getSchema(objectSchemaJson);
        assertNotNull(objectSchema, "Object schema should be created");

        // -----------------------------------------------------------------
        // 4️⃣  Schema with $ref – the referenced definition requires a "name"
        // -----------------------------------------------------------------
        String refSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "person": { "$ref": "#/definitions/person" }
              },
              "definitions": {
                "person": {
                  "type": "object",
                  "required": [ "name" ],
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
              }
            }
            """;
        refSchema = factory.getSchema(refSchemaJson);
        assertNotNull(refSchema, "$ref schema should be created");

        // -----------------------------------------------------------------
        // 5️⃣  Recursive‑anchor schema – simple self‑reference example
        // -----------------------------------------------------------------
        String recursiveSchemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$recursiveAnchor": true,
              "type": "object",
              "properties": {
                "self": { "$recursiveRef": "#" }
              }
            }
            """;
        recursiveSchema = factory.getSchema(recursiveSchemaJson);
        assertNotNull(recursiveSchema, "Recursive schema should be created");
    }

    @Test
    void validate_ValidNode_UsesOutputFormat() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑ok");

        String result = schema.validate(validNode, format);

        assertEquals("formatted‑ok", result);
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(),
                "For a valid instance the message set must be empty");
    }

    @Test
    void validate_InvalidNode_UsesOutputFormat() throws Exception {
        JsonNode invalidNode = MAPPER.readTree("{\"age\":\"thirty\"}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("formatted‑error");

        String result = schema.validate(invalidNode, format);

        assertEquals("formatted‑error", result);
        assertNotNull(format.getCapturedMessages());
        assertFalse(format.getCapturedMessages().isEmpty(),
                "For an invalid instance the message set must contain at least one ValidationMessage");
    }

    @Test
    void validate_NullRootNode_ThrowsNullPointerException() {
        JsonNode nullNode = null;
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("unused");
        assertThrows(NullPointerException.class,
                () -> schema.validate(nullNode, format),
                "When rootNode is null a NullPointerException is expected");
    }

    @Test
    void validate_NullFormat_ThrowsIllegalArgumentException() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> nullFormat = null;
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(validNode, nullFormat),
                "When format is null an IllegalArgumentException is expected");
    }

    @Test
    void validate_OutputFormatThrowsRuntimeException() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> throwing = messages -> {
            throw new RuntimeException("boom");
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(validNode, throwing));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void validate_OutputFormatThrowsError() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> throwing = messages -> {
            throw new AssertionError("assertion failed");
        };
        AssertionError err = assertThrows(AssertionError.class,
                () -> schema.validate(validNode, throwing));
        assertEquals("assertion failed", err.getMessage());
    }

    @Test
    void validate_OutputFormatReturnsNull() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>(null);
        String result = schema.validate(validNode, format);
        assertNull(result, "When OutputFormat.apply returns null the method should propagate null");
    }

    @Test
    void validate_OutputFormatReturnsCustomObject() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        CustomResult expected = new CustomResult("dto");
        RecordingOutputFormat<CustomResult> format = new RecordingOutputFormat<>(expected);
        CustomResult actual = schema.validate(validNode, format);
        assertSame(expected, actual, "The custom object returned by the format must be propagated unchanged");
    }

    @Test
    void validate_MissingNode_TreatedAsValid() throws Exception {
        JsonNode missingNode = JsonNodeFactory.instance.missingNode();
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ok");

        String result = emptySchema.validate(missingNode, format);

        assertEquals("ok", result);
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(),
                "Empty schema should treat MissingNode as valid, producing an empty message set");
    }

    @Test
    void validate_JsonNullAgainstObjectSchema_ProducesMessage() throws Exception {
        JsonNode jsonNull = JsonNodeFactory.instance.nullNode();
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("failure");

        String result = objectSchema.validate(jsonNull, format);

        assertEquals("failure", result);
        assertNotNull(format.getCapturedMessages());
        assertFalse(format.getCapturedMessages().isEmpty(),
                "Validating a JSON null against an object‑type schema must produce a validation message");
    }

    @Test
    void validate_RefSchemaInvalidInstance_ProducesMessage() throws Exception {
        JsonNode invalid = MAPPER.readTree("{\"person\":{}}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("ref‑failure");

        String result = refSchema.validate(invalid, format);

        assertEquals("ref‑failure", result);
        assertNotNull(format.getCapturedMessages());
        assertFalse(format.getCapturedMessages().isEmpty(),
                "Missing required property inside $ref should generate a validation message");
    }

    @Test
    void validate_RecursiveAnchorValidInstance() throws Exception {
        JsonNode valid = MAPPER.readTree("{\"self\":{}}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("recursive‑ok");

        String result = recursiveSchema.validate(valid, format);

        assertEquals("recursive‑ok", result);
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(),
                "A valid instance for the recursive schema must result in an empty message set");
    }

    @Test
    void validate_WithExecutionContextCustomizer_CustomizerIsInvoked() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        RecordingOutputFormat<String> format = new RecordingOutputFormat<>("customizer‑ok");

        final boolean[] customizerCalled = {false};
        ExecutionContextCustomizer customizer = executionContext -> customizerCalled[0] = true;

        String result = schema.validate(validNode, format, customizer);

        assertEquals("customizer‑ok", result);
        assertTrue(customizerCalled[0], "The ExecutionContextCustomizer must have been executed");
        assertNotNull(format.getCapturedMessages());
        assertTrue(format.getCapturedMessages().isEmpty(),
                "When the instance is valid the message set must be empty");
    }
}
```