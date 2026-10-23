```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for {@link JsonSchema#getTypeValidator()}.
 *
 * <p>The test suite covers all scenarios described in the supplied JSON test
 * cases, including:</p>
 * <ul>
 *   <li>Schemas with and without a {@code type} keyword.</li>
 *   <li>Primitive, composite and empty {@code type} definitions.</li>
 *   <li>Behaviour after explicit {@link JsonSchema#initializeValidators()} calls,
 *       repeated initialisation and consistency of the returned instance.</li>
 *   <li>Resolution of {@code $ref} references.</li>
 * </ul>
 *
 * <p>All external collaborators are mocked, allowing the focus to stay on the
 * {@code getTypeValidator()} method itself.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getTypeValidator_Test {

    /** Shared mapper for building {@link JsonNode} objects from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Mocked {@link ValidationContext} used for every {@link JsonSchema} instance. */
    @Mock
    private ValidationContext validationContext;

    /** Base URI used for {@link JsonSchema#from(...)} calls. */
    private static final URI BASE_URI = URI.create("http://example.com/schema");

    /** Helper that builds a {@link JsonSchema} from the given JSON string and schema path. */
    private JsonSchema buildSchema(String schemaJson, String schemaPath) throws Exception {
        // The factory method requires a JsonNode representation of the schema.
        JsonNode schemaNode = MAPPER.readTree(schemaJson);

        // Minimal stubbing – the real implementation only needs non‑null return
        // values for a few configuration getters.
        when(validationContext.getMetaSchema()).thenReturn(null);
        when(validationContext.getConfig()).thenReturn(null);
        when(validationContext.getUriFactory()).thenReturn(null);
        when(validationContext.getCurrentUri()).thenReturn(BASE_URI);

        return JsonSchema.from(validationContext,
                schemaPath,
                BASE_URI,
                schemaNode,
                null,          // no parent
                false);        // do not suppress sub‑schema retrieval
    }

    // ------------------------------------------------------------------------
    //  Parameterised test sources
    // ------------------------------------------------------------------------

    /** Cases where a TypeValidator must be present (non‑null). */
    private static Stream<Arguments> presentTypeValidatorProvider() {
        return Stream.of(
                // simple primitive types
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
                        "#/properties/code", "string+constraints")
        );
    }

    /** Cases where no TypeValidator should be created (null). */
    private static Stream<Arguments> absentTypeValidatorProvider() {
        return Stream.of(
                Arguments.of("{ \"minimum\": 5 }", "#/properties/age"),
                Arguments.of("{}", "#"),
                // empty type array – not a valid type definition
                Arguments.of("{ \"type\": [] }", "#/properties/invalid")
        );
    }

    /** Cases that require an explicit {@code initializeValidators()} call. */
    private static Stream<Arguments> explicitInitProvider() {
        return Stream.of(
                Arguments.of("{ \"type\": \"integer\" }", "#")
        );
    }

    /** Cases for repeated initialisation – the same instance must survive. */
    private static Stream<Arguments> repeatedInitProvider() {
        return Stream.of(
                Arguments.of("{ \"type\": \"string\" }", "#/properties/name")
        );
    }

    /** Cases for checking reference equality across multiple getter calls. */
    private static Stream<Arguments> sameInstanceProvider() {
        return Stream.of(
                Arguments.of("{ \"type\": \"integer\" }", "#/properties/id")
        );
    }

    /** Case for $ref resolution. */
    private static Stream<Arguments> refResolutionProvider() {
        String schema = "{\n" +
                "  \"$ref\": \"#/definitions/positiveInteger\",\n" +
                "  \"definitions\": {\n" +
                "    \"positiveInteger\": { \"type\": \"integer\", \"minimum\": 1 }\n" +
                "  }\n" +
                "}";
        return Stream.of(Arguments.of(schema, "#/properties/count"));
    }

    // ------------------------------------------------------------------------
    //  Test implementations
    // ------------------------------------------------------------------------

    @ParameterizedTest(name = "[{index}] typeValidator present – schemaPath={1}")
    @MethodSource("presentTypeValidatorProvider")
    @DisplayName("getTypeValidator() returns non‑null when a valid 'type' keyword is present")
    void testTypeValidatorPresent(String schemaJson, String schemaPath, String _description) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // For most schemas the TypeValidator is created lazily during initialiseValidators().
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
                "getTypeValidator() must return null");
    }

    @ParameterizedTest(name = "[{index}] explicit initialiseValidators()")
    @MethodSource("explicitInitProvider")
    @DisplayName("getTypeValidator() remains non‑null after explicit initialiseValidators()")
    void testExplicitInitialization(String schemaJson, String schemaPath) throws Exception {
        JsonSchema schema = buildSchema(schemaJson, schemaPath);
        // First explicit initialisation
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

        assertAll("reference equality checks",
                () -> assertSame(first, second, "First and second calls should return the same instance"),
                () -> assertSame(second, third, "Second and third calls should return the same instance")
        );
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

    // ------------------------------------------------------------------------
    //  Additional explicit tests for edge‑cases not covered by parameterised data
    // ------------------------------------------------------------------------

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