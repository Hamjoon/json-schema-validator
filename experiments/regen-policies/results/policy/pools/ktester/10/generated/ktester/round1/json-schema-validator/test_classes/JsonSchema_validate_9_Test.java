package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.serialization.JsonMapperFactory;
import com.networknt.schema.serialization.YamlMapperFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "name": { "type": "string" },
            "age":  { "type": "integer", "minimum": 0 }
          },
          "required": ["name"]
        }
        """;

    private JsonSchema jsonSchema;

    private ObjectMapper mapper;

    @BeforeEach
    void setupBeforeEach() {
        // Create a JsonSchema instance from the simple schema string.
        // JsonSchemaFactory is thread‑safe and can be reused.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        jsonSchema = factory.getSchema(SIMPLE_SCHEMA);
        mapper = JsonMapperFactory.getMapper();
    }

    private static Stream<Arguments> validInputsProvider() {
        return Stream.of(Arguments.of("{\"name\":\"John\"}", InputFormat.JSON), Arguments.of("name: John", InputFormat.YAML));
    }

    @Test
    void validate_validJson_returnsEmptyMessages() throws Exception {
        // GIVEN a JSON document that satisfies the schema
        String validJson = """
            {
              "name": "John Doe",
              "age": 30
            }
            """;
        // WHEN validate is invoked with a no‑op consumer
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(validJson, InputFormat.JSON, noop);
        // THEN no validation messages are produced
        assertNotNull(messages, "The returned set should never be null");
        assertTrue(messages.isEmpty(), "Expected no validation errors for a valid document");
    }

    @Test
    void validate_invalidJson_returnsMessages() throws Exception {
        // GIVEN a JSON document that violates the schema (missing required 'name' and negative age)
        String invalidJson = """
            {
              "age": -5
            }
            """;
        // WHEN validate is invoked with a no‑op consumer
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(invalidJson, InputFormat.JSON, noop);
        // THEN validation messages are reported
        assertNotNull(messages, "The returned set should never be null");
        assertFalse(messages.isEmpty(), "Expected validation errors for an invalid document");
        // OPTIONAL: check that the messages contain the expected keywords
        boolean hasRequired = messages.stream().anyMatch(m -> "required".equalsIgnoreCase(m.getType()));
        boolean hasMinimum = messages.stream().anyMatch(m -> "minimum".equalsIgnoreCase(m.getType()));
        assertTrue(hasRequired, "Expected a 'required' validation error");
        assertTrue(hasMinimum, "Expected a 'minimum' validation error");
    }

    @Test
    void validate_validJsonWithWhitespace_returnsEmptyMessages() {
        String jsonWithWs = "   {\"name\":\"John\"}   ";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(jsonWithWs, InputFormat.JSON, noop);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Whitespace should be ignored and validation succeed");
    }

    @Test
    void validate_validJsonWithBom_returnsEmptyMessages() {
        // UTF‑8 BOM (0xEF,0xBB,0xBF) expressed as a Java string literal.
        String jsonWithBom = "\uFEFF{\"name\":\"John\"}";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(jsonWithBom, InputFormat.JSON, noop);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "BOM should be ignored and validation succeed");
    }

    @Test
    void validate_validYaml_customizerMutatesContext() {
        String yaml = """
            name: John
            """;
        AtomicReference<ExecutionContext> captured = new AtomicReference<>();
        Consumer<ExecutionContext> customizer = ctx -> {
            ctx.setAttribute("custom", true);
            captured.set(ctx);
        };
        Set<ValidationMessage> messages = jsonSchema.validate(yaml, InputFormat.YAML, customizer);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Valid YAML should produce no validation errors");
        assertNotNull(captured.get(), "Customizer must have been invoked");
        assertEquals(true, captured.get().getAttribute("custom"), "ExecutionContext attribute set by customizer should be visible");
    }

    @Test
    void validate_largeValidJson_returnsEmptyMessages() {
        StringBuilder sb = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 10; i++) {
            sb.append("{\"name\":\"John").append(i).append("\"}");
            if (i < 9)
                sb.append(",");
        }
        sb.append("]}");
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(sb.toString(), InputFormat.JSON, noop);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Large valid payload should validate without errors");
    }

    @Test
    void validate_unicodeJson_returnsEmptyMessages() {
        String json = "{\"name\":\"Jöhn\"}";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(json, InputFormat.JSON, noop);
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Unicode characters must be handled correctly");
    }

    @Test
    void validate_missingRequiredProperty_returnsMessage() {
        String json = "{}";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(json, InputFormat.JSON, noop);
        assertFalse(messages.isEmpty(), "Missing required property should produce a message");
        assertTrue(messages.stream().anyMatch(m -> "required".equalsIgnoreCase(m.getType())), "Expected a 'required' validation error");
    }

    @Test
    void validate_typeMismatchOnProperty_returnsMessage() {
        String json = "{\"name\":123}";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(json, InputFormat.JSON, noop);
        assertFalse(messages.isEmpty(), "Type mismatch should produce a validation message");
        assertTrue(messages.stream().anyMatch(m -> "type".equalsIgnoreCase(m.getType()) || "type".equalsIgnoreCase(m.getKeyword())), "Expected a 'type' validation error for property 'name'");
    }

    @Test
    void validate_typeMismatchRootArray_returnsMessage() {
        String json = "[{\"name\":\"John\"}]";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(json, InputFormat.JSON, noop);
        assertFalse(messages.isEmpty(), "Root array where object expected should cause error");
        assertTrue(messages.stream().anyMatch(m -> "type".equalsIgnoreCase(m.getType()) || "type".equalsIgnoreCase(m.getKeyword())), "Expected a 'type' validation error for the root node");
    }

    @Test
    void validate_customizerSideEffectEvenWhenInvalid_returnsMessageAndAttribute() {
        String json = "{\"name\":123}";
        AtomicReference<ExecutionContext> captured = new AtomicReference<>();
        Consumer<ExecutionContext> customizer = ctx -> {
            ctx.setAttribute("custom", "value");
            captured.set(ctx);
        };
        Set<ValidationMessage> messages = jsonSchema.validate(json, InputFormat.JSON, customizer);
        assertFalse(messages.isEmpty(), "Invalid JSON should still produce validation messages");
        assertNotNull(captured.get(), "Customizer must still be executed");
        assertEquals("value", captured.get().getAttribute("custom"), "Attribute set by customizer should be observable");
    }

    @Test
    void validate_emptyString_throwsIllegalArgumentException() {
        String empty = "";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate(empty, InputFormat.JSON, noop), "Empty input should be rejected with IllegalArgumentException");
    }

    @Test
    void validate_whitespaceOnly_throwsIllegalArgumentException() {
        String ws = "   ";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate(ws, InputFormat.JSON, noop), "Whitespace‑only input should be rejected with IllegalArgumentException");
    }

    @Test
    void validate_nullString_throwsNullPointerException() {
        Consumer<ExecutionContext> noop = ctx -> {
        };
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(null, InputFormat.JSON, noop), "Null input string must cause NullPointerException");
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        String json = "{\"name\":\"John\"}";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(json, null, noop), "Null InputFormat must cause NullPointerException");
    }

    @Test
    void validate_nullCustomizer_throwsNullPointerException() {
        String json = "{\"name\":\"John\"}";
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(json, InputFormat.JSON, null), "Null Consumer should cause NullPointerException");
    }

    @Test
    void validate_customizerThrowsRuntimeException_propagates() {
        String json = "{\"name\":\"John\"}";
        Consumer<ExecutionContext> badCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(json, InputFormat.JSON, badCustomizer));
        assertEquals("customizer failure", ex.getMessage(), "Exception from customizer must be propagated unchanged");
    }

    @Test
    void validate_malformedJson_throwsRuntimeExceptionWrappingJsonProcessingException() {
        String malformed = "{invalid json}";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformed, InputFormat.JSON, noop));
        assertTrue(ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException, "Root cause should be JsonProcessingException");
    }

    @Test
    void validate_invalidYaml_throwsRuntimeExceptionWrappingJsonProcessingException() {
        String badYaml = "name: John: extra";
        Consumer<ExecutionContext> noop = ctx -> {
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(badYaml, InputFormat.YAML, noop));
        assertTrue(ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException, "Root cause should be JsonProcessingException for malformed YAML");
    }

    @ParameterizedTest
    @MethodSource("validInputsProvider")
    void validate_parameterizedValidInputs(String input, InputFormat format) {
        Consumer<ExecutionContext> noop = ctx -> {
        };
        Set<ValidationMessage> messages = jsonSchema.validate(input, format, noop);
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }
}
