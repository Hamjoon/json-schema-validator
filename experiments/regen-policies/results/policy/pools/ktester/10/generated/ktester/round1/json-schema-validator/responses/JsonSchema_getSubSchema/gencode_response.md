```java
package com.networknt.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit‑tests for {@link JsonSchema#getSubSchema(JsonNodePath)}.
 *
 * <p>The tests cover the full decision matrix of the method, based on the
 * JSON‑Pointer fragment supplied by the caller.  All interactions are performed
 * through the public API – no private fields or methods are accessed directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getSubSchema_Test {

    /** Instance under test – created via the package‑private static factory. */
    private JsonSchema jsonSchemaUnderTest;

    /** Collaborators required by {@code getSubSchema}. */
    @Mock private ValidationContext validationContext;
    @Mock private SchemaLocation rootSchemaLocation;
    @Mock private JsonNodePath rootEvaluationPath;
    @Mock private JsonNode rootSchemaNode;               // the whole schema document

    /** Mocks that participate in traversal. */
    @Mock private JsonSchema documentMock;                // result of findSchemaResourceRoot()
    @Mock private JsonSchema parentMock;                  // current parent during loop

    /** Various fragments used in the tests. */
    @Mock private JsonNodePath fragment;

    /** Node representing a child schema (returned by parent.getNode(...)). */
    @Mock private JsonNode subSchemaNode;

    /** Helper factory – the real implementation is needed to create a JsonSchema
     *  instance via the static {@code from} method. */
    private static final JsonSchemaFactory schemaFactory = new JsonSchemaFactory();

    @BeforeEach
    void setup() {
        // Build the JsonSchema instance under test.
        jsonSchemaUnderTest = JsonSchema.from(
                validationContext,
                rootSchemaLocation,
                rootEvaluationPath,
                rootSchemaNode,
                null,                     // top‑level schema has no parent
                false                     // do not suppress sub‑schema retrieval
        );

        // By default the schema reports itself as the document root.
        when(jsonSchemaUnderTest.findSchemaResourceRoot()).thenReturn(documentMock);
        when(documentMock.findSchemaResourceRoot()).thenReturn(documentMock);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(validationContext, rootSchemaLocation, rootEvaluationPath,
                rootSchemaNode, documentMock, parentMock, fragment, subSchemaNode);
    }

    /* --------------------------------------------------------------------- */
    /*  Positive – simple object property segment                           */
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
        when(validationContext.newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock))
                .thenReturn(childSchema);

        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);

        // THEN
        assertSame(childSchema, result, "Should return the schema created by ValidationContext");
        verify(parentMock, times(1)).getNode("title");
        verify(validationContext, times(1))
                .newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock);
    }

    /* --------------------------------------------------------------------- */
    /*  Positive – mixed object/array traversal (4 segments)                */
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
        JsonSchema p4 = mock(JsonSchema.class); // final child (returned)

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
    /*  Positive – numeric segment only                                      */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NumericSegment() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(2); // numeric index

        when(parentMock.getNode(2)).thenReturn(subSchemaNode);
        when(parentMock.getSchemaLocation()).thenReturn(rootSchemaLocation);
        when(parentMock.getEvaluationPath()).thenReturn(rootEvaluationPath);
        SchemaLocation appendedLocation = mock(SchemaLocation.class);
        JsonNodePath appendedPath = mock(JsonNodePath.class);
        when(rootSchemaLocation.append(2)).thenReturn(appendedLocation);
        when(rootEvaluationPath.append(2)).thenReturn(appendedPath);
        JsonSchema child = mock(JsonSchema.class);
        when(validationContext.newSchema(appendedLocation, appendedPath, subSchemaNode, parentMock))
                .thenReturn(child);

        // WHEN
        JsonSchema result = jsonSchemaUnderTest.getSubSchema(fragment);

        // THEN
        assertSame(child, result);
        verify(parentMock).getNode(2);
    }

    /* --------------------------------------------------------------------- */
    /*  Positive – Boolean segment (treated as string)                       */
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
    /*  Positive – fragment resolves in an external document (fallback)       */
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
    /*  Negative – fragment missing, fallback also missing → exception        */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_MissingFragmentThrows() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("nonexistent");
        when(parentMock.getNode("nonexistent")).thenReturn(null);
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(null);

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertTrue(ex.getMessage().contains("Unable to find subschema"));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge – empty fragment → null result                                   */
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
    /*  Edge – primitive node (e.g., TextNode)                                 */
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
    /*  Edge – numeric segment on non‑array parent → fallback external doc     */
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
    /*  Edge – large numeric index out of bounds → exception                    */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_LargeNumericIndexThrows() {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(999);
        when(parentMock.getNode(999)).thenReturn(null);
        when(documentMock.fetchSubSchemaNode(validationContext)).thenReturn(null);

        // WHEN / THEN
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertTrue(ex.getMessage().contains("Unable to find subschema"));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge – null fragment argument → NullPointerException                  */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NullFragmentThrowsNPE() {
        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> jsonSchemaUnderTest.getSubSchema(null));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge – parent with null ValidationContext → NullPointerException      */
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
        assertThrows(NullPointerException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge – findSchemaResourceRoot returns null → NullPointerException    */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_NullDocumentRootThrowsNPE() {
        // GIVEN
        when(jsonSchemaUnderTest.findSchemaResourceRoot()).thenReturn(null);
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("title");

        // WHEN / THEN
        assertThrows(NullPointerException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge – unsupported segment type (custom object) → IllegalArgumentException */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_UnsupportedSegmentTypeThrows() {
        // GIVEN a custom object segment
        Object unsupported = new Object() {
            @Override public String toString() { return "Unsupported"; }
        };
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn(unsupported);
        // Simulate parent throwing IllegalArgumentException for the segment
        when(parentMock.getNode(unsupported)).thenThrow(new IllegalArgumentException("Unsupported segment"));

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertEquals("Unsupported segment", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge – fetchSubSchemaNode throws JsonProcessingException (propagation) */
    /* --------------------------------------------------------------------- */
    @Test
    void testGetSubSchema_FetchSubSchemaNodeThrowsJsonProcessingException() throws Exception {
        // GIVEN
        when(fragment.getNameCount()).thenReturn(1);
        when(fragment.getElement(0)).thenReturn("missing");
        when(parentMock.getNode("missing")).thenReturn(null);
        // fetchSubSchemaNode throws JsonProcessingException
        when(documentMock.fetchSubSchemaNode(validationContext))
                .thenThrow(new JsonProcessingException("parsing error") {});

        // WHEN / THEN
        JsonProcessingException ex = assertThrows(JsonProcessingException.class,
                () -> jsonSchemaUnderTest.getSubSchema(fragment));
        assertEquals("parsing error", ex.getOriginalMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Parameterized source for simple property tests (reuse)               */
    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> simplePropertyProvider() {
        return Stream.of(
                Arguments.of("title"),
                Arguments.of("description")
        );
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
```