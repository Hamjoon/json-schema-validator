```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.util.Collections;

/**
 * Unit tests for {@link JsonSchema#validateAndCollect(JsonNode)}.
 *
 * The original test tried to instantiate {@link JsonSchema} via reflection
 * passing {@code null} for {@link ValidationContext}.  The constructor of
 * {@code JsonSchema} dereferences the context (it calls
 * {@code validationContext.getMetaSchema()}), which caused a
 * {@link NullPointerException} during test setup.
 *
 * The fix is to create a real {@code JsonSchema} instance using the public
 * {@link JsonSchemaFactory}.  This supplies a proper {@code ValidationContext}
 * and allows the tests to exercise the real validation logic.
 */
class JsonSchema_validateAndCollect_2_Test {

    /** Jackson helper to create {@link JsonNode} from a JSON literal. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Instance of {@link JsonSchema} created via {@link JsonSchemaFactory}. */
    private JsonSchema jsonSchema;

    /**
     * Creates an empty schema (i.e. a schema without any validation keywords)
     * using the default factory.  An empty schema is sufficient for the
     * validation‑path tests in this class.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The factory builds a proper ValidationContext internally.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        // An empty JSON object represents an empty schema.
        JsonNode emptySchemaNode = MAPPER.createObjectNode();
        jsonSchema = factory.getSchema(emptySchemaNode);
    }

    /**
     * Helper that parses a JSON literal (or returns {@code null} when the
     * literal is the string {@code "null"}).
     */
    private JsonNode json(String literal) {
        if ("null".equals(literal)) {
            return null;
        }
        try {
            return MAPPER.readTree(literal);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON literal used in test: " + literal, e);
        }
    }

    @Test
    void testValidateAndCollectWithValidNode() {
        JsonNode node = json("{\"name\":\"John\",\"age\":30}");
        ValidationResult result = Assertions.assertDoesNotThrow(
                () -> jsonSchema.validateAndCollect(node),
                "validateAndCollect should not throw for a valid JSON node");
        Assertions.assertNotNull(result, "Result must not be null");
    }

    @Test
    void testValidateAndCollectWithNullNode() {
        ValidationResult result = Assertions.assertDoesNotThrow(
                () -> jsonSchema.validateAndCollect(null),
                "validateAndCollect should handle a null node without throwing");
        Assertions.assertNotNull(result, "Result must not be null even for a null node");
    }

    @Test
    void testValidateAndCollectWithEmptyObject() {
        JsonNode node = json("{}");
        ValidationResult result = jsonSchema.validateAndCollect(node);
        Assertions.assertNotNull(result, "Result must not be null for empty object");
    }

    @Test
    void testValidateAndCollectWithArray() {
        JsonNode node = json("[1,2,3]");
        ValidationResult result = jsonSchema.validateAndCollect(node);
        Assertions.assertNotNull(result, "Result must not be null for JSON array");
    }
}
```