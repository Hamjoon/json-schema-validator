```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link JsonSchema#toString()}.
 *
 * The test creates a {@link JsonSchema} instance via the public {@link JsonSchemaFactory}
 * (so we do not need to use the private constructor) and verifies that the
 * {@code toString()} implementation produces the expected string:
 *
 * <pre>
 * "\"\" : {\"type\":\"string\"}"
 * </pre>
 *
 * The empty quotes represent the root evaluation path – the path is empty for
 * a schema that is created directly from a JSON document.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    private static JsonSchemaFactory factory;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use the latest draft (2020‑12) – the exact version is not important for this test.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
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
        // Nothing to clean up globally.
    }

    @Test
    void toString_returnsEvaluationPathAndSchemaNode() throws Exception {
        // GIVEN – a minimal JSON schema
        String schemaJson = "{\"type\":\"string\"}";
        JsonNode schemaNode = MAPPER.readTree(schemaJson);

        // WHEN – we obtain a JsonSchema instance from the factory
        JsonSchema schema = factory.getSchema(schemaNode);

        // AND – we call the method under test
        String result = schema.toString();

        // THEN – the result must contain the root evaluation path (empty) and the
        //        compact representation of the schema node.
        // JsonNode.toString() returns a compact JSON string without spaces.
        String expected = "\"\" : " + schemaNode.toString();
        assertEquals(expected, result, "JsonSchema.toString() did not produce the expected format");
    }
}
```