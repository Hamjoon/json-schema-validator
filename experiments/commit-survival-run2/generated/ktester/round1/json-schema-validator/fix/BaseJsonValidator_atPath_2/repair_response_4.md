```java
package com.networknt.schema;

import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

class BaseJsonValidator_atPath_2_Test {

    private TestValidator validator;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used only for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator() {
            // Most arguments are not used by {@code atPath}, so they can be null / dummy values.
            super("", null, null, null, null, false);
        }

        // The abstract {@code validate} method(s) are not needed for these tests.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        // Some older versions of the library declare a different abstract signature.
        // Providing this overload makes the test compile against both versions.
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                               JsonNode rootNode, String at) {
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        validator = new TestValidator();
    }

    /**
     * Helper that injects a {@link PathType} into the validator using reflection.
     */
    private void injectPathType(PathType type) throws Exception {
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, type);
    }

    private static Stream<Arguments> provideAtPathNormalCases() {
        return Stream.of(
                // JSON_POINTER examples
                Arguments.of(PathType.JSON_POINTER, "/a/b", 2, "/a/b/2"),
                Arguments.of(PathType.JSON_POINTER, "", 0, "/0"),
                // JSON_PATH examples
                Arguments.of(PathType.JSON_PATH, "$.store.book", 5, "$.store.book[5]"),
                Arguments.of(PathType.JSON_PATH, "$", 1, "$[1]"),
                // negative index (both strategies treat it as a string)
                Arguments.of(PathType.JSON_POINTER, "/some/path", -1, "/some/path/-1"),
                // maximum int value
                Arguments.of(PathType.JSON_POINTER, "/max/int", Integer.MAX_VALUE, "/max/int/2147483647")
        );
    }

    private static Stream<Arguments> provideJsonPointerEdgeCases() {
        return Stream.of(
                Arguments.of(PathType.JSON_POINTER, "/", 0, "/0"),
                Arguments.of(PathType.JSON_POINTER, "/a/b/", 5, "/a/b/5"),
                Arguments.of(PathType.JSON_POINTER, "/a~b", 1, "/a~b/1"),
                Arguments.of(PathType.JSON_POINTER, "/a b", 2, "/a b/2"),
                Arguments.of(PathType.JSON_POINTER, "/max/int", Integer.MIN_VALUE, "/max/int/-2147483648")
        );
    }

    private static Stream<Arguments> provideJsonPathEdgeCases() {
        return Stream.of(
                Arguments.of(PathType.JSON_PATH, "$.store.book", 0, "$.store.book[0]"),
                Arguments.of(PathType.JSON_PATH, "$", 0, "$[0]"),
                Arguments.of(PathType.JSON_PATH, "$.array[2]", 3, "$.array[2][3]"),
                Arguments.of(PathType.JSON_PATH, "$.complex.property", -5, "$.complex.property[-5]"),
                Arguments.of(PathType.JSON_PATH, "$.unicode_π", 1, "$.unicode_π[1]"),
                Arguments.of(PathType.JSON_PATH, "", 4, "[4]")
        );
    }

    @Test
    void atPath_delegatesToPathTypeAppend_correctResult() throws Exception {
        injectPathType(PathType.JSON_POINTER);
        String result = validator.atPath("root", 3);
        String expected = PathType.JSON_POINTER.append("root", 3);
        assertEquals(expected, result, "Result should match PathType.append implementation");
    }

    @Test
    void atPath_handlesEmptyPathAndZeroIndex() throws Exception {
        injectPathType(PathType.JSON_POINTER);
        String result = validator.atPath("", 0);
        String expected = PathType.JSON_POINTER.append("", 0);
        assertEquals(expected, result, "Result should match PathType.append implementation for empty path");
    }

    @ParameterizedTest(name = "{index}: [{0}] atPath(\"{1}\",{2}) → \"{3}\"")
    @MethodSource("provideAtPathNormalCases")
    void atPath_delegatesCorrectly(PathType pathType, String currentPath, int index, String expected) throws Exception {
        injectPathType(pathType);
        String actual = validator.atPath(currentPath, index);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("provideJsonPointerEdgeCases")
    void atPath_jsonPointerEdgeCases(PathType pathType, String currentPath, int index, String expected) throws Exception {
        injectPathType(pathType);
        assertEquals(expected, validator.atPath(currentPath, index));
    }

    @ParameterizedTest
    @MethodSource("provideJsonPathEdgeCases")
    void atPath_jsonPathEdgeCases(PathType pathType, String currentPath, int index, String expected) throws Exception {
        injectPathType(pathType);
        assertEquals(expected, validator.atPath(currentPath, index));
    }

    @Test
    void atPath_nullCurrentPath_throwsNPE() throws Exception {
        injectPathType(PathType.JSON_POINTER);
        assertThrows(NullPointerException.class, () -> validator.atPath(null, 3));
    }

    @Test
    void atPath_nullPathType_throwsNPE() throws Exception {
        // Directly set the private field to null via reflection.
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, null);
        assertThrows(NullPointerException.class, () -> validator.atPath("/a", 1));
    }
}
```