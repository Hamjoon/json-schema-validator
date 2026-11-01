package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationPath_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    JsonNodePath evaluationPath;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * Only the abstract methods are stubbed; validation logic is irrelevant for these tests.
     */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        protected String getNodeFieldType() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
            return null;
        }

        @Override
        public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer) {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The BaseJsonValidator constructor reads configuration values.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        when(config.isCustomMessageSupported()).thenReturn(true);
        when(config.getMessageSource()).thenReturn(DefaultMessageSource.getInstance());
        // These two are optional – returning null triggers the defaults in the constructor.
        when(config.getApplyDefaultsStrategy()).thenReturn(null);
        when(config.getPathType()).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        reset(validationContext, config, schemaLocation, schemaNode, parentSchema, validatorType, evaluationPath);
    }

    /**
     * Supplies the three test scenarios:
     *
     * <ul>
     *   <li>Non‑null path – we expect reference equality.</li>
     *   <li>Null path – we expect a null return.</li>
     * </ul>
     */
    private static Stream<Arguments> evaluationPathProvider() {
        // A mock JsonNodePath representing a concrete path (e.g., "/root/child").
        JsonNodePath mockPath = mock(JsonNodePath.class);
        return Stream.of(Arguments.of(mockPath, "same"), Arguments.of(null, "null"));
    }

    @Test
    void getEvaluationPath_returnsProvidedPath() {
        // Arrange
        TestValidator validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert
        assertSame(evaluationPath, result, "BaseJsonValidator#getEvaluationPath should return the exact instance supplied to the constructor");
    }

    @Test
    void getEvaluationPath_returnsNullWhenConstructedWithNull() {
        // Arrange
        TestValidator validator = new TestValidator(schemaLocation, // explicit null evaluation path
        null, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert
        assertNull(result, "When constructed with a null evaluationPath, getEvaluationPath should return null");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized test covering all three scenarios described in the   *
     *  JSON test case group.                                               *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: evaluationPath={0}")
    @MethodSource("evaluationPathProvider")
    void getEvaluationPath_behaviour(JsonNodePath suppliedPath, String expectation) {
        // Arrange: create the validator with the supplied path (may be null)
        TestValidator validator = new TestValidator(schemaLocation, suppliedPath, schemaNode, parentSchema, validatorType, validationContext);
        // Act
        JsonNodePath result = validator.getEvaluationPath();
        // Assert according to the expectation description
        switch(expectation) {
            case "same":
                // The getter must return the exact same object reference.
                assertSame(suppliedPath, result, "Expected getEvaluationPath() to return the same instance that was injected");
                break;
            case "null":
                assertNull(result, "Expected getEvaluationPath() to return null when constructed with null");
                break;
            default:
                fail("Unknown expectation token: " + expectation);
        }
    }
}
