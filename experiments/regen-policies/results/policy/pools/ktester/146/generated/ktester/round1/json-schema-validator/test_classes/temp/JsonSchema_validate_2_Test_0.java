package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    /**
     * ObjectMapper used to build {@link JsonNode}s from JSON strings.
     */
    private static ObjectMapper mapper;

    /**
     * The schema under test.
     */
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a shared ObjectMapper – thread‑safe for read‑only use.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        /* -------------------------------------------------------------
         * Build a simple schema:
         *
         * {
         *   "$schema": "https://json-schema.org/draft/2020-12/schema",
         *   "type": "object",
         *   "properties": {
         *     "name": { "type": "string" }
         *   },
         *   "required": ["name"]
         * }
         *
         * The JsonSchemaFactory is the supported entry point for creating a
         * JsonSchema instance.  No reflection or private constructors are used.
         * ------------------------------------------------------------- */
        String schemaJson = "{" + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\"," + "\"type\":\"object\"," + "\"properties\":{" + "   \"name\":{ \"type\":\"string\" }" + "}," + "\"required\":[\"name\"]" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Use the default configuration – it is sufficient for these tests.
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should be created by the factory");
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
        mapper = null;
    }

    /**
     * Helper that creates a {@link JsonSchema} from a JSON string using the
     * default 2020‑12 draft factory.
     */
    private JsonSchema createSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    private static Stream<Arguments> enumConstProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        return Stream.of(Arguments.of(m.readTree("\"RED\""), "{\"enum\":[\"RED\",\"GREEN\",\"BLUE\"]}", true), Arguments.of(m.readTree("\"YELLOW\""), "{\"enum\":[\"RED\",\"GREEN\",\"BLUE\"]}", false), Arguments.of(m.readTree("42"), "{\"const\":42}", true), Arguments.of(m.readTree("43"), "{\"const\":42}", false));
    }

    private static Stream<Arguments> numericProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        return Stream.of(Arguments.of(m.readTree("10"), "{\"type\":\"integer\",\"minimum\":5,\"maximum\":15}", true), Arguments.of(m.readTree("4"), "{\"type\":\"integer\",\"minimum\":5}", false), Arguments.of(m.readTree("20.5"), "{\"type\":\"number\",\"maximum\":20}", false), Arguments.of(m.readTree("9"), "{\"type\":\"integer\",\"multipleOf\":3}", false));
    }

    private static Stream<Arguments> arrayProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        return Stream.of(Arguments.of(m.readTree("[1,2,3]"), "{\"type\":\"array\",\"minItems\":2,\"maxItems\":5}", true), Arguments.of(m.readTree("[1]"), "{\"type\":\"array\",\"minItems\":2}", false), Arguments.of(m.readTree("[1,2,3,4,5,6]"), "{\"type\":\"array\",\"maxItems\":5}", false), Arguments.of(m.readTree("[\"a\",\"b\",\"a\"]"), "{\"type\":\"array\",\"uniqueItems\":true}", false), Arguments.of(m.readTree("[1,\"two\",3]"), "{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}", false));
    }

    private static Stream<Arguments> combinatorProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        return Stream.of(Arguments.of(m.readTree("\"cat\""), "{\"oneOf\":[{\"type\":\"string\",\"enum\":[\"dog\",\"cat\"]},{\"type\":\"integer\"}]}", true), Arguments.of(m.readTree("42"), "{\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}", true), Arguments.of(m.readTree("\"dog\""), "{\"oneOf\":[{\"type\":\"string\",\"enum\":[\"dog\",\"cat\"]},{\"type\":\"string\",\"pattern\":\"^d.*\"}]}", false), Arguments.of(m.readTree("\"blue\""), "{\"anyOf\":[{\"type\":\"string\",\"enum\":[\"red\",\"green\"]},{\"type\":\"string\",\"pattern\":\"^b.*\"}]}", true), Arguments.of(m.readTree("5"), "{\"allOf\":[{\"type\":\"integer\",\"minimum\":0},{\"type\":\"integer\",\"maximum\":10}]}", true));
    }

    private static Stream<Arguments> conditionalProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        String schemaIfThenElse = "{" + "\"if\":{\"properties\":{\"type\":{\"const\":\"A\"}}}," + "\"then\":{\"properties\":{\"value\":{\"type\":\"integer\",\"minimum\":0}},\"required\":[\"value\"]}," + "\"else\":{\"properties\":{\"value\":{\"type\":\"string\"}},\"required\":[\"value\"]}" + "}";
        String schemaIfThenElseMismatch = "{" + "\"if\":{\"properties\":{\"type\":{\"const\":\"A\"}}}," + "\"then\":{\"properties\":{\"value\":{\"type\":\"integer\"}},\"required\":[\"value\"]}," + "\"else\":{\"properties\":{\"value\":{\"type\":\"string\"}},\"required\":[\"value\"]}" + "}";
        return Stream.of(Arguments.of(m.readTree("{\"type\":\"A\",\"value\":5}"), schemaIfThenElse, true), Arguments.of(m.readTree("{\"type\":\"B\",\"value\":5}"), schemaIfThenElseMismatch, false));
    }

    private static Stream<Arguments> additionalPropsProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        String base = "{\"type\":\"object\",\"properties\":{\"known\":{\"type\":\"integer\"}},\"additionalProperties\":false}";
        return Stream.of(Arguments.of(m.readTree("{\"known\":1}"), base, true), Arguments.of(m.readTree("{\"known\":1,\"extra\":\"no\"}"), base, false));
    }

    private static Stream<Arguments> nullableProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        String schema = "{\"type\":[\"string\",\"null\"]}";
        return Stream.of(Arguments.of(m.readTree("null"), schema, true), Arguments.of(m.readTree("\"text\""), schema, true), Arguments.of(m.readTree("123"), schema, false));
    }

    private static Stream<Arguments> emptySchemaProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        return Stream.of(Arguments.of(m.readTree("\"anything\""), true), Arguments.of(m.readTree("123"), true));
    }

    private static Stream<Arguments> notKeywordProvider() throws Exception {
        ObjectMapper m = new ObjectMapper();
        return Stream.of(Arguments.of(m.readTree("\"allowed\""), "{\"not\":{\"type\":\"integer\"}}", true), Arguments.of(m.readTree("42"), "{\"not\":{\"type\":\"integer\"}}", false));
    }

    private static Stream<Arguments> malformedSchemaProvider() {
        return Stream.of(// schema root not an object
        Arguments.of("[ { \"type\": \"string\" } ]", JsonSchemaException.class), // type keyword not string/array
        Arguments.of("{ \"type\": 123 }", JsonSchemaException.class), // $ref not a string
        Arguments.of("{ \"$ref\": 123 }", JsonSchemaException.class), // unknown keyword
        Arguments.of("{ \"unknownKeyword\": true }", JsonSchemaException.class), // circular $ref without recursive anchor
        Arguments.of("{ \"$id\": \"#root\", \"$ref\": \"#root\" }", JsonSchemaException.class), // type array containing non‑string
        Arguments.of("{ \"type\": [\"string\", 123] }", JsonSchemaException.class));
    }

    /**
     * Verify that {@code validate(JsonNode)} returns a non‑empty set when the
     * supplied JSON does not satisfy the schema (wrong type for {@code name}).
     */
    @Test
    void validate_invalidJsonNode_returnsValidationMessages() throws Exception {
        // JSON where "name" is a number – violates the "type":"string" rule.
        String invalidJson = "{ \"name\": 123 }";
        JsonNode node = mapper.readTree(invalidJson);
        Set<ValidationMessage> messages = schema.validate(node);
        assertNotNull(messages, "Resulting set must not be null");
        assertFalse(messages.isEmpty(), "There should be at least one validation error");
        // The exact message text may vary with locale; we only assert that a
        // message concerning the property "name" is present.
        boolean containsName = messages.stream().anyMatch(m -> m.getPath().equals("name"));
        assertTrue(containsName, "Validation errors should reference the 'name' property");
    }

    /**
     * Verify that a valid JSON document yields an empty validation result.
     */
    @Test
    void validate_validJsonNode_returnsEmptySet() throws Exception {
        String validJson = "{ \"name\": \"John Doe\" }";
        JsonNode node = mapper.readTree(validJson);
        Set<ValidationMessage> messages = schema.validate(node);
        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "A valid document should produce no validation errors");
    }

    /**
     * --------------------------------------------------------------------
     *  Basic delegation & null handling
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_nullRootNode_throwsNullPointerException() {
        // Use a minimal valid schema – the actual schema content is irrelevant
        // because the method should fail before it touches the schema.
        schema = createSchema("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class, () -> schema.validate((JsonNode) null));
    }

    /**
     * --------------------------------------------------------------------
     *  Simple valid / invalid cases – reuse the schema from the original
     *  test class (object with required string property "name").
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_validObject_returnsEmptySet() throws Exception {
        schema = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"John\"}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertTrue(msgs.isEmpty(), "Valid object should produce no messages");
    }

    @Test
    void validate_invalidObject_typeMismatch_returnsMessages() throws Exception {
        schema = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":123}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Type mismatch must generate a message");
        assertTrue(msgs.stream().anyMatch(m -> "name".equals(m.getPath())), "Message should reference the 'name' property");
    }

    /**
     * --------------------------------------------------------------------
     *  Required property missing
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_missingRequiredProperty_returnsMessage() throws Exception {
        schema = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"age\":25}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Missing required property must be reported");
    }

    /**
     * --------------------------------------------------------------------
     *  $ref handling (normal and URL‑encoded)
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_refResolvedSuccessfully_returnsEmptySet() throws Exception {
        String schemaJson = "{" + "\"$defs\":{" + "   \"person\":{ \"type\":\"object\", \"properties\":{\"name\":{\"type\":\"string\"}}, \"required\":[\"name\"] }" + "}," + "\"$ref\":\"#/$defs/person\"" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertTrue(msgs.isEmpty(), "Resolved $ref with valid instance should pass");
    }

    @Test
    void validate_refUnresolved_returnsMessage() throws Exception {
        String schemaJson = "{" + "\"$defs\":{" + "   \"person\":{ \"type\":\"object\" }" + "}," + "\"$ref\":\"#/unknown\"" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Unresolved $ref must produce a validation message");
    }

    @Test
    void validate_urlEncodedRef_resolvesCorrectly() throws Exception {
        String schemaJson = "{" + "\"definitions\":{" + "   \"color\":{ \"type\":\"string\", \"enum\":[\"red\",\"green\",\"blue\"] }" + "}," + "\"$ref\":\"#/definitions%2Fcolor\"" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("\"green\"");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertTrue(msgs.isEmpty(), "URL‑encoded $ref should resolve and validate");
    }

    @Test
    void validate_urlEncodedRefUnresolved_returnsMessage() throws Exception {
        String schemaJson = "{" + "\"definitions\":{" + "   \"color\":{ \"type\":\"string\" }" + "}," + "\"$ref\":\"#/definitions%2Funknown\"" + "}";
        schema = createSchema(schemaJson);
        JsonNode node = mapper.readTree("\"any\"");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Unresolved URL‑encoded $ref should yield a message");
    }

    /**
     * --------------------------------------------------------------------
     *  Boolean schema handling
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_booleanFalseSchema_alwaysFails() throws Exception {
        schema = createSchema("false");
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Boolean false schema must reject any instance");
    }

    @Test
    void validate_booleanTrueSchema_alwaysPasses() throws Exception {
        schema = createSchema("true");
        JsonNode node = mapper.readTree("null");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertTrue(msgs.isEmpty(), "Boolean true schema must accept any instance");
    }

    /**
     * --------------------------------------------------------------------
     *  Enum & const constraints
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: enum const test")
    @MethodSource("enumConstProvider")
    void validate_enumConst(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        if (expectValid) {
            assertTrue(msgs.isEmpty(), "Instance should be valid");
        } else {
            assertFalse(msgs.isEmpty(), "Instance should be invalid");
        }
    }

    /**
     * --------------------------------------------------------------------
     *  Numeric limits & multipleOf
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: numeric limits")
    @MethodSource("numericProvider")
    void validate_numericLimits(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "Expected " + (expectValid ? "no" : "some") + " validation messages");
    }

    /**
     * --------------------------------------------------------------------
     *  Array constraints
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: array constraints")
    @MethodSource("arrayProvider")
    void validate_arrayConstraints(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "Array validation expected " + (expectValid ? "valid" : "invalid"));
    }

    /**
     * --------------------------------------------------------------------
     *  Combinators (oneOf, anyOf, allOf)
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: combinators")
    @MethodSource("combinatorProvider")
    void validate_combinators(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "Combinator validation expected " + (expectValid ? "valid" : "invalid"));
    }

    /**
     * --------------------------------------------------------------------
     *  Conditional if/then/else
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: if/then/else")
    @MethodSource("conditionalProvider")
    void validate_conditional(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "Conditional validation expected " + (expectValid ? "valid" : "invalid"));
    }

    /**
     * --------------------------------------------------------------------
     *  additionalProperties = false
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: additionalProperties")
    @MethodSource("additionalPropsProvider")
    void validate_additionalProperties(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "additionalProperties validation expected " + (expectValid ? "valid" : "invalid"));
    }

    /**
     * --------------------------------------------------------------------
     *  Nullable type handling (type array with "null")
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: nullable types")
    @MethodSource("nullableProvider")
    void validate_nullableTypes(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "Nullable type validation expected " + (expectValid ? "valid" : "invalid"));
    }

    /**
     * --------------------------------------------------------------------
     *  Empty schema (no constraints)
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: empty schema")
    @MethodSource("emptySchemaProvider")
    void validate_emptySchema(JsonNode instance, boolean expectValid) {
        schema = createSchema("{}");
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "Empty schema should accept any instance");
    }

    /**
     * --------------------------------------------------------------------
     *  not keyword
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: not keyword")
    @MethodSource("notKeywordProvider")
    void validate_notKeyword(JsonNode instance, String schemaJson, boolean expectValid) {
        schema = createSchema(schemaJson);
        Set<ValidationMessage> msgs = schema.validate(instance);
        assertEquals(expectValid, msgs.isEmpty(), "not‑keyword validation expected " + (expectValid ? "valid" : "invalid"));
    }

    /**
     * --------------------------------------------------------------------
     *  Malformed schema – expect JsonSchemaException
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: malformed schema")
    @MethodSource("malformedSchemaProvider")
    void validate_malformedSchema_throwsException(String schemaJson, Class<? extends Exception> expected) {
        assertThrows(expected, () -> createSchema(schemaJson));
    }

    /**
     * --------------------------------------------------------------------
     *  ExecutionContextCustomizer induced exception
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_executionCustomizerThrows_propagatesException() throws Exception {
        schema = createSchema("{}");
        JsonNode node = mapper.readTree("\"any\"");
        ExecutionContextCustomizer failingCustomizer = ec -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, failingCustomizer));
        assertEquals("customizer failure", ex.getMessage());
    }

    /**
     * --------------------------------------------------------------------
     *  Format validation (uri) – should return a ValidationMessage, not an exception
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_formatUri_invalidUri_returnsMessage() throws Exception {
        schema = createSchema("{\"type\":\"string\",\"format\":\"uri\"}");
        // not a valid URI
        JsonNode node = mapper.readTree("\"trigger\"");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Invalid URI format should produce a validation message");
    }

    /**
     * --------------------------------------------------------------------
     *  Recursive anchor handling – simple sanity check (no infinite recursion)
     *  -----------------------------------------------------------------
     */
    @Test
    void validate_recursiveAnchor_validInstance_noStackOverflow() throws Exception {
        String schemaJson = "{" + "\"$id\":\"#myAnchor\"," + "\"$recursiveAnchor\":true," + "\"type\":\"object\"," + "\"properties\":{" + "   \"self\":{ \"$ref\":\"#myAnchor\" }" + "}," + "\"required\":[\"self\"]" + "}";
        schema = createSchema(schemaJson);
        // instance satisfies recursive definition
        JsonNode node = mapper.readTree("{\"self\":{}}");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertTrue(msgs.isEmpty(), "Recursive anchor with valid instance should produce no errors");
    }

    @Test
    void validate_recursiveAnchor_invalidInstance_reportsError() throws Exception {
        String schemaJson = "{" + "\"$id\":\"#myAnchor\"," + "\"$recursiveAnchor\":true," + "\"type\":\"object\"," + "\"properties\":{" + "   \"self\":{ \"$ref\":\"#myAnchor\" }" + "}," + "\"required\":[\"self\"]" + "}";
        schema = createSchema(schemaJson);
        // missing required "self"
        JsonNode node = mapper.readTree("{ }");
        Set<ValidationMessage> msgs = schema.validate(node);
        assertFalse(msgs.isEmpty(), "Missing required property in recursive schema should be reported");
    }
}
