```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Set;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;
    private ExecutionContext executionContext;
    private JsonNodePath rootPath;

    /**
     * Build a simple object‑type schema that requires a {@code name} property of type {@code string}.
     */
    private static JsonSchema createSchema() {
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }
                """;

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the objects that will be reused by each test method.
        this.schema = createSchema();
        this.executionContext = new ExecutionContext();               // default config
        this.rootPath = new JsonNodePath(PathType.JSON);               // root instance location
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – all objects are garbage‑collected after the test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void validate_validInstance_returnsNoErrors() throws Exception {
        // given
        JsonNode instance = MAPPER.readTree("{ \"name\": \"John\" }");

        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);

        // then
        assertTrue(messages.isEmpty(), "A valid instance should produce no validation messages");
    }

    @Test
    void validate_invalidInstance_returnsErrors() throws Exception {
        // given
        JsonNode instance = MAPPER.readTree("{ \"name\": 1 }");   // name is not a string

        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);

        // then
        assertFalse(messages.isEmpty(), "An invalid instance should produce validation messages");
        // At least one message must refer to the 'type' violation for the 'name' property.
        boolean containsTypeError = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑mismatch validation message");
    }
}
```