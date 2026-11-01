```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BaseJsonValidator#getKeyword()}.
 */
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
    ValidatorTypeCode validatorType;          // also used as Keyword
    @Mock
    ValidationContext validationContext;

    // ----- System under test -----
    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the keyword value that BaseJsonValidator will expose via getKeyword()
        when(validatorType.getValue()).thenReturn("testKeyword");

        // Create a concrete instance of the abstract BaseJsonValidator
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getKeyword_returnsConfiguredKeywordValue() {
        // When
        String result = validator.getKeyword();

        // Then
        assertEquals("testKeyword", result,
                "BaseJsonValidator#getKeyword() should return the value supplied by the Keyword instance");
    }

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} that only
     * implements the abstract {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // Validation logic is irrelevant for the getKeyword() test.
            return null;
        }
    }
}
```