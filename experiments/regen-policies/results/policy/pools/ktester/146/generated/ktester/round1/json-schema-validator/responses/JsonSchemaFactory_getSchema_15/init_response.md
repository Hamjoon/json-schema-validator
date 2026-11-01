```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(String, InputFormat, SchemaValidatorsConfig)}.
 */
class JsonSchemaFactory_getSchema_15_Test {

    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory instance that can be reused for all tests.
        // The default meta‑schema is loaded automatically for the chosen version.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state needed at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No cleanup required for this test class.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verify that a valid JSON schema string is parsed correctly and a {@link JsonSchema}
     * instance is returned.
     */
    @Test
    void shouldReturnJsonSchemaWhenInputIsValid() {
        // given
        String schema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when
        JsonSchema jsonSchema = factory.getSchema(schema, InputFormat.JSON, config);

        // then
        assertNotNull(jsonSchema, "The returned JsonSchema should not be null");
        // The JsonSchema implementation exposes the underlying JsonNode via getSchemaNode().
        // Verify that the parsed node contains the expected "type" property.
        assertEquals("object",
                jsonSchema.getSchemaNode().get("type").asText(),
                "Root type should be 'object'");
    }

    /**
     * Verify that an {@link IOException} occurring while reading the schema is wrapped
     * in a {@link JsonSchemaException}.
     */
    @Test
    void shouldWrapIOExceptionInJsonSchemaExceptionWhenSchemaIsInvalid() {
        // given
        // This is not valid JSON and will cause the underlying mapper to throw an IOException.
        String malformedSchema = "{ invalid json }";
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // when / then
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(malformedSchema, InputFormat.JSON, config),
                "A malformed JSON schema should result in a JsonSchemaException");
    }
}
```