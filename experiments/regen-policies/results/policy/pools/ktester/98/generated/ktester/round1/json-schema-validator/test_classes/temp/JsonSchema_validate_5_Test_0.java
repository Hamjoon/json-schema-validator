package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_5_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    // a schema without constraints (used for MissingNode)
    private JsonSchema emptySchema;

    // schema that requires an object (used for json‑null test)
    private JsonSchema objectSchema;

    // schema that contains a $ref
    private JsonSchema refSchema;

    // schema that uses "$recursiveAnchor": true
    private JsonSchema recursiveSchema;

    // -----------------------------------------------------------------
    static final class CustomResult {

        final String payload;

        CustomResult(String payload) {
            this.payload = payload;
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
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
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
    void validate_ValidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
        // given: a valid JSON node and a mocked OutputFormat
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        when(format.apply(any())).thenReturn("formatted‑ok");
        // when: validate is invoked
        String result = schema.validate(validNode, format);
        // then: the OutputFormat is called with an empty set of messages and the result is returned
        assertEquals("formatted‑ok", result);
        // capture the argument passed to the format – it must be an empty set for a valid instance
        verify(format, times(1)).apply(argThat(messages -> messages != null && messages.isEmpty()));
    }

    @Test
    void validate_InvalidNode_UsesOutputFormat(@Mock OutputFormat<String> format) throws Exception {
        // given: an invalid JSON node (age should be integer) and a mocked OutputFormat
        JsonNode invalidNode = MAPPER.readTree("{\"age\":\"thirty\"}");
        when(format.apply(any())).thenReturn("formatted‑error");
        // when: validate is invoked
        String result = schema.validate(invalidNode, format);
        // then: the OutputFormat receives a non‑empty set of ValidationMessage(s)
        assertEquals("formatted‑error", result);
        verify(format, times(1)).apply(argThat((Set<ValidationMessage> msgs) -> msgs != null && !msgs.isEmpty()));
    }

    // -----------------------------------------------------------------
    @Test
    void validate_NullRootNode_ThrowsNullPointerException(@Mock OutputFormat<String> format) {
        JsonNode nullNode = null;
        assertThrows(NullPointerException.class, () -> schema.validate(nullNode, format), "When rootNode is null a NullPointerException is expected");
    }

    @Test
    void validate_NullFormat_ThrowsIllegalArgumentException() throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        OutputFormat<String> nullFormat = null;
        assertThrows(IllegalArgumentException.class, () -> schema.validate(validNode, nullFormat), "When format is null an IllegalArgumentException is expected");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_OutputFormatThrowsRuntimeException(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        when(format.apply(any())).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(validNode, format));
        assertEquals("boom", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void validate_OutputFormatThrowsError(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        when(format.apply(any())).thenThrow(new AssertionError("assertion failed"));
        AssertionError err = assertThrows(AssertionError.class, () -> schema.validate(validNode, format));
        assertEquals("assertion failed", err.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void validate_OutputFormatReturnsNull(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        when(format.apply(any())).thenReturn(null);
        String result = schema.validate(validNode, format);
        assertNull(result, "When OutputFormat.apply returns null the method should propagate null");
    }

    @Test
    void validate_OutputFormatReturnsCustomObject(@Mock OutputFormat<CustomResult> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        CustomResult expected = new CustomResult("dto");
        when(format.apply(any())).thenReturn(expected);
        CustomResult actual = schema.validate(validNode, format);
        assertSame(expected, actual, "The custom object returned by the format must be propagated unchanged");
    }

    // -----------------------------------------------------------------
    @Test
    void validate_MissingNode_TreatedAsValid(@Mock OutputFormat<String> format) throws Exception {
        JsonNode missingNode = JsonNodeFactory.instance.missingNode();
        when(format.apply(any())).thenReturn("ok");
        // The empty schema validates everything, including a MissingNode
        String result = emptySchema.validate(missingNode, format);
        assertEquals("ok", result);
        verify(format).apply(argThat(messages -> messages != null && messages.isEmpty()));
    }

    // -----------------------------------------------------------------
    @Test
    void validate_JsonNullAgainstObjectSchema_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
        JsonNode jsonNull = JsonNodeFactory.instance.nullNode();
        when(format.apply(any())).thenReturn("failure");
        String result = objectSchema.validate(jsonNull, format);
        assertEquals("failure", result);
        verify(format).apply(argThat(messages -> messages != null && !messages.isEmpty()));
    }

    // -----------------------------------------------------------------
    @Test
    void validate_RefSchemaInvalidInstance_ProducesMessage(@Mock OutputFormat<String> format) throws Exception {
        // JSON missing the required "name" inside "person"
        JsonNode invalid = MAPPER.readTree("{\"person\":{}}");
        when(format.apply(any())).thenReturn("ref‑failure");
        String result = refSchema.validate(invalid, format);
        assertEquals("ref‑failure", result);
        verify(format).apply(argThat(messages -> messages != null && !messages.isEmpty()));
    }

    // -----------------------------------------------------------------
    @Test
    void validate_RecursiveAnchorValidInstance(@Mock OutputFormat<String> format) throws Exception {
        // The instance satisfies the recursive definition (empty object is fine)
        JsonNode valid = MAPPER.readTree("{\"self\":{}}");
        when(format.apply(any())).thenReturn("recursive‑ok");
        String result = recursiveSchema.validate(valid, format);
        assertEquals("recursive‑ok", result);
        verify(format).apply(argThat(messages -> messages != null && messages.isEmpty()));
    }

    // -----------------------------------------------------------------
    @Test
    void validate_WithExecutionContextCustomizer_CustomizerIsInvoked(@Mock OutputFormat<String> format) throws Exception {
        JsonNode validNode = MAPPER.readTree("{\"age\":30}");
        when(format.apply(any())).thenReturn("customizer‑ok");
        // Use the overload that accepts an ExecutionContextCustomizer.
        // The customizer will set a flag that we can assert afterwards.
        final boolean[] customizerCalled = { false };
        ExecutionContextCustomizer customizer = executionContext -> customizerCalled[0] = true;
        // The method under test (the three‑arg overload) returns the same type as the
        // two‑arg overload, so we can reuse the mock format.
        String result = schema.validate(validNode, format, customizer);
        assertEquals("customizer‑ok", result);
        assertTrue(customizerCalled[0], "The ExecutionContextCustomizer must have been executed");
        verify(format).apply(argThat(messages -> messages != null && messages.isEmpty()));
    }
}
