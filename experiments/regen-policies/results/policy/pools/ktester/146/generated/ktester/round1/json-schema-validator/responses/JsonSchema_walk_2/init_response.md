```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_2_Test {

    /** shared factory for all tests */
    private static JsonSchemaFactory factory;

    /** fresh schema instance for each test */
    private JsonSchema jsonSchema;

    /** Jackson mapper to create JsonNode instances */
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory that can resolve schemas from the class‑path / URLs.
        // The simple schema used in the tests does not need external references,
        // therefore the default factory is sufficient.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal schema that requires an object with a required string property "name".
        String schemaData = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": [ "name" ]
                }""";

        // No special configuration – the default works for this simple schema.
        jsonSchema = factory.getSchema(schemaData);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up; method present for symmetry with the template.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release globally.
    }

    /**
     * When {@code validate} is {@code true} the walk operation must perform validation.
     * The supplied JSON node violates the schema (numeric instead of string) and therefore
     * the returned {@link ValidationResult} should contain a validation message.
     */
    @Test
    void walk_shouldValidateWhenFlagIsTrue_andReportError() throws Exception {
        // given – a JSON node that does NOT satisfy the schema
        JsonNode node = mapper.readTree("{\"name\": 1}");

        // when – walk with validation enabled
        ValidationResult result = jsonSchema.walk(node, true);

        // then – the result must be invalid and contain exactly one message
        assertFalse(result.isValid(), "Result should be marked as invalid");
        assertEquals(1, result.getMessages().size(),
                "Exactly one validation message is expected for the type mismatch");
    }

    /**
     * When {@code validate} is {@code false} the walk operation should skip validation.
     * The same invalid JSON node is used, but the result must be considered valid and
     * contain no messages.
     */
    @Test
    void walk_shouldSkipValidationWhenFlagIsFalse() throws Exception {
        // given – a JSON node that would fail validation
        JsonNode node = mapper.readTree("{\"name\": 1}");

        // when – walk with validation disabled
        ValidationResult result = jsonSchema.walk(node, false);

        // then – the walk result is considered valid and carries no messages
        assertTrue(result.isValid(), "Result should be marked as valid when validation is disabled");
        assertEquals(0, result.getMessages().size(),
                "No validation messages should be produced when validation is disabled");
    }
}
```