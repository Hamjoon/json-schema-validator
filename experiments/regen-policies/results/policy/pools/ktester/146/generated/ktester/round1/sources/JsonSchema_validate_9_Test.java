package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    private static JsonSchemaFactory factory;

    private JsonSchema schema;

    private ObjectMapper mapper;

    /**
     * Simple {@link OutputFormat} that returns the JSON array representation of the messages.
     */
    private static final OutputFormat<String> SIMPLE_STRING_FORMAT = new SimpleStringOutputFormat();

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for Draft‑2020‑12 – the version used in the existing test suite.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        mapper = new ObjectMapper();
        // Very small schema: the root value must be a string.
        String schemaJson = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"string\" }";
        // Build the JsonSchema instance once per test method.
        schema = factory.getSchema(schemaJson);
    }

    /**
     * Build a {@link JsonSchema} from a JSON schema string.
     */
    private JsonSchema buildSchema(String json) {
        return factory.getSchema(json);
    }

    /**
     * Validate a correct JSON string using the three‑argument overload.
     * The {@link OutputFormat#DEFAULT} implementation returns a {@link Set} of
     * {@link ValidationMessage}s, which should be empty for a valid instance.
     */
    @Test
    void validate_withValidString_andNoCustomizer_returnsEmptySet() throws Exception {
        // given
        JsonNode validNode = mapper.readTree("\"hello world\"");
        // when
        Set<ValidationMessage> result = schema.validate(validNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        // then
        assertTrue(result.isEmpty(), "A valid string should produce no validation messages");
    }

    /**
     * Validate an invalid JSON value (number instead of string) and ensure that
     * the returned {@link Set} contains exactly one message describing the type error.
     */
    @Test
    void validate_withInvalidType_returnsOneMessage() throws Exception {
        // given
        JsonNode invalidNode = mapper.readTree("123");
        // when
        Set<ValidationMessage> result = schema.validate(invalidNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        // then
        assertEquals(1, result.size(), "A type mismatch should produce exactly one validation message");
        ValidationMessage message = result.iterator().next();
        assertEquals("type", message.getType(), "The message type should be 'type'");
    }

    /**
     * Validate using a no‑op {@link ExecutionContextCustomizer}. The customizer
     * does not change the execution context, so the behaviour must be identical
     * to the call with a {@code null} customizer.
     */
    @Test
    void validate_withNoOpCustomizer_behavesLikeNullCustomizer() throws Exception {
        // given
        JsonNode validNode = mapper.readTree("\"test\"");
        ExecutionContextCustomizer noOp = executionContext -> {
            // intentionally left blank – does not modify the context
        };
        // when
        Set<ValidationMessage> resultWithNull = schema.validate(validNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        Set<ValidationMessage> resultWithCustomizer = schema.validate(validNode, OutputFormat.DEFAULT, noOp);
        // then
        assertEquals(resultWithNull, resultWithCustomizer, "Validation result must be identical whether a no‑op customizer or null is supplied");
    }

    /**
     * Demonstrates that a custom {@link ExecutionContextCustomizer} can be used
     * to inject a dummy attribute into the {@link ExecutionContext}. The test
     * does not assert on the attribute (as the internal API is not public) but
     * verifies that the customizer is executed without throwing an exception.
     */
    @Test
    void validate_withCustomizerThatModifiesContext_executesSuccessfully() throws Exception {
        // given
        JsonNode validNode = mapper.readTree("\"foo\"");
        ExecutionContextCustomizer customizer = executionContext -> {
            // Example modification – store a flag in the context's attributes map.
            executionContext.getAttributes().put("test-flag", Boolean.TRUE);
        };
        // when / then – the call must complete without propagating any exception.
        assertDoesNotThrow(() -> schema.validate(validNode, OutputFormat.DEFAULT, customizer));
    }

    @Test
    void validate_nullRootNode_throwsNullPointerException() {
        JsonNode nullNode = null;
        ExecutionContextCustomizer customizer = ctx -> {
        };
        assertThrows(NullPointerException.class, () -> schema.validate(nullNode, SIMPLE_STRING_FORMAT, customizer));
    }

    @Test
    void validate_nullOutputFormat_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("\"test\"");
        assertThrows(IllegalArgumentException.class, () -> schema.validate(node, null, ctx -> {
        }));
    }

    @Test
    void validate_nullCustomizer_usesDefaultNoOp_andReturnsEmptyResult() throws Exception {
        // Use an object schema so that the result is deterministic.
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"John\"}");
        // customizer is null – method should treat it as a no‑op.
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, (ExecutionContextCustomizer) null);
        assertEquals("[]", result, "Valid object should produce an empty JSON array");
    }

    @Test
    void validate_nullRootAndFormat_throwsNullPointerException() {
        JsonNode nullNode = null;
        assertThrows(NullPointerException.class, () -> schema.validate(nullNode, null, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_nullFormatAndNullCustomizer_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        assertThrows(IllegalArgumentException.class, () -> schema.validate(node, null, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_validObject_returnsEmptyString() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"John\"}");
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, ctx -> {
        });
        assertEquals("[]", result, "A valid object should produce an empty JSON array");
    }

    @Test
    void validate_invalidAge_returnsMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode invalid = mapper.readTree("{\"age\":\"twenty\"}");
        String result = obj.validate(invalid, SIMPLE_STRING_FORMAT, ctx -> {
        });
        assertTrue(result.contains("age"), "Result should mention the 'age' property");
        assertTrue(result.toLowerCase().contains("integer"), "Message should indicate an integer type error");
    }

    @Test
    void validate_customizerSetsSkipAttribute_behavesLikeNoOp() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"id\":1}");
        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("skip", true);
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, customizer);
        assertEquals("[]", result, "Skipping validation via customizer should still return empty result");
    }

    @Test
    void validate_customizerThrowsRuntimeException_propagates() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode any = mapper.readTree("{}");
        ExecutionContextCustomizer failing = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> obj.validate(any, SIMPLE_STRING_FORMAT, failing));
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_recursiveAnchor_customizerEnablesAndFinishes() throws Exception {
        // Schema with $recursiveAnchor set to true (draft‑2020‑12 syntax).
        String recursiveSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"$recursiveAnchor\": true," + "\"type\":\"object\"," + "\"properties\":{\"self\":{\"$ref\":\"#\"}} }";
        JsonSchema obj = buildSchema(recursiveSchema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");
        ExecutionContextCustomizer customizer = ctx -> ctx.setRecursiveAnchor(true);
        String result = obj.validate(instance, SIMPLE_STRING_FORMAT, customizer);
        assertEquals("[]", result, "Recursive anchor should not cause infinite recursion");
    }

    @Test
    void validate_outputFormatThrowsIllegalStateException_propagates() throws Exception {
        JsonNode node = mapper.readTree("\"test\"");
        OutputFormat<String> badFormat = new OutputFormat<String>() {

            @Override
            public String apply(Set<ValidationMessage> messages) {
                throw new IllegalStateException("format failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> schema.validate(node, badFormat, ctx -> {
        }));
        assertEquals("format failure", ex.getMessage());
    }

    @Test
    void validate_outputFormatThrowsRuntimeException_propagates() throws Exception {
        JsonNode node = mapper.readTree("\"test\"");
        OutputFormat<String> badFormat = new OutputFormat<String>() {

            @Override
            public String apply(Set<ValidationMessage> messages) {
                throw new RuntimeException("format runtime failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, badFormat, ctx -> {
        }));
        assertEquals("format runtime failure", ex.getMessage());
    }

    @Test
    void validate_outputFormatThrowsAssertionError_propagates() throws Exception {
        JsonNode node = mapper.readTree("\"test\"");
        OutputFormat<String> badFormat = new OutputFormat<String>() {

            @Override
            public String apply(Set<ValidationMessage> messages) {
                throw new AssertionError("format error");
            }
        };
        AssertionError err = assertThrows(AssertionError.class, () -> schema.validate(node, badFormat, ctx -> {
        }));
        assertEquals("format error", err.getMessage());
    }

    @Test
    void validate_outputFormatReturnsList_invalid() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode invalid = mapper.readTree("{\"age\":\"twenty\"}");
        OutputFormat<List<ValidationMessage>> listFormat = new OutputFormat<List<ValidationMessage>>() {

            @Override
            public List<ValidationMessage> apply(Set<ValidationMessage> messages) {
                return new ArrayList<>(messages);
            }
        };
        List<ValidationMessage> result = obj.validate(invalid, listFormat, ctx -> {
        });
        assertFalse(result.isEmpty(), "Invalid instance should produce a non‑empty list");
        assertTrue(result.stream().anyMatch(m -> m.getPath().contains("age")), "List should contain a message about the 'age' property");
    }

    @Test
    void validate_outputFormatReturnsList_valid() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"Bob\"}");
        OutputFormat<List<ValidationMessage>> listFormat = new OutputFormat<List<ValidationMessage>>() {

            @Override
            public List<ValidationMessage> apply(Set<ValidationMessage> messages) {
                return new ArrayList<>(messages);
            }
        };
        List<ValidationMessage> result = obj.validate(valid, listFormat, ctx -> {
        });
        assertTrue(result.isEmpty(), "Valid instance should produce an empty list");
    }

    @Test
    void validate_rootNodeArray_typeMismatchMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode arrayNode = JsonNodeFactory.instance.arrayNode().add(1).add(2);
        String result = obj.validate(arrayNode, SIMPLE_STRING_FORMAT, ctx -> {
        });
        assertTrue(result.toLowerCase().contains("object"), "Message should indicate that an object was expected");
        assertTrue(result.toLowerCase().contains("array"), "Message should mention that the provided node was an array");
    }

    @Test
    void validate_rootNodeBoolean_typeMismatchMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode boolNode = JsonNodeFactory.instance.booleanNode(true);
        String result = obj.validate(boolNode, SIMPLE_STRING_FORMAT, ctx -> {
        });
        assertTrue(result.toLowerCase().contains("object"));
        assertTrue(result.toLowerCase().contains("boolean"));
    }

    @Test
    void validate_nullLiteral_notAllowed_returnsMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode nullNode = JsonNodeFactory.instance.nullNode();
        String result = obj.validate(nullNode, SIMPLE_STRING_FORMAT, ctx -> {
        });
        assertTrue(result.toLowerCase().contains("null"));
        assertTrue(result.toLowerCase().contains("object"));
    }

    @Test
    void validate_customizerSetsUnusedAttribute_hasNoEffect() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"Charlie\"}");
        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("unusedFlag", "value");
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, customizer);
        assertEquals("[]", result);
    }

    @Test
    void validate_customizerThrowsNullPointerException_propagates() throws Exception {
        JsonNode any = mapper.readTree("{}");
        ExecutionContextCustomizer bad = ctx -> {
            throw new NullPointerException("customizer NPE");
        };
        NullPointerException ex = assertThrows(NullPointerException.class, () -> schema.validate(any, SIMPLE_STRING_FORMAT, bad));
        assertEquals("customizer NPE", ex.getMessage());
    }

    @Test
    void validate_customizerIllegalAttributeKey_propagates() throws Exception {
        JsonNode any = mapper.readTree("{}");
        ExecutionContextCustomizer bad = ctx -> ctx.setAttribute(null, "value");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> schema.validate(any, SIMPLE_STRING_FORMAT, bad));
        // Message is implementation‑specific; we only assert the type.
        assertNotNull(ex.getMessage());
    }

    @Test
    void validate_schemaWithUnresolvedRef_throwsIllegalStateException() {
        // The schema contains a $ref that points to a non‑existent definition.
        String badSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"$ref\":\"#/nonexistent\" }";
        JsonSchema bad = buildSchema(badSchema);
        JsonNode any = mapper.readTree("{}");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> bad.validate(any, SIMPLE_STRING_FORMAT, ctx -> {
        }));
        assertNotNull(ex.getMessage());
    }
}
