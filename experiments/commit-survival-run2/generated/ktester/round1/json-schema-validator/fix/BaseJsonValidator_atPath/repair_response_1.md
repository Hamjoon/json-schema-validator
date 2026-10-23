```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
 * Tests for the protected {@code atPath} method of {@link BaseJsonValidator}.
 */
class BaseJsonValidator_atPath_Test {

    /** Simple {@link SchemaValidatorsConfig} implementation used in the tests. */
    private static class SimpleConfig extends SchemaValidatorsConfig {
        private PathType pathType;
        private ApplyDefaultsStrategy applyDefaultsStrategy;
        private boolean failFast;

        @Override public PathType getPathType() { return pathType; }
        @Override public void setPathType(PathType pathType) { this.pathType = pathType; }

        @Override public ApplyDefaultsStrategy getApplyDefaultsStrategy() { return applyDefaultsStrategy; }
        @Override public void setApplyDefaultsStrategy(ApplyDefaultsStrategy s) { this.applyDefaultsStrategy = s; }

        @Override public boolean isFailFast() { return failFast; }
        @Override public void setFailFast(boolean f) { this.failFast = f; }
    }

    /** Minimal {@link ValidationContext} implementation that only supplies a config. */
    private ValidationContext validationContext;

    private TestValidator validator;
    private TestValidator defaultValidator;
    private SimpleConfig config;

    /** Concrete subclass of {@link BaseJsonValidator} exposing the protected {@code atPath}. */
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
    void setUp() {
        // configure a simple, non‑fail‑fast JSON‑Pointer configuration
        config = new SimpleConfig();
        config.setPathType(PathType.JSON_POINTER);
        config.setFailFast(false);
        config.setApplyDefaultsStrategy(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);

        // a very small ValidationContext implementation – only getConfig is required
        validationContext = new ValidationContext() {
            @Override public SchemaValidatorsConfig getConfig() { return config; }
        };

        // validator instances used in the tests
        validator = new TestValidator("", JsonNodeFactory.instance.objectNode(),
                null, null, validationContext);
        defaultValidator = validator; // both use the same JSON‑Pointer config
    }

    private static Stream<Arguments> jsonPointerPathProvider() {
        return Stream.of(
                Arguments.of("Normal token appended with '/' separator", "/data", "items", "/data/items"),
                Arguments.of("Empty current path – token becomes first segment", "", "root", "/root"),
                Arguments.of("Empty token – trailing '/' indicates empty segment", "/array", "", "/array/"),
                Arguments.of("Token containing '~' and '/' are escaped per JSON Pointer rules",
                        "/a~b", "c/d", "/a~0b/c~1d"),
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
        assertThrows(NullPointerException.class,
                () -> validator.atPath("/test", "field"));
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