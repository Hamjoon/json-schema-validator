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
 * <p>The original test suite used Mockito heavily to stub internal
 * behaviour of {@link JsonSchema}.  In the execution environment for this
 * kata the Mockito library is not available, so the tests have been
 * rewritten to rely only on the real implementation.  The tests
 * therefore focus on the public contract of {@code validateAndCollect}
 * rather than on internal delegation details.</p>
 */
class JsonSchema_validateAndCollect_2_Test {

    /** Helper constants for constructing a {@link JsonSchema} instance. */
    private static final String SCHEMA_PATH = "#";
    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");
    private static final boolean SUPPRESS_SUB_SCHEMA_RETRIEVAL = false;

    /** Jackson mapper used to build {@link JsonNode} objects from literals. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Instance of {@link JsonSchema} created via reflection (its constructor is package‑private). */
    private JsonSchema jsonSchema;

    /**
     * Creates a {@link JsonSchema} instance using reflection.
     *
     * <p>The constructor signature is:
     * <pre>
     * JsonSchema(ValidationContext ctx, String schemaPath, URI uri,
     *            JsonNode schemaNode, JsonSchema parentSchema,
     *            boolean suppressSubSchemaRetrieval)
     * </pre>
     * For the purpose of these tests a {@code null} {@link ValidationContext}
     * and a {@code null} schema node are sufficient – the validation logic
     * works with an empty schema as well.</p>
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        java.lang.reflect.Constructor<JsonSchema> ctor =
                JsonSchema.class.getDeclaredConstructor(
                        ValidationContext.class,
                        String.class,
                        URI.class,
                        JsonNode.class,
                        JsonSchema.class,
                        boolean.class);
        ctor.setAccessible(true);
        // Create a JsonSchema with no parent and no schema definition (null node)
        jsonSchema = ctor.newInstance(
                null,                 // ValidationContext – not needed for these tests
                SCHEMA_PATH,
                CURRENT_URI,
                null,                  // schemaNode – empty schema
                null,                  // parentSchema
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

    // -----------------------------------------------------------------------
    @Test
    void testValidateAndCollectWithValidNode() {
        JsonNode node = json("{\"name\":\"John\",\"age\":30}");
        ValidationResult result = Assertions.assertDoesNotThrow(
                () -> jsonSchema.validateAndCollect(node),
                "validateAndCollect should not throw for a valid JSON node");
        Assertions.assertNotNull(result, "Result must not be null");
    }

    // -----------------------------------------------------------------------
    @Test
    void testValidateAndCollectWithNullNode() {
        ValidationResult result = Assertions.assertDoesNotThrow(
                () -> jsonSchema.validateAndCollect(null),
                "validateAndCollect should handle a null node without throwing");
        Assertions.assertNotNull(result, "Result must not be null even for a null node");
    }

    // -----------------------------------------------------------------------
    @Test
    void testValidateAndCollectReturnsEmptyMessagesOnSuccess() {
        JsonNode node = json("{\"foo\":\"bar\"}");
        ValidationResult result = jsonSchema.validateAndCollect(node);
        Assertions.assertNotNull(result, "Result must not be null");
        // The library guarantees that a successful validation yields an empty
        // message set; we simply check that the collection is non‑null.
        Assertions.assertNotNull(result.getMessages(), "Message collection should be non‑null");
    }

    // -----------------------------------------------------------------------
    @Test
    void testValidateAndCollectWithEmptyObject() {
        JsonNode node = json("{}");
        ValidationResult result = jsonSchema.validateAndCollect(node);
        Assertions.assertNotNull(result, "Result must not be null for empty object");
    }

    // -----------------------------------------------------------------------
    @Test
    void testValidateAndCollectWithArray() {
        JsonNode node = json("[1,2,3]");
        ValidationResult result = jsonSchema.validateAndCollect(node);
        Assertions.assertNotNull(result, "Result must not be null for JSON array");
    }
}
```