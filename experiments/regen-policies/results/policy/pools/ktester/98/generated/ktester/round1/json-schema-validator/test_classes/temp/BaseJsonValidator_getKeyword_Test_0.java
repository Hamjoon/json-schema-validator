package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getKeyword_Test {

    // ----- Mocks for constructor dependencies -----
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode // also used as Keyword
    validatorType;

    @Mock
    ValidationContext validationContext;

    // ----- System under test -----
    private TestValidator validator;

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} that only
     * implements the abstract {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation logic is irrelevant for the getKeyword() tests.
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the keyword value that BaseJsonValidator will expose via getKeyword()
        when(validatorType.getValue()).thenReturn("testKeyword");
        // Create a concrete instance of the abstract BaseJsonValidator
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
    }

    @Test
    void getKeyword_returnsConfiguredKeywordValue() {
        // When
        String result = validator.getKeyword();
        // Then
        assertEquals("testKeyword", result, "BaseJsonValidator#getKeyword() should return the value supplied by the Keyword instance");
    }

    /**
     * Parameterised test that verifies the method works for any keyword value.
     *
     * @param keyword the keyword string to be returned by the mock
     */
    @ParameterizedTest
    @ValueSource(strings = { "type", "properties" })
    void getKeyword_returnsVariousKeywordValues(String keyword) {
        // Arrange – change the stub for this specific iteration
        when(validatorType.getValue()).thenReturn(keyword);
        // Create a fresh validator that will capture the new stub value.
        TestValidator localValidator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        String result = localValidator.getKeyword();
        // Assert
        assertEquals(keyword, result, "BaseJsonValidator#getKeyword() should return the keyword value supplied at construction");
    }

    /**
     * When the internal {@code keyword} field (in {@link ValidationMessageHandler})
     * is {@code null}, {@code getKeyword()} is expected to throw a {@link NullPointerException}.
     */
    @Test
    void getKeyword_throwsNPEWhenKeywordIsNull() throws Exception {
        // Arrange – use reflection to set the protected field to null
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, null);
        // Act & Assert
        assertThrows(NullPointerException.class, validator::getKeyword, "BaseJsonValidator#getKeyword() should throw NPE when the internal keyword field is null");
    }
}
