```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    /*** Test data ***/
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"string\" }";

    /*** Objects used in the test ***/
    private ObjectMapper objectMapper;
    private JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the ObjectMapper and obtain a factory instance using the public API.
        objectMapper = new ObjectMapper();
        // Use the default factory for a known draft (V4 is always available in the library).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void shouldCreateJsonSchemaFromJsonNode() throws Exception {
        // given: a JsonNode representing a minimal JSON‑Schema document
        JsonNode schemaNode = objectMapper.readTree(SIMPLE_SCHEMA);

        // when: the factory creates a JsonSchema from that node
        JsonSchema jsonSchema = factory.getSchema(schemaNode);

        // then: a non‑null JsonSchema instance is returned
        assertNotNull(jsonSchema, "JsonSchema should not be null");
        // The concrete type returned by the library is JsonSchemaImpl (or a subclass);
        // we only need to verify that it implements the JsonSchema interface.
        assertTrue(jsonSchema instanceof JsonSchema, "Returned object must implement JsonSchema");
    }
}
```