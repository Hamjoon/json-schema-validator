```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.Set;

/**
 * Tests for {@link PropertiesValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 * The tests use real {@link JsonSchemaFactory} instances so that the internal
 * {@link PropertiesValidator} is exercised without resorting to reflection or
 * private‑method access.
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_walk_Test extends BaseJsonSchemaValidatorTest {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version; the exact version is not important for the
        // behaviour we are testing.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * When {@code shouldValidateSchema} is {@code true} the validator must perform
     * the actual validation and return any {@link ValidationMessage}s produced by
     * the underlying schema.
     */
    @Test
    void walkShouldValidateWhenFlagTrue() throws Exception {
        // schema: property "age" must be an integer
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // node violates the schema – "age" is a string, not an integer
        JsonNode node = getJsonNodeFromStringContent("{\"age\":\"not-an-int\"}");

        // invoke the walk method with validation enabled
        ValidationResult result = schema.walk(node, true);

        Set<ValidationMessage> messages = result.getValidationMessages();
        Assertions.assertFalse(messages.isEmpty(),
                "Expected validation messages because the instance does not satisfy the schema");
    }

    /**
     * When {@code shouldValidateSchema} is {@code false} the validator must only
     * walk the schema (triggering any walk listeners) but must **not** perform
     * validation. Consequently the returned message set should be empty.
     */
    @Test
    void walkShouldSkipValidationWhenFlagFalse() throws Exception {
        // same schema as above – property "age" must be an integer
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // node still violates the schema, but we ask the walk method *not* to validate
        JsonNode node = getJsonNodeFromStringContent("{\"age\":\"not-an-int\"}");

        // invoke the walk method with validation disabled
        ValidationResult result = schema.walk(node, false);

        Set<ValidationMessage> messages = result.getValidationMessages();
        Assertions.assertTrue(messages.isEmpty(),
                "Expected no validation messages because validation was disabled");
    }
}
```