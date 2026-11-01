package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.node.ArrayNode;

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

    @Test
    void getSubSchema_existingProperty_returnsSubSchema() {
        // ---------------------------------------------------------------
        // GIVEN – a fragment that points to the existing property “prop”
        // ---------------------------------------------------------------
        JsonNodePath fragment = mockFragment("properties", "prop");
        // ---------------------------------------------------------------
        // WHEN – we ask the root schema for the subschema
        // ---------------------------------------------------------------
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
        // ---------------------------------------------------------------
        // THEN – a non‑null JsonSchema is returned and its node matches
        //        the expected child node.
        // ---------------------------------------------------------------
        assertNotNull(result, "Resulting subschema must not be null");
        JsonNode expectedNode = rootSchemaNode.at("/properties/prop");
        assertSame(expectedNode, result.getSchemaNode(), "The subschema should wrap the node referenced by the fragment");
    }

    @Test
    void getSubSchema_missingProperty_throwsInvalidSchemaRefException() {
        // ---------------------------------------------------------------
        // GIVEN – a fragment that does NOT exist in the schema
        // ---------------------------------------------------------------
        JsonNodePath fragment = mockFragment("properties", "missing");
        // The root schema’s document root (itself) will be used as the
        // “document” variable inside getSubSchema.  When the fragment cannot be
        // resolved we need the fallback path to return {@code null} so that the
        // method throws the expected exception.
        JsonSchema documentRoot = spy(schemaUnderTest);
        // findSchemaResourceRoot() is used to obtain the document root.
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        // fetchSubSchemaNode() is the second‑level lookup – we make it return null.
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        // ---------------------------------------------------------------
        // WHEN / THEN – the call must raise InvalidSchemaRefException
        // ---------------------------------------------------------------
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class, () -> schemaUnderTest.getSubSchema(fragment), "Missing fragment should result in InvalidSchemaRefException");
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
        rootSchemaNode = MAPPER.createObjectNode();
        ArrayNode itemsArray = rootSchemaNode.putArray("items");
        ObjectNode arrayItem = MAPPER.createObjectNode();
        arrayItem.put("type", "string");
        itemsArray.add(arrayItem);
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        // Fragment /items/0  -> ["items", 0]
        JsonNodePath fragment = mockFragmentWithNumbers("items", 0);
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
        assertNotNull(result, "Result must not be null");
        JsonNode expectedNode = rootSchemaNode.at("/items/0");
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
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("properties", "name");
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
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
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("definitions", "person", "properties", "age");
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
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
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("singleLevel");
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
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
        rootSchemaNode = MAPPER.createObjectNode();
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("nonexistent");
        // make findSchemaResourceRoot() return the root itself (default)
        JsonSchema documentRoot = spy(schemaUnderTest);
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        // fetchSubSchemaNode returns null → exception path
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class, () -> schemaUnderTest.getSubSchema(fragment));
        assertEquals(ValidatorTypeCode.REF.getValue(), ex.getValidationMessage().getType(), "ValidationMessage type must be REF");
    }

    /**
     * --------------------------------------------------------------------
     *  6. Fragment not found – fetchSubSchemaNode returns a schema that can resolve the fragment
     *  -----------------------------------------------------------------
     */
    @Test
    void fragmentNotFound_delegateSchemaResolvesFragment_returnsDelegatedSubSchema() {
        // Root schema is empty – it cannot resolve the fragment.
        rootSchemaNode = MAPPER.createObjectNode();
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("externalRef");
        // Mock the document root and the delegated schema.
        JsonSchema documentRoot = spy(schemaUnderTest);
        JsonSchema delegated = mock(JsonSchema.class);
        JsonSchema leaf = mock(JsonSchema.class);
        when(delegated.getSubSchema(fragment)).thenReturn(leaf);
        // documentRoot.fetchSubSchemaNode returns the delegated schema.
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        doReturn(delegated).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
        assertSame(leaf, result, "Result should be the leaf schema returned by the delegated schema");
    }

    /**
     * --------------------------------------------------------------------
     *  7. Intermediate sub‑schema created because $id is present
     *  -----------------------------------------------------------------
     */
    @Test
    void earlySubSchemaCreationWhenIdPresent_stillProcessesAllSegments() {
        // Build a schema with $id on the first segment.
        rootSchemaNode = MAPPER.createObjectNode();
        ObjectNode components = rootSchemaNode.putObject("components");
        components.put("$id", "http://example.com/components");
        ObjectNode schemas = components.putObject("schemas");
        ObjectNode user = schemas.putObject("User");
        user.put("$id", "http://example.com/User");
        ObjectNode props = user.putObject("properties");
        props.putObject("id").put("type", "string");
        // make resolveSchemaId return a non‑null value for nodes that contain $id
        when(validationContext.resolveSchemaId(argThat(node -> node.has("$id")))).thenReturn("someId");
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("components", "schemas", "User", "properties", "id");
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
        assertNotNull(result);
        JsonNode expected = rootSchemaNode.at("/components/schemas/User/properties/id");
        assertSame(expected, result.getSchemaNode());
        // Verify that newSchema was called three times (components, schemas, User)
        verify(validationContext, times(3)).newSchema(any(), any(), any(), any());
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
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        // 0 as Number
        JsonNodePath fragment = mockFragmentWithNumbers("properties", 0);
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
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
        rootSchemaNode = MAPPER.createObjectNode();
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        // zero segments
        JsonNodePath fragment = mockFragment();
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
        assertNull(result, "Empty fragment should result in null (no sub‑schema created)");
    }

    /**
     * --------------------------------------------------------------------
     * 10. Delegated schema resolves fragment but later fails – exception propagates
     *  -----------------------------------------------------------------
     */
    @Test
    void delegatedSchemaFailsToResolve_throwsInvalidSchemaRefException() {
        rootSchemaNode = MAPPER.createObjectNode();
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("external", "invalid");
        // Mock delegated schema that throws when asked to resolve the fragment.
        JsonSchema delegated = mock(JsonSchema.class);
        when(delegated.getSubSchema(fragment)).thenThrow(new InvalidSchemaRefException(ValidationMessage.builder().type(ValidatorTypeCode.REF.getValue()).code("internal.unresolvedRef").message("{0}: Reference {1} cannot be resolved").instanceLocation(null).schemaLocation(null).evaluationPath(null).arguments(fragment).build()));
        JsonSchema documentRoot = spy(schemaUnderTest);
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        doReturn(delegated).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        InvalidSchemaRefException ex = assertThrows(InvalidSchemaRefException.class, () -> schemaUnderTest.getSubSchema(fragment));
        assertEquals(ValidatorTypeCode.REF.getValue(), ex.getValidationMessage().getType());
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
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragmentWithNumbers("items", 0, "properties", "name");
        JsonSchema result = schemaUnderTest.getSubSchema(fragment);
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
        rootSchemaNode = MAPPER.createObjectNode();
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        assertThrows(NullPointerException.class, () -> schemaUnderTest.getSubSchema(null));
    }

    /**
     * --------------------------------------------------------------------
     * 13. Root schema resource is null leading to NullPointerException
     *  -----------------------------------------------------------------
     */
    @Test
    void rootSchemaResourceNull_throwsNullPointerException() {
        rootSchemaNode = MAPPER.createObjectNode();
        schemaUnderTest = JsonSchema.from(validationContext, rootLocation, rootEvalPath, rootSchemaNode, null, false);
        JsonNodePath fragment = mockFragment("any");
        // Stub findSchemaResourceRoot to return null.
        doReturn(null).when(schemaUnderTest).findSchemaResourceRoot();
        assertThrows(NullPointerException.class, () -> schemaUnderTest.getSubSchema(fragment));
    }

    /**
     * --------------------------------------------------------------------
     * 14. Parent schema node is null causing NullPointerException in getNode
     *  -----------------------------------------------------------------
     */
    @Test
    void parentNodeNull_throwsNullPointerException() {
        // Build a JsonSchema whose getSchemaNode() returns null.
        JsonSchema broken = spy(JsonSchema.from(validationContext, rootLocation, rootEvalPath, null, null, false));
        // Ensure findSchemaResourceRoot returns itself (default behaviour)
        doReturn(broken).when(broken).findSchemaResourceRoot();
        JsonNodePath fragment = mockFragment("missingNode");
        assertThrows(NullPointerException.class, () -> broken.getSubSchema(fragment));
    }

    /**
     * --------------------------------------------------------------------
     * 15. ValidationContext is null causing NullPointerException during $id resolution
     *  -----------------------------------------------------------------
     */
    @Test
    void nullValidationContext_throwsNullPointerException() {
        // Create a JsonSchema with a null ValidationContext via reflection.
        JsonSchema schema = JsonSchema.from(null, rootLocation, rootEvalPath, MAPPER.createObjectNode(), null, false);
        JsonNodePath fragment = mockFragment("any");
        // The method will call parent.getValidationContext() which returns null.
        assertThrows(NullPointerException.class, () -> schema.getSubSchema(fragment));
    }
}
