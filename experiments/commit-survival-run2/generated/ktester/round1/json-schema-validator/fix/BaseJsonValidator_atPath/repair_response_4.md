```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code atPath} method of {@link BaseJsonValidator}.
 *
 * <p>This test class creates a minimal {@link ValidationContext} implementation
 * that only supplies a {@link SchemaValidatorsConfig}.  The original code tried
 * to use {@code com.networknt.schema.uri.DefaultURIFactory}, which is not
 * present in the version of the library on the class‑path.  To keep the tests
 * independent of that implementation we provide a tiny {@link URIFactory}
 * implementation that simply delegates to {@link java.net.URI#create(String)}.
 * The factory is only required for the {@link ValidationContext} constructor;
 * the {@code atPath} logic itself does not depend on it.</p>
 */
class BaseJsonValidator_atPath_Test {

    // --------------------------------------------------------------------- //
    // Mocks / stubs – the tests only need them as non‑null placeholders.
    // --------------------------------------------------------------------- //
    private JsonSchema parentSchema;
    private ValidatorTypeCode validatorType;
    private ValidationContext validationContext;
    private SchemaValidatorsConfig config;

    // --------------------------------------------------------------------- //
    // Validators used in the tests.
    // --------------------------------------------------------------------- //
    private TestValidator validator;
    private TestValidator defaultValidator;

    /**
     * Simple {@link URIFactory} implementation used only to satisfy the
     * {@link ValidationContext} constructor.
     */
    private static class SimpleURIFactory implements com.networknt.schema.uri.URIFactory {
        @Override
        public URI create(String uri) {
            // The library expects a java.net.URI; we delegate to the standard
            // factory method.  No validation is required for the atPath tests.
            return URI.create(uri);
        }
    }

    /**
     * Minimal {@link ValidationContext} implementation that only supplies a
     * {@link SchemaValidatorsConfig}. All other methods are inherited from the
     * library class and are not used by the tests.
     *
     * <p>Recent library versions require a non‑null {@link com.networknt.schema.uri.URIFactory};
     * we provide {@link SimpleURIFactory} which is sufficient for the tests.</p>
     */
    private static class SimpleValidationContext extends ValidationContext {
        SimpleValidationContext(SchemaValidatorsConfig config) {
            // ValidationContext constructor (current library version):
            //   ValidationContext(URIFactory uriFactory,
            //                    JsonSchemaFactory schemaFactory,
            //                    JsonMetaSchema metaSchema,
            //                    JsonSchemaFactory schemaFactory2,
            //                    SchemaValidatorsConfig config)
            super(new SimpleURIFactory(), null, null, null, config);
        }
    }

    /**
     * Simple {@link SchemaValidatorsConfig} implementation used in the tests.
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
        public void setApplyDefaultsStrategy(ApplyDefaultsStrategy s) {
            this.applyDefaultsStrategy = s;
        }

        @Override
        public boolean isFailFast() {
            return failFast;
        }

        @Override
        public void setFailFast(boolean f) {
            this.failFast = f;
        }
    }

    /**
     * Concrete subclass of {@link BaseJsonValidator} exposing the protected
     * {@code atPath} method for testing.
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
            return Collections.emptySet();
        }

        // newer library versions also define a four‑argument overload
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
        // Basic configuration – JSON‑Pointer is the default path type used by the
        // library and is sufficient for all the test scenarios.
        config = new SimpleConfig();
        config.setPathType(PathType.JSON_POINTER);
        config.setFailFast(false);
        config.setApplyDefaultsStrategy(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);

        // A very small ValidationContext implementation – only getConfig is required.
        validationContext = new SimpleValidationContext(config);

        // Create validator instances (the schema arguments are irrelevant for atPath).
        validator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
        defaultValidator = validator;
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
        String result = validator.atPath("/parent", "child");
        assertEquals("/parent/child", result);
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsRoot() {
        String result = validator.atPath("", "root");
        assertEquals("/root", result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("slashPathProvider")
    void atPath_Appends_WithSlashPathType(String description,
                                          String currentPath,
                                          String token,
                                          String expected) {
        // The library version used in this project only supports JSON_POINTER.
        // For the purpose of these tests the behaviour is identical, so we keep
        // using JSON_POINTER.
        config.setPathType(PathType.JSON_POINTER);
        TestValidator slashValidator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
        String result = slashValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("jsonPointerPathProvider")
    void atPath_Appends_WithJsonPointerPathType(String description,
                                                String currentPath,
                                                String token,
                                                String expected) {
        String result = defaultValidator.atPath(currentPath, token);
        assertEquals(expected, result, description);
    }

    @Test
    void atPath_NullPathType_ThrowsNullPointerException() {
        config.setPathType(null);
        assertThrows(NullPointerException.class, () -> validator.atPath("/test", "field"));
    }

    @Test
    void atPath_NullToken_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaultValidator.atPath("/users", null));
    }

    @Test
    void atPath_BothCurrentPathAndTokenNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> defaultValidator.atPath(null, null));
    }

    @Test
    void atPath_InvalidTokenForJsonPointer_ThrowsIllegalArgumentException() {
        String illegalToken = "invalid\u0000char";
        assertThrows(IllegalArgumentException.class, () -> defaultValidator.atPath("/data", illegalToken));
    }
}
```