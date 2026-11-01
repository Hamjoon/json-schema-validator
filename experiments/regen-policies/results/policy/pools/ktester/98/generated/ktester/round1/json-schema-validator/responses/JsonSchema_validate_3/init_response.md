```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#validate(JsonNode, Consumer)}.
 */
class JsonSchema_validate_3_Test {

    private static ObjectMapper mapper;
    private JsonSchema schema;

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper is thread‑safe; initialise once for the whole test class.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Simple schema that requires a string property "name".
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";

        // Use the factory to obtain a JsonSchema instance – this respects all
        // internal constructor requirements without us having to build the
        // complex dependency graph manually.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up between tests.
        this.schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
        mapper = null;
    }

    @Test
    void validate_WithConsumerCustomizer_ReturnsExpectedMessages() throws Exception {
        // JSON instance where "name" is an integer – violates the schema.
        String jsonData = "{ \"name\": 1 }";
        JsonNode rootNode = mapper.readTree(jsonData);

        // A no‑op customizer; we only need to exercise the overload.
        Consumer<ExecutionContext> customizer = ctx -> {
            // No modifications required for this test.
        };

        Set<ValidationMessage> messages = schema.validate(rootNode, customizer);

        // The schema should produce exactly one validation error (type mismatch).
        assertEquals(1, messages.size(), "Expected a single validation error");

        ValidationMessage message = messages.iterator().next();
        // The default message contains the word "type" for a type mismatch.
        assertTrue(message.getMessage().toLowerCase().contains("type"),
                "Validation message should indicate a type problem");
    }
}
```