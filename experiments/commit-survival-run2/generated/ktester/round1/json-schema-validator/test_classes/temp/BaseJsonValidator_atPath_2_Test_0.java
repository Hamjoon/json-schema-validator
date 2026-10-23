package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atPath_2_Test {

    private TestValidator validator;

    /**
     * A mocked {@code PathType} that will be injected into the validator instance.
     */
    @Mock
    private PathType mockPathType;

    /**
     * A minimal concrete implementation of {@link BaseJsonValidator} used only for testing.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator() {
            // Pass dummy values to the super‑constructor; most arguments can be null because
            // the constructor only stores them in fields that are not used by {@code atPath}.
            super("", null, null, null, null, false);
        }

        // The abstract {@code validate} method is not needed for these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return java.util.Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        validator = new TestValidator();
        // Configure the mock to return a predictable string for any inputs.
        when(mockPathType.append(anyString(), anyInt())).thenAnswer(invocation -> {
            String currentPath = invocation.getArgument(0);
            int index = invocation.getArgument(1);
            return "mocked[" + currentPath + "][" + index + "]";
        });
        // Inject the mock into the private final field 'pathType' via reflection.
        Field pathTypeField = BaseJsonValidator.class.getDeclaredField("pathType");
        pathTypeField.setAccessible(true);
        pathTypeField.set(validator, mockPathType);
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
        return Stream.of(// JSON_POINTER examples
        Arguments.of(PathType.JSON_POINTER, "/a/b", 2, "/a/b/2"), Arguments.of(PathType.JSON_POINTER, "", 0, "/0"), // JSON_PATH examples
        Arguments.of(PathType.JSON_PATH, "$.store.book", 5, "$.store.book[5]"), Arguments.of(PathType.JSON_PATH, "$", 1, "$[1]"), // negative index (both strategies treat it as a string)
        Arguments.of(PathType.JSON_POINTER, "/some/path", -1, "/some/path/-1"), // maximum int value
        Arguments.of(PathType.JSON_POINTER, "/max/int", Integer.MAX_VALUE, "/max/int/2147483647"));
    }

    private static Stream<Arguments> provideJsonPointerEdgeCases() {
        return Stream.of(Arguments.of(PathType.JSON_POINTER, "/", 0, "/0"), Arguments.of(PathType.JSON_POINTER, "/a/b/", 5, "/a/b/5"), Arguments.of(PathType.JSON_POINTER, "/a~b", 1, "/a~b/1"), Arguments.of(PathType.JSON_POINTER, "/a b", 2, "/a b/2"), Arguments.of(PathType.JSON_POINTER, "/max/int", Integer.MIN_VALUE, "/max/int/-2147483648"));
    }

    private static Stream<Arguments> provideJsonPathEdgeCases() {
        return Stream.of(Arguments.of(PathType.JSON_PATH, "$.store.book", 0, "$.store.book[0]"), Arguments.of(PathType.JSON_PATH, "$", 0, "$[0]"), Arguments.of(PathType.JSON_PATH, "$.array[2]", 3, "$.array[2][3]"), Arguments.of(PathType.JSON_PATH, "$.complex.property", -5, "$.complex.property[-5]"), Arguments.of(PathType.JSON_PATH, "$.unicode_π", 1, "$.unicode_π[1]"), Arguments.of(PathType.JSON_PATH, "", 4, "[4]"));
    }

    @Test
    void atPath_delegatesToPathTypeAppend_correctResult() {
        String result = validator.atPath("root", 3);
        assertEquals("mocked[root][3]", result, "The result should be the value returned by the mocked PathType.append method");
        // Verify that the mock was called exactly once with the expected arguments.
        verify(mockPathType, times(1)).append("root", 3);
    }

    @Test
    void atPath_handlesEmptyPathAndZeroIndex() {
        String result = validator.atPath("", 0);
        assertEquals("mocked[][0]", result, "Even with an empty currentPath, the method should forward the values to PathType.append");
        verify(mockPathType, times(1)).append("", 0);
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
        // Directly set the private final field to null.
        Field field = BaseJsonValidator.class.getDeclaredField("pathType");
        field.setAccessible(true);
        field.set(validator, null);
        assertThrows(NullPointerException.class, () -> validator.atPath("/a", 1));
    }
}
