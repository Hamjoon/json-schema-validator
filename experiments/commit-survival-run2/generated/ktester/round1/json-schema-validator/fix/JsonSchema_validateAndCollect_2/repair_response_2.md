```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.util.Collections;

/**
 * Tests for {@link JsonSchema#validateAndCollect(JsonNode)} that do not require any
 * external mocking framework. The tests focus on the basic contract of the method:
 * it should not throw, should return a non‑null {@link ValidationResult},
 * and on successful validation the message set must be empty.
 *
 * More elaborate behaviour (delegation, dynamic anchors, keyword walk listeners, etc.)
 * is exercised in other test classes that make use of Mockito.
 */
class JsonSchema_validateAndCollect_2_Test {

    /** Jackson helper to create {@link JsonNode} instances from JSON literals. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Instance of {@link JsonSchema} created via reflection (the constructor is package‑private). */
    private JsonSchema jsonSchema;

    /** Constants used for constructing the {@link JsonSchema} instance. */
    private static final String SCHEMA_PATH = "#";
    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");
    private static final boolean SUPPRESS_SUB_SCHEMA_RETRIEVAL = false;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // The JsonSchema constructor is package‑private; use reflection to instantiate it.
        java.lang.reflect.Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class, String.class, URI.class, JsonNode.class,
                JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // No parent schema and an empty schema node (null) are sufficient for these tests.
        jsonSchema = ctor.newInstance(
                null,                     // ValidationContext – not needed for these tests
                SCHEMA_PATH,
                CURRENT_URI,
                null,                     // schemaNode – empty schema
                null,                     // parentSchema
                SUPPRESS_SUB_SCHEMA_RETRIEVAL);
    }

    /**
     * Parses a JSON literal (or returns {@code null} when the literal is the string "null").
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
    void testValidateAndCollectReturnsEmptyMessagesOnSuccess() {
        JsonNode node = json("{\"foo\":\"bar\"}");
        ValidationResult result = jsonSchema.validateAndCollect(node);
        Assertions.assertNotNull(result, "Result must not be null");
        // The library guarantees that a successful validation yields an empty message set.
        Assertions.assertTrue(result.isSuccess(), "Result should indicate success");
        Assertions.assertNotNull(result.getMessages(), "Message collection should be non‑null");
        Assertions.assertTrue(result.getMessages().isEmpty(), "Message collection should be empty on success");
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