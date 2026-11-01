package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Set;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atPath_Test {

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
    private TestValidator validator;

    // --------------------------------------------------------------------- //
    private TestValidator defaultValidator;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used solely for exercising the
     * protected {@code atPath} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic required for atPath tests.
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // constructed without hitting a NullPointerException.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        // Use the JSON‑Pointer path type – this is the default in the library.
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        // Apply‑defaults strategy is required by the constructor.
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        // Create a concrete instance BaseJsonValidator_atPath_Testthe abstract BaseJsonValidator.
        validator = new TestValidator(// schemaPath (irrelevant for atPath)
        "", // empty schema node
        JsonNodeFactory.instance.objectNode(), parentSchema, validatorType, validationContext);
        // Basic stubbing required for the BaseJsonValidator constructor.
        when(validationContext.getConfig()).thenReturn(config);
        // Initialise Mockito annotations (handled automatically by @ExtendWith)
        when(config.isFailFast()).thenReturn(false);
        // Configure the ValidationContext hierarchy so that BaseJsonValidator can be
        // Default PathType for most tests is JSON_POINTER.
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(config.getApplyDefaultsStrategy()).thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);
        defaultValidator = new TestValidator(// schemaPath – irrelevant for atPath
        "", // empty schema node
        JsonNodeFactory.instance.objectNode(), parentSchema, validatorType, validationContext);
    }

    private static Stream<Arguments> slashPathProvider() {
        return Stream.of(Arguments.of("Appending normal token to a non‑empty path", "/users/123", "address", "/users/123/address"), Arguments.of("Current path empty – token becomes first segment", "", "root", "/root"), Arguments.of("Empty token – should result in a trailing '/'", "/items", "", "/items/"));
    }

    private static Stream<Arguments> jsonPointerPathProvider() {
        return Stream.of(Arguments.of("Normal token appended with '/' separator", "/data", "items", "/data/items"), Arguments.of("Empty current path – token becomes first segment", "", "root", "/root"), Arguments.of("Empty token – trailing '/' indicates empty segment", "/array", "", "/array/"), Arguments.of("Token containing '~' and '/' are escaped per JSON Pointer rules", "/a~b", "c/d", "/a~0b/c~1d"), Arguments.of("Null current path is treated as empty", null, "first", "/first"));
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsNonEmpty() {
        // GIVEN
        String currentPath = "/parent";
        String token = "child";
        // WHEN
        String result = validator.atPath(currentPath, token);
        // THEN
        assertEquals("/parent/child", result, "atPath should concatenate the token to the current JSON‑Pointer path using '/'");
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsRoot() {
        // GIVEN
        String currentPath = "";
        String token = "root";
        // WHEN
        String result = validator.atPath(currentPath, token);
        // THEN
        assertEquals("/root", result, "When the current path is empty, atPath should return a leading '/' followed by the token");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slashPathProvider")
    void atPath_Appends_WithSlashPathType(String description, String currentPath, String token, String expected) {
        // Arrange – create a validator that uses PathType.SLASH
        when(config.getPathType()).thenReturn(PathType.SLASH);
        TestValidator validator = new TestValidator("", JsonNodeFactory.instance.objectNode(), parentSchema, validatorType, validationContext);
        // Act
        String result = validator.atPath(currentPath, token);
        // Assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("jsonPointerPathProvider")
    void atPath_Appends_WithJsonPointerPathType(String description, String currentPath, String token, String expected) {
        // validator already uses JSON_POINTER due to default setup
        String result = defaultValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @Test
    void atPath_NullPathType_ThrowsNullPointerException() {
        // Arrange – configure the config to return null for PathType
        when(config.getPathType()).thenReturn(null);
        TestValidator validator = new TestValidator("", JsonNodeFactory.instance.objectNode(), parentSchema, validatorType, validationContext);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> validator.atPath("/test", "field"), "When internal pathType is null a NullPointerException is expected");
    }

    @Test
    void atPath_NullToken_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> defaultValidator.atPath("/users", null), "A null token should cause NullPointerException from PathType.append");
    }

    @Test
    void atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> defaultValidator.atPath(null, null), "Both parameters null must propagate NullPointerException");
    }

    @Test
    void atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException() {
        // Token contains a null character – illegal in JSON Pointer
        String illegalToken = "invalid\u0000char";
        assertThrows(IllegalArgumentException.class, () -> defaultValidator.atPath("/data", illegalToken), "Illegal characters in token should cause IllegalArgumentException");
    }
}
