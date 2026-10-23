```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaseJsonValidator#getSchemaPath() tests")
class BaseJsonValidator_getSchemaPath_Test {

    private static final String SCHEMA_PATH = "/example/schema/path";

    private JsonNode mockSchemaNode;
    private JsonSchema mockParentSchema;
    private ValidatorTypeCode mockValidatorType;
    private ValidationContext mockValidationContext;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} required
     * for testing the non‑abstract behaviour of {@code getSchemaPath()}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation logic is irrelevant for the getSchemaPath() tests.
            return Collections.emptySet();
        }

        // The abstract method with the full signature that BaseJsonValidator inherits.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // No Mockito – we simply use nulls because the validator does not
        // interact with these objects in the tests.
        mockSchemaNode = null;
        mockParentSchema = null;
        mockValidatorType = null;
        mockValidationContext = null;
    }

    /**
     * Supplies the exhaustive set of inputs/expected outputs extracted from the
     * JSON test‑case description.
     */
    private static Stream<Arguments> schemaPathProvider() {
        return Stream.of(
            // null handling
            Arguments.of(null, null),
            // empty string
            Arguments.of("", ""),
            // typical JSON‑Pointer examples
            Arguments.of("/properties/name", "/properties/name"),
            Arguments.of("/items/0/properties/age", "/items/0/properties/age"),
            // escaped characters
            Arguments.of("/$defs/Person~1address", "/$defs/Person~1address"),
            Arguments.of("/properties/~0name", "/properties/~0name"),
            Arguments.of("/properties/~1name", "/properties/~1name"),
            Arguments.of("/properties/name~0~1", "/properties/name~0~1"),
            // whitespace and control characters
            Arguments.of(" ", " "),
            Arguments.of("\t\n", "\t\n"),
            // Unicode and emoji
            Arguments.of("/properties/naïve", "/properties/naïve"),
            Arguments.of("/properties/😀", "/properties/😀"),
            // backslash literal
            Arguments.of("/properties/\\name", "/properties/\\name"),
            // root pointer and simple cases
            Arguments.of("/", "/"),
            Arguments.of("/a", "/a"),
            // long and deep paths
            Arguments.of("/very/long/path/", "/very/long/path/"),
            Arguments.of("/properties/name/0/1/2/3/4/5/6/7/8/9",
                         "/properties/name/0/1/2/3/4/5/6/7/8/9"),
            // query‑string‑like and fragment identifiers
            Arguments.of("/properties/name?query=param", "/properties/name?query=param"),
            Arguments.of("/properties/name#fragment", "/properties/name#fragment")
        );
    }

    @Test
    void getSchemaPath_returnsPathProvidedInConstructor() {
        // GIVEN – a concrete validator instantiated with a known schema path
        TestValidator validator = new TestValidator(SCHEMA_PATH,
                mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN – the method under test is invoked
        String actualPath = validator.getSchemaPath();
        // THEN – the returned path matches the one supplied to the constructor
        assertEquals(SCHEMA_PATH, actualPath,
                "BaseJsonValidator.getSchemaPath() should return the schemaPath supplied at construction");
    }

    @Test
    void getSchemaPath_isImmutableAfterConstruction() {
        // GIVEN – a validator created with an initial path
        TestValidator validator = new TestValidator(SCHEMA_PATH,
                mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN – the method is called multiple times
        String firstCall = validator.getSchemaPath();
        String secondCall = validator.getSchemaPath();
        // THEN – each call returns the same value
        assertAll(
                () -> assertEquals(SCHEMA_PATH, firstCall,
                        "First call should return the original path"),
                () -> assertEquals(SCHEMA_PATH, secondCall,
                        "Second call should return the same original path")
        );
    }

    /**
     * Verifies that {@code BaseJsonValidator#getSchemaPath()} returns exactly
     * the value that was supplied to the constructor – even when that value
     * is {@code null}, empty, contains whitespace, control characters,
     * Unicode, escape sequences, query‑string fragments, very long strings,
     * or any other admissible content.
     *
     * @param schemaPath the value passed to the super‑constructor
     * @param expected   the value we expect {@code getSchemaPath()} to return
     */
    @ParameterizedTest(name = "[{index}] schemaPath=\"{0}\" → expected=\"{1}\"")
    @MethodSource("schemaPathProvider")
    void getSchemaPath_returnsSuppliedValue(String schemaPath, String expected) {
        // GIVEN – a concrete validator instantiated with the parameterised path
        TestValidator validator = new TestValidator(schemaPath,
                mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
        // WHEN – the method under test is invoked
        String actual = validator.getSchemaPath();
        // THEN – the returned value matches exactly what was supplied
        assertEquals(expected, actual,
                () -> "Expected getSchemaPath() to return [" + expected + "] for input [" + schemaPath + "]");
    }
}
```