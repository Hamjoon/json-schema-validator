```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getSchema(URI, JsonNode)}.
 *
 * The tests use a real {@link JsonSchemaFactory} instance created via the
 * public {@code getInstance} factory method.  No private members of the target
 * class are accessed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_12_Test {

    /** Factory used for all tests. */
    private static JsonSchemaFactory factory;

    /** Jackson mapper used to create {@link JsonNode} instances. */
    private static ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version – the concrete version is not important for the
        // behaviour we are testing (creation of a JsonSchema from a URI + JsonNode).
        mapper = new ObjectMapper();
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
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
     * Verifies that {@code getSchema(URI, JsonNode)} returns a non‑null
     * {@link JsonSchema} whose location matches the supplied {@link URI}.
     */
    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN – a simple JSON schema and a URI that identifies it
        String schemaJson = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;
        JsonNode schemaNode = mapper.readTree(schemaJson);
        URI schemaUri = new URI("http://example.com/my-schema.json");

        // WHEN – the factory creates a JsonSchema from the URI and the JsonNode
        JsonSchema jsonSchema = factory.getSchema(schemaUri, schemaNode);

        // THEN – the result is a valid JsonSchema instance
        assertNotNull(jsonSchema, "The factory should return a non‑null JsonSchema");
        // The location stored inside the JsonSchema must be the same as the supplied URI
        assertEquals(schemaUri.toString(),
                     jsonSchema.getSchemaLocation().toString(),
                     "Schema location should match the supplied URI");
    }
}
```