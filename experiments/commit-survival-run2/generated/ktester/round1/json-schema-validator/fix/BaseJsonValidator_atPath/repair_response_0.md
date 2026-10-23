```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the protected {@code atPath} method of {@link BaseJsonValidator}.
 *
 * <p>This test class does not rely on Mockito; instead it uses simple concrete
 * implementations / stubs for the required collaborators.</p>
 */
class BaseJsonValidator_atPath_Test {

    /** Simple {@link SchemaValidatorsConfig} implementation that allows us to
     *  control the values returned by {@link ValidationContext#getConfig()}.
     */
    private static class SimpleConfig extends SchemaValidatorsConfig {
        private PathType pathType;
        private ApplyDefaultsStrategy applyDefaultsStrategy;
        private boolean failFast;

        @Override
        public PathType getPathType() {
            return pathType;
        }

        @Override
        public void setPathType(PathType pathType) {
            this.pathType = pathType;
        }

        @Override
        public ApplyDefaultsStrategy getApplyDefaultsStrategy() {
            return applyDefaultsStrategy;
        }

        @Override
        public void setApplyDefaultsStrategy(ApplyDefaultsStrategy strategy) {
            this.applyDefaultsStrategy = strategy;
        }

        @Override
        public boolean isFailFast() {
            return failFast;
        }

        @Override
        public void setFailFast(boolean failFast) {
            this.failFast = failFast;
        }
    }

    /** Minimal {@link ValidationContext} implementation that only supplies a
     *  {@link SchemaValidatorsConfig}. All other methods throw
     *  {@link UnsupportedOperationException} because they are not needed for
     *  the {@code atPath} tests.
     */
    private static class SimpleValidationContext implements ValidationContext {
        private final SchemaValidatorsConfig config;

        SimpleValidationContext(SchemaValidatorsConfig config) {
            this.config = config;
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return config;
        }

        // -----------------------------------------------------------------
        // The remaining methods of the interface are not required for the
        // tests; they are implemented to satisfy the compiler only.
        // -----------------------------------------------------------------
        @Override public JsonSchema getRootSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonSchema getParentSchema() { throw new UnsupportedOperationException(); }
        @Override public JsonNode getRootNode() { throw new UnsupportedOperationException(); }
        @Override public JsonNode getParentNode() { throw new UnsupportedOperationException(); }
        @Override public String getCurrentPath() { throw new UnsupportedOperationException(); }
        @Override public String getCurrentPath(JsonNode node) { throw new UnsupportedOperationException(); }
        @Override public String getCurrentPath(String field) { throw new UnsupportedOperationException(); }
        @Override public boolean isFailFast() { return config.isFailFast(); }
        @Override public boolean isOpenAPI() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV4() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV6() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV7() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraft2019_09() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraft2020_12() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraft2019_09OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV7OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV6OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV4OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV4OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV6OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraftV7OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraft2019_09OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaDraft2020_12OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrOpenAPI3_1OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrOpenAPI3_1OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraftV4OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraftV4OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrJsonSchemaDraftV4OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlierOrJsonSchemaDraftV4OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraftV6OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraftV6OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrJsonSchemaDraftV6OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlierOrJsonSchemaDraftV6OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraftV7OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraftV7OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrJsonSchemaDraftV7OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlierOrJsonSchemaDraftV7OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraft2019_09OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraft2019_09OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrJsonSchemaDraft2019_09OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlierOrJsonSchemaDraft2019_09OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrJsonSchemaDraft2020_12OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlierOrJsonSchemaDraft2020_12OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraftV4OrLaterOrJsonSchemaDraftV6OrLaterOrJsonSchemaDraftV7OrLaterOrJsonSchemaDraft2019_09OrLaterOrJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraftV4OrLaterOrJsonSchemaDraftV6OrLaterOrJsonSchemaDraftV7OrLaterOrJsonSchemaDraft2019_09OrLaterOrJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrEarlierOrJsonSchemaDraftV4OrEarlierOrJsonSchemaDraftV6OrEarlierOrJsonSchemaDraftV7OrEarlierOrJsonSchemaDraft2019_09OrEarlierOrJsonSchemaDraft2020_12OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrEarlierOrJsonSchemaDraftV4OrEarlierOrJsonSchemaDraftV6OrEarlierOrJsonSchemaDraftV7OrEarlierOrJsonSchemaDraft2019_09OrEarlierOrJsonSchemaDraft2020_12OrEarlier() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPIVersionSupported() { throw new UnsupportedOperationException(); }
        @Override public boolean isJsonSchemaVersionSupported() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_0OrLaterOrJsonSchemaDraftV4OrLaterOrJsonSchemaDraftV6OrLaterOrJsonSchemaDraftV7OrLaterOrJsonSchemaDraft2019_09OrLaterOrJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
        @Override public boolean isOpenAPI3_1OrLaterOrJsonSchemaDraftV4OrLaterOrJsonSchemaDraftV6OrLaterOrJsonSchemaDraftV7OrLaterOrJsonSchemaDraft2019_09OrLaterOrJsonSchemaDraft2020_12OrLater() { throw new UnsupportedOperationException(); }
    }

    /** Concrete subclass of {@link BaseJsonValidator} that provides empty validation logic.
     *  It is only used to expose the protected {@code atPath} method for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode,
                      JsonSchema parentSchema, ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        // The JsonValidator interface (implemented by BaseJsonValidator) also defines a
        // four‑argument overload in recent versions of the library.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    private SimpleConfig config;
    private ValidationContext validationContext;
    private TestValidator validator;          // uses default JSON_POINTER path type
    private TestValidator defaultValidator;   // another instance for reuse

    @BeforeEach
    void setUp() {
        // Configure a default config (JSON_POINTER, non‑fail‑fast, empty defaults strategy)
        config = new SimpleConfig();
        config.setPathType(PathType.JSON_POINTER);
        config.setFailFast(false);
        config.setApplyDefaultsStrategy(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);

        validationContext = new SimpleValidationContext(config);

        // Create validator instances
        validator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
        defaultValidator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
    }

    private static Stream<Arguments> slashPathProvider() {
        return Stream.of(
                Arguments.of("Appending normal token to a non‑empty path", "/users/123", "address", "/users/123/address"),
                Arguments.of("Current path empty – token becomes first segment", "", "root", "/root"),
                Arguments.of("Empty token – should result in a trailing '/'", "/items", "", "/items/")
        );
    }

    private static Stream<Arguments> jsonPointerPathProvider() {
        return Stream.of(
                Arguments.of("Normal token appended with '/' separator", "/data", "items", "/data/items"),
                Arguments.of("Empty current path – token becomes first segment", "", "root", "/root"),
                Arguments.of("Empty token – trailing '/' indicates empty segment", "/array", "", "/array/"),
                Arguments.of("Token containing '~' and '/' are escaped per JSON Pointer rules", "/a~b", "c/d", "/a~0b/c~1d"),
                Arguments.of("Null current path is treated as empty", null, "first", "/first")
        );
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsNonEmpty() {
        String currentPath = "/parent";
        String token = "child";
        String result = validator.atPath(currentPath, token);
        assertEquals("/parent/child", result);
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsRoot() {
        String currentPath = "";
        String token = "root";
        String result = validator.atPath(currentPath, token);
        assertEquals("/root", result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slashPathProvider")
    void atPath_Appends_WithSlashPathType(String description, String currentPath,
                                          String token, String expected) {
        // Switch the configuration to SLASH path type for this test
        config.setPathType(PathType.SLASH);
        TestValidator slashValidator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
        String result = slashValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("jsonPointerPathProvider")
    void atPath_Appends_WithJsonPointerPathType(String description, String currentPath,
                                                String token, String expected) {
        // defaultValidator already uses JSON_POINTER
        String result = defaultValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @Test
    void atPath_NullPathType_ThrowsNullPointerException() {
        config.setPathType(null);
        TestValidator nullPathValidator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
        assertThrows(NullPointerException.class,
                () -> nullPathValidator.atPath("/test", "field"));
    }

    @Test
    void atPath_NullToken_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> defaultValidator.atPath("/users", null));
    }

    @Test
    void atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> defaultValidator.atPath(null, null));
    }

    @Test
    void atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException() {
        String illegalToken = "invalid\u0000char";
        assertThrows(IllegalArgumentException.class,
                () -> defaultValidator.atPath("/data", illegalToken));
    }
}
```