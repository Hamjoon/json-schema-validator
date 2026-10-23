```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getSchemaPath()}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * implementation {@code TestValidator} is defined inside the test class.  The
 * implementation only needs to provide the {@code validate} method because the
 * {@code getSchemaPath()} logic does not depend on any validation behaviour.</p>
 *
 * <p>All collaborators required by the constructor are mocked with Mockito.
 * The parameterised test {@code getSchemaPath_returnsSuppliedValue} exercises
 * the exhaustive list of scenarios described in the JSON test‑case document.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaPath_Test {

    /* --------------------------------------------------------------------- */
    /* Test fixtures                                                       */
    /* --------------------------------------------------------------------- */

    private JsonNode mockSchemaNode;
    private JsonSchema mockParentSchema;
    private ValidatorTypeCode mockValidatorType;
    private ValidationContext mockValidationContext;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh mocks for every test – prevents cross‑test leakage.
        mockSchemaNode = mock(JsonNode.class);
        mockParentSchema = mock(JsonSchema.class);
        mockValidatorType = mock(ValidatorTypeCode.class);
        mockValidationContext = mock(ValidationContext.class);
    }

    /* --------------------------------------------------------------------- */
    /* Parameterised test – all cases from the supplied JSON               */
    /* --------------------------------------------------------------------- */

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
        TestValidator validator = new TestValidator(
                schemaPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext
        );

        // WHEN – the method under test is invoked
        String actual = validator.getSchemaPath();

        // THEN – the returned value matches exactly what was supplied
        assertEquals(expected, actual,
                () -> "Expected getSchemaPath() to return [" + expected + "] for input [" + schemaPath + "]");
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

    /* --------------------------------------------------------------------- */
    /* Helper concrete class                                               */
    /* --------------------------------------------------------------------- */

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
    }
}
```