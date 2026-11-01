```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_6_Test {

    private static final String SIMPLE_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;

    private JsonSchema jsonSchema;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a JsonSchema instance using the factory – this is the only public way
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema(SIMPLE_SCHEMA);
        mapper = new ObjectMapper();
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
        mapper = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, boolean, Consumer<ExecutionContext>)}
     * correctly validates a valid JSON instance when {@code validate} is {@code true}.
     */
    @Test
    void walk_WithValidNodeAndValidateTrue_ShouldReturnEmptyValidationResult() throws Exception {
        // given
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode validNode = mapper.readTree("{\"name\":\"John Doe\"}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        // when
        ValidationResult result = jsonSchema.walk(executionContext, validNode, true, noOpCustomizer);

        // then
        assertNotNull(result, "Result must not be null");
        // ValidationResult exposes the messages via getValidationMessages()
        assertTrue(result.getValidationMessages().isEmpty(),
                "Expected no validation messages for a valid instance");
    }

    /**
     * Verify that the same method returns a {@link ValidationResult} containing
     * messages when validation is requested and the instance is invalid.
     */
    @Test
    void walk_WithInvalidNodeAndValidateTrue_ShouldReturnValidationMessages() throws Exception {
        // given
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        // 'name' is an integer – violates the schema which expects a string
        JsonNode invalidNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        // when
        ValidationResult result = jsonSchema.walk(executionContext, invalidNode, true, noOpCustomizer);

        // then
        assertNotNull(result, "Result must not be null");
        assertFalse(result.getValidationMessages().isEmpty(),
                "Expected validation messages for an invalid instance");
    }

    /**
     * Verify that when {@code validate} is {@code false} the walk operation still
     * returns a {@link ValidationResult} but does not perform validation, i.e.
     * the message set is empty regardless of the node content.
     */
    @Test
    void walk_WithValidateFalse_ShouldSkipValidation() throws Exception {
        // given
        ExecutionContext executionContext = jsonSchema.createExecutionContext();
        JsonNode anyNode = mapper.readTree("{\"name\":123}");
        Consumer<ExecutionContext> noOpCustomizer = ctx -> { /* no‑op */ };

        // when
        ValidationResult result = jsonSchema.walk(executionContext, anyNode, false, noOpCustomizer);

        // then
        assertNotNull(result, "Result must not be null");
        assertTrue(result.getValidationMessages().isEmpty(),
                "Validation messages should be empty when validation is disabled");
    }
}
```