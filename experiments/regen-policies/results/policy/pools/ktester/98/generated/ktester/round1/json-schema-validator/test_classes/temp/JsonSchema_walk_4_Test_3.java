package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_4_Test {

    /**
     * The schema instance under test – created via the public factory.
     */
    private JsonSchema schema;

    /**
     * A mock {@link JsonNode} used as input to the walk method.
     */
    @Mock
    private JsonNode mockNode;

    /**
     * A mock {@link OutputFormat} used to capture the walk result.
     */
    @Mock
    private OutputFormat<String> mockOutputFormat;

    /**
     * A constant value that the mocked overloaded walk method will return.
     */
    private static final String WALK_RESULT = "walk‑result";

    /**
     * Factory used to create {@link JsonSchema} instances from a JSON schema string.
     */
    private static JsonSchemaFactory factory;

    /**
     * Jackson mapper to turn JSON literals into {@link JsonNode}s.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A reusable {@link OutputFormat} that simply returns the collected {@link ValidationMessage}s.
     */
    private static final OutputFormat<Set<ValidationMessage>> COLLECTING_FORMAT = new CollectingOutputFormat<Set<ValidationMessage>>() {

        @Override
        public Set<ValidationMessage> collect(JsonNode node, Set<ValidationMessage> messages) {
            return messages;
        }
    };

    /**
     * An {@link OutputFormat} that deliberately throws an exception when {@code collect} is invoked.
     */
    private static final OutputFormat<Set<ValidationMessage>> FAULTY_FORMAT = new OutputFormat<Set<ValidationMessage>>() {

        @Override
        public Set<ValidationMessage> collect(JsonNode node, Set<ValidationMessage> messages) {
            throw new IllegalStateException("collect failed");
        }
    };

    @BeforeAll
    static void initFactory() {
        // Use the 2020‑12 draft – sufficient for all test schemas.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal schema (empty object) – sufficient for constructing a JsonSchema.
        String minimalSchema = "{}";
        schema = factory.getSchema(minimalSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        reset(mockNode, mockOutputFormat);
    }

    /**
     * Supplies test data for validation‑enabled scenarios.
     *
     * Each {@code Arguments} tuple contains:
     * <ul>
     *   <li>the JSON schema definition string,
     *   <li>the instance JSON string,
     *   <li>a {@code boolean} indicating whether the result is expected to be empty,
     *   <li>a description used for the test name.
     * </ul>
     */
    private static Stream<Arguments> provideValidAndInvalidInstances() {
        return Stream.of(// valid instance – required string property present
        Arguments.of("{ \"type\":\"object\", \"required\":[\"name\"], \"properties\":{\"name\":{\"type\":\"string\"}} }", "{ \"name\":\"John Doe\" }", true, "valid instance – required property satisfied"), // invalid – type mismatch for required property
        Arguments.of("{ \"type\":\"object\", \"required\":[\"name\"], \"properties\":{\"name\":{\"type\":\"string\"}} }", "{ \"name\":123 }", false, "invalid instance – wrong type for property"), // invalid – missing required property
        Arguments.of("{ \"type\":\"object\", \"required\":[\"name\"], \"properties\":{\"name\":{\"type\":\"string\"}} }", "{ }", false, "invalid instance – required property missing"), // invalid – additionalProperties false
        Arguments.of("{ \"type\":\"object\", \"properties\":{\"name\":{\"type\":\"string\"}}, \"additionalProperties\":false }", "{ \"name\":\"John\", \"age\":30 }", false, "invalid instance – disallowed additional property"), // invalid – enum violation
        Arguments.of("{ \"type\":\"object\", \"properties\":{\"status\":{\"type\":\"string\",\"enum\":[\"active\",\"inactive\"]}}, \"required\":[\"status\"] }", "{ \"status\":\"unknown\" }", false, "invalid instance – enum value not allowed"), // invalid – pattern mismatch
        Arguments.of("{ \"type\":\"object\", \"properties\":{\"email\":{\"type\":\"string\",\"pattern\":\"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\\\.[A-Za-z]{2,}$\"}} }", "{ \"email\":\"not-an-email\" }", false, "invalid instance – pattern does not match"), // invalid – numeric minimum violation
        Arguments.of("{ \"type\":\"object\", \"properties\":{\"age\":{\"type\":\"integer\",\"minimum\":0}} }", "{ \"age\":-5 }", false, "invalid instance – number below minimum"), // invalid – array minItems violation
        Arguments.of("{ \"type\":\"object\", \"properties\":{\"tags\":{\"type\":\"array\",\"minItems\":1}} }", "{ \"tags\":[] }", false, "invalid instance – array too short"));
    }

    /**
     * Verifies that {@code walk(JsonNode, OutputFormat<T>, boolean)} forwards the call
     * to the overloaded {@code walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, …)} method
     * and returns the value produced by that method.
     */
    @Test
    void walk_delegatesToOverloadedMethod_andReturnsResult() {
        // --------------------------------------------------------------------
        // GIVEN – a spy of the JsonSchema that returns a predefined result from
        // the overloaded walk method.
        // --------------------------------------------------------------------
        JsonSchema schemaSpy = Mockito.spy(schema);
        // Stub the most specific overload that the public method eventually reaches.
        // The last parameter is an ExecutionContextCustomizer; the public method passes
        // {@code null}, which matches the {@code any()} matcher.
        when(schemaSpy.walk(any(ExecutionContext.class), eq(mockNode), eq(mockOutputFormat), anyBoolean(), any())).thenReturn(WALK_RESULT);
        // --------------------------------------------------------------------
        // WHEN – invoking the public walk method.
        // --------------------------------------------------------------------
        String actual = schemaSpy.walk(mockNode, mockOutputFormat, true);
        // --------------------------------------------------------------------
        // THEN – the returned value matches the stubbed result and the overload
        // was invoked exactly once with the expected arguments.
        // --------------------------------------------------------------------
        assertEquals(WALK_RESULT, actual, "The walk method should return the value from the overloaded method");
        // Verify that the overloaded method was called with the same node, output format
        // and the validate flag we supplied.
        verify(schemaSpy, times(1)).walk(any(ExecutionContext.class), eq(mockNode), eq(mockOutputFormat), eq(true), isNull());
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideValidAndInvalidInstances")
    void walk_validationEnabled_returnsExpectedMessages(String schemaJson, String instanceJson, boolean expectEmpty, String _description) throws Exception {
        JsonSchema testSchema = factory.getSchema(schemaJson);
        JsonNode instance = MAPPER.readTree(instanceJson);
        Set<ValidationMessage> result = testSchema.walk(instance, COLLECTING_FORMAT, true);
        if (expectEmpty) {
            assertTrue(result.isEmpty(), "Expected no validation messages, but got: " + result);
        } else {
            assertFalse(result.isEmpty(), "Expected at least one validation message for: " + instanceJson);
        }
    }

    @Test
    void walk_validationDisabled_alwaysReturnsEmptySet() throws Exception {
        String schemaJson = "{ \"type\":\"object\", \"required\":[\"name\"], \"properties\":{\"name\":{\"type\":\"string\"}} }";
        JsonSchema testSchema = factory.getSchema(schemaJson);
        // type error, but validation disabled
        JsonNode badInstance = MAPPER.readTree("{ \"name\":123 }");
        Set<ValidationMessage> result = testSchema.walk(badInstance, COLLECTING_FORMAT, false);
        assertTrue(result.isEmpty(), "When validation is disabled the result must be empty");
    }

    @Test
    void walk_nullNode_throwsNullPointerException() {
        JsonSchema testSchema = factory.getSchema("{}");
        assertThrows(NullPointerException.class, () -> testSchema.walk(null, COLLECTING_FORMAT, true), "Passing a null JsonNode should raise NPE");
    }

    @Test
    void walk_nullOutputFormat_throwsIllegalArgumentException() {
        JsonSchema testSchema = factory.getSchema("{}");
        JsonNode node = MAPPER.createObjectNode();
        assertThrows(IllegalArgumentException.class, () -> testSchema.walk(node, null, true), "Passing a null OutputFormat should raise IAE");
    }

    @Test
    void walk_booleanSchemaTrue_acceptsAnyInstance() throws Exception {
        // boolean schema true
        JsonSchema trueSchema = factory.getSchema("true");
        JsonNode anyNode = MAPPER.readTree("12345");
        Set<ValidationMessage> result = trueSchema.walk(anyNode, COLLECTING_FORMAT, true);
        assertTrue(result.isEmpty(), "Boolean true schema must accept any instance without messages");
    }

    @Test
    void walk_booleanSchemaFalse_rejectsAnyInstance() throws Exception {
        // boolean schema false
        JsonSchema falseSchema = factory.getSchema("false");
        JsonNode anyNode = MAPPER.readTree("\"any value\"");
        Set<ValidationMessage> result = falseSchema.walk(anyNode, COLLECTING_FORMAT, true);
        assertFalse(result.isEmpty(), "Boolean false schema must produce at least one validation message");
    }

    @Test
    void walk_outputFormatThrowsException_propagatesIt() throws Exception {
        JsonSchema testSchema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = MAPPER.readTree("{\"any\":\"value\"}");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> testSchema.walk(node, FAULTY_FORMAT, true), "Exception from OutputFormat must be propagated");
        assertEquals("collect failed", ex.getMessage());
    }

    @Test
    void walk_recursiveAnchor_doesNotInfiniteLoop() throws Exception {
        // Schema with a recursive $ref to itself (via "#")
        String schemaJson = "{\n" + "  \"$id\": \"#\",\n" + "  \"type\": \"object\",\n" + "  \"properties\": {\n" + "    \"self\": { \"$ref\": \"#\" },\n" + "    \"name\": { \"type\": \"string\" }\n" + "  },\n" + "  \"required\": [\"name\"]\n" + "}";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode instance = MAPPER.readTree("{ \"self\": { \"$ref\": \"#\" }, \"name\": \"Bob\" }");
        Set<ValidationMessage> result = schema.walk(instance, COLLECTING_FORMAT, true);
        // Should succeed – no infinite recursion, and required property is present.
        assertTrue(result.isEmpty(), "Recursive anchor should be handled gracefully without messages");
    }

    @Test
    void walk_refToNonexistentFragment_throwsException() throws Exception {
        String schemaJson = "{ \"type\":\"object\", \"properties\":{\"self\":{\"$ref\":\"#/nonexistent\"}} }";
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode instance = MAPPER.readTree("{ \"self\": {} }");
        // The library throws a runtime exception when a $ref cannot be resolved.
        assertThrows(RuntimeException.class, () -> schema.walk(instance, COLLECTING_FORMAT, true), "Unresolvable $ref should cause a runtime exception");
    }

    @Test
    void walk_schemaWithUnsupportedKeyword_throwsIllegalStateException() throws Exception {
        // The keyword "x-unknownKeyword" is not part of the spec and should trigger a failure during validator creation.
        String schemaJson = "{ \"type\":\"object\", \"x-unknownKeyword\":true }";
        // The exception is thrown at schema creation time.
        assertThrows(RuntimeException.class, () -> factory.getSchema(schemaJson), "Schema containing unknown keyword must cause an exception");
    }
}
