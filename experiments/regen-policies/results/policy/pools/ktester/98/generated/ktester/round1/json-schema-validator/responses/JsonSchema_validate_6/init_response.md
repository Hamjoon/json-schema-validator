```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#validate(String, InputFormat)}.
 *
 * The tests cover both a valid JSON instance (expecting no validation messages)
 * and an invalid JSON instance (expecting at least one validation message).
 *
 * The {@link JsonSchema} instance is created via {@link JsonSchemaFactory}
 * using a simple schema that requires a {@code name} property of type {@code string}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_6_Test {

    /** Factory used to create {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Schema under test. */
    private JsonSchema schema;

    /** Configuration used when creating the schema. */
    private static SchemaValidatorsConfig config;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft (the most recent draft supported by the library)
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Simple config – we do not need any special behaviour for these tests
        config = SchemaValidatorsConfig.builder().build();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A minimal schema that requires a string property "name"
        String schemaData = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{ \"type\":\"string\" }"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";

        // Create the JsonSchema instance that will be exercised in each test
        schema = factory.getSchema(schemaData, config);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the test tidy
        schema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
        config = null;
    }

    @Test
    void validate_validInput_returnsEmptySet() {
        // GIVEN a JSON document that satisfies the schema
        String validJson = "{ \"name\": \"John Doe\" }";

        // WHEN the validate method is invoked
        Set<ValidationMessage> messages = schema.validate(validJson, InputFormat.JSON);

        // THEN no validation messages should be produced
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages for valid input, but got: " + messages);
    }

    @Test
    void validate_invalidInput_returnsMessages() {
        // GIVEN a JSON document that violates the schema (name is not a string)
        String invalidJson = "{ \"name\": 123 }";

        // WHEN the validate method is invoked
        Set<ValidationMessage> messages = schema.validate(invalidJson, InputFormat.JSON);

        // THEN at least one validation message should be produced
        assertFalse(messages.isEmpty(),
                "Expected validation messages for invalid input, but the set was empty");
        // The schema only has one rule, so we expect exactly one message
        assertEquals(1, messages.size(),
                () -> "Expected exactly one validation message, but got: " + messages.size());

        // OPTIONAL: verify that the message refers to a type error
        ValidationMessage msg = messages.iterator().next();
        assertEquals("type", msg.getType(),
                () -> "Expected a type validation error, but got: " + msg.getType());
    }
}
```