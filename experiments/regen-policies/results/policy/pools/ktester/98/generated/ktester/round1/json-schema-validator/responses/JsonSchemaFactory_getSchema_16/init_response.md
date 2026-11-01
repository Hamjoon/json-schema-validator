```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_16_Test {

    /** Shared factory used for all tests */
    private static JsonSchemaFactory factory;

    /** ObjectMapper to create JsonNode instances from JSON strings */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default factory for Draft‑2020‑12 – it is fully functional for the
        // simple schema we are going to use in the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test clean‑up required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release; the factory does not hold external resources that need
        // explicit shutdown for this test suite.
    }

    @Test
    void getSchema_withUriJsonNodeAndConfig_returnsJsonSchema() throws Exception {
        // GIVEN – a simple JSON schema, a URI identifying it and a default validators config
        URI schemaUri = URI.create("http://example.com/simple-schema");
        JsonNode schemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // WHEN – the factory creates a JsonSchema from the supplied arguments
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode, config);

        // THEN – a non‑null JsonSchema instance is returned and it contains the original node
        assertNotNull(jsonSchema, "The factory should return a non‑null JsonSchema");
        // The JsonSchema implementation exposes the original node via getSchemaNode()
        // (this is part of the public API of JsonSchema).
        assertEquals(schemaNode, jsonSchema.getSchemaNode(),
                "The JsonSchema should retain the original JsonNode supplied to the factory");
    }
}
```