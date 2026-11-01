```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_3_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema for the supplied version flag.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        mapper = new ObjectMapper();
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
     * Verify that {@code getSchema(JsonNode)} returns a non‑null {@link JsonSchema}
     * instance when a valid schema node is supplied.
     */
    @Test
    void getSchema_validJsonNode_returnsJsonSchema() throws Exception {
        // a minimal JSON‑Schema that accepts any string
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = mapper.readTree(schemaJson);

        JsonSchema jsonSchema = factory.getSchema(schemaNode);

        assertNotNull(jsonSchema, "The factory should return a non‑null JsonSchema");
        // The returned schema should be able to validate a matching instance.
        JsonNode instance = mapper.readTree("\"hello\"");
        assertTrue(jsonSchema.validate(instance).isEmpty(),
                "A valid instance should produce no validation errors");
    }

    /**
     * Verify that a {@code null} argument is rejected with a {@link NullPointerException}.
     */
    @Test
    void getSchema_nullJsonNode_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> factory.getSchema((JsonNode) null));
    }
}
```