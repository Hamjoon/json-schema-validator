```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the protected {@code atPath(String, String)} method of {@link BaseJsonValidator}.
 *
 * <p>A minimal concrete subclass {@link TestValidator} is used so that the protected method can be
 * exercised. All collaborators are mocked – no private members of {@code BaseJsonValidator} are accessed
 * directly.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atPath_Test {

    // --------------------------------------------------------------------- //
    // Mocked collaborators
    // --------------------------------------------------------------------- //
    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidatorTypeCode validatorType;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    // --------------------------------------------------------------------- //
    // Test fixture (default validator – JSON_POINTER path type)
    // --------------------------------------------------------------------- //
    private TestValidator defaultValidator;

    @BeforeEach
    void setUp() {
        // Basic stubbing required for the BaseJsonValidator constructor.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        // Default PathType for most tests is JSON_POINTER.
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(config.getApplyDefaultsStrategy())
                .thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);

        defaultValidator = new TestValidator(
                "",                                   // schemaPath – irrelevant for atPath
                JsonNodeFactory.instance.objectNode(),// empty schema node
                parentSchema,
                validatorType,
                validationContext);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for PathType.SLASH
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slashPathProvider")
    void atPath_Appends_WithSlashPathType(String description,
                                          String currentPath,
                                          String token,
                                          String expected) {
        // Arrange – create a validator that uses PathType.SLASH
        when(config.getPathType()).thenReturn(PathType.SLASH);
        TestValidator validator = new TestValidator(
                "", JsonNodeFactory.instance.objectNode(),
                parentSchema, validatorType, validationContext);

        // Act
        String result = validator.atPath(currentPath, token);

        // Assert
        assertEquals(expected, result, description);
    }

    private static Stream<Arguments> slashPathProvider() {
        return Stream.of(
                Arguments.of(
                        "Appending normal token to a non‑empty path",
                        "/users/123", "address", "/users/123/address"),
                Arguments.of(
                        "Current path empty – token becomes first segment",
                        "", "root", "/root"),
                Arguments.of(
                        "Empty token – should result in a trailing '/'",
                        "/items", "", "/items/")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for PathType.JSON_POINTER
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("jsonPointerPathProvider")
    void atPath_Appends_WithJsonPointerPathType(String description,
                                                String currentPath,
                                                String token,
                                                String expected) {
        // validator already uses JSON_POINTER due to default setup
        String result = defaultValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    private static Stream<Arguments> jsonPointerPathProvider() {
        return Stream.of(
                Arguments.of(
                        "Normal token appended with '/' separator",
                        "/data", "items", "/data/items"),
                Arguments.of(
                        "Empty current path – token becomes first segment",
                        "", "root", "/root"),
                Arguments.of(
                        "Empty token – trailing '/' indicates empty segment",
                        "/array", "", "/array/"),
                Arguments.of(
                        "Token containing '~' and '/' are escaped per JSON Pointer rules",
                        "/a~b", "c/d", "/a~0b/c~1d"),
                Arguments.of(
                        "Null current path is treated as empty",
                        null, "first", "/first")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Null handling / exceptional cases
     * --------------------------------------------------------------------- */

    @Test
    void atPath_NullPathType_ThrowsNullPointerException() {
        // Arrange – configure the config to return null for PathType
        when(config.getPathType()).thenReturn(null);
        TestValidator validator = new TestValidator(
                "", JsonNodeFactory.instance.objectNode(),
                parentSchema, validatorType, validationContext);

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> validator.atPath("/test", "field"),
                "When internal pathType is null a NullPointerException is expected");
    }

    @Test
    void atPath_NullToken_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> defaultValidator.atPath("/users", null),
                "A null token should cause NullPointerException from PathType.append");
    }

    @Test
    void atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> defaultValidator.atPath(null, null),
                "Both parameters null must propagate NullPointerException");
    }

    @Test
    void atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException() {
        // Token contains a null character – illegal in JSON Pointer
        String illegalToken = "invalid\u0000char";
        assertThrows(IllegalArgumentException.class,
                () -> defaultValidator.atPath("/data", illegalToken),
                "Illegal characters in token should cause IllegalArgumentException");
    }

    // --------------------------------------------------------------------- //
    // Helper concrete subclass – only implements the abstract validate() method.
    // --------------------------------------------------------------------- //

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used solely for exercising the
     * protected {@code atPath} method.
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
            // No validation logic required for atPath tests.
            return Collections.emptySet();
        }
    }
}
```