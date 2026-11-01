```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a concrete factory instance – the static builder gives us a fully‑initialised object.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Verifies that the protected {@code newJsonSchema} method can create a {@link JsonSchema}
     * instance without throwing and that the returned object is not {@code null}.
     *
     * The method is accessed via reflection because it is {@code protected}.
     */
    @Test
    void testNewJsonSchemaCreatesSchema() throws Exception {
        // --- given ------------------------------------------------------------
        // a simple JSON schema node
        String schemaJson = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }";
        JsonNode schemaNode = objectMapper.readTree(schemaJson);

        // a schema location (the static factory method `of` is part of the library)
        SchemaLocation schemaLocation = SchemaLocation.of("http://example.com/test-schema.json");

        // default validators configuration
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        // --- when -------------------------------------------------------------
        // use reflection to call the protected method
        Method newJsonSchemaMethod = JsonSchemaFactory.class.getDeclaredMethod(
                "newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        newJsonSchemaMethod.setAccessible(true);

        JsonSchema result = (JsonSchema) newJsonSchemaMethod.invoke(factory, schemaLocation, schemaNode, config);

        // --- then -------------------------------------------------------------
        assertNotNull(result, "newJsonSchema should return a non‑null JsonSchema instance");
    }
}
```