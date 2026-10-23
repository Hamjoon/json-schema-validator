```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#atPath(String, int)}.
 *
 * <p>
 * The tests verify that {@code atPath} correctly delegates to the injected
 * {@link PathType} implementation.  Different {@code PathType} values (JSON_POINTER
 * and JSON_PATH) produce different formatting rules, therefore a comprehensive set
 * of scenarios – normal cases, edge cases, and error situations – is exercised.
 * The {@code pathType} field is {@code private final} in the production code, so
 * it is replaced via reflection for each test case.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidatorAtPathTest {

    /** Minimal concrete subclass required because {@link BaseJsonValidator} is abstract. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator() {
            // Most constructor arguments are irrelevant for the atPath tests.
            super("", null, null, null, null, false);
        }

        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return java.util.Collections.emptySet();
        }
    }

    private TestValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new TestValidator();
    }

    /** Helper that injects a {@link PathType} into the validator using reflection. */
    private void injectPathType(PathType type) throws Exception {
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, type);
    }

    /* -------------------------------------------------
     *  Normal behaviour – delegation to PathType.append
     * ------------------------------------------------- */

    @ParameterizedTest(name = "{index}: [{0}] atPath(\"{1}\",{2}) → \"{3}\"")
    @MethodSource("provideAtPathNormalCases")
    void atPath_delegatesCorrectly(PathType pathType,
                                   String currentPath,
                                   int index,
                                   String expected) throws Exception {
        injectPathType(pathType);
        String actual = validator.atPath(currentPath, index);
        assertEquals(expected, actual);
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

    /* -------------------------------------------------
     *  JSON_POINTER edge cases
     * ------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("provideJsonPointerEdgeCases")
    void atPath_jsonPointerEdgeCases(PathType pathType,
                                     String currentPath,
                                     int index,
                                     String expected) throws Exception {
        injectPathType(pathType);
        assertEquals(expected, validator.atPath(currentPath, index));
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

    /* -------------------------------------------------
     *  JSON_PATH edge cases
     * ------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("provideJsonPathEdgeCases")
    void atPath_jsonPathEdgeCases(PathType pathType,
                                  String currentPath,
                                  int index,
                                  String expected) throws Exception {
        injectPathType(pathType);
        assertEquals(expected, validator.atPath(currentPath, index));
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

    /* -------------------------------------------------
     *  Exceptional scenarios
     * ------------------------------------------------- */

    @Test
    void atPath_nullCurrentPath_throwsNPE() throws Exception {
        injectPathType(PathType.JSON_POINTER);
        assertThrows(NullPointerException.class, () -> validator.atPath(null, 3));
    }

    @Test
    void atPath_nullPathType_throwsNPE() throws Exception {
        // Directly set the private final field to null.
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, null);
        assertThrows(NullPointerException.class, () -> validator.atPath("/a", 1));
    }
}
```