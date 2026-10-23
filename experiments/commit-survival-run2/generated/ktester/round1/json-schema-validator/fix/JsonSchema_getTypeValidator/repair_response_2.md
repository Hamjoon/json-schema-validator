```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;
import com.networknt.schema.meta.JsonMetaSchema;

/**
 * Tests for {@link JsonSchema#getTypeValidator()} and related behaviour.
 */
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
     * ValidationContext used for all test instances.
     */
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
        // Minimal ValidationContext – the library only needs the factories,
        // a meta‑schema, a schema factory and a config (all can be the defaults).
        validationContext = new ValidationContext(
                new URIFactory(),
                new URNFactory(),
                JsonMetaSchema.getV4(),
                JsonSchemaFactory.getInstance(),
                new SchemaValidatorsConfig());

        // Build a JsonNode representing a schema that contains a "type"
        JsonNode schemaNode = MAPPER.readTree(SCHEMA_WITH_TYPE);

        // Create the JsonSchema instance via the package‑private factory.
        jsonSchema = JsonSchema.from(
                validationContext,
                "test-schema",   // schemaPath
                BASE_URI,        // schemaUri
                schemaNode,      // schemaNode
                null,            // parent schema
                false);          // isRef
        // At this point the schema has not yet run its validator initialisation.
        // The typeValidator field should therefore be null.
    }

    /**
     * Helper that builds a {@link JsonSchema} from the given JSON string and schema path.
     */
    private JsonSchema buildSchema(String schemaJson, String schemaPath) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaJson);
        // Re‑use the same minimal ValidationContext for every schema.
        if (validationContext == null) {
            validationContext = new ValidationContext(
                    new URIFactory(),
                    new URNFactory(),
                    JsonMetaSchema.getV4(),
                    JsonSchemaFactory.getInstance(),
                    new SchemaValidatorsConfig());
        }
        return JsonSchema.from(
                validationContext,
                schemaPath,      // schemaPath
                BASE_URI,        // schemaUri
                schemaNode,      // schemaNode
                null,            // parent schema
                false);          // isRef
    }

    /**
     * Cases where a TypeValidator must be present (non‑null).
     */
    private static Stream<Arguments> presentTypeValidatorProvider() {
        return Stream.of(
                Arguments.of("{ \"type\": \"string\" }", "#", "string"),
                Arguments.of("{ \"type\": \"number\" }", "#/properties/price", "number"),
                Arguments.of("{ \"type\": \"integer\" }", "#/properties/count", "integer"),
                Arguments.of("{ \"type\": \"boolean\" }", "#/properties/active", "boolean"),
                Arguments.of("{ \"type\": \"object\" }", "#/properties/data", "object"),
                Arguments.of("{ \"type\": \"array\" }", "#/properties/list", "array"),
                Arguments.of("{ \"type\": \"null\" }", "#/properties/nullable", "null"),
                // composite type (array of types)
                Arguments.of("{ \"type\": [\"string\", \"null\"] }", "#/properties/name", "composite"),
                // primitive with extra constraints
                Arguments.of("{ \"type\": \"string\", \"minLength\": 3, \"enum\": [\"abc\", \"def\"] }",
                        "#/properties/code", "string+constraints"));
    }

    /**
     * Cases where no TypeValidator should be created (null).
     */
    private static Stream<Arguments> absentTypeValidatorProvider() {
        return Stream.of(
                Arguments.of("{ \"minimum\": 5 }", "#/properties/age"),
                Arguments.of("{}", "#"),
                // empty type array – not a valid type definition
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
        String schema = "{\n" +
                "  \"$ref\": \"#/definitions/positiveInteger\",\n" +
                "  \"definitions\": {\n" +
                "    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 1 }\n" +
                "  }\n" +
                "}";
        return Stream.of(Arguments.of(schema, "#/properties/count"));
    }

    @Test
    @DisplayName("getTypeValidator() returns null before validators are initialised")
    void testGetTypeValidatorInitiallyNull() {
        // Pre‑condition – the schema has not been initialised yet.
        assertFalse(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be false before initialiseValidators()");
        // The getter should return null.
        assertNull(jsonSchema.getTypeValidator(),
                "getTypeValidator() should return null before initialiseValidators()");
    }

    @Test
    @DisplayName("getTypeValidator() returns a non‑null TypeValidator after initialiseValidators()")
    void testGetTypeValidatorAfterInitialize() {
        jsonSchema.initializeValidators();
        assertTrue(jsonSchema.hasTypeValidator(),
                "hasTypeValidator() should be true after initialiseValidators()");
        TypeValidator typeValidator = jsonSchema.getTypeValidator();
        assertNotNull(typeValidator,
                "getTypeValidator() should return a non‑null instance after initialiseValidators()");
    }

    @ParameterizedTest(name = "[{index}] typeValidator present – schemaPath={1}")
    @MethodSource("presentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns non‑null when a valid 'type' keyword is present")
    void testTypeValidatorPresent(String schemaJson, String schemaPath, String _description) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(),
                "hasTypeValidator() should be true when schema defines a type");
        assertNotNull(schema.getTypeValidator(),
                "getTypeValidator() must return a non‑null instance");
    }

    @ParameterizedTest(name = "[{index}] typeValidator absent – schemaPath={1}")
    @MethodSource("absentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns null when no valid 'type' keyword is present")
    void testTypeValidatorAbsent(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertFalse(schema.hasTypeValidator(),
                "hasTypeValidator() should be false when schema does not define a valid type");
        assertNull(schema.getTypeValidator(),
                "getTypeValidator() must be null");
    }

    @ParameterizedTest(name = "[{index}] explicit initialiseValidators()")
    @MethodSource("explicitInitProvider")
    @DisplayName("getTypeValidator() remains non‑null after explicit initialiseValidators()")
    void testExplicitInitialization(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(),
                "hasTypeValidator() should be true after initialiseValidators()");
        assertNotNull(schema.getTypeValidator(),
                "getTypeValidator() must be non‑null after initialiseValidators()");
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
        assertSame(first, second,
                "TypeValidator instance must be identical after repeated initialiseValidators()");
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
        assertAll("reference equality checks",
                () -> assertSame(first, second, "First and second calls should return the same instance"),
                () -> assertSame(second, third, "Second and third calls should return the same instance"));
    }

    @ParameterizedTest(name = "[{index}] $ref resolution")
    @MethodSource("refResolutionProvider")
    @DisplayName("$ref resolves to a schema containing a 'type' keyword")
    void testRefResolution(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // $ref resolution happens during initialiseValidators()
        schema.initializeValidators();
        assertTrue(schema.hasTypeValidator(),
                "hasTypeValidator() should be true after $ref resolution");
        assertNotNull(schema.getTypeValidator(),
                "getTypeValidator() must be non‑null after $ref resolution");
    }

    @Test
    @DisplayName("getTypeValidator() returns null before any initialisation")
    void testGetterBeforeInitialisation() throws Exception {
        JsonSchema schema = buildSchema("{ \"type\": \"string\" }", "#");
        // No initialiseValidators() invoked.
        assertFalse(schema.hasTypeValidator(),
                "hasTypeValidator() should be false before initialiseValidators()");
        assertNull(schema.getTypeValidator(),
                "getTypeValidator() should be null before initialiseValidators()");
    }
}
```