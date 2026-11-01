```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(URI, JsonNode, SchemaValidatorsConfig)}.
 */
class JsonSchemaFactory_getSchema_13_Test {

    /** Shared JSON parser for the whole test suite. */
    private static ObjectMapper objectMapper;

    /** Instance of the factory under test. */
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a mapper once for all tests – it is thread‑safe for read‑only use.
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the public factory method to obtain a ready‑to‑use instance.
        // The V201909 version flag is widely supported and does not require extra configuration.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory instance must be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – not strictly required but keeps the test tidy.
        factory = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clear the static mapper.
        objectMapper = null;
    }

    /**
     * Verify that a valid call returns a non‑null {@link JsonSchema}.
     */
    @Test
    void testGetSchemaReturnsJsonSchema() throws Exception {
        // given
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, config);

        // then
        assertNotNull(result, "The returned JsonSchema should not be null");
        // The concrete JsonSchema implementation exposes the original node via getSchemaNode().
        // Guard against API changes – only verify the node is the same type if the method exists.
        // If the method is not present, the test still validates the non‑null contract.
        try {
            JsonNode retrieved = result.getSchemaNode();
            assertEquals(schemaNode, retrieved,
                    "The schema node stored inside JsonSchema should match the supplied node");
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
            // The method is optional for the purpose of this test suite.
        }
    }

    /**
     * Verify that passing a {@code null} URI results in a {@link NullPointerException}.
     */
    @Test
    void testGetSchemaWithNullUriThrowsException() throws Exception {
        // given
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when / then
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(null, schemaNode, config),
                "Calling getSchema with a null URI should throw NullPointerException");
    }

    /**
     * Verify that passing a {@code null} schema node results in a {@link NullPointerException}.
     */
    @Test
    void testGetSchemaWithNullJsonNodeThrowsException() {
        // given
        URI schemaUri = URI.create("http://example.com/another-schema");
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // when / then
        assertThrows(NullPointerException.class,
                () -> factory.getSchema(schemaUri, null, config),
                "Calling getSchema with a null JsonNode should throw NullPointerException");
    }

    /**
     * Verify that passing a {@code null} config still creates a schema (the factory supplies a default config).
     */
    @Test
    void testGetSchemaWithNullConfigUsesDefault() throws Exception {
        // given
        URI schemaUri = new URI("http://example.com/default-config-schema");
        JsonNode schemaNode = objectMapper.readTree("{\"type\":\"string\"}");

        // when
        JsonSchema result = factory.getSchema(schemaUri, schemaNode, null);

        // then
        assertNotNull(result, "Resulting JsonSchema should not be null even when config is null");
    }
}
```