```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.resource.SchemaLoader;

/**
 * Unit tests for {@link JsonSchemaFactory#getMappedSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * The tests use the public {@link JsonSchemaFactory#getSchema(String)} API which internally
 * delegates to {@code getMappedSchema}.  One test verifies a successful load of a simple
 * JSON schema, while the other forces an {@code IOException} by mocking the {@link SchemaLoader}
 * to return a {@code null} {@code InputStream}, which should be wrapped in a
 * {@link JsonSchemaException}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that a simple JSON schema can be loaded successfully via {@code getMappedSchema}.
     */
    @Test
    void getMappedSchema_successfulLoad() {
        // a minimal JSON schema
        String schemaJson = "{\"type\":\"object\"}";

        // Build a factory that knows how to resolve the URI "urn:test" to the above JSON.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        sl -> sl.schemas(Collections.singletonMap("urn:test", schemaJson))));

        // The public API delegates to the protected getMappedSchema method.
        JsonSchema schema = factory.getSchema("urn:test");

        assertNotNull(schema, "The returned JsonSchema should not be null");
        JsonNode node = schema.getSchemaNode();
        assertNotNull(node, "The underlying JsonNode should not be null");
        assertEquals("object", node.get("type").asText(),
                "The loaded schema should contain the expected \"type\" property");
    }

    /**
     * Force {@code getMappedSchema} to encounter a {@code null} InputStream.
     * The method should wrap the resulting {@code IOException} into a {@link JsonSchemaException}.
     */
    @Test
    void getMappedSchema_throwsWhenInputStreamNull() throws Exception {
        // Create a real factory instance (the concrete implementation is irrelevant for this test).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

        // Mock the SchemaLoader that JsonSchemaFactory uses internally.
        SchemaLoader loaderMock = org.mockito.Mockito.mock(SchemaLoader.class);

        // The concrete class returned by SchemaLoader#getSchema is not part of the public API.
        // We therefore use reflection to obtain it and mock the required method.
        Class<?> schemaClass = Class.forName("com.networknt.schema.resource.Schema");
        Object schemaMock = org.mockito.Mockito.mock(schemaClass);
        // Simulate a null InputStream – this triggers the IOException path inside getMappedSchema.
        org.mockito.Mockito.when(schemaMock.getClass()
                .getMethod("getInputStream")
                .invoke(schemaMock))
                .thenReturn(null);
        // When the factory asks the loader for any schema, return our mock.
        org.mockito.Mockito.when(loaderMock.getSchema(org.mockito.ArgumentMatchers.any()))
                .thenReturn(schemaMock);

        // Inject the mocked loader into the factory via reflection (the field is private final).
        Field loaderField = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        loaderField.setAccessible(true);
        loaderField.set(factory, loaderMock);

        // The public getSchema method will invoke the protected getMappedSchema.
        assertThrows(JsonSchemaException.class,
                () -> factory.getSchema("urn:test"),
                "A JsonSchemaException should be thrown when the InputStream is null");
    }
}
```