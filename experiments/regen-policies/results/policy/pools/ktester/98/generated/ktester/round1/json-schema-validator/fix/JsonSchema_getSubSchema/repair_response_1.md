```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Simplified tests for {@link JsonSchema#getSubSchema(JsonNodePath)}.
 *
 * <p>These tests avoid the use of external mocking libraries and instead rely on
 * real {@link JsonSchema} instances created via {@link JsonSchemaFactory}.
 * Only a subset of the original test scenarios is retained – enough to verify
 * the core behaviour of {@code getSubSchema} while keeping the test code
 * self‑contained and compilable.</p>
 */
class JsonSchema_getSubSchema_Test {

    /** Shared Jackson mapper – used to build tiny schema trees. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Root schema node used by the tests. */
    private static ObjectNode rootSchemaNode;

    /** JsonSchema instance created from {@code rootSchemaNode}. */
    private static JsonSchema schemaUnderTest;

    @BeforeAll
    static void setup() {
        // Build a minimal schema:
        // {
        //   "type": "object",
        //   "properties": {
        //       "prop": { "type": "string" }
        //   }
        // }
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.put("type", "object");
        ObjectNode properties = rootSchemaNode.putObject("properties");
        properties.putObject("prop").put("type", "string");

        // Create a JsonSchema instance using the factory (no mocks needed).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
    }

    /**
     * Helper that builds a {@link JsonNodePath} representing a JSON‑Pointer.
     *
     * <p>This implementation is a tiny concrete subclass of {@code JsonNodePath}
     * that only supplies the methods required by {@code JsonSchema#getSubSchema}
     * for the tests in this file.</p>
     */
    private static JsonNodePath fragment(String... segments) {
        return new JsonNodePath() {
            @Override
            public PathType getPathType() {
                return PathType.JSON_POINTER;
            }

            @Override
            public int getNameCount() {
                return segments.length;
            }

            @Override
            public Object getElement(int index) {
                return segments[index];
            }
        };
    }

    @Test
    void getSubSchema_existingProperty_returnsSubSchema() {
        // GIVEN – a fragment that points to the existing property “prop”
        JsonNodePath frag = fragment("properties", "prop");
        // WHEN – we ask the root schema for the subschema
        JsonSchema result = schemaUnderTest.getSubSchema(frag);
        // THEN – a non‑null JsonSchema is returned and its node matches the expected child node.
        assertNotNull(result, "Resulting subschema must not be null");
        JsonNode expectedNode = rootSchemaNode.at("/properties/prop");
        assertSame(expectedNode, result.getSchemaNode(),
                "The subschema should wrap the node referenced by the fragment");
    }

    @Test
    void getSubSchema_missingProperty_throwsInvalidSchemaRefException() {
        // GIVEN – a fragment that does NOT exist in the schema
        JsonNodePath frag = fragment("properties", "missing");
        // WHEN / THEN – the call must raise InvalidSchemaRefException
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class,
                () -> schemaUnderTest.getSubSchema(frag),
                "Missing fragment should result in InvalidSchemaRefException");
        assertNotNull(ex.getValidationMessage(),
                "Exception should contain a ValidationMessage describing the problem");
    }

    @Test
    void numericSegmentOnArray_parentIsArray_returnsSubSchema() {
        // Build schema: { "items": [ { "type": "string" } ] }
        ObjectNode arrayRoot = MAPPER.createObjectNode();
        ArrayNode itemsArray = arrayRoot.putArray("items");
        ObjectNode arrayItem = MAPPER.createObjectNode();
        arrayItem.put("type", "string");
        itemsArray.add(arrayItem);

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema arraySchema = factory.getSchema(arrayRoot);

        // Fragment /items/0  -> ["items", 0]
        JsonNodePath frag = new JsonNodePath() {
            @Override
            public PathType getPathType() {
                return PathType.JSON_POINTER;
            }

            @Override
            public int getNameCount() {
                return 2;
            }

            @Override
            public Object getElement(int index) {
                return index == 0 ? "items" : 0;
            }
        };

        JsonSchema result = arraySchema.getSubSchema(frag);
        assertNotNull(result, "Result must not be null");
        JsonNode expectedNode = arrayRoot.at("/items/0");
        assertSame(expectedNode, result.getSchemaNode(),
                "Returned schema should wrap the array element node");
    }

    @Test
    void emptyFragment_returnsNull() {
        JsonNodePath empty = fragment(); // no segments
        JsonSchema result = schemaUnderTest.getSubSchema(empty);
        assertNull(result, "Empty fragment should result in null (no sub‑schema created)");
    }

    @Test
    void nullFragment_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> schemaUnderTest.getSubSchema(null),
                "Passing a null fragment must throw NPE");
    }
}
```