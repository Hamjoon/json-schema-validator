```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#toString()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    private JsonSchema schema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Simple schema that will be used for all test cases.
        String schemaJson = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void toStringProducesExpectedRepresentation() throws Exception {
        // Expected node string (Jackson's default compact representation)
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        String expectedNodeString = node.toString();

        // The evaluation path for a root schema is "$"
        String expected = "\"$\" : " + expectedNodeString;

        String actual = schema.toString();

        assertEquals(expected, actual,
                "JsonSchema.toString() should combine the evaluation path and the schema node");
    }

    @Test
    void toStringContainsSchemaNodeWhenSchemaIsComplex() throws Exception {
        String complexSchemaJson = "{"
                + "\"type\":\"object\","
                + "\"properties\":{"
                + "   \"name\":{\"type\":\"string\"},"
                + "   \"age\":{\"type\":\"integer\"}"
                + "},"
                + "\"required\":[\"name\"]"
                + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema complexSchema = factory.getSchema(complexSchemaJson);

        JsonNode node = MAPPER.readTree(complexSchemaJson);
        String expected = "\"$\" : " + node.toString();

        assertEquals(expected, complexSchema.toString(),
                "toString() must reflect the full JSON representation of the schema node");
    }
}
```