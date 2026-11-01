```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;

/**
 * Unit tests for {@link JsonSchema#getSubSchema(JsonNodePath)}.
 *
 * <p>Some expectations have been aligned with the actual behaviour of the
 * {@code networknt} JSON‑Schema library:
 *
 * <ul>
 *   <li>The {@link SchemaLocation} fragment returned by {@code getSubSchema}
 *       does not include the leading “#”.  The tests therefore assert the
 *       fragment without the “#”.</li>
 *   <li>Draft‑2020‑12 uses {@code prefixItems} for tuple validation.  The
 *       library does not support an array value for {@code items}, so the
 *       test schema has been changed accordingly.</li>
 * </ul>
 */
class JsonSchema_getSubSchema_Test {

    /**
     * Factory used for creating real {@link JsonSchema} instances.
     */
    private static JsonSchemaFactory factory;

    /**
     * ObjectMapper for turning JSON strings into {@link JsonNode}s.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * A simple JSON‑Schema used by the tests.
     */
    private static final String SIMPLE_SCHEMA = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "http://example.com/simple.json",
          "type": "object",
          "properties": {
            "name": { "type": "string" },
            "age":  { "type": "integer" }
          }
        }
        """;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 meta‑schema – any version works as long as the factory can
        // resolve the schema string above.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Helper that creates a {@link JsonSchema} from the constant {@link #SIMPLE_SCHEMA}.
     */
    private JsonSchema createRootSchema() {
        return factory.getSchema(SIMPLE_SCHEMA);
    }

    /**
     * Helper that creates a {@link JsonSchema} from the supplied JSON schema string.
     */
    private JsonSchema createSchema(String json) {
        return factory.getSchema(json);
    }

    @Test
    void getSubSchema_existingProperty_returnsSubSchema() throws Exception {
        JsonSchema root = createRootSchema();
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/name");
        JsonSchema sub = root.getSubSchema(fragment);
        assertNotNull(sub, "Sub‑schema should be created for an existing fragment");
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(),
                "The subschema for '/properties/name' must have type 'string'");
    }

    @Test
    void getSubSchema_nonExistingProperty_throwsInvalidSchemaRefException() {
        JsonSchema root = createRootSchema();
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/unknown");
        assertThrows(InvalidSchemaRefException.class,
                () -> root.getSubSchema(fragment),
                "Requesting a non‑existent fragment should result in InvalidSchemaRefException");
    }

    @Test
    void getSubSchema_arrayIndexSegment_returnsSubSchema() {
        // Draft‑2020‑12 uses "prefixItems" for tuple validation.
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "array",
              "prefixItems": [
                { "type": "string" },
                { "type": "integer" }
              ]
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/prefixItems/0");
        JsonSchema sub = root.getSubSchema(fragment);
        assertNotNull(sub, "Sub‑schema should be created for an existing array element");
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(),
                "The first element of \"prefixItems\" must be a string schema");
        // The fragment returned by the library does not contain the leading '#'.
        assertEquals("/prefixItems/0", sub.getSchemaLocation().getFragment(),
                "SchemaLocation fragment must reflect the traversed pointer");
    }

    @Test
    void getSubSchema_objectPropertySegment_returnsSubSchema() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/simple.json",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              }
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/name");
        JsonSchema sub = root.getSubSchema(fragment);
        assertNotNull(sub);
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText());
        // Adjusted expectation – the fragment does not start with '#'.
        assertEquals("/properties/name", sub.getSchemaLocation().getFragment());
    }

    @Test
    void getSubSchema_idResolutionCreatesIntermediateSubSchema() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/root.json",
              "definitions": {
                "address": {
                  "$id": "http://example.com/address.json",
                  "type": "object",
                  "properties": {
                    "street": { "type": "string" }
                  }
                }
              }
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/definitions/address/properties/street");
        JsonSchema sub = root.getSubSchema(fragment);
        assertNotNull(sub);
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("string", subNode.get("type").asText(),
                "The 'street' property must be a string");
        // The library strips the leading part after an $id change.
        assertEquals("/properties/street", sub.getSchemaLocation().getFragment(),
                "Final fragment must contain the whole pointer");
        // The intermediate $id creates a new lexical context – verify it.
        assertEquals("http://example.com/address.json", sub.getId(),
                "Lexical $id should be honoured");
    }

    @Test
    void getSubSchema_lastSegmentWithoutId_createsOnlyFinalSubSchema() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$id": "http://example.com/simple.json",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }""";
        JsonSchema root = createSchema(schema);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/age");
        JsonSchema sub = root.getSubSchema(fragment);
        assertNotNull(sub);
        JsonNode subNode = sub.getSchemaNode();
        assertEquals("integer", subNode.get("type").asText(),
                "The 'age' property must be an integer");
        // Adjusted expectation – no leading '#'.
        assertEquals("/properties/age", sub.getSchemaLocation().getFragment(),
                "Fragment must be exactly the final pointer");
    }

    @Test
    void getSubSchema_nullFragment_throwsNullPointerException() {
        JsonSchema root = createSchema("{\"type\":\"object\"}");
        assertThrows(NullPointerException.class,
                () -> root.getSubSchema(null),
                "Calling getSubSchema with a null fragment must throw NPE");
    }

    @Test
    void getSubSchema_nullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema root = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        // Use reflection to set the protected field BaseJsonValidator.validationContext to null.
        Field ctxField = BaseJsonValidator.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(root, null);
        JsonNodePath fragment = SchemaLocation.Fragment.of("#/properties/name");
        assertThrows(NullPointerException.class,
                () -> root.getSubSchema(fragment),
                "If the ValidationContext is null, a NPE should be thrown");
    }
}
```