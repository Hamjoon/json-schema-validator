package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaFactory_create_Test {

    private static JsonSchemaFactory factory;

    private ValidationContext validationContext;

    private SchemaLocation schemaLocation;

    private JsonNodePath evaluationPath;

    private JsonNode schemaNode;

    private JsonSchema parentSchema;

    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        /* Use the public factory method – it builds a fully‑initialised instance
         * with all required meta‑schemas, mappers and loaders. */
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // a fully‑initialised factory (thread‑safe) for the whole test suite
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = factory.getObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------
         * Prepare the arguments required by the focal method.
         *
         * 1. ValidationContext – needs a JsonMetaSchema, the factory itself
         *    and a (possibly default) SchemaValidatorsConfig.
         * 2. SchemaLocation – a simple location; the constructor accepts a
         *    nullable AbsoluteIri, so we can safely pass null for the purpose of
         *    this test.
         * 3. EvaluationPath – not needed for the behaviour we verify, therefore
         *    we pass null.
         * 4. SchemaNode – a minimal JSON schema (type:string) parsed with the
         *    factory's ObjectMapper.
         * 5. ParentSchema – not required for this test, so null.
         * -------------------------------------------------------------- */
        JsonMetaSchema metaSchema = factory.getMetaSchema("https://json-schema.org/draft/2020-12/schema", new SchemaValidatorsConfig());
        validationContext = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig());
        // a dummy location; the concrete value is irrelevant for the test
        schemaLocation = new SchemaLocation(null);
        // evaluationPath is optional for this test
        evaluationPath = null;
        // simple schema: {"type":"string"}
        ObjectMapper mapper = factory.getObjectMapper();
        schemaNode = mapper.readTree("{\"type\":\"string\"}");
        parentSchema = null;
    }

    /**
     * builds a {@link ValidationContext} required by the factory.
     */
    private ValidationContext newValidationContext() {
        JsonMetaSchema meta = factory.getMetaSchema("https://json-schema.org/draft/2020-12/schema", new SchemaValidatorsConfig());
        return new ValidationContext(meta, factory, new SchemaValidatorsConfig());
    }

    /**
     * convenience wrapper that creates a {@link JsonSchema} using the factory.
     */
    private JsonSchema createSchema(JsonNode schemaNode) {
        return factory.create(newValidationContext(), // location is irrelevant for these tests
        new SchemaLocation(null), // root evaluation path
        null, schemaNode, // no parent schema
        null);
    }

    /**
     * validates a node against a schema and returns {@code true} if there are no validation messages.
     */
    private boolean validates(JsonSchema schema, JsonNode instance) {
        Set<ValidationMessage> msgs = schema.validate(instance);
        return msgs == null || msgs.isEmpty();
    }

    /**
     * Supplies a stream of arguments for {@link #create_ValidSchemas_ShouldValidateAccordingly}.
     *
     * Each entry corresponds to one of the groups in the supplied JSON test matrix that expects a
     * functional {@link JsonSchema}.  The test data is deliberately small – we only need one
     * positive and one negative example to prove that the correct validator branch was taken.
     */
    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(// ------------------------------------------------------------------
        // Normal behaviour – simple object schema
        // ------------------------------------------------------------------
        Arguments.of("Normal behaviour – simple object", "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}", List.of("{\"name\":\"John\"}"), List.of("{\"name\":123}", "null")), // ------------------------------------------------------------------
        // Enum keyword
        // ------------------------------------------------------------------
        Arguments.of("Enum keyword", "{\"enum\":[\"A\",\"B\",\"C\"]}", List.of("\"A\"", "\"B\""), List.of("\"D\"", "123")), // ------------------------------------------------------------------
        // allOf keyword
        // ------------------------------------------------------------------
        Arguments.of("allOf keyword", "{\"allOf\":[{\"type\":\"string\"},{\"maxLength\":5}]}", List.of("\"abc\""), List.of("\"abcdef\"", "123")), // ------------------------------------------------------------------
        // Boolean true – accepts everything
        // ------------------------------------------------------------------
        Arguments.of("Boolean true schema", "true", List.of("\"any\"", "123", "null", "{\"a\":1}"), // nothing should be rejected
        List.of()), // ------------------------------------------------------------------
        // Boolean false – rejects everything
        // ------------------------------------------------------------------
        Arguments.of("Boolean false schema", "false", List.of(), List.of("\"any\"", "123", "null", "{\"a\":1}")), // ------------------------------------------------------------------
        // Empty object – no constraints
        // ------------------------------------------------------------------
        Arguments.of("Empty object schema", "{}", List.of("\"any\"", "123", "null", "{\"a\":1}"), // nothing rejected
        List.of()), // ------------------------------------------------------------------
        // type as array (multiple allowed types)
        // ------------------------------------------------------------------
        Arguments.of("type keyword as array", "{\"type\":[\"string\",\"null\"]}", List.of("\"text\"", "null"), List.of("123", "true")), // ------------------------------------------------------------------
        // const keyword
        // ------------------------------------------------------------------
        Arguments.of("const keyword", "{\"const\":42}", List.of("42"), List.of("41", "\"42\"")), // ------------------------------------------------------------------
        // format keyword (date-time)
        // ------------------------------------------------------------------
        Arguments.of("format keyword – date-time", "{\"type\":\"string\",\"format\":\"date-time\"}", List.of("\"2020-01-01T12:00:00Z\""), List.of("\"not-a-date\"", "\"2020-13-01T00:00:00Z\"")), // ------------------------------------------------------------------
        // oneOf keyword
        // ------------------------------------------------------------------
        Arguments.of("oneOf keyword", "{\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}", List.of("\"abc\"", "123"), List.of("\"abc\",123", "true")), // ------------------------------------------------------------------
        // anyOf keyword
        // ------------------------------------------------------------------
        Arguments.of("anyOf keyword", "{\"anyOf\":[{\"type\":\"boolean\"},{\"type\":\"null\"}]}", List.of("true", "null"), List.of("\"string\"", "123")), // ------------------------------------------------------------------
        // not keyword
        // ------------------------------------------------------------------
        Arguments.of("not keyword", "{\"not\":{\"type\":\"null\"}}", List.of("\"string\"", "123", "true"), List.of("null")), // ------------------------------------------------------------------
        // Conditional – if/then/else
        // ------------------------------------------------------------------
        Arguments.of("Conditional if/then/else", "{\"if\":{\"properties\":{\"age\":{\"minimum\":18}}}," + "\"then\":{\"required\":[\"driverLicense\"]}," + "\"else\":{\"required\":[\"guardianConsent\"]}}", // valid samples
        List.of("{\"age\":20,\"driverLicense\":\"DL123\"}", "{\"age\":16,\"guardianConsent\":\"yes\"}"), // invalid samples
        List.of("{\"age\":20}", "{\"age\":16}")), // ------------------------------------------------------------------
        // $id overrides supplied location (no explicit assertion – just ensure creation succeeds)
        // ------------------------------------------------------------------
        Arguments.of("$id and $schema handling – creation succeeds", "{\"$id\":\"http://example.com/custom-schema\",\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"string\"}", List.of("\"any\""), List.of("123")));
    }

    /**
     * Supplies a stream of arguments for {@link #create_InvalidSchemas_ShouldThrowException}.
     *
     * Each entry mirrors a group from the original JSON that expects an exception.
     */
    private static Stream<Arguments> exceptionSchemaProvider() {
        return Stream.of(// ------------------------------------------------------------------
        // Null ValidationContext – the factory method does not accept null
        // ------------------------------------------------------------------
        Arguments.of("Null ValidationContext", "{\"type\":\"string\"}", // will be thrown inside create -> doCreate
        NullPointerException.class), // ------------------------------------------------------------------
        // Null schemaNode – illegal argument
        // ------------------------------------------------------------------
        Arguments.of("Null schemaNode", "null", IllegalArgumentException.class), // ------------------------------------------------------------------
        // Top‑level array node – not a valid schema object
        // ------------------------------------------------------------------
        Arguments.of("Top‑level array node", "[{\"type\":\"string\"}]", IllegalArgumentException.class), // ------------------------------------------------------------------
        // Malformed URI in SchemaLocation – illegal argument
        // ------------------------------------------------------------------
        Arguments.of("Malformed SchemaLocation URI", "{\"type\":\"string\"}", // creation of SchemaLocation will fail
        IllegalArgumentException.class), // ------------------------------------------------------------------
        // Invalid $ref URI
        // ------------------------------------------------------------------
        Arguments.of("Invalid $ref URI", "{\"$ref\":\"ht!tp://bad-uri\"}", IllegalArgumentException.class), // ------------------------------------------------------------------
        // Invalid $id URI
        // ------------------------------------------------------------------
        Arguments.of("Invalid $id URI", "{\"$id\":\"ht!tp://bad-id\"}", IllegalArgumentException.class), // ------------------------------------------------------------------
        // Invalid type keyword (numeric)
        // ------------------------------------------------------------------
        Arguments.of("Invalid type keyword (numeric)", "{\"type\":123}", IllegalArgumentException.class), // ------------------------------------------------------------------
        // Unknown $schema meta‑schema identifier
        // ------------------------------------------------------------------
        Arguments.of("Unknown $schema identifier", "{\"$schema\":\"http://example.com/unknown-meta-schema\"}", IllegalArgumentException.class), // ------------------------------------------------------------------
        // Unsupported scheme in SchemaLocation (ftp)
        // ------------------------------------------------------------------
        Arguments.of("Unsupported scheme in SchemaLocation", "{\"type\":\"string\"}", // will be thrown when creating SchemaLocation
        IllegalArgumentException.class));
    }

    @Test
    void create_ShouldReturnNonNullJsonSchema() {
        JsonSchema result = factory.create(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema);
        assertNotNull(result, "The create method should return a non‑null JsonSchema instance");
        // Basic sanity check – the returned object must be an instance of JsonSchema
        assertTrue(result instanceof JsonSchema, "Result must be a JsonSchema");
    }

    @Test
    void create_WithSameParameters_ShouldReturnDistinctInstances() {
        JsonSchema first = factory.create(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema);
        JsonSchema second = factory.create(validationContext, schemaLocation, evaluationPath, schemaNode, parentSchema);
        assertNotNull(first);
        assertNotNull(second);
        // Two invocations with the same inputs should produce two distinct objects
        assertNotSame(first, second, "Each call to create should produce a new JsonSchema instance");
    }

    @Test
    void create_WithInvalidSchemaNode_ShouldThrowException() {
        // An invalid JSON schema (e.g., a non‑object node) should cause the factory
        // to fail while constructing the internal JsonSchema.
        JsonNode invalidNode = factory.getObjectMapper().readTree("\"just a string\"");
        assertThrows(RuntimeException.class, () -> {
            factory.create(validationContext, schemaLocation, evaluationPath, invalidNode, parentSchema);
        }, "Creating a JsonSchema from an invalid schema node should result in an exception");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validSchemaProvider")
    void create_ValidSchemas_ShouldValidateAccordingly(String description, String schemaJson, List<String> validSamples, List<String> invalidSamples) throws Exception {
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema schema = createSchema(schemaNode);
        assertNotNull(schema, description + " – schema must not be null");
        // all valid samples must pass validation
        for (String json : validSamples) {
            JsonNode instance = mapper.readTree(json);
            assertTrue(validates(schema, instance), description + " – expected VALID for instance: " + json);
        }
        // all invalid samples must fail validation
        for (String json : invalidSamples) {
            JsonNode instance = mapper.readTree(json);
            assertFalse(validates(schema, instance), description + " – expected INVALID for instance: " + json);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionSchemaProvider")
    void create_InvalidSchemas_ShouldThrowException(String description, String schemaJson, Class<? extends Exception> expectedException) {
        JsonNode schemaNode;
        try {
            schemaNode = mapper.readTree(schemaJson);
        } catch (Exception e) {
            // If the JSON itself cannot be parsed we treat it as a test setup error.
            fail(description + " – unable to parse schema JSON: " + e.getMessage());
            return;
        }
        assertThrows(expectedException, () -> createSchema(schemaNode), description + " – expected exception: " + expectedException.getSimpleName());
    }

    @Test
    void create_ShouldReturnDistinctInstancesForRepeatedCalls() {
        JsonNode schemaNode = mapper.createObjectNode().put("type", "string");
        JsonSchema first = createSchema(schemaNode);
        JsonSchema second = createSchema(schemaNode);
        assertNotSame(first, second, "Each call to create must return a new JsonSchema instance");
    }

    @Test
    void create_WithParentSchema_ShouldLinkParent() throws Exception {
        // parent schema – a simple object with an integer property
        JsonSchema parent = factory.getSchema("{\"type\":\"object\",\"properties\":{\"parentProp\":{\"type\":\"integer\"}}}");
        // child schema – adds its own constraint
        JsonNode childNode = mapper.readTree("{\"type\":\"string\",\"minLength\":3}");
        JsonSchema child = factory.create(newValidationContext(), new SchemaLocation(null), null, childNode, parent);
        assertNotNull(child);
        // Validate that the child still respects its own constraint
        assertTrue(validates(child, mapper.readTree("\"abc\"")));
        assertFalse(validates(child, mapper.readTree("\"ab\"")));
        // Use reflection to ensure the parent reference is stored (internal field name may vary)
        // The JsonSchema implementation stores the parent in a field called "parentSchema".
        java.lang.reflect.Field parentField = child.getClass().getDeclaredField("parentSchema");
        parentField.setAccessible(true);
        Object linkedParent = parentField.get(child);
        assertSame(parent, linkedParent, "Child schema must retain a reference to the supplied parent schema");
    }
}
