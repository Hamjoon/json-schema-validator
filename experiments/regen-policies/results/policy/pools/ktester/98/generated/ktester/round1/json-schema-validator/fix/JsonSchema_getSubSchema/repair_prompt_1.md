
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.mockito.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:7: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:10: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:16: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:32: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:38: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:44: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:68: error: variable properties is already defined in method setupBeforeAll()
        ObjectNode properties = rootSchemaNode.putObject("properties");
                   ^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:71: error: non-static variable schemaUnderTest cannot be referenced from a static context
        schemaUnderTest = JsonSchema.from(null, null, null, rootSchemaNode, null, false);
        ^
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:81: error: cannot find symbol
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:85: error: cannot find symbol
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
        ^
  symbol:   class Answer
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:94: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                         ^
  symbol:   method any(Class<SchemaLocation>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:94: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                                                    ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:94: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:94: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                                                                                                  ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:102: error: cannot find symbol
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
                                               ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:104: error: cannot find symbol
        Answer<JsonSchema> newSchemaAnswer = invocation -> {
        ^
  symbol:   class Answer
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:113: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                         ^
  symbol:   method any(Class<SchemaLocation>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:113: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                                                    ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:113: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:113: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenAnswer(newSchemaAnswer);
                                                                                                                  ^
  symbol:   method any(Class<JsonSchema>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:120: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:121: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:122: error: cannot find symbol
        when(fragment.getNameCount()).thenReturn(segments.length);
        ^
  symbol:   method when(int)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:124: error: cannot find symbol
            when(fragment.getElement(i)).thenReturn(segments[i]);
            ^
  symbol:   method when(Object)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:135: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:136: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:137: error: cannot find symbol
        when(fragment.getNameCount()).thenReturn(segments.length);
        ^
  symbol:   method when(int)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:139: error: cannot find symbol
            when(fragment.getElement(i)).thenReturn(segments[i]);
            ^
  symbol:   method when(Object)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:152: error: no suitable constructor found for JsonNodePath(no arguments)
        return new JsonNodePath() {
               ^
    constructor JsonNodePath.JsonNodePath(PathType) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,String) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,int) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:193: error: cannot find symbol
        JsonSchema documentRoot = spy(schemaUnderTest);
                                  ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:195: error: cannot find symbol
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:197: error: cannot find symbol
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:197: error: cannot find symbol
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
                                                             ^
  symbol:   method any(Class<ValidationContext>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:296: error: cannot find symbol
        JsonSchema documentRoot = spy(schemaUnderTest);
                                  ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:297: error: cannot find symbol
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:299: error: cannot find symbol
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:299: error: cannot find symbol
        doReturn(null).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
                                                             ^
  symbol:   method any(Class<ValidationContext>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:316: error: cannot find symbol
        JsonSchema documentRoot = spy(schemaUnderTest);
                                  ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:317: error: cannot find symbol
        JsonSchema delegated = mock(JsonSchema.class);
                               ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:318: error: cannot find symbol
        JsonSchema leaf = mock(JsonSchema.class);
                          ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:319: error: cannot find symbol
        when(delegated.getSubSchema(fragment)).thenReturn(leaf);
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:321: error: cannot find symbol
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:322: error: cannot find symbol
        doReturn(delegated).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:322: error: cannot find symbol
        doReturn(delegated).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
                                                                  ^
  symbol:   method any(Class<ValidationContext>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:344: error: cannot find symbol
        when(validationContext.resolveSchemaId(argThat(node -> node.has("$id")))).thenReturn("someId");
                                               ^
  symbol:   method argThat((node)->no[...]$id"))
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:352: error: cannot find symbol
        verify(validationContext, times(3)).newSchema(any(), any(), any(), any());
                                  ^
  symbol:   method times(int)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:352: error: cannot find symbol
        verify(validationContext, times(3)).newSchema(any(), any(), any(), any());
                                                      ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:352: error: cannot find symbol
        verify(validationContext, times(3)).newSchema(any(), any(), any(), any());
                                                             ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:352: error: cannot find symbol
        verify(validationContext, times(3)).newSchema(any(), any(), any(), any());
                                                                    ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:352: error: cannot find symbol
        verify(validationContext, times(3)).newSchema(any(), any(), any(), any());
                                                                           ^
  symbol:   method any()
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:401: error: cannot find symbol
        JsonSchema delegated = mock(JsonSchema.class);
                               ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:402: error: cannot find symbol
        when(delegated.getSubSchema(fragment)).thenThrow(new InvalidSchemaRefException(ValidationMessage.builder().type(ValidatorTypeCode.REF.getValue()).code("internal.unresolvedRef").message("{0}: Reference {1} cannot be resolved").instanceLocation(null).schemaLocation(null).evaluationPath(null).arguments(fragment).build()));
        ^
  symbol:   method when(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:403: error: cannot find symbol
        JsonSchema documentRoot = spy(schemaUnderTest);
                                  ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:404: error: cannot find symbol
        doReturn(documentRoot).when(schemaUnderTest).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:405: error: cannot find symbol
        doReturn(delegated).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:405: error: cannot find symbol
        doReturn(delegated).when(documentRoot).fetchSubSchemaNode(any(ValidationContext.class));
                                                                  ^
  symbol:   method any(Class<ValidationContext>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:455: error: cannot find symbol
        doReturn(null).when(schemaUnderTest).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:467: error: cannot find symbol
        JsonSchema broken = spy(JsonSchema.from(validationContext, rootLocation, rootEvalPath, null, null, false));
                            ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getSubSchema_Test.java:469: error: cannot find symbol
        doReturn(broken).when(broken).findSchemaResourceRoot();
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getSubSchema_Test
59 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method getSubSchema(JsonNodePath)': '\n     * Gets the sub schema given the json pointer fragment.\n     * \n     * @param fragment the json pointer fragment\n     * @return the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.ValidationMessage`:\n\t\nclass `com.networknt.schema.ValidatorTypeCode`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidatorTypeCode com.networknt.schema.ValidatorTypeCode.REF;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private JsonNode handleNullNode(String, JsonSchema)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`;\n\t\tmethod `private List<JsonValidator> read(JsonNode)` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`, api document: \n     * Please note that the key in {@link #validators} map is the evaluation path.\n     ;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getSubSchema(JsonNodePath)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


