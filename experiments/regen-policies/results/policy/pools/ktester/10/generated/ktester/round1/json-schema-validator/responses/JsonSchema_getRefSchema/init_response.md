```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    // Mocks and shared objects
    @Mock ValidationContext validationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory schemaFactory; // needed for ValidationContext ctor (if ever called)
    ConcurrentMap<String, JsonSchema> schemaResources;
    ConcurrentMap<String, JsonSchema> dynamicAnchors;

    JsonSchema schema;          // the instance under test
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
        // verify that getSubSchema was indeed called
        verify(spySchema, times(1)).getSubSchema(fragment);
    }

    /*** Anchor branch – schema found in schemaResources ***/
    @Test
    void shouldReturnSchemaFromResourcesWhenAnchorExists() {
        // arrange
        JsonNodePath fragment = mock(JsonNodePath.class);
        // any non‑JSON_POINTER type – using a dummy enum constant
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
}
```