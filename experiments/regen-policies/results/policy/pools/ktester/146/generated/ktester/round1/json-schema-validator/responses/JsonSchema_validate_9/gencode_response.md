```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * <p>The test suite covers null‑argument handling, custom {@link ExecutionContextCustomizer}
 * behaviour, custom {@link OutputFormat} implementations, type‑mismatch scenarios and
 * error propagation.  The {@code @BeforeAll}/{@code @BeforeEach} methods are left
 * untouched as required by the framework.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema schema;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use Draft‑2020‑12 – the same version as the original test suite.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        mapper = new ObjectMapper();
        // Default schema (string) – overridden in many tests where a different schema is needed.
        String schemaJson = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"string\" }";
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up.
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Build a {@link JsonSchema} from a JSON schema string. */
    private JsonSchema buildSchema(String json) {
        return factory.getSchema(json);
    }

    /** Simple {@link OutputFormat} that returns the JSON array representation of the messages. */
    private static final OutputFormat<String> SIMPLE_STRING_FORMAT = new SimpleStringOutputFormat();

    /* --------------------------------------------------------------------- */
    /* Null handling group                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullRootNode_throwsNullPointerException() {
        JsonNode nullNode = null;
        ExecutionContextCustomizer customizer = ctx -> {};
        assertThrows(NullPointerException.class,
                () -> schema.validate(nullNode, SIMPLE_STRING_FORMAT, customizer));
    }

    @Test
    void validate_nullOutputFormat_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("\"test\"");
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(node, null, ctx -> {}));
    }

    @Test
    void validate_nullCustomizer_usesDefaultNoOp_andReturnsEmptyResult() throws Exception {
        // Use an object schema so that the result is deterministic.
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"John\"}");

        // customizer is null – method should treat it as a no‑op.
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, (ExecutionContextCustomizer) null);
        assertEquals("[]", result, "Valid object should produce an empty JSON array");
    }

    @Test
    void validate_nullRootAndFormat_throwsNullPointerException() {
        JsonNode nullNode = null;
        assertThrows(NullPointerException.class,
                () -> schema.validate(nullNode, null, (ExecutionContextCustomizer) null));
    }

    @Test
    void validate_nullFormatAndNullCustomizer_throwsIllegalArgumentException() throws Exception {
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        assertThrows(IllegalArgumentException.class,
                () -> schema.validate(node, null, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------------- */
    /* Successful validation – no errors                                     */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_validObject_returnsEmptyString() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"John\"}");

        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, ctx -> {});
        assertEquals("[]", result, "A valid object should produce an empty JSON array");
    }

    /* --------------------------------------------------------------------- */
    /* Validation with errors                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_invalidAge_returnsMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode invalid = mapper.readTree("{\"age\":\"twenty\"}");

        String result = obj.validate(invalid, SIMPLE_STRING_FORMAT, ctx -> {});
        assertTrue(result.contains("age"), "Result should mention the 'age' property");
        assertTrue(result.toLowerCase().contains("integer"), "Message should indicate an integer type error");
    }

    /* --------------------------------------------------------------------- */
    /* Custom ExecutionContext – flag manipulation                           */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_customizerSetsSkipAttribute_behavesLikeNoOp() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"id\":1}");

        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("skip", true);
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, customizer);
        assertEquals("[]", result, "Skipping validation via customizer should still return empty result");
    }

    @Test
    void validate_customizerThrowsRuntimeException_propagates() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode any = mapper.readTree("{}");

        ExecutionContextCustomizer failing = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> obj.validate(any, SIMPLE_STRING_FORMAT, failing));
        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Recursive anchor handling                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_recursiveAnchor_customizerEnablesAndFinishes() throws Exception {
        // Schema with $recursiveAnchor set to true (draft‑2020‑12 syntax).
        String recursiveSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"$recursiveAnchor\": true,"
                + "\"type\":\"object\","
                + "\"properties\":{\"self\":{\"$ref\":\"#\"}} }";
        JsonSchema obj = buildSchema(recursiveSchema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");

        ExecutionContextCustomizer customizer = ctx -> ctx.setRecursiveAnchor(true);
        String result = obj.validate(instance, SIMPLE_STRING_FORMAT, customizer);
        assertEquals("[]", result, "Recursive anchor should not cause infinite recursion");
    }

    /* --------------------------------------------------------------------- */
    /* Custom OutputFormat – exception propagation                           */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_outputFormatThrowsIllegalStateException_propagates() throws Exception {
        JsonNode node = mapper.readTree("\"test\"");
        OutputFormat<String> badFormat = new OutputFormat<String>() {
            @Override
            public String apply(Set<ValidationMessage> messages) {
                throw new IllegalStateException("format failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.validate(node, badFormat, ctx -> {}));
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
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate(node, badFormat, ctx -> {}));
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
        AssertionError err = assertThrows(AssertionError.class,
                () -> schema.validate(node, badFormat, ctx -> {}));
        assertEquals("format error", err.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Custom OutputFormat – collection result                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_outputFormatReturnsList_invalid() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode invalid = mapper.readTree("{\"age\":\"twenty\"}");

        OutputFormat<List<ValidationMessage>> listFormat = new OutputFormat<List<ValidationMessage>>() {
            @Override
            public List<ValidationMessage> apply(Set<ValidationMessage> messages) {
                return new ArrayList<>(messages);
            }
        };

        List<ValidationMessage> result = obj.validate(invalid, listFormat, ctx -> {});
        assertFalse(result.isEmpty(), "Invalid instance should produce a non‑empty list");
        assertTrue(result.stream().anyMatch(m -> m.getPath().contains("age")),
                "List should contain a message about the 'age' property");
    }

    @Test
    void validate_outputFormatReturnsList_valid() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"Bob\"}");

        OutputFormat<List<ValidationMessage>> listFormat = new OutputFormat<List<ValidationMessage>>() {
            @Override
            public List<ValidationMessage> apply(Set<ValidationMessage> messages) {
                return new ArrayList<>(messages);
            }
        };

        List<ValidationMessage> result = obj.validate(valid, listFormat, ctx -> {});
        assertTrue(result.isEmpty(), "Valid instance should produce an empty list");
    }

    /* --------------------------------------------------------------------- */
    /* Invalid rootNode type handling                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_rootNodeArray_typeMismatchMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode arrayNode = JsonNodeFactory.instance.arrayNode().add(1).add(2);

        String result = obj.validate(arrayNode, SIMPLE_STRING_FORMAT, ctx -> {});
        assertTrue(result.toLowerCase().contains("object"),
                "Message should indicate that an object was expected");
        assertTrue(result.toLowerCase().contains("array"),
                "Message should mention that the provided node was an array");
    }

    @Test
    void validate_rootNodeBoolean_typeMismatchMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode boolNode = JsonNodeFactory.instance.booleanNode(true);

        String result = obj.validate(boolNode, SIMPLE_STRING_FORMAT, ctx -> {});
        assertTrue(result.toLowerCase().contains("object"));
        assertTrue(result.toLowerCase().contains("boolean"));
    }

    /* --------------------------------------------------------------------- */
    /* JSON null literal handling                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullLiteral_notAllowed_returnsMessage() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\" }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode nullNode = JsonNodeFactory.instance.nullNode();

        String result = obj.validate(nullNode, SIMPLE_STRING_FORMAT, ctx -> {});
        assertTrue(result.toLowerCase().contains("null"));
        assertTrue(result.toLowerCase().contains("object"));
    }

    /* --------------------------------------------------------------------- */
    /* ExecutionCustomizer – attribute set but not used                       */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_customizerSetsUnusedAttribute_hasNoEffect() throws Exception {
        String objSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema obj = buildSchema(objSchema);
        JsonNode valid = mapper.readTree("{\"name\":\"Charlie\"}");

        ExecutionContextCustomizer customizer = ctx -> ctx.setAttribute("unusedFlag", "value");
        String result = obj.validate(valid, SIMPLE_STRING_FORMAT, customizer);
        assertEquals("[]", result);
    }

    /* --------------------------------------------------------------------- */
    /* ExecutionCustomizer – throws NullPointerException                     */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_customizerThrowsNullPointerException_propagates() throws Exception {
        JsonNode any = mapper.readTree("{}");
        ExecutionContextCustomizer bad = ctx -> {
            throw new NullPointerException("customizer NPE");
        };
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate(any, SIMPLE_STRING_FORMAT, bad));
        assertEquals("customizer NPE", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* ExecutionCustomizer – illegal attribute key                              */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_customizerIllegalAttributeKey_propagates() throws Exception {
        JsonNode any = mapper.readTree("{}");
        ExecutionContextCustomizer bad = ctx -> ctx.setAttribute(null, "value");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> schema.validate(any, SIMPLE_STRING_FORMAT, bad));
        // Message is implementation‑specific; we only assert the type.
        assertNotNull(ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* Schema with unresolved $ref – IllegalStateException                    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_schemaWithUnresolvedRef_throwsIllegalStateException() {
        // The schema contains a $ref that points to a non‑existent definition.
        String badSchema = "{ \"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"$ref\":\"#/nonexistent\" }";
        JsonSchema bad = buildSchema(badSchema);
        JsonNode any = mapper.readTree("{}");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bad.validate(any, SIMPLE_STRING_FORMAT, ctx -> {}));
        assertNotNull(ex.getMessage());
    }
}
```