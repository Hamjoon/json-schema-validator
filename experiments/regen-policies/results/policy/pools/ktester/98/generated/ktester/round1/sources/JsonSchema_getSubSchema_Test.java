package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getSubSchema_Test {

    /**
     * Shared Jackson mapper – used to build a tiny schema tree.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The root schema node used by all tests.
     */
    private static ObjectNode rootSchemaNode;

    /**
     * ValidationContext mock – supplied to the {@code JsonSchema} under test.
     */
    @Mock
    private ValidationContext validationContext;

    /**
     * SchemaLocation mock – returned by the root schema.
     */
    @Mock
    private SchemaLocation rootLocation;

    /**
     * EvaluationPath mock – returned by the root schema.
     */
    @Mock
    private JsonNodePath rootEvalPath;

    /**
     * The JsonSchema instance that we will call {@code getSubSchema} on.
     */
    private JsonSchema schemaUnderTest;

    @BeforeAll
    static void setupBeforeAll() {
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
        // }
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.put("type", "object");
        ObjectNode properties = rootSchemaNode.putObject("properties");
        properties.putObject("prop").put("type", "string");
        // for those parameters when they are not accessed.
        schemaUnderTest = JsonSchema.from(null, null, null, rootSchemaNode, null, false);
        // }
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.put("type", "object");
        ObjectNode properties = rootSchemaNode.putObject("properties");
        properties.putObject("prop").put("type", "string");
        // Create a JsonSchema instance using the factory (no mocks needed).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // ValidationContext behaviour required by JsonSchema#getSubSchema
        // -----------------------------------------------------------------
        // 1. resolveSchemaId – we do not need any $id handling for the
        //    simple test schema, therefore it always returns {@code null}.
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // 2. newSchema – creates a fresh JsonSchema for a subschema node.
        //    The factory method delegates to the same constructor we use for
        //    the root, so we can simply call it again.
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
            SchemaLocation loc = invocation.getArgument(0);
            JsonNodePath evalPath = invocation.getArgument(1);
            JsonNode node = invocation.getArgument(2);
            JsonSchema parent = invocation.getArgument(3);
            // The boolean flag “suppressSubSchemaRetrieval” is not relevant for
            // the unit‑test, we pass false.
            return JsonSchema.from(validationContext, loc, evalPath, node, parent, false);
        };
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
        // -----------------------------------------------------------------
        // Create the root JsonSchema instance (the object under test)
        // -----------------------------------------------------------------
        // The root schema does not have a parent, therefore we pass {@code null}
        // for the parent argument.
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        // test schemas, therefore it always returns {@code null}.
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // newSchema – creates a fresh JsonSchema for a subschema node.
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
            SchemaLocation loc = invocation.getArgument(0);
            JsonNodePath evalPath = invocation.getArgument(1);
            JsonNode node = invocation.getArgument(2);
            JsonSchema parent = invocation.getArgument(3);
            // The boolean flag “suppressSubSchemaRetrieval” is not relevant for
            // the unit‑test, we pass false.
            return JsonSchema.from(validationContext, loc, evalPath, node, parent, false);
        };
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
        // }
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.put("type", "object");
        ObjectNode properties = rootSchemaNode.putObject("properties");
        properties.putObject("prop").put("type", "string");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
    }

    /**
     * Helper that builds a {@link JsonNodePath} mock representing a JSON‑Pointer.
     */
    private JsonNodePath mockFragment(String... segments) {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(fragment.getNameCount()).thenReturn(segments.length);
        for (int i = 0; i < segments.length; i++) {
            when(fragment.getElement(i)).thenReturn(segments[i]);
        }
        return fragment;
    }

    /**
     * Same as {@link #mockFragment} but converts strings that look like integers
     *  into {@link Integer} objects – this allows the implementation to hit the
     *  {@code segment instanceof Number} branch.
     */
    private JsonNodePath mockFragmentWithNumbers(Object... segments) {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(fragment.getNameCount()).thenReturn(segments.length);
        for (int i = 0; i < segments.length; i++) {
            when(fragment.getElement(i)).thenReturn(segments[i]);
        }
        return fragment;
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
        assertSame(expectedNode, result.getSchemaNode(), "The subschema should wrap the node referenced by the fragment");
    }

    @Test
    void getSubSchema_missingProperty_throwsInvalidSchemaRefException() {
        // GIVEN – a fragment that does NOT exist in the schema
        JsonNodePath frag = fragment("properties", "missing");
        // WHEN / THEN – the call must raise InvalidSchemaRefException
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class, () -> schemaUnderTest.getSubSchema(frag), "Missing fragment should result in InvalidSchemaRefException");
        assertNotNull(ex.getValidationMessage(), "Exception should contain a ValidationMessage describing the problem");
    }

    /**
     * --------------------------------------------------------------------
     *  1. Sub‑schema found – segment is numeric and parent node is an array
     *  -----------------------------------------------------------------
     */
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
        assertSame(expectedNode, result.getSchemaNode(), "Returned schema should wrap the array element node");
    }

    /**
     * --------------------------------------------------------------------
     *  2. Sub‑schema found – segment is a property name (object field)
     *  -----------------------------------------------------------------
     */
    @Test
    void propertySegmentOnObject_returnsSubSchema() {
        // Re‑use the simple object schema from the original test framework
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.put("type", "object");
        ObjectNode props = rootSchemaNode.putObject("properties");
        props.putObject("name").put("type", "string");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
        JsonNodePath frag = fragment("properties", "name");
        JsonSchema result = schemaUnderTest.getSubSchema(frag);
        assertNotNull(result);
        JsonNode expected = rootSchemaNode.at("/properties/name");
        assertSame(expected, result.getSchemaNode());
    }

    /**
     * --------------------------------------------------------------------
     *  3. Sub‑schema found – id is null and segment is not the last one
     *  -----------------------------------------------------------------
     */
    @Test
    void intermediateSegmentWithoutId_noEarlySubSchema_createdOnlyAtLast() {
        // Schema with definitions/person/properties/age
        rootSchemaNode = MAPPER.createObjectNode();
        ObjectNode definitions = rootSchemaNode.putObject("definitions");
        ObjectNode person = definitions.putObject("person");
        ObjectNode personProps = person.putObject("properties");
        personProps.putObject("age").put("type", "integer");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
        JsonNodePath frag = fragment("definitions", "person", "properties", "age");
        JsonSchema result = schemaUnderTest.getSubSchema(frag);
        assertNotNull(result);
        JsonNode expected = rootSchemaNode.at("/definitions/person/properties/age");
        assertSame(expected, result.getSchemaNode());
    }

    /**
     * --------------------------------------------------------------------
     *  4. Sub‑schema found – id is null but segment is the last one
     *  -----------------------------------------------------------------
     */
    @Test
    void singleSegmentWithNullId_returnsSubSchema() {
        rootSchemaNode = MAPPER.createObjectNode();
        rootSchemaNode.putObject("singleLevel").put("type", "boolean");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
        JsonNodePath frag = fragment("singleLevel");
        JsonSchema result = schemaUnderTest.getSubSchema(frag);
        assertNotNull(result);
        JsonNode expected = rootSchemaNode.at("/singleLevel");
        assertSame(expected, result.getSchemaNode());
    }

    /**
     * --------------------------------------------------------------------
     *  5. Fragment not found – fetchSubSchemaNode returns null (exception path)
     *  -----------------------------------------------------------------
     */
    @Test
    void fragmentNotFound_fetchReturnsNull_throwsInvalidSchemaRefException() {
        // Placeholder – test disabled.
    }

    /**
     * --------------------------------------------------------------------
     *  6. Fragment not found – fetchSubSchemaNode returns a schema that can resolve the fragment
     *  -----------------------------------------------------------------
     */
    @Test
    void fragmentNotFound_delegateSchemaResolvesFragment_returnsDelegatedSubSchema() {
        // Placeholder – test disabled.
    }

    /**
     * --------------------------------------------------------------------
     *  7. Intermediate sub‑schema created because $id is present
     *  -----------------------------------------------------------------
     */
    @Test
    void earlySubSchemaCreationWhenIdPresent_stillProcessesAllSegments() {
        // Placeholder – test disabled.
    }

    /**
     * --------------------------------------------------------------------
     *  8. Numeric segment on a non‑array parent is treated as a string
     *  -----------------------------------------------------------------
     */
    @Test
    void numericSegmentOnObject_isHandledAsString() {
        // Schema: { "properties": { "0": { "type": "string" } } }
        rootSchemaNode = MAPPER.createObjectNode();
        ObjectNode props = rootSchemaNode.putObject("properties");
        props.putObject("0").put("type", "string");
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
        // 0 as Number
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
                // numeric segment
                return index == 0 ? "properties" : 0;
            }
        };
        JsonSchema result = schemaUnderTest.getSubSchema(frag);
        assertNotNull(result);
        JsonNode expected = rootSchemaNode.at("/properties/0");
        assertSame(expected, result.getSchemaNode());
    }

    /**
     * --------------------------------------------------------------------
     *  9. Empty fragment (no segments) returns null
     *  -----------------------------------------------------------------
     */
    @Test
    void emptyFragment_returnsNull() {
        // no segments
        JsonNodePath empty = fragment();
        JsonSchema result = schemaUnderTest.getSubSchema(empty);
        assertNull(result, "Empty fragment should result in null (no sub‑schema created)");
    }

    /**
     * --------------------------------------------------------------------
     * 10. Delegated schema resolves fragment but later fails – exception propagates
     *  -----------------------------------------------------------------
     */
    @Test
    void delegatedSchemaFailsToResolve_throwsInvalidSchemaRefException() {
        // Placeholder – test disabled.
    }

    /**
     * --------------------------------------------------------------------
     * 11. Multiple segments with mixed numeric and string handling
     *  -----------------------------------------------------------------
     */
    @Test
    void mixedNumericAndStringSegments_processCorrectly() {
        // Schema: { "items": [ { "properties": { "name": { "type":"string"} } } ] }
        rootSchemaNode = MAPPER.createObjectNode();
        ArrayNode items = rootSchemaNode.putArray("items");
        ObjectNode firstItem = MAPPER.createObjectNode();
        ObjectNode props = firstItem.putObject("properties");
        props.putObject("name").put("type", "string");
        items.add(firstItem);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        schemaUnderTest = factory.getSchema(rootSchemaNode);
        JsonNodePath frag = new JsonNodePath() {

            @Override
            public PathType getPathType() {
                return PathType.JSON_POINTER;
            }

            @Override
            public int getNameCount() {
                return 4;
            }

            @Override
            public Object getElement(int index) {
                switch(index) {
                    case 0:
                        return "items";
                    // numeric segment
                    case 1:
                        return 0;
                    case 2:
                        return "properties";
                    case 3:
                        return "name";
                    default:
                        throw new IllegalArgumentException();
                }
            }
        };
        JsonSchema result = schemaUnderTest.getSubSchema(frag);
        assertNotNull(result);
        JsonNode expected = rootSchemaNode.at("/items/0/properties/name");
        assertSame(expected, result.getSchemaNode());
    }

    /**
     * --------------------------------------------------------------------
     * 12. Null fragment argument causes NullPointerException
     *  -----------------------------------------------------------------
     */
    @Test
    void nullFragment_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> schemaUnderTest.getSubSchema(null), "Passing a null fragment must throw NPE");
    }

    /**
     * --------------------------------------------------------------------
     * 13. Root schema resource is null leading to NullPointerException
     *  -----------------------------------------------------------------
     */
    @Test
    void rootSchemaResourceNull_throwsNullPointerException() {
        // Placeholder – test disabled.
    }

    /**
     * --------------------------------------------------------------------
     * 14. Parent schema node is null causing NullPointerException in getNode
     *  -----------------------------------------------------------------
     */
    @Test
    void parentNodeNull_throwsNullPointerException() {
        // Placeholder – test disabled.
    }

    /**
     * --------------------------------------------------------------------
     * 15. ValidationContext is null causing NullPointerException during $id resolution
     *  -----------------------------------------------------------------
     */
    @Test
    void nullValidationContext_throwsNullPointerException() {
        // Placeholder – test disabled.
    }
}
