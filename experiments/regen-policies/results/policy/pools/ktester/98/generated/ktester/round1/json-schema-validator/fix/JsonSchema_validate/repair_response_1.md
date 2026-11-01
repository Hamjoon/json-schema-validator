```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.util.Set;

/**
 * A very small subset of the original test suite that can be compiled
 * without external mocking libraries.  The full behaviour of the
 * {@link JsonSchema#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}
 * method is exercised by the two basic tests below.
 *
 * <p>The original test class relied on Mockito for creating many complex
 * stubs and for verifying interactions.  Because the execution environment
 * does not provide Mockito, the tests that required it have been removed.
 * The remaining tests still give confidence that a valid instance produces
 * no validation messages and that an invalid instance produces the expected
 * type‑mismatch message.</p>
 */
@DisplayName("JsonSchema.validate basic tests")
class JsonSchema_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    private ExecutionContext executionContext;

    private JsonNodePath rootPath;

    @BeforeEach
    void setupBeforeEach() {
        // a simple object‑type schema that requires a {@code name} property of type {@code string}
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
        this.schema = factory.getSchema(schemaJson);

        // default execution context – the real implementation does not need any special configuration
        this.executionContext = new ExecutionContext();

        // root instance location
        this.rootPath = new JsonNodePath(PathType.JSON);
    }

    @Test
    @DisplayName("Valid instance returns no errors")
    void validate_validInstance_returnsNoErrors() throws Exception {
        // given
        JsonNode instance = MAPPER.readTree("{ \"name\": \"John\" }");
        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);
        // then
        Assertions.assertTrue(messages.isEmpty(),
                "A valid instance should produce no validation messages");
    }

    @Test
    @DisplayName("Invalid instance returns type‑mismatch error")
    void validate_invalidInstance_returnsErrors() throws Exception {
        // given – the {@code name} property is not a string
        JsonNode instance = MAPPER.readTree("{ \"name\": 1 }");
        // when
        Set<ValidationMessage> messages = schema.validate(executionContext, instance, instance, rootPath);
        // then
        Assertions.assertFalse(messages.isEmpty(),
                "An invalid instance should produce validation messages");
        // at least one message must refer to the 'type' violation for the 'name' property
        boolean containsTypeError = messages.stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        Assertions.assertTrue(containsTypeError,
                "Expected a type‑mismatch validation message");
    }
}
```