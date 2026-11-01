```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AnyOfValidator#canShortCircuit(ExecutionContext)}.
 *
 * The method simply returns the negation of
 * {@code executionContext.getExecutionConfig().isAnnotationCollectionEnabled()}.
 * Therefore we verify both possible outcomes by mocking the {@link ExecutionContext}
 * and its {@link ExecutionConfig}.
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_canShortCircuit_Test {

    /** Instance under test. */
    private AnyOfValidator anyOfValidator;

    /** Mocks required for the constructor of {@link AnyOfValidator}. */
    @Mock
    private SchemaLocation mockSchemaLocation;
    @Mock
    private JsonNodePath mockEvaluationPath;
    @Mock
    private JsonSchema mockParentSchema;
    @Mock
    private ValidationContext mockValidationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The validator expects the schema node to be an array.
        // An empty array is sufficient – it avoids any calls to
        // ValidationContext#newSchema().
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyArrayNode = mapper.readTree("[]");

        anyOfValidator = new AnyOfValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                emptyArrayNode,
                mockParentSchema,
                mockValidationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void canShortCircuit_returnsTrueWhenAnnotationCollectionDisabled() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isAnnotationCollectionEnabled()).thenReturn(false);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act
        boolean result = anyOfValidator.canShortCircuit(execContext);

        // Assert
        assertTrue(result, "Short‑circuit should be allowed when annotation collection is disabled");
    }

    @Test
    void canShortCircuit_returnsFalseWhenAnnotationCollectionEnabled() {
        // Arrange
        ExecutionContext execContext = mock(ExecutionContext.class);
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(execConfig.isAnnotationCollectionEnabled()).thenReturn(true);
        when(execContext.getExecutionConfig()).thenReturn(execConfig);

        // Act
        boolean result = anyOfValidator.canShortCircuit(execContext);

        // Assert
        assertFalse(result, "Short‑circuit should NOT be allowed when annotation collection is enabled");
    }
}
```