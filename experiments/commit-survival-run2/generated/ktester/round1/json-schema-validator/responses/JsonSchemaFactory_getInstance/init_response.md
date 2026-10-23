```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaFactory_getInstance_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing to initialise before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Verify that {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} returns a
     * non‑null factory and that the factory can be used to create a {@link JsonSchema}
     * from a minimal JSON schema string.
     */
    @Test
    void getInstance_ShouldReturnFactoryAbleToParseSchema() throws Exception {
        // Use a well‑known version flag (draft‑07). The enum constant exists in the library.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory, "Factory must not be null");

        // A minimal, valid JSON schema (empty object means "any JSON value").
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "JsonSchema instance must not be null");

        // Parse a simple JSON document and validate it – it should pass without exception.
        JsonNode instance = MAPPER.readTree("{\"foo\":\"bar\"}");
        assertDoesNotThrow(() -> schema.validate(instance), "Validation of a valid instance should not throw");
    }

    /**
     * Verify that passing a {@code null} version flag results in an
     * {@link IllegalArgumentException} (the internal {@code checkVersion} method validates the argument).
     */
    @Test
    void getInstance_NullVersionFlag_ShouldThrowException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> JsonSchemaFactory.getInstance(null),
                "Calling getInstance with null should throw IllegalArgumentException");
        assertTrue(thrown.getMessage().contains("null"), "Exception message should mention null");
    }
}
```