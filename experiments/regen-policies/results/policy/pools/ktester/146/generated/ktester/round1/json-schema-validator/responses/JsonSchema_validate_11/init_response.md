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
 * Unit tests for {@link JsonSchema#validate(String, InputFormat, ExecutionContextCustomizer)}.
 *
 * The test creates a simple schema, supplies an input that violates the schema
 * and verifies that:
 * <ul>
 *   <li>the validation result contains the expected {@link ValidationMessage}</li>
 *   <li>the provided {@link ExecutionContextCustomizer} is invoked exactly once</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_11_Test {

    private static final String SCHEMA_JSON = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              },
              "required": ["age"]
            }
            """;

    private static final String INVALID_INPUT = """
            {
              "age": "not-an-integer"
            }
            """;

    private JsonSchema schema;
    private InputFormat inputFormat;

    @Mock
    private ExecutionContextCustomizer executionContextCustomizer;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a JsonSchema instance using the factory – this mirrors normal usage.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(
                        java.util.Collections.emptyMap()))); // no external refs

        // Use default configuration (pre‑load is true by default – fine for this test)
        schema = factory.getSchema(SCHEMA_JSON);
        inputFormat = InputFormat.JSON; // the enum constant representing JSON payloads
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(executionContextCustomizer);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void validate_WithExecutionContextCustomizer_ShouldInvokeCustomizerAndReturnMessages() {
        // Act: invoke the method under test
        Set<ValidationMessage> messages = schema.validate(INVALID_INPUT, inputFormat, executionContextCustomizer);

        // Assert: a validation error about the type mismatch is reported
        assertNotNull(messages, "Validation result must not be null");
        assertEquals(1, messages.size(), "Exactly one validation message is expected");

        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("integer"),
                "Message should indicate an integer type problem");

        // Verify that the customizer was called exactly once with a non‑null ExecutionContext
        verify(executionContextCustomizer, times(1)).customize(any(ExecutionContext.class));
    }
}
```