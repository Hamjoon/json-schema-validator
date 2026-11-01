package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getSubSchema_Test {

    /**
     * The instance under test – a thin wrapper that delegates to the mocked
     *  collaborators.
     */
    private JsonSchema jsonSchemaUnderTest;

    /**
     * Collaborators that are required by {@code getSubSchema}.
     */
    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaLocation rootSchemaLocation;

    @Mock
    private JsonNodePath rootEvaluationPath;

    // the whole schema document
    @Mock
    private JsonNode rootSchemaNode;

    // will act as the current parent during traversal
    @Mock
    private JsonSchema parentMock;

    // result of {@code findSchemaResourceRoot()}
    @Mock
    private JsonSchema documentMock;

    // the JSON‑Pointer fragment supplied by the caller
    @Mock
    private JsonNodePath fragment;

    // node that represents the child schema
    @Mock
    private JsonNode subSchemaNode;

    /**
     * Helper used to create a real {@link JsonSchema} instance via the package‑private
     *  static factory method.
     */
    private static JsonSchemaFactory schemaFactory = new JsonSchemaFactory();

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the JsonSchema under test using the public factory method.
        // The constructor is private, therefore we use the static `from` method.
        jsonSchemaUnderTest = JsonSchema.from(validationContext, rootSchemaLocation, rootEvaluationPath, rootSchemaNode, // no parent – this is the top‑level schema
        null, // do not suppress sub‑schema retrieval
        false);
        // This matches the behaviour of the real implementation.
        when(jsonSchemaUnderTest.findSchemaResourceRoot()).thenReturn(documentMock);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        // Build the JsonSchema instance under test.
        jsonSchemaUnderTest = JsonSchema.from(validationContext, rootSchemaLocation, rootEvaluationPath, rootSchemaNode, // top‑level schema has no parent
        null, // do not suppress sub‑schema retrieval
        false);
        // By default, the schema under test reports itself as the root document.
        // By default the schema reports itself as the document root.
        when(jsonSchemaUnderTest.findSchemaResourceRoot()).thenReturn(documentMock);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test contamination.
        Mockito.reset(validationContext, rootSchemaLocation, rootEvaluationPath, rootSchemaNode, parentMock, documentMock, fragment, subSchemaNode);
        Mockito.reset(validationContext, rootSchemaLocation, rootEvaluationPath, rootSchemaNode, documentMock, parentMock, fragment, subSchemaNode);
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> simplePropertyProvider() {
        return Stream.of(Arguments.of("title"), Arguments.of("description"));
    }

    /**
     * Positive test – the fragment points to an existing subschema.
     */
    @Test
    void testGetSubSchemaWhenFragmentExists() {
        // -------------------------------------------------
        // GIVEN – a fragment consisting of a single element "properties"
        // -------------------------------------------------
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("properties");
        // The parent (initially the document) returns a non‑null node for the segment.
        when(parentMock.getNode("properties")).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        // The schema location and evaluation path must be able to append the segment.
        SchemaLocation appendedLocation = mock(SchemaLocation.class);
        JsonNodePath appendedPath = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("properties")).thenReturn(appendedLocation);
        when(rootEvaluationPath.append("properties")).thenReturn(appendedPath);
        // The validation context creates a new JsonSchema for the child node.
        JsonSchema createdChild = mock(JsonSchema.class);
        when(validationContext.newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock)).thenReturn(createdChild);
        // -------------------------------------------------
        // WHEN
        // -------------------------------------------------
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // -------------------------------------------------
        // THEN – the method should return the child schema created above.
        // -------------------------------------------------
        assertSame(createdChild, result, "The returned JsonSchema should be the one created by ValidationContext");
        // Verify the traversal loop was executed exactly once.
        verify(parentMock, times(1)).getNode("properties");
        verify(validationContext, times(1)).newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock);
    }

    /**
     * Negative test – the fragment cannot be resolved, leading to an exception.
     */
    @Test
    void testGetSubSchemaWhenFragmentMissingThrowsException() {
        // -------------------------------------------------
        // GIVEN – a fragment that does not match any node in the document.
        // -------------------------------------------------
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("nonexistent");
        // The parent (document) returns null for the missing segment.
        when(parentMock.getNode("nonexistent")).thenReturn(null);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
        // Simulate that the fallback lookup also returns null.
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(null);
        // -------------------------------------------------
        // WHEN / THEN – expect a JsonSchemaException.
        // -------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertTrue(ex.getMessage().contains("Unable to find subschema"), "Exception message should contain the standard error text");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_SimpleProperty() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("title");
        // parent (the document) returns a node for "title"
        when(parentMock.getNode("title")).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        // location/path appending
        SchemaLocation appendedLocation = mock(SchemaLocation.class);
        JsonNodePath appendedPath = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("title")).thenReturn(appendedLocation);
        when(rootEvaluationPath.append("title")).thenReturn(appendedPath);
        // ValidationContext creates the child schema
        JsonSchema childSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock)).thenReturn(childSchema);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(childSchema, result, "Should return the schema created by ValidationContext");
        verify(parentMock, times(1)).getNode("title");
        verify(validationContext, times(1)).newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_MixedSegments() {
        // GIVEN a fragment: ["properties","items",0,"type"]
        List<Object> segments = List.of("properties", "items", 0, "type");
        when(fragment.getNameCount()).thenReturn(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            when(fragment.getElement(i)).thenReturn(segments.get(i));
        }
        // We'll create a chain of parent mocks: doc -> p1 -> p2 -> p3
        JsonSchema p1 = mock(JsonSchema.class);
        JsonSchema p2 = mock(JsonSchema.class);
        JsonSchema p3 = mock(JsonSchema.class);
        // final child (returned)
        JsonSchema p4 = mock(JsonSchema.class);
        // ---- iteration 0: "properties" ------------------------------------
        when(parentMock.getNode("properties")).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation loc1 = mock(SchemaLocation.class);
        JsonNodePath path1 = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("properties")).thenReturn(loc1);
        when(rootEvaluationPath.append("properties")).thenReturn(path1);
        when(validationContext.newSchema(loc1, path1, subSchemaNode, parentMock)).thenReturn(p1);
        // ---- iteration 1: "items" -----------------------------------------
        when(p1.getNode("items")).thenReturn(subSchemaNode);
        when(p1.getSchemaLocation()).thenReturn(loc1);
        when(p1.getEvaluationPath()).thenReturn(path1);
        SchemaLocation loc2 = mock(SchemaLocation.class);
        JsonNodePath path2 = mock(JsonNodePath.class);
        when(loc1.append("items")).thenReturn(loc2);
        when(path1.append("items")).thenReturn(path2);
        when(validationContext.newSchema(loc2, path2, subSchemaNode, p1)).thenReturn(p2);
        // ---- iteration 2: numeric index 0 ---------------------------------
        when(p2.getNode(0)).thenReturn(subSchemaNode);
        when(p2.getSchemaLocation()).thenReturn(loc2);
        when(p2.getEvaluationPath()).thenReturn(path2);
        SchemaLocation loc3 = mock(SchemaLocation.class);
        JsonNodePath path3 = mock(JsonNodePath.class);
        when(loc2.append(0)).thenReturn(loc3);
        when(path2.append(0)).thenReturn(path3);
        when(validationContext.newSchema(loc3, path3, subSchemaNode, p2)).thenReturn(p3);
        // ---- iteration 3: "type" -----------------------------------------
        when(p3.getNode("type")).thenReturn(subSchemaNode);
        when(p3.getSchemaLocation()).thenReturn(loc3);
        when(p3.getEvaluationPath()).thenReturn(path3);
        SchemaLocation loc4 = mock(SchemaLocation.class);
        JsonNodePath path4 = mock(JsonNodePath.class);
        when(loc3.append("type")).thenReturn(loc4);
        when(path3.append("type")).thenReturn(path4);
        when(validationContext.newSchema(loc4, path4, subSchemaNode, p3)).thenReturn(p4);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(p4, result, "The deepest child schema should be returned");
        // verify each step was hit once
        verify(parentMock).getNode("properties");
        verify(p1).getNode("items");
        verify(p2).getNode(0);
        verify(p3).getNode("type");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NumericSegment() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        // numeric index
        when(fragment.getElement(0)).thenReturn(2);
        when(parentMock.getNode(2)).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation appendedLocation = mock(SchemaLocation.class);
        JsonNodePath appendedPath = mock(JsonNodePath.class);
        when(rootSchemaLocation.append(2)).thenReturn(appendedLocation);
        when(rootEvaluationPath.append(2)).thenReturn(appendedPath);
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock)).thenReturn(child);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(child, result);
        verify(parentMock).getNode(2);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_BooleanSegment() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(Boolean.TRUE);
        when(parentMock.getNode(Boolean.TRUE)).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation loc = mock(SchemaLocation.class);
        JsonNodePath path = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("true")).thenReturn(loc);
        when(rootEvaluationPath.append("true")).thenReturn(path);
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(loc, path, subSchemaNode, parentMock)).thenReturn(child);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(child, result);
        verify(parentMock).getNode(Boolean.TRUE);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_FallbackToExternalDocument() {
        // GIVEN a fragment that is not present in the root document
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("external");
        // Root document returns null for the segment
        when(parentMock.getNode("external")).thenReturn(null);
        // documentMock.findSchemaResourceRoot() returns itself (already stubbed)
        JsonSchema externalDoc = mock(JsonSchema.class);
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(externalDoc);
        // The external document successfully resolves the fragment
        JsonSchema externalSub = mock(JsonSchema.class);
        when(externalDoc.getSubSchema(fragment)).thenReturn(externalSub);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(externalSub, result, "Should return the sub‑schema from the external document");
        verify(documentMock).fetchSubSchemaNode(validationContext);
        verify(externalDoc).getSubSchema(fragment);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_MissingFragmentThrows() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("nonexistent");
        when(parentMock.getNode("nonexistent")).thenReturn(null);
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(null);
        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertTrue(ex.getMessage().contains("Unable to find subschema"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_EmptyFragmentReturnsNull() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(0);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertNull(result, "An empty fragment should result in null (no traversal)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_PrimitiveNode() {
        // GIVEN a fragment that points to a primitive schema node
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("default");
        // Simulate that the node is a TextNode
        TextNode textNode = new TextNode("default value");
        when(parentMock.getNode("default")).thenReturn(textNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation loc = mock(SchemaLocation.class);
        JsonNodePath path = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("default")).thenReturn(loc);
        when(rootEvaluationPath.append("default")).thenReturn(path);
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(loc, path, textNode, parentMock)).thenReturn(child);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(child, result);
        verify(parentMock).getNode("default");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NumericOnObjectFallback() {
        // Fragment: ["properties","title",0]
        List<Object> segs = List.of("properties", "title", 0);
        when(fragment.getNameCount()).thenReturn(segs.size());
        for (int i = 0; i < segs.size(); i++) {
            when(fragment.getElement(i)).thenReturn(segs.get(i));
        }
        // Step 0: "properties" exists
        when(parentMock.getNode("properties")).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation loc1 = mock(SchemaLocation.class);
        JsonNodePath path1 = mock(JsonNodePath.class);
        when(rootSchemaLocation.append("properties")).thenReturn(loc1);
        when(rootEvaluationPath.append("properties")).thenReturn(path1);
        JsonSchema p1 = mock(JsonSchema.class);
        when(validationContext.newSchema(loc1, path1, subSchemaNode, parentMock)).thenReturn(p1);
        // Step 1: "title" exists under p1
        when(p1.getNode("title")).thenReturn(subSchemaNode);
        when(p1.getSchemaLocation()).thenReturn(loc1);
        when(p1.getEvaluationPath()).thenReturn(path1);
        SchemaLocation loc2 = mock(SchemaLocation.class);
        JsonNodePath path2 = mock(JsonNodePath.class);
        when(loc1.append("title")).thenReturn(loc2);
        when(path1.append("title")).thenReturn(path2);
        JsonSchema p2 = mock(JsonSchema.class);
        when(validationContext.newSchema(loc2, path2, subSchemaNode, p1)).thenReturn(p2);
        // Step 2: numeric 0 on a non‑array parent (p2) returns null
        when(p2.getNode(0)).thenReturn(null);
        // Trigger fallback
        JsonSchema externalDoc = mock(JsonSchema.class);
        JsonSchema externalSub = mock(JsonSchema.class);
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(externalDoc);
        when(externalDoc.getSubSchema(fragment)).thenReturn(externalSub);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(externalSub, result);
        verify(documentMock).fetchSubSchemaNode(validationContext);
        verify(externalDoc).getSubSchema(fragment);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_LargeNumericIndexThrows() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(999);
        when(parentMock.getNode(999)).thenReturn(null);
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(null);
        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertTrue(ex.getMessage().contains("Unable to find subschema"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NullFragmentThrowsNPE() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> jsonSchemaUnderTest.getSubSchema(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NullValidationContextThrowsNPE() {
        // GIVEN a fragment that resolves to a node
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("title");
        when(parentMock.getNode("title")).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        // parent.getValidationContext() returns null (default behavior of a mock)
        // Force the parent to be the one used inside the loop
        // Override the internal parent traversal to start with parentMock
        // (by making jsonSchemaUnderTest.findSchemaResourceRoot() return a document
        // whose getNode returns parentMock – we achieve this by stubbing getNode on
        // documentMock to return parentMock's node.)
        when(documentMock.getNode("title")).thenReturn(subSchemaNode);
        // Ensure validationContext is null when called
        when(parentMock.getValidationContext()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NullDocumentRootThrowsNPE() {
        // GIVEN
        when(jsonSchemaUnderTest.findSchemaResourceRoot()).thenReturn(null);
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("title");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_UnsupportedSegmentTypeThrows() {
        // GIVEN a custom object segment
        Object unsupported = new Object() {

            @Override
            public String toString() {
                return "Unsupported";
            }
        };
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(unsupported);
        // Simulate parent throwing IllegalArgumentException for the segment
        when(parentMock.getNode(unsupported)).thenThrow(new IllegalArgumentException("Unsupported segment"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertEquals("Unsupported segment", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_FetchSubSchemaNodeThrowsJsonProcessingException() throws Exception {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("missing");
        when(parentMock.getNode("missing")).thenReturn(null);
        // fetchSubSchemaNode throws JsonProcessingException
        when(documentMock.fetchSubSchemaNode(validationContext)).thenThrow(new JsonProcessingException("parsing error") {
        });
        // WHEN / THEN
        JsonProcessingException ex = assertThrows(JsonProcessingException.class, () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertEquals("parsing error", ex.getOriginalMessage());
    }

    @ParameterizedTest
    @MethodSource("simplePropertyProvider")
    void testGetSubSchema_SimpleProperty_Parameterized(String segment) {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(segment);
        when(parentMock.getNode(segment)).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation loc = mock(SchemaLocation.class);
        JsonNodePath path = mock(JsonNodePath.class);
        when(rootSchemaLocation.append(segment)).thenReturn(loc);
        when(rootEvaluationPath.append(segment)).thenReturn(path);
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(loc, path, subSchemaNode, parentMock)).thenReturn(child);
        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);
        // THEN
        assertSame(child, result);
    }
}
