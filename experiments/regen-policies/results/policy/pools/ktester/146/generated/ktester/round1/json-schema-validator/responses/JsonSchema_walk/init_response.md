```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    /** Schema used for all test cases */
    private JsonSchema schema;

    /** Jackson mapper to create JsonNode instances from JSON strings */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Simple schema that references another schema – identical to the one used in the
     *  concurrency test of the project. */
    private static final String SCHEMA_DATA = "{\n"
            + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
            + "  \"$id\": \"http://example.org/schema.json\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"$ref\": \"ref.json\"\n"
            + "}";

    /** The referenced schema – defines a required string property “name”. */
    private static final String REF_SCHEMA_DATA = "{\n"
            + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n"
            + "  \"$id\": \"http://example.org/ref.json\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": {\n"
            + "      \"type\": \"string\",\n"
            + "      \"description\": \"The name\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"required\": [\"name\"]\n"
            + "}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a factory that knows how to resolve the external reference.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        schemaLoaders -> schemaLoaders.schemas(
                                Collections.singletonMap("http://example.org/ref.json", REF_SCHEMA_DATA))));

        // Disable pre‑loading – the same configuration used in the original test.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .preloadJsonSchema(false)
                .build();

        // Create the JsonSchema instance that will be exercised.
        this.schema = factory.getSchema(SCHEMA_DATA, config);
        assertNotNull(schema, "Schema should be created successfully");
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, true, …)} performs validation
     * and returns a {@link ValidationResult} containing the expected validation error.
     */
    @Test
    void walkWithValidationEnabled() throws Exception {
        // Input where the required property “name” is of the wrong type (number instead of string)
        String inputJson = "{ \"name\": 1 }";
        JsonNode node = MAPPER.readTree(inputJson);

        // Create a fresh execution context for the walk.
        ExecutionContext ctx = schema.createExecutionContext();

        // Simple customizer that does nothing – required by the method signature.
        ExecutionContextCustomizer customizer = executionContext -> {
            // no‑op
        };

        ValidationResult result = schema.walk(ctx, node, true, customizer);

        // The schema should report exactly one validation error (type mismatch).
        assertEquals(1, result.getMessages().size(),
                "When validation is enabled the result should contain one error");
    }

    /**
     * Verify that {@code walk(ExecutionContext, JsonNode, false, …)} skips validation
     * and therefore returns an empty set of messages.
     */
    @Test
    void walkWithValidationDisabled() throws Exception {
        // Same malformed input as the previous test.
        String inputJson = "{ \"name\": 1 }";
        JsonNode node = MAPPER.readTree(inputJson);

        ExecutionContext ctx = schema.createExecutionContext();
        ExecutionContextCustomizer customizer = executionContext -> {
            // no‑op
        };

        ValidationResult result = schema.walk(ctx, node, false, customizer);

        // No validation should have been performed, so the message set must be empty.
        assertEquals(0, result.getMessages().size(),
                "When validation is disabled the result should contain no errors");
    }

    /**
     * Verify that the walk method works correctly when a custom {@link ExecutionContextCustomizer}
     * mutates the execution context (e.g., changes the collector). The customizer itself is
     * not part of the library's public contract, so we only assert that the walk still
     * completes and returns a non‑null result.
     */
    @Test
    void walkWithCustomExecutionContextCustomizer() throws Exception {
        String inputJson = "{ \"name\": \"valid\" }"; // valid input – no errors expected
        JsonNode node = MAPPER.readTree(inputJson);

        ExecutionContext ctx = schema.createExecutionContext();

        // Customizer that could, for example, replace the collector – here we just keep it simple.
        ExecutionContextCustomizer customizer = executionContext -> {
            // In a real scenario we might set a custom CollectorContext here.
        };

        ValidationResult result = schema.walk(ctx, node, true, customizer);

        // The input is valid, therefore the result should contain zero messages.
        assertEquals(0, result.getMessages().size(),
                "Valid input should produce no validation messages even with a customizer");
    }
}
```