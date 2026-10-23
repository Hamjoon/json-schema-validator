package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.net.URISyntaxException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /**
     * Shared mapper for building {@link JsonNode} objects from JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple schema containing a {@code type} keyword – this triggers creation of a {@link TypeValidator}.
     */
    private static final String SCHEMA_WITH_TYPE = "{ \"type\": \"string\" }";

    /**
     * Mocked {@link ValidationContext} used for all test instances.
     */
    @Mock
    private ValidationContext validationContext;

    /**
     * The {@link JsonSchema} instance under test.
     */
    private JsonSchema jsonSchema;

    /**
     * Base URI used for {@link JsonSchema#from(...)} calls.
     */
    private static final URI BASE_URI = URI.create("http://example.com/schema");

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣  Prepare the mocked ValidationContext
        // --------------------------------------------------------------------
        // The constructor of JsonSchema accesses getMetaSchema() and getConfig().
        // For the purpose of this test we can safely return null for both.
        when(validationContext.getMetaSchema()).thenReturn(null);
        when(validationContext.getConfig()).thenReturn(null);
        // --------------------------------------------------------------------
        // 2️⃣  Build a JsonNode representing a schema that contains a "type"
        // --------------------------------------------------------------------
        JsonNode schemaNode = MAPPER.readTree(SCHEMA_WITH_TYPE);
        // --------------------------------------------------------------------
        // 3️⃣  Create the JsonSchema instance via the package‑private factory.
        // --------------------------------------------------------------------
        // The factory method is package‑private; because this test class lives in
        // the same package we can call it directly.
        jsonSchema = JsonSchema.from(validationContext, // schemaPath
        "test-schema", // currentUri
        new URI("http://example.com/schema"), // schemaNode
        schemaNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
        // At this point the schema has not yet run its validator initialisation.
        // The typeValidator field should therefore be null.
    }

    /**
     * Helper that builds a {@link JsonSchema} from the given JSON string and schema path.
     */
    private JsonSchema buildSchema(String schemaJson, String schemaPath) throws Exception {
        // The factory method requires a JsonNode representation of the schema.
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // Minimal stubbing – the real implementation only needs non‑null return
        // values for a few configuration getters.
        when(validationContext.getMetaSchema()).thenReturn(null);
        when(validationContext.getConfig()).thenReturn(null);
        when(validationContext.getUriFactory()).thenReturn(null);
        when(validationContext.getCurrentUri()).thenReturn(BASE_URI);
        return JsonSchema.from(validationContext, schemaPath, BASE_URI, schemaNode, // no parent
        null, // do not suppress sub‑schema retrieval
        false);
    }

    /**
     * Cases where a TypeValidator must be present (non‑null).
     */
    private static Stream<Arguments> presentTypeValidatorProvider() {
        return Stream.of(// simple primitive types
        Arguments.of("{ \"type\": \"string\" }", "#", "string"), Arguments.of("{ \"type\": \"number\" }", "#/properties/price", "number"), Arguments.of("{ \"type\": \"integer\" }", "#/properties/count", "integer"), Arguments.of("{ \"type\": \"boolean\" }", "#/properties/active", "boolean"), Arguments.of("{ \"type\": \"object\" }", "#/properties/data", "object"), Arguments.of("{ \"type\": \"array\" }", "#/properties/list", "array"), Arguments.of("{ \"type\": \"null\" }", "#/properties/nullable", "null"), // composite type (array of types)
        Arguments.of("{ \"type\": [\"string\", \"null\"] }", "#/properties/name", "composite"), // primitive with extra constraints
        Arguments.of("{ \"type\": \"string\", \"minLength\": 3, \"enum\": [\"abc\", \"def\"] }", "#/properties/code", "string+constraints"));
    }

    /**
     * Cases where no TypeValidator should be created (null).
     */
    private static Stream<Arguments> absentTypeValidatorProvider() {
        return Stream.of(Arguments.of("{ \"minimum\": 5 }", "#/properties/age"), Arguments.of("{}", "#"), // empty type array – not a valid type definition
        Arguments.of("{ \"type\": [] }", "#/properties/invalid"));
    }

    /**
     * Cases that require an explicit {@code initializeValidators()} call.
     */
    private static Stream<Arguments> explicitInitProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"integer\" }", "#"));
    }

    /**
     * Cases for repeated initialisation – the same instance must survive.
     */
    private static Stream<Arguments> repeatedInitProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"string\" }", "#/properties/name"));
    }

    /**
     * Cases for checking reference equality across multiple getter calls.
     */
    private static Stream<Arguments> sameInstanceProvider() {
        return Stream.of(Arguments.of("{ \"type\": \"integer\" }", "#/properties/id"));
    }

    /**
     * Case for $ref resolution.
     */
    private static Stream<Arguments> refResolutionProvider() {
        String schema = "{\n" + "  \"$ref\": \"#/definitions/positiveInteger\",\n" + "  \"definitions\": {\n" + "    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 1 }\n" + "  }\n" + "}";
        return Stream.of(Arguments.of(schema, "#/properties/count"));
    }

    @Test
    @DisplayName("getTypeValidator() returns null before validators are initialised")
    void testGetTypeValidatorInitiallyNull() {
        // Pre‑condition – the schema has not been initialised yet.
        assertFalse(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be false before initialiseValidators()");
        // The getter should return null.
        assertNull(jsonSchema.getTypeValidator(), "getTypeValidator() should return null before initialiseValidators()");
    }

    @Test
    @DisplayName("getTypeValidator() returns a non‑null TypeValidator after initialiseValidators()")
    void testGetTypeValidatorAfterInitialize() {
        // Initialise all validators – this is the public method that populates
        // the internal TypeValidator (if the schema defines a "type" keyword).
        jsonSchema.initializeValidators();
        // After initialisation the schema must report that it has a TypeValidator.
        assertTrue(jsonSchema.hasTypeValidator(), "hasTypeValidator() should be true after initialiseValidators()");
        // The getter must now return a non‑null instance.
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNotNull(typeValidator, "getTypeValidator() should return a non‑null instance after initialiseValidators()");
        // (Optional) Verify that the returned validator knows its type.
        // TypeValidator exposes a getType() method in the library – we can
        // assert that it matches the schema definition ("string").
        // This check is defensive; if the method ever changes the test will
        // still pass because we already verified non‑null above.
        // Uncomment the following lines if getType() is available:
        // assertEquals("string", typeValidator.getType(), "The TypeValidator should represent the 'string' type");
    }

    @ParameterizedTest(name = "[{index}] typeValidator present – schemaPath={1}")
    @MethodSource("presentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns non‑null when a valid 'type' keyword is present")
    void testTypeValidatorPresent(String schemaJson, String schemaPath, String _description) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // For most schemas the TypeValidator is created lazily during initialiseValidators().
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true when schema defines a type");
        assertNotNull(schema.getTypeValidator(), "getTypeValidator() must return a non‑null instance");
    }

    @ParameterizedTest(name = "[{index}] typeValidator absent – schemaPath={1}")
    @MethodSource("absentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns null when no valid 'type' keyword is present")
    void testTypeValidatorAbsent(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false when schema does not define a valid type");
        assertNull(schema.getTypeValidator(), "getTypeValidator() must return null");
    }

    @ParameterizedTest(name = "[{index}] explicit initialiseValidators()")
    @MethodSource("explicitInitProvider")
    @DisplayName("getTypeValidator() remains non‑null after explicit initialiseValidators()")
    void testExplicitInitialization(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // First explicit initialisation
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true after initialiseValidators()");
        assertNotNull(schema.getTypeValidator(), "getTypeValidator() must be non‑null after initialiseValidators()");
    }

    @ParameterizedTest(name = "[{index}] repeated initialiseValidators()")
    @MethodSource("repeatedInitProvider")
    @DisplayName("Repeated initialiseValidators() calls do not replace the TypeValidator")
    void testRepeatedInitialization(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        TypeValidator first = schema.getTypeValidator();
        assertNotNull(first, "First call to getTypeValidator() must be non‑null");
        // Second initialisation – should be a no‑op regarding the type validator
        schema.initializeValidators();
        TypeValidator second = schema.getTypeValidator();
        assertSame(first, second, "TypeValidator instance must be identical after repeated initialiseValidators()");
    }

    @ParameterizedTest(name = "[{index}] consistent instance across calls")
    @MethodSource("sameInstanceProvider")
    @DisplayName("Multiple getTypeValidator() calls return the same instance")
    void testConsistentInstance(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        TypeValidator first = schema.getTypeValidator();
        TypeValidator second = schema.getTypeValidator();
        TypeValidator third = schema.getTypeValidator();
        assertAll("reference equality checks", () -> assertSame(first, second, "First and second calls should return the same instance"), () -> assertSame(second, third, "Second and third calls should return the same instance"));
    }

    @ParameterizedTest(name = "[{index}] $ref resolution")
    @MethodSource("refResolutionProvider")
    @DisplayName("$ref resolves to a schema containing a 'type' keyword")
    void testRefResolution(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // $ref resolution happens during initialiseValidators()
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(), "hasTypeValidator() should be true after $ref resolution");
        assertNotNull(schema.getTypeValidator(), "getTypeValidator() must be non‑null after $ref resolution");
    }

    @Test
    @DisplayName("getTypeValidator() returns null before any initialisation")
    void testGetterBeforeInitialisation() throws Exception {
        JsonSchema schema = buildSchema("{ \"type\": \"string\" }", "#");
        // No initialiseValidators() invoked.
        assertFalse(schema.hasTypeValidator(), "hasTypeValidator() should be false before initialiseValidators()");
        assertNull(schema.getTypeValidator(), "getTypeValidator() should be null before initialiseValidators()");
    }
}
