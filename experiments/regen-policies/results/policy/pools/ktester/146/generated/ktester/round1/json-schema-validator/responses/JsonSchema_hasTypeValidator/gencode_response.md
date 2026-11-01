```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#hasTypeValidator()}.
 *
 * The tests create {@link JsonSchema} instances via {@link JsonSchemaFactory},
 * which internally builds the required {@link ValidationContext},
 * {@link SchemaLocation}, etc. This avoids the need to call private
 * constructors directly and respects the public API of the library.
 *
 * The test suite covers:
 * <ul>
 *   <li>schemas that define a top‑level {@code type} (single, array, null, invalid)</li>
 *   <li>schemas that rely on $ref resolution</li>
 *   <li>schemas that use combinators such as {@code anyOf}</li>
 *   <li>behaviour after explicit validator initialisation</li>
 *   <li>error scenarios (invalid type value, unresolvable ref, circular ref, etc.)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_hasTypeValidator_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version; the concrete version does not affect the
        // behaviour of hasTypeValidator().
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /* --------------------------------------------------------------------- */
    /*  Simple positive / negative cases (already covered by the original   */
    /*  test class, but kept here for completeness)                        */
    /* --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_whenSchemaDefinesType_returnsTrue() {
        // GIVEN a schema that contains a "type" keyword
        String schemaJson = "{ \"type\": \"string\" }";

        // WHEN the schema is parsed
        JsonSchema schema = factory.getSchema(schemaJson);

        // THEN hasTypeValidator() reports true
        assertTrue(schema.hasTypeValidator(),
                "Expected hasTypeValidator() to be true when a type validator is present");
    }

    @Test
    void hasTypeValidator_whenSchemaLacksType_returnsFalse() {
        // GIVEN a schema without a "type" keyword
        String schemaJson = "{ \"properties\": { \"name\": { \"type\": \"string\" } } }";

        // WHEN the schema is parsed
        JsonSchema schema = factory.getSchema(schemaJson);

        // THEN hasTypeValidator() reports false because the root schema has no type validator
        assertFalse(schema.hasTypeValidator(),
                "Expected hasTypeValidator() to be false when no type validator is present at the root");
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterised tests for the remaining groups                        */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("provideSchemasForHasTypeValidator")
    void hasTypeValidator_variousScenarios(String schemaJson, Boolean expectedResult, String description) {
        JsonSchema schema = factory.getSchema(schemaJson);
        assertEquals(expectedResult, schema.hasTypeValidator(),
                () -> "Unexpected result for case: " + description);
    }

    private static Stream<Arguments> provideSchemasForHasTypeValidator() {
        return Stream.of(
                // object type – should be true
                Arguments.of("{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }",
                        true,
                        "Top‑level object type with properties"),
                // array of types – should be true
                Arguments.of("{ \"type\": [\"string\", \"null\"] }",
                        true,
                        "Top‑level type as an array of allowed types"),
                // anyOf without top‑level type – should be false
                Arguments.of("{ \"anyOf\": [ { \"type\": \"string\" }, { \"type\": \"number\" } ] }",
                        false,
                        "anyOf with sub‑schemas that have types but no root type"),
                // $ref to a schema that defines a type – should be true
                Arguments.of("{ \"$ref\": \"#/definitions/target\", \"definitions\": { \"target\": { \"type\": \"array\", \"items\": { \"type\": \"integer\" } } } }",
                        true,
                        "$ref resolves to a schema with a type"),
                // $ref to a schema without a top‑level type – should be false
                Arguments.of("{ \"$ref\": \"#/definitions/target\", \"definitions\": { \"target\": { \"properties\": { \"id\": { \"type\": \"string\" } } } } }",
                        false,
                        "$ref resolves to a schema lacking a top‑level type"),
                // enum only – should be false
                Arguments.of("{ \"enum\": [1, 2, 3] }",
                        false,
                        "Schema defines only an enum, no type"),
                // type explicitly set to null – should be false
                Arguments.of("{ \"type\": null }",
                        false,
                        "Explicit null value for type keyword")
        );
    }

    @ParameterizedTest(name = "{index} => afterInitializeValidators: {2}")
    @MethodSource("provideSchemasForInitializeValidators")
    void hasTypeValidator_afterInitializeValidators(String schemaJson, Boolean expectedResult, String description) {
        JsonSchema schema = factory.getSchema(schemaJson);
        // Explicitly initialise validators – this should not change the outcome of hasTypeValidator()
        schema.initializeValidators();
        assertEquals(expectedResult, schema.hasTypeValidator(),
                () -> "After initializeValidators(): " + description);
    }

    private static Stream<Arguments> provideSchemasForInitializeValidators() {
        return Stream.of(
                // No top‑level type, initialise validators – still false
                Arguments.of("{ \"properties\": { \"age\": { \"type\": \"integer\" } } }",
                        false,
                        "Root schema without type, but with property schemas"),
                // Top‑level type present – still true after initialise
                Arguments.of("{ \"type\": \"boolean\", \"properties\": { \"flag\": { \"type\": \"boolean\" } } }",
                        true,
                        "Root schema with boolean type")
        );
    }

    /* --------------------------------------------------------------------- */
    /*  Exception‑driven scenarios                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void hasTypeValidator_invalidTypeValue_throwsIllegalArgumentException() {
        String schemaJson = "{ \"type\": 123 }";
        JsonSchema schema = factory.getSchema(schemaJson);
        // The IllegalArgumentException is expected when the type keyword is not a valid value.
        assertThrows(IllegalArgumentException.class, schema::hasTypeValidator,
                "Expected IllegalArgumentException for non‑string/non‑array type value");
    }

    @Test
    void hasTypeValidator_unresolvableRef_throwsJsonSchemaException() {
        String schemaJson = "{ \"$ref\": \"#/definitions/missing\" }";
        // The factory itself will throw JsonSchemaException during parsing because the reference cannot be resolved.
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schemaJson),
                "Expected JsonSchemaException for an unresolvable $ref");
    }

    @Test
    void hasTypeValidator_circularRef_throwsStackOverflowError() {
        String schemaJson = "{ \"$ref\": \"#/definitions/self\", \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } } }";
        // The parser attempts to resolve the circular reference; we expect a StackOverflowError (or a custom detection exception).
        assertThrows(StackOverflowError.class,
                () -> {
                    JsonSchema schema = factory.getSchema(schemaJson);
                    schema.hasTypeValidator(); // trigger resolution
                },
                "Expected StackOverflowError for a direct circular $ref");
    }

    @Test
    void hasTypeValidator_malformedSchemaNode_throwsNullPointerException() {
        // Passing the literal string "null" results in a JSON literal null node.
        String schemaJson = "null";
        JsonSchema schema = factory.getSchema(schemaJson);
        // The internal getTypeValidator() will attempt to dereference a null node.
        assertThrows(NullPointerException.class, schema::hasTypeValidator,
                "Expected NullPointerException when the root schema node is null");
    }

    @Test
    void hasTypeValidator_typeArrayWithInvalidElement_throwsIllegalArgumentException() {
        String schemaJson = "{ \"type\": [\"string\", 5, \"null\"] }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(IllegalArgumentException.class, schema::hasTypeValidator,
                "Expected IllegalArgumentException when type array contains a non‑string element");
    }

    @Test
    void hasTypeValidator_unsupportedKeywordCombination_throwsUnsupportedOperationException() {
        String schemaJson = "{ \"type\": \"object\", \"anyOf\": [ { \"type\": \"string\" } ] }";
        JsonSchema schema = factory.getSchema(schemaJson);
        // Depending on the implementation, this combination may be unsupported.
        assertThrows(UnsupportedOperationException.class, schema::hasTypeValidator,
                "Expected UnsupportedOperationException for disallowed type + anyOf combination");
    }

    @Test
    void hasTypeValidator_neverThrowsException() {
        // This test ensures that for a well‑formed schema the method never throws.
        String schemaJson = "{ \"type\": \"string\" }";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertDoesNotThrow(schema::hasTypeValidator,
                "hasTypeValidator() should never throw for a valid schema");
    }
}
```