```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getEvaluationPath()}.
 *
 * <p>The abstract {@link BaseJsonValidator} cannot be instantiated directly, therefore a minimal concrete
 * subclass {@link TestValidator} is defined inside the test class. The tests verify three behaviours
 * described in the JSON test cases:</p>
 *
 * <ul>
 *   <li>When a non‑null {@link JsonNodePath} is supplied via the constructor, the getter returns the exact same instance.</li>
 *   <li>When {@code null} is supplied, the getter returns {@code null} (no default is created).</li>
 *   <li>The returned reference must be identical (reference equality) to the one injected – not a copy.</li>
 * </ul>
 *
 * <p>Mockito is used to stub the {@link ValidationContext} and its configuration, because the
 * {@link BaseJsonValidator} constructor accesses those objects.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getEvaluationPath_Test {

    /* --------------------------------------------------------------------- *
     *  Mocked collaborators required by the BaseJsonValidator constructor. *
     * --------------------------------------------------------------------- */
    @Mock ValidationContext validationContext;
    @Mock SchemaValidatorsConfig config;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNode schemaNode;
    @Mock JsonSchema parentSchema;
    @Mock ValidatorTypeCode validatorType;
    @Mock JsonNodePath evaluationPath; // can be null in specific test cases

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
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
        // Reset mocks to guarantee test isolation.
        reset(validationContext, config, schemaLocation, schemaNode,
                parentSchema, validatorType, evaluationPath);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * Only the abstract methods are stubbed; validation logic is irrelevant for these tests.
     */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                    validatorType, validationContext);
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
        public <T> T validate(ExecutionContext executionContext,
                               JsonNode node,
                               OutputFormat<T> format,
                               ExecutionContextCustomizer executionCustomizer) {
            return null;
        }
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized test covering all three scenarios described in the   *
     *  JSON test case group.                                               *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: evaluationPath={0}")
    @MethodSource("evaluationPathProvider")
    void getEvaluationPath_behaviour(JsonNodePath suppliedPath, String expectation) {
        // Arrange: create the validator with the supplied path (may be null)
        TestValidator validator = new TestValidator(
                schemaLocation,
                suppliedPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext
        );

        // Act
        JsonNodePath result = validator.getEvaluationPath();

        // Assert according to the expectation description
        switch (expectation) {
            case "same":
                // The getter must return the exact same object reference.
                assertSame(suppliedPath, result,
                        "Expected getEvaluationPath() to return the same instance that was injected");
                break;
            case "null":
                assertNull(result,
                        "Expected getEvaluationPath() to return null when constructed with null");
                break;
            default:
                fail("Unknown expectation token: " + expectation);
        }
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
        return Stream.of(
                Arguments.of(mockPath, "same"),
                Arguments.of(null, "null")
        );
    }

    /* --------------------------------------------------------------------- *
     *  Additional explicit tests mirroring the original test class – they   *
     *  serve as documentation and guarantee coverage even if the           *
     *  parameterized test is altered.                                       *
     * --------------------------------------------------------------------- */

    @Test
    void getEvaluationPath_returnsProvidedPath() {
        // Arrange
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext
        );

        // Act
        JsonNodePath result = validator.getEvaluationPath();

        // Assert
        assertSame(evaluationPath, result,
                "BaseJsonValidator#getEvaluationPath should return the exact instance supplied to the constructor");
    }

    @Test
    void getEvaluationPath_returnsNullWhenConstructedWithNull() {
        // Arrange
        TestValidator validator = new TestValidator(
                schemaLocation,
                null,               // explicit null evaluation path
                schemaNode,
                parentSchema,
                validatorType,
                validationContext
        );

        // Act
        JsonNodePath result = validator.getEvaluationPath();

        // Assert
        assertNull(result,
                "When constructed with a null evaluationPath, getEvaluationPath should return null");
    }
}
```