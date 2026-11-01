```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(String, InputFormat, boolean, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_9_Test {

    /** The factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** A simple schema used in the tests – a JSON string type. */
    private static final String SCHEMA_DATA = "{ \"type\": \"string\" }";

    /** Input that does NOT satisfy the schema (a number instead of a string). */
    private static final String INVALID_INPUT = "123";

    /** Input that satisfies the schema (a JSON string). */
    private static final String VALID_INPUT = "\"hello\"";

    /** Input format used for the tests. */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /** The {@link JsonSchema} instance under test. */
    private static JsonSchema schema;

    /** Mocked customizer – we only verify that it is invoked. */
    @Mock
    private ExecutionContextCustomizer executionContextCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft (the same version used in the original concurrency test)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(null)));
        // Build the schema once – it is thread‑safe and can be reused across tests
        schema = factory.getSchema(SCHEMA_DATA);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that previous interactions do not interfere
        reset(executionContextCustomizer);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test class
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to release; the factory does not hold external resources
    }

    /**
     * When {@code validate} is {@code true} the walk operation must perform validation
     * and return the corresponding {@link ValidationResult}. The customizer should be
     * invoked exactly once.
     */
    @Test
    void walkShouldValidateAndReturnMessagesWhenValidateTrue() {
        // GIVEN
        // (schema, input and mock are already prepared by the @Before methods)

        // WHEN
        ValidationResult result = schema.walk(INVALID_INPUT, INPUT_FORMAT, true, executionContextCustomizer);

        // THEN
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertEquals(1, messages.size(),
                "Invalid input should produce exactly one validation message");

        // Verify that the customizer was called – the concrete implementation of the
        // customizer is not important for this test, only that it was used.
        verify(executionContextCustomizer, times(1)).customize(any(ExecutionContext.class));
    }

    /**
     * When {@code validate} is {@code false} the walk operation must skip validation
     * and therefore return an empty set of messages. The customizer is still expected
     * to be invoked.
     */
    @Test
    void walkShouldSkipValidationWhenValidateFalse() {
        // GIVEN

        // WHEN
        ValidationResult result = schema.walk(VALID_INPUT, INPUT_FORMAT, false, executionContextCustomizer);

        // THEN
        assertNotNull(result, "walk should never return null");
        Set<ValidationMessage> messages = result.getMessages();
        assertTrue(messages.isEmpty(),
                "Validation should be skipped when the validate flag is false");

        verify(executionContextCustomizer, times(1)).customize(any(ExecutionContext.class));
    }
}
```