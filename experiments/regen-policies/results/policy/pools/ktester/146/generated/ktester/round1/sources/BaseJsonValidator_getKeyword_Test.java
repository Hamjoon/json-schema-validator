package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getKeyword_Test {

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidationContext validationContext;

    @Mock
    Keyword keyword;

    @Mock
    MessageSource messageSource;

    /**
     * Instance of a concrete validator used in the tests.
     */
    private BaseJsonValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the keyword to return a known value.
        when(keyword.getValue()).thenReturn("myKeyword");
        // Create a concrete validator that delegates to the protected constructor
        // of BaseJsonValidator. All other parameters are irrelevant for the
        // getKeyword() behaviour and can be simple mocks or empty values.
        validator = new BaseJsonValidator(/* suppressSubSchemaRetrieval */
        false, /* schemaNode */
        schemaNode, /* validationContext */
        validationContext, /* errorMessageType */
        null, /* errorMessageKeyword */
        null, /* messageSource */
        messageSource, /* keyword */
        keyword, /* parentSchema */
        parentSchema, /* schemaLocation */
        schemaLocation, /* evaluationPath */
        evaluationPath, /* evaluationParentSchema */
        null, /* errorMessage map */
        Collections.emptyMap()) {

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                // No validation logic needed for the getKeyword() test.
                return Collections.emptySet();
            }
        };
    }

    /**
     * Helper that builds a concrete {@link BaseJsonValidator} instance.
     *
     * @param kw the {@link Keyword} to be injected (may be {@code null})
     * @return a ready‑to‑use validator
     */
    private BaseJsonValidator createValidator(Keyword kw) {
        return new BaseJsonValidator(/* suppressSubSchemaRetrieval */
        false, /* schemaNode */
        schemaNode, /* validationContext */
        validationContext, /* errorMessageType */
        null, /* errorMessageKeyword */
        null, /* messageSource */
        messageSource, /* keyword */
        kw, /* parentSchema */
        parentSchema, /* schemaLocation */
        schemaLocation, /* evaluationPath */
        evaluationPath, /* evaluationParentSchema */
        null, /* errorMessage map */
        Collections.emptyMap()) {

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                // Validation logic is irrelevant for getKeyword() tests.
                return Collections.emptySet();
            }
        };
    }

    private static Stream<Arguments> provideKeywordReturnValues() {
        return Stream.of(// normal string
        Arguments.of("type"), // null returned by getValue()
        Arguments.of((String) null), // empty string
        Arguments.of(""), // whitespace only
        Arguments.of("   "), // very long string (10,000 'a's)
        Arguments.of("a".repeat(10_000)), // Unicode string with non‑ASCII characters
        Arguments.of("προσαρμογή"));
    }

    @Test
    void getKeyword_returnsKeywordValueFromKeywordObject() {
        // Act
        String result = validator.getKeyword();
        // Assert
        assertEquals("myKeyword", result, "BaseJsonValidator#getKeyword() should return the value supplied by the Keyword instance.");
        // Verify that the keyword mock was queried exactly once.
        verify(keyword, times(1)).getValue();
    }

    @ParameterizedTest(name = "{index} => keyword value=''{0}''")
    @MethodSource("provideKeywordReturnValues")
    void getKeyword_returnsValueFromKeyword(String keywordValue) {
        // Arrange – stub the keyword mock to return the supplied value (may be null).
        when(keyword.getValue()).thenReturn(keywordValue);
        validator = createValidator(keyword);
        // Act
        String result = validator.getKeyword();
        // Assert
        assertEquals(keywordValue, result, "BaseJsonValidator#getKeyword() should return exactly the value supplied by Keyword#getValue().");
        verify(keyword, times(1)).getValue();
    }

    @Test
    void getKeyword_nullKeyword_throwsNullPointerException() {
        // Arrange – create a validator with a null keyword reference.
        validator = createValidator(null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getKeyword, "When the internal keyword field is null, getKeyword() should throw NPE.");
    }

    @Test
    void getKeyword_keywordGetValueThrowsNullPointerException_propagates() {
        // Arrange
        Keyword badKeyword = mock(Keyword.class);
        when(badKeyword.getValue()).thenThrow(new NullPointerException("boom"));
        validator = createValidator(badKeyword);
        // Act & Assert
        NullPointerException ex = assertThrows(NullPointerException.class, validator::getKeyword, "Exception from keyword.getValue() must propagate unchanged.");
        assertEquals("boom", ex.getMessage());
        verify(badKeyword, times(1)).getValue();
    }

    @Test
    void getKeyword_keywordGetValueThrowsIllegalArgumentException_propagates() {
        // Arrange
        Keyword badKeyword = mock(Keyword.class);
        when(badKeyword.getValue()).thenThrow(new IllegalArgumentException("illegal"));
        validator = createValidator(badKeyword);
        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, validator::getKeyword);
        assertEquals("illegal", ex.getMessage());
        verify(badKeyword, times(1)).getValue();
    }

    @Test
    void getKeyword_keywordGetValueThrowsRuntimeException_propagates() {
        // Arrange
        Keyword badKeyword = mock(Keyword.class);
        when(badKeyword.getValue()).thenThrow(new RuntimeException("runtime"));
        validator = createValidator(badKeyword);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, validator::getKeyword);
        assertEquals("runtime", ex.getMessage());
        verify(badKeyword, times(1)).getValue();
    }

    @Test
    void getKeyword_keywordGetValueThrowsAssertionError_propagates() {
        // Arrange
        Keyword badKeyword = mock(Keyword.class);
        when(badKeyword.getValue()).thenThrow(new AssertionError("assert"));
        validator = createValidator(badKeyword);
        // Act & Assert
        AssertionError err = assertThrows(AssertionError.class, validator::getKeyword);
        assertEquals("assert", err.getMessage());
        verify(badKeyword, times(1)).getValue();
    }

    @Test
    void getKeyword_keywordGetValueThrowsIllegalStateException_propagates() {
        // Arrange
        Keyword badKeyword = mock(Keyword.class);
        when(badKeyword.getValue()).thenThrow(new IllegalStateException("illegal state"));
        validator = createValidator(badKeyword);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::getKeyword);
        assertEquals("illegal state", ex.getMessage());
        verify(badKeyword, times(1)).getValue();
    }
}
