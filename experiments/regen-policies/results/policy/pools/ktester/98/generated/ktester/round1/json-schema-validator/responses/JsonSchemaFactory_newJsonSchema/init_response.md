```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@code newJsonSchema} protected method of {@link JsonSchemaFactory}.
 *
 * <p>The test creates a concrete {@link JsonSchemaFactory} instance via the public
 * {@code getInstance} factory method and then accesses the protected method through
 * reflection. The goal is to verify that a {@link JsonSchema} object is created
 * without errors and that the returned instance is not {@code null}.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;
    private static SchemaValidatorsConfig config;

    /** Create a shared factory and default objects once for all tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it builds a fully‑initialised instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();

        // The library provides a builder for the config; use the defaults.
        config = SchemaValidatorsConfig.builder().build();
    }

    /** Reset any mutable state before each test (none needed at the moment). */
    @BeforeEach
    void setupBeforeEach() {
        // No per‑test mutable state required.
    }

    /** Clean‑up after each test (none needed). */
    @AfterEach
    void teardownAfterEach() {
        // No resources to release.
    }

    /** Clean‑up after all tests (none needed). */
    @AfterAll
    static void teardownAfterAll() {
        // No global resources to close.
    }

    /**
     * Verify that {@code newJsonSchema} creates a non‑null {@link JsonSchema}
     * instance when supplied with a simple schema node.
     */
    @Test
    void newJsonSchemaCreatesSchema() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        // a minimal JSON schema: {"type":"string"}
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test-schema"));

        // ----- WHEN ----------------------------------------------------------
        // invoke the protected method via reflection
        Method newJsonSchemaMethod = JsonSchemaFactory.class
                .getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class,
                        SchemaValidatorsConfig.class);
        newJsonSchemaMethod.setAccessible(true);
        JsonSchema result = (JsonSchema) newJsonSchemaMethod.invoke(factory, location, schemaNode, config);

        // ----- THEN ----------------------------------------------------------
        assertNotNull(result, "newJsonSchema should return a non‑null JsonSchema");
        // The returned JsonSchema should expose the original node (public getter exists).
        // If the getter is not public, we fall back to reflection.
        JsonNode returnedNode;
        try {
            returnedNode = result.getSchemaNode();
        } catch (NoSuchMethodError | NoSuchMethodException e) {
            // Fallback: use reflection to read the private field 'schemaNode'
            Method getNode = JsonSchema.class.getDeclaredMethod("getSchemaNode");
            getNode.setAccessible(true);
            returnedNode = (JsonNode) getNode.invoke(result);
        }
        assertEquals(schemaNode, returnedNode,
                "The JsonSchema should contain the same schema node that was supplied");
    }

    /**
     * Verify that calling {@code newJsonSchema} with a {@code null} schema node
     * throws a {@link NullPointerException}. This mirrors the contract of the
     * underlying implementation (it expects a non‑null node).
     */
    @Test
    void newJsonSchemaRejectsNullSchemaNode() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test-schema"));

        // ----- WHEN ----------------------------------------------------------
        Method newJsonSchemaMethod = JsonSchemaFactory.class
                .getDeclaredMethod("newJsonSchema", SchemaLocation.class, JsonNode.class,
                        SchemaValidatorsConfig.class);
        newJsonSchemaMethod.setAccessible(true);

        // ----- THEN ----------------------------------------------------------
        assertThrows(NullPointerException.class, () -> {
            newJsonSchemaMethod.invoke(factory, location, null, config);
        });
    }
}
```