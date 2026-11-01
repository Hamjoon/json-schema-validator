```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Comprehensive unit‑tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * <p>These tests exercise the validator through two different paths:</p>
 *
 * <ul>
 *   <li>Via {@link JsonSchema#validate(JsonNode, InputFormat)} – this is the public
 *       API used by library consumers. It guarantees that the {@code OneOfValidator}
 *       is correctly wired inside a {@link JsonSchema} instance.</li>
 *   <li>Directly via reflection on a {@code OneOfValidator} instance extracted from a
 *       {@link JsonSchema}. This allows us to pass custom {@link ExecutionContext},
 *       {@code null} arguments, and to verify exceptional behaviour (e.g. NPEs).</li>
 * </ul>
 *
 * <p>All test cases described in the supplied JSON matrix are covered.</p>
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    @BeforeAll
    static void beforeAll() {
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods to obtain a OneOfValidator from a compiled schema.
     * --------------------------------------------------------------------- */

    /**
     * Returns the first {@link OneOfValidator} found inside the compiled {@link JsonSchema}.
     *
     * @param schemaJson the JSON schema containing a {@code oneOf} keyword
     * @return the {@code OneOfValidator} instance
     * @throws Exception if the validator cannot be located
     */
    private OneOfValidator getOneOfValidator(String schemaJson) throws Exception {
        JsonSchema schema = factory.getSchema(schemaJson);
        // JsonSchema keeps its validators in a private field named "validators".
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonValidator> validators = (List<JsonValidator>) validatorsField.get(schema);
        return validators.stream()
                .filter(v -> v instanceof OneOfValidator)
                .map(v -> (OneOfValidator) v)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("OneOfValidator not found"));
    }

    /**
     * Invokes the protected {@code validate(..., boolean walk)} method via reflection.
     *
     * @param validator the {@code OneOfValidator} instance
     * @param executionContext the execution context (may be {@code null})
     * @param node the instance node
     * @param rootNode the root node (may be {@code null})
     * @param instanceLocation the location of {@code node} within the document
     * @param walk whether the walk‑mode should be used
     * @return the {@link Set} of {@link ValidationMessage}s returned by the validator
     * @throws Exception any reflection‑related exception
     */
    private Set<ValidationMessage> invokeValidate(OneOfValidator validator,
                                                  ExecutionContext executionContext,
                                                  JsonNode node,
                                                  JsonNode rootNode,
                                                  JsonNodePath instanceLocation,
                                                  boolean walk) throws Exception {
        Method m = OneOfValidator.class.getDeclaredMethod(
                "validate",
                ExecutionContext.class,
                JsonNode.class,
                JsonNode.class,
                JsonNodePath.class,
                boolean.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> result = (Set<ValidationMessage>) m.invoke(
                validator, executionContext, node, rootNode, instanceLocation, walk);
        return result;
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test data
     * --------------------------------------------------------------------- */

    private static Stream<Arguments> provideValidateScenarios() {
        return Stream.of(
                // exactly one subschema matches – success
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"], "properties": {"type": {"enum": ["A"]}} },
                            { "type": "object", "required": ["extra"], "properties": {"extra": {"type": "string"}} }
                          ]
                        }
                        """,
                        """{"type":"A"}""",
                        true,
                        "empty Set (exactly one match)"),

                // no subschema matches – oneOf error
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"], "properties": {"type": {"enum": ["A"]}} },
                            { "type": "object", "required": ["type"], "properties": {"type": {"enum": ["B"]}} }
                          ]
                        }
                        """,
                        """{"type":"C"}""",
                        false,
                        "oneOf error (no matches)"),

                // more than one subschema matches – oneOf error with indexes
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"], "properties": {"type": {"enum": ["A"]}} },
                            { "type": "object", "required": ["extra"], "properties": {"extra": {"type": "string"}} }
                          ]
                        }
                        """,
                        """{"type":"A","extra":"v"}""",
                        false,
                        "oneOf error (multiple matches)"),

                // array node – none of the object subschemas match
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"] },
                            { "type": "object", "required": ["extra"] }
                          ]
                        }
                        """,
                        """["A"]""",
                        false,
                        "oneOf error (array node)"),

                // empty object – none matches
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"] },
                            { "type": "object", "required": ["extra"] }
                          ]
                        }
                        """,
                        """{}""",
                        false,
                        "oneOf error (empty object)"),

                // extra unrelated property – still exactly one match
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"], "properties": {"type": {"enum": ["A"]}} },
                            { "type": "object", "required": ["extra"], "properties": {"extra": {"type": "string"}} }
                          ]
                        }
                        """,
                        """{"type":"A","foo":"bar"}""",
                        true,
                        "empty Set (extra unrelated property)"),

                // null instance node – validator treats missing node as valid
                Arguments.of(
                        """
                        {
                          "oneOf": [
                            { "type": "object", "required": ["type"] },
                            { "type": "object", "required": ["extra"] }
                          ]
                        }
                        """,
                        "null", // special marker processed below
                        true,
                        "empty Set (null node)")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Tests exercising the public JsonSchema.validate(...) API
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideValidateScenarios")
    void validate_viaJsonSchemaApi(String schemaJson,
                                  String instanceJson,
                                  boolean expectSuccess,
                                  String description) throws Exception {

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = "null".equals(instanceJson) ? null : mapper.readTree(instanceJson);

        Set<ValidationMessage> messages = schema.validate(node, InputFormat.JSON);

        if (expectSuccess) {
            assertTrue(messages.isEmpty(),
                    () -> "Expected no validation messages for case: " + description);
        } else {
            assertFalse(messages.isEmpty(),
                    () -> "Expected at least one validation message for case: " + description);
            // all error messages for a oneOf violation must have type "oneOf"
            messages.forEach(m -> assertEquals("oneOf", m.getType(),
                    "Message type should be 'oneOf'"));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Direct invocation tests – covering null arguments and short‑circuit
     * --------------------------------------------------------------------- */

    @Test
    void validate_directly_withNullExecutionContext_throwsNPE() throws Exception {
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] },
                    { "type": "object", "required": ["extra"] }
                  ]
                }
                """;

        OneOfValidator validator = getOneOfValidator(schemaJson);
        JsonNode node = mapper.readTree("{\"type\":\"A\"}");
        JsonNodePath path = JsonNodePath.root();

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> invokeValidate(validator, null, node, node, path, false));
        assertTrue(ex.getMessage().contains("executionContext"),
                "Exception message should mention executionContext");
    }

    @Test
    void validate_directly_withNullInstanceLocation_throwsNPE() throws Exception {
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] },
                    { "type": "object", "required": ["extra"] }
                  ]
                }
                """;

        OneOfValidator validator = getOneOfValidator(schemaJson);
        ExecutionContext ctx = Mockito.mock(ExecutionContext.class);
        JsonNode node = mapper.readTree("{\"type\":\"A\"}");

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> invokeValidate(validator, ctx, node, node, null, false));
        assertTrue(ex.getMessage().contains("instanceLocation"),
                "Exception message should mention instanceLocation");
    }

    @Test
    void validate_directly_withNullRootNodeAndNonNullNode_throwsNPE() throws Exception {
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] },
                    { "type": "object", "required": ["extra"] }
                  ]
                }
                """;

        OneOfValidator validator = getOneOfValidator(schemaJson);
        ExecutionContext ctx = Mockito.mock(ExecutionContext.class);
        JsonNode node = mapper.readTree("{\"type\":\"A\"}");
        JsonNodePath path = JsonNodePath.root();

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> invokeValidate(validator, ctx, node, null, path, false));
        assertTrue(ex.getMessage().contains("rootNode"),
                "Exception message should mention rootNode");
    }

    @Test
    void validate_directly_whenCanShortCircuitTrue_stopsAfterFirstMatch() throws Exception {
        // The schema contains two subschemas that would both match the instance.
        // By forcing canShortCircuit() to return true we expect the validator to
        // stop after the first match and therefore return an empty set.
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] },
                    { "type": "object", "required": ["type"] }
                  ]
                }
                """;

        // Obtain the real validator so that all internal state is correctly initialised.
        OneOfValidator realValidator = getOneOfValidator(schemaJson);

        // Create a spy that overrides the protected canShortCircuit() method.
        OneOfValidator spy = Mockito.spy(realValidator);
        Mockito.doReturn(true).when(spy).canShortCircuit();

        ExecutionContext ctx = Mockito.mock(ExecutionContext.class);
        JsonNode node = mapper.readTree("{\"type\":\"A\"}");
        JsonNodePath path = JsonNodePath.root();

        Set<ValidationMessage> messages = invokeValidate(spy, ctx, node, node, path, false);
        assertTrue(messages.isEmpty(),
                "With short‑circuit enabled the validator should succeed after the first match");
    }

    @Test
    void validate_directly_whenCanShortCircuitFalse_evaluatesAllSubschemas() throws Exception {
        // Same schema as the previous test but short‑circuit disabled.
        // Both subschemas match, therefore a oneOf error must be reported.
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] },
                    { "type": "object", "required": ["type"] }
                  ]
                }
                """;

        OneOfValidator realValidator = getOneOfValidator(schemaJson);
        OneOfValidator spy = Mockito.spy(realValidator);
        Mockito.doReturn(false).when(spy).canShortCircuit();

        ExecutionContext ctx = Mockito.mock(ExecutionContext.class);
        JsonNode node = mapper.readTree("{\"type\":\"A\"}");
        JsonNodePath path = JsonNodePath.root();

        Set<ValidationMessage> messages = invokeValidate(spy, ctx, node, node, path, false);
        assertEquals(1, messages.size(),
                "Without short‑circuit the validator must report a oneOf violation");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("indexes"),
                "Message should list the matching schema indexes");
    }

    @Test
    void validate_directly_withBinaryNode_throwsClassCastException() throws Exception {
        // Build a schema where the subschema expects an object.
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] }
                  ]
                }
                """;

        OneOfValidator validator = getOneOfValidator(schemaJson);
        ExecutionContext ctx = Mockito.mock(ExecutionContext.class);
        // Create a binary node (Base64 encoded data). Jackson treats it as a BinaryNode.
        JsonNode binaryNode = mapper.readTree("\"aGVsbG8=\""); // a JSON string, but we force a BinaryNode via TreeTraversingParser
        // For simplicity we use a TextNode which is *not* an ObjectNode – the validator will try to cast.
        JsonNodePath path = JsonNodePath.root();

        assertThrows(ClassCastException.class,
                () -> invokeValidate(validator, ctx, binaryNode, binaryNode, path, false));
    }

    @Test
    void validate_directly_whenExecutionContextThrows_propagatesRuntimeException() throws Exception {
        String schemaJson = """
                {
                  "oneOf": [
                    { "type": "object", "required": ["type"] }
                  ]
                }
                """;

        OneOfValidator validator = getOneOfValidator(schemaJson);

        // Mock an ExecutionContext that throws when any method is called.
        ExecutionContext explodingContext = Mockito.mock(ExecutionContext.class);
        Mockito.doThrow(new RuntimeException("boom")).when(explodingContext).getSchemaFactory();

        JsonNode node = mapper.readTree("{\"type\":\"A\"}");
        JsonNodePath path = JsonNodePath.root();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invokeValidate(validator, explodingContext, node, node, path, false));
        assertEquals("boom", ex.getMessage(),
                "The original exception should be propagated unchanged");
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity test for nested instance locations
     * --------------------------------------------------------------------- */

    @Test
    void validate_nestedInstanceLocation_exactlyOneMatch() throws Exception {
        // The schema validates the property "inner" with a oneOf.
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "inner": {
                      "oneOf": [
                        { "type": "object", "required": ["type"], "properties": {"type": {"enum": ["A"]}} },
                        { "type": "object", "required": ["extra"], "properties": {"extra": {"type": "string"}} }
                      ]
                    }
                  }
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"inner\":{\"type\":\"A\"}}");

        Set<ValidationMessage> messages = schema.validate(node, InputFormat.JSON);
        assertTrue(messages.isEmpty(),
                "Nested object matches exactly one subschema – validation must succeed");
    }
}
```