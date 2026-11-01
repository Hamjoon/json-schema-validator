```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getRefSchema(JsonNodePath)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>JSON‑Pointer handling – delegation to {@code getSubSchema} and propagation of its result (including {@code null}).</li>
 *   <li>Anchor handling – lookup in {@link ValidationContext#getSchemaResources()},
 *       fallback to {@link ValidationContext#getDynamicAnchors()}, precedence,
 *       handling of {@code null} entries, empty fragment strings and non‑empty base IRI.</li>
 *   <li>Exceptional situations – missing anchors, {@code null} arguments, and
 *       {@code null} collaborators or maps.</li>
 * </ul>
 *
 * All collaborators are mocked; the {@link JsonSchema} instance is created via reflection
 * because its constructor is package‑private.  A Mockito spy is used to stub the protected
 * {@code getSubSchema} method where needed.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNode schemaNode;
    private JsonSchema jsonSchema;   // real instance created via reflection
    private JsonSchema spySchema;    // spy that allows us to stub getSubSchema

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* --------------------------------------------------------------------
         * Mock the collaborators required by the private JsonSchema ctor.
         * -------------------------------------------------------------------- */
        validationContext = mock(ValidationContext.class);
        // Return mutable maps so tests can put entries into them.
        when(validationContext.getSchemaResources()).thenReturn(new HashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new HashMap<>());

        schemaLocation = mock(SchemaLocation.class);
        // By default the base IRI is null (empty string case).
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);

        schemaNode = mock(JsonNode.class);

        /* --------------------------------------------------------------------
         * Create a real JsonSchema instance using reflection (the constructor is
         * private).  The instance is then wrapped in a Mockito spy so that we can
         * stub the protected getSubSchema method for the pointer tests.
         * -------------------------------------------------------------------- */
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(validationContext,
                schemaLocation,
                new JsonNodePath(PathType.JSON_POINTER), // evaluationPath – not used in the tests
                schemaNode,
                null,
                false);

        spySchema = spy(jsonSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – all mocks are GC‑collected.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* ------------------------------------------------------------------------
     *  JSON‑Pointer branch
     * ------------------------------------------------------------------------ */

    @Test
    void getRefSchema_whenFragmentIsJsonPointer_delegatesToGetSubSchema() {
        // Arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);

        JsonSchema expected = mock(JsonSchema.class);
        doReturn(expected).when(spySchema).getSubSchema(fragment);

        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);

        // Assert
        assertSame(expected, actual, "Should delegate to getSubSchema for JSON‑Pointer fragments");
        verify(spySchema, times(1)).getSubSchema(fragment);
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

    /* ------------------------------------------------------------------------
     *  Anchor – basic lookup in schemaResources
     * ------------------------------------------------------------------------ */

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInSchemaResources() {
        // Arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/definitions/foo");

        String anchorKey = "#" + "/definitions/foo";
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);

        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);

        // Assert
        assertSame(expected, actual, "Should return schema from schemaResources");
    }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInDynamicAnchors() {
        // Arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/defs/bar");

        String anchorKey = "#" + "/defs/bar";
        JsonSchema expected = mock(JsonSchema.class);
        // schemaResources empty, dynamicAnchors contains the entry
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().put(anchorKey, expected);

        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);

        // Assert
        assertSame(expected, actual, "Should fall back to dynamicAnchors when not in schemaResources");
    }

    @Test
    void getRefSchema_whenAnchorNotFound_throwsJsonSchemaException() {
        // Arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/missing");

        // Ensure both maps are empty
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().clear();

        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> spySchema.getRefSchema(fragment),
                "Missing anchor must cause JsonSchemaException");
        assertTrue(ex.getMessage().contains("Unable to find anchor"));
    }

    /* ------------------------------------------------------------------------
     *  Anchor – non‑empty base IRI handling
     * ------------------------------------------------------------------------ */

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

        String anchorKey = "#"; // empty fragment yields just "#"
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);

        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);

        // Assert
        assertSame(expected, actual, "Empty fragment should produce anchor \"#\" and be resolved");
    }

    /* ------------------------------------------------------------------------
     *  Null / exceptional arguments
     * ------------------------------------------------------------------------ */

    @Test
    void getRefSchema_nullFragment_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> spySchema.getRefSchema(null),
                "Calling getRefSchema with null should throw NPE");
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
        assertThrows(NullPointerException.class,
                () -> spy.getRefSchema(fragment),
                "Null schemaLocation should cause NPE when resolving anchor");
    }

    @Test
    void getRefSchema_nullValidationContext_throwsNullPointerException() throws Exception {
        // Build a JsonSchema with a null ValidationContext
        JsonSchema schema = createJsonSchema(null, schemaLocation);
        JsonSchema spy = spy(schema);

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/bar");

        assertThrows(NullPointerException.class,
                () -> spy.getRefSchema(fragment),
                "Null validationContext should cause NPE when accessing its maps");
    }

    @Test
    void getRefSchema_nullSchemaResourcesMap_throwsNullPointerException() {
        // Stub the validationContext to return null for schemaResources
        when(validationContext.getSchemaResources()).thenReturn(null);
        // dynamicAnchors remains a real map (empty)
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/missing");

        assertThrows(NullPointerException.class,
                () -> spySchema.getRefSchema(fragment),
                "Null schemaResources map should cause NPE before checking dynamicAnchors");
    }

    @Test
    void getRefSchema_nullDynamicAnchorsMap_throwsNullPointerException() {
        // schemaResources empty, dynamicAnchors null
        when(validationContext.getDynamicAnchors()).thenReturn(null);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("/missing");

        assertThrows(NullPointerException.class,
                () -> spySchema.getRefSchema(fragment),
                "Null dynamicAnchors map should cause NPE after schemaResources miss");
    }

    @Test
    void getRefSchema_fragmentToStringThrowsException_propagatesException() {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenThrow(new RuntimeException("toString failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spySchema.getRefSchema(fragment),
                "Exception from fragment.toString() must propagate");
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void getRefSchema_fragmentGetPathTypeThrowsException_propagatesException() {
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenThrow(new IllegalStateException("path type failure"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> spySchema.getRefSchema(fragment),
                "Exception from fragment.getPathType() must propagate");
        assertEquals("path type failure", ex.getMessage());
    }

    /* ------------------------------------------------------------------------
     *  Helper method to create a JsonSchema instance via reflection.
     * ------------------------------------------------------------------------ */
    private JsonSchema createJsonSchema(ValidationContext ctx, SchemaLocation loc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(
                ctx,
                loc,
                new JsonNodePath(PathType.JSON_POINTER), // evaluationPath – irrelevant for these tests
                schemaNode,
                null,
                false);
    }
}
```