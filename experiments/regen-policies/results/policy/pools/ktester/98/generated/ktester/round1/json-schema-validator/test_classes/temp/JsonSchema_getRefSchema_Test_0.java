package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;

    private SchemaLocation schemaLocation;

    private JsonNode schemaNode;

    // real instance created via reflection
    private JsonSchema jsonSchema;

    // spy that allows us to stub getSubSchema
    private JsonSchema spySchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------------
         * Mock the collaborators that are required by the private JsonSchema ctor.
         * -------------------------------------------------------------------- */
        validationContext = mock(ValidationContext.class);
        // the maps returned by the ValidationContext are real mutable maps so that
        // the test can put entries into them.
        when(validationContext.getSchemaResources()).thenReturn(new HashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new HashMap<>());
        schemaLocation = mock(SchemaLocation.class);
        // For the anchor‑related tests we want the base IRI to be null,
        // which makes the base part of the anchor an empty string.
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        schemaNode = mock(JsonNode.class);
        /* --------------------------------------------------------------------
         * Create a real JsonSchema instance using reflection (the constructor is
         * private).  The instance is then wrapped in a Mockito spy so that we can
         * stub the protected getSubSchema method for the pointer test.
         * -------------------------------------------------------------------- */
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(validationContext, schemaLocation, // evaluationPath – not used in the tests
        new JsonNodePath(PathType.JSON_POINTER), schemaNode, null, false);
        spySchema = spy(jsonSchema);
    }

    /* ------------------------------------------------------------------------
     *  Helper method to create a JsonSchema instance via reflection.
     * ------------------------------------------------------------------------ */
    private JsonSchema createJsonSchema(ValidationContext ctx, SchemaLocation loc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(ctx, loc, // evaluationPath – irrelevant for these tests
        new JsonNodePath(PathType.JSON_POINTER), schemaNode, null, false);
    }

    @Test
    void getRefSchema_whenFragmentIsJsonPointer_delegatesToGetSubSchema() {
        // Arrange – a fragment whose path type is JSON_POINTER
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        JsonSchema expected = mock(JsonSchema.class);
        // Stub the protected getSubSchema method on the spy.
        doReturn(expected).when(spySchema).getSubSchema(fragment);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Expected delegation to getSubSchema for JSON‑Pointer fragments");
        verify(spySchema, times(1)).getSubSchema(fragment);
    }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInSchemaResources() {
        // Arrange – a fragment that is *not* a JSON_POINTER (null path type forces the else‑branch)
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(null);
        when(fragment.toString()).thenReturn("myAnchor");
        // The method builds the anchor key as "#" + fragmentString because the base IRI is empty.
        String anchorKey = "#" + "myAnchor";
        JsonSchema expected = mock(JsonSchema.class);
        // Populate the schemaResources map that the ValidationContext returns.
        validationContext.getSchemaResources().put(anchorKey, expected);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Expected lookup in schemaResources for anchor");
    }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInDynamicAnchors() {
        // Arrange – same fragment as previous test but the schemaResources map does *not* contain it.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(null);
        when(fragment.toString()).thenReturn("dynamicAnchor");
        String anchorKey = "#" + "dynamicAnchor";
        JsonSchema expected = mock(JsonSchema.class);
        // Ensure schemaResources is empty and dynamicAnchors contains the entry.
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().put(anchorKey, expected);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Expected lookup in dynamicAnchors when not present in schemaResources");
    }

    @Test
    void getRefSchema_whenAnchorNotFound_throwsJsonSchemaException() {
        // Arrange – fragment that resolves to a key that is absent from both maps.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(null);
        when(fragment.toString()).thenReturn("missing");
        // Ensure both maps are empty.
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().clear();
        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> spySchema.getRefSchema(fragment), "Expected JsonSchemaException when anchor cannot be resolved");
        assertTrue(ex.getMessage().contains("Unable to find anchor"));
    }

    @Test
    void getRefSchema_whenJsonPointerReturnsNull_propagatesNull() {
        // Arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        doReturn(null).when(spySchema).getSubSchema(fragment);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertNull(actual, "Null result from getSubSchema must be propagated");
        verify(spySchema, times(1)).getSubSchema(fragment);
    }

    @Test
    void getRefSchema_anchorWithBaseIri_foundInSchemaResources() throws Exception {
        // Arrange: base IRI = http://example.com/schema.json
        SchemaLocation locWithIri = mock(SchemaLocation.class);
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        JsonSchema spy = spy(schema);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/defs/foo");
        String anchorKey = "http://example.com/schema.json#/defs/foo";
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);
        // Act
        JsonSchema actual = spy.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Should resolve anchor with base IRI from schemaResources");
    }

    @Test
    void getRefSchema_anchorWithBaseIri_foundInDynamicAnchors() throws Exception {
        // Arrange: base IRI = http://example.com/schema.json
        SchemaLocation locWithIri = mock(SchemaLocation.class);
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        JsonSchema spy = spy(schema);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/defs/bar");
        String anchorKey = "http://example.com/schema.json#/defs/bar";
        JsonSchema expected = mock(JsonSchema.class);
        // Ensure schemaResources does not contain the key
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().put(anchorKey, expected);
        // Act
        JsonSchema actual = spy.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Should resolve anchor with base IRI from dynamicAnchors");
    }

    @Test
    void getRefSchema_anchorPrecedence_schemaResourcesOverDynamicAnchors() throws Exception {
        // Arrange: base IRI = http://example.com/schema.json
        SchemaLocation locWithIri = mock(SchemaLocation.class);
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        JsonSchema spy = spy(schema);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/common");
        String anchorKey = "http://example.com/schema.json#/common";
        JsonSchema fromResources = mock(JsonSchema.class);
        JsonSchema fromDynamic = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, fromResources);
        validationContext.getDynamicAnchors().put(anchorKey, fromDynamic);
        // Act
        JsonSchema actual = spy.getRefSchema(fragment);
        // Assert
        assertSame(fromResources, actual, "schemaResources must win over dynamicAnchors when both contain the key");
    }

    @Test
    void getRefSchema_anchorFallbackWhenSchemaResourcesEntryNull() throws Exception {
        // Arrange: base IRI = http://example.com/schema.json
        SchemaLocation locWithIri = mock(SchemaLocation.class);
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        JsonSchema spy = spy(schema);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/fallback");
        String anchorKey = "http://example.com/schema.json#/fallback";
        JsonSchema expected = mock(JsonSchema.class);
        // schemaResources contains a null entry
        validationContext.getSchemaResources().put(anchorKey, null);
        validationContext.getDynamicAnchors().put(anchorKey, expected);
        // Act
        JsonSchema actual = spy.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Should continue searching dynamicAnchors when schemaResources entry is null");
    }

    @Test
    void getRefSchema_anchorWithEmptyFragmentString() {
        // Arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("");
        // empty fragment yields just "#"
        String anchorKey = "#";
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Empty fragment should produce anchor \"#\" and be resolved");
    }

    @Test
    void getRefSchema_nullFragment_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> spySchema.getRefSchema(null), "Calling getRefSchema with null should throw NPE");
    }

    @Test
    void getRefSchema_nullSchemaLocation_throwsNullPointerException() throws Exception {
        // Build a JsonSchema with a null SchemaLocation
        JsonSchema schema = createJsonSchema(validationContext, null);
        JsonSchema spy = spy(schema);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/foo");
        // Act & Assert
        assertThrows(NullPointerException.class, () -> spy.getRefSchema(fragment), "Null schemaLocation should cause NPE when resolving anchor");
    }

    @Test
    void getRefSchema_nullValidationContext_throwsNullPointerException() throws Exception {
        // Build a JsonSchema with a null ValidationContext
        JsonSchema schema = createJsonSchema(null, schemaLocation);
        JsonSchema spy = spy(schema);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/bar");
        assertThrows(NullPointerException.class, () -> spy.getRefSchema(fragment), "Null validationContext should cause NPE when accessing its maps");
    }

    @Test
    void getRefSchema_nullSchemaResourcesMap_throwsNullPointerException() {
        // Stub the validationContext to return null for schemaResources
        when(validationContext.getSchemaResources()).thenReturn(null);
        // dynamicAnchors remains a real map (empty)
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/missing");
        assertThrows(NullPointerException.class, () -> spySchema.getRefSchema(fragment), "Null schemaResources map should cause NPE before checking dynamicAnchors");
    }

    @Test
    void getRefSchema_nullDynamicAnchorsMap_throwsNullPointerException() {
        // schemaResources empty, dynamicAnchors null
        when(validationContext.getDynamicAnchors()).thenReturn(null);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/missing");
        assertThrows(NullPointerException.class, () -> spySchema.getRefSchema(fragment), "Null dynamicAnchors map should cause NPE after schemaResources miss");
    }

    @Test
    void getRefSchema_fragmentToStringThrowsException_propagatesException() {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenThrow(new RuntimeException("toString failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> spySchema.getRefSchema(fragment), "Exception from fragment.toString() must propagate");
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void getRefSchema_fragmentGetPathTypeThrowsException_propagatesException() {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenThrow(new IllegalStateException("path type failure"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spySchema.getRefSchema(fragment), "Exception from fragment.getPathType() must propagate");
        assertEquals("path type failure", ex.getMessage());
    }
}
