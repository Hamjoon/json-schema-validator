```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    // -------------------------------------------------------------------------
    // Mocks and shared objects – common for most tests
    // -------------------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory schemaFactory; // not used directly but required for ctor stubbing

    ConcurrentMap<String, JsonSchema> schemaResources;
    ConcurrentMap<String, JsonSchema> dynamicAnchors;

    JsonSchema schema;          // instance under test (created in @BeforeEach)
    JsonNode emptySchemaNode;   // a minimal schema node

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise maps that will be returned by the mocked ValidationContext
        schemaResources = new ConcurrentHashMap<>();
        dynamicAnchors = new ConcurrentHashMap<>();

        // stub the ValidationContext behaviour required by JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(schemaResources);
        when(validationContext.getDynamicAnchors()).thenReturn(dynamicAnchors);
        // resolveSchemaId is used only for $id handling – we do not need an id for the tests
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // meta‑schema anchor handling – not needed for these tests
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);

        // a tiny (empty) schema node – sufficient for construction
        emptySchemaNode = new ObjectNode(JsonNodeFactory.instance);

        // SchemaLocation with a null AbsoluteIri (base will be empty string)
        SchemaLocation schemaLocation = new SchemaLocation(null);

        // evaluation path – the type does not matter for construction
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // create the JsonSchema instance using the package‑private factory method
        schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                emptySchemaNode, null, false);
        assertNotNull(schema, "Schema instance should be created");
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // no global teardown required
    }

    // -------------------------------------------------------------------------
    // Existing tests (JSON‑Pointer & simple anchor handling)
    // -------------------------------------------------------------------------

    /*** JSON‑Pointer branch – should delegate to getSubSchema ***/
    @Test
    void shouldReturnSubSchemaWhenFragmentIsJsonPointer() {
        // arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        JsonSchema expectedSub = mock(JsonSchema.class);

        // spy the schema so we can stub getSubSchema without affecting other behaviour
        JsonSchema spySchema = spy(schema);
        doReturn(expectedSub).when(spySchema).getSubSchema(fragment);

        // act
        JsonSchema actual = spySchema.getRefSchema(fragment);

        // assert
        assertSame(expectedSub, actual, "When fragment is a JSON‑Pointer the returned schema must be the sub‑schema");
        verify(spySchema, times(1)).getSubSchema(fragment);
    }

    /*** Anchor branch – schema found in schemaResources ***/
    @Test
    void shouldReturnSchemaFromResourcesWhenAnchorExists() {
        // arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("myAnchor");

        JsonSchema expected = mock(JsonSchema.class);
        // base is empty because schemaLocation has no absolute IRI
        schemaResources.put("#myAnchor", expected);

        // act
        JsonSchema actual = schema.getRefSchema(fragment);

        // assert
        assertSame(expected, actual, "Anchor should resolve to the schema stored in schemaResources");
    }

    /*** Anchor branch – schema found in dynamicAnchors ***/
    @Test
    void shouldReturnSchemaFromDynamicAnchorsWhenAnchorExists() {
        // arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("dynAnchor");

        JsonSchema expected = mock(JsonSchema.class);
        dynamicAnchors.put("#dynAnchor", expected);

        // act
        JsonSchema actual = schema.getRefSchema(fragment);

        // assert
        assertSame(expected, actual, "Anchor should resolve to the schema stored in dynamicAnchors when not in resources");
    }

    /*** Anchor branch – anchor not found – should throw ***/
    @Test
    void shouldThrowWhenAnchorCannotBeResolved() {
        // arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("missingAnchor");

        // ensure maps are empty for this key
        assertFalse(schemaResources.containsKey("#missingAnchor"));
        assertFalse(dynamicAnchors.containsKey("#missingAnchor"));

        // act & assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.getRefSchema(fragment),
                "When an anchor cannot be resolved a JsonSchemaException must be thrown");
        assertTrue(ex.getMessage().contains("Unable to find anchor"),
                "Exception message should contain hint about missing anchor");
    }

    // -------------------------------------------------------------------------
    // Additional tests covering the exhaustive JSON specification
    // -------------------------------------------------------------------------

    /*** Anchor branch – schemaLocation has a non‑empty absolute IRI ***/
    @Test
    void shouldResolveAnchorUsingBaseIriFromSchemaLocation() {
        // arrange – create a schema with a real base IRI
        SchemaLocation location = new SchemaLocation(URI.create("http://example.com/schemas/main.json"));
        JsonSchema schemaWithBase = JsonSchema.from(validationContext, location,
                new JsonNodePath(PathType.JSON_POINTER), emptySchemaNode, null, false);

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("myAnchor");

        JsonSchema expected = mock(JsonSchema.class);
        // key must be "http://example.com/schemas/main.json#myAnchor"
        schemaResources.put("http://example.com/schemas/main.json#myAnchor", expected);

        // act
        JsonSchema actual = schemaWithBase.getRefSchema(fragment);

        // assert
        assertSame(expected, actual, "Anchor should be resolved using the absolute IRI from schemaLocation");
    }

    /*** Anchor branch – URL‑encoded fragment is not decoded ***/
    @Test
    void shouldLookupExactKeyWhenFragmentContainsPercentEncoding() {
        // arrange – base IRI present
        SchemaLocation location = new SchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schemaWithBase = JsonSchema.from(validationContext, location,
                new JsonNodePath(PathType.JSON_POINTER), emptySchemaNode, null, false);

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("my%20anchor"); // contains %20

        JsonSchema expected = mock(JsonSchema.class);
        schemaResources.put("http://example.com/schema.json#my%20anchor", expected);

        // act
        JsonSchema actual = schemaWithBase.getRefSchema(fragment);

        // assert
        assertSame(expected, actual, "The raw (not decoded) fragment must be used for the map lookup");
    }

    /*** Anchor branch – schemaLocation absolute IRI is null (base empty) ***/
    @Test
    void shouldHandleNullAbsoluteIriWhenResolvingAnchor() {
        // arrange – schemaLocation already has null IRI from @BeforeEach (base empty)
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("plainAnchor");

        JsonSchema expected = mock(JsonSchema.class);
        schemaResources.put("#plainAnchor", expected);

        // act
        JsonSchema actual = schema.getRefSchema(fragment);

        // assert
        assertSame(expected, actual, "When base IRI is null the anchor key should start with '#'");
    }

    /*** Null fragment argument – should throw NullPointerException ***/
    @Test
    void shouldThrowNpeWhenFragmentIsNull() {
        // act & assert
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(null),
                "Calling getRefSchema(null) must throw NullPointerException");
    }

    /*** Null schemaLocation field inside JsonSchema – should throw NPE ***/
    @Test
    void shouldThrowNpeWhenSchemaLocationFieldIsNull() throws Exception {
        // arrange – use reflection to set the private field to null
        Field locationField = BaseJsonValidator.class.getDeclaredField("schemaLocation");
        locationField.setAccessible(true);
        locationField.set(schema, null);

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("anchor");

        // act & assert
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(fragment),
                "If schemaLocation is null a NullPointerException should be thrown");
    }

    /*** Null validationContext field inside JsonSchema – should throw NPE ***/
    @Test
    void shouldThrowNpeWhenValidationContextFieldIsNull() throws Exception {
        // arrange – null out the inherited validationContext field
        Field ctxField = BaseJsonValidator.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ctxField.set(schema, null);

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("anchor");

        // act & assert
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(fragment),
                "If validationContext is null a NullPointerException should be thrown");
    }

    /*** ValidationContext.getSchemaResources() returns null – NPE ***/
    @Test
    void shouldThrowNpeWhenSchemaResourcesMapIsNull() {
        // arrange – stub the mock to return null for getSchemaResources()
        when(validationContext.getSchemaResources()).thenReturn(null);
        // dynamicAnchors can stay non‑null (empty) – they won't be reached
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("anchor");

        // act & assert
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(fragment),
                "A null schemaResources map should cause NullPointerException");
    }

    /*** ValidationContext.getDynamicAnchors() returns null after static lookup fails – NPE ***/
    @Test
    void shouldThrowNpeWhenDynamicAnchorsMapIsNullAfterStaticMiss() {
        // arrange – static map empty, dynamic map null
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(null);

        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        when(fragment.toString()).thenReturn("missingAnchor");

        // act & assert
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(fragment),
                "A null dynamicAnchors map after static miss should cause NullPointerException");
    }
}
```