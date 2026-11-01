```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#getRefSchema(JsonNodePath)}.
 *
 * The tests cover the two branches of the method:
 * <ul>
 *   <li>When the fragment is a JSON‑Pointer – the method should delegate to {@code getSubSchema}.</li>
 *   <li>When the fragment is an Anchor – the method should look the schema up in the
 *       {@link ValidationContext#getSchemaResources()} map first and then in
 *       {@link ValidationContext#getDynamicAnchors()}.</li>
 * </ul>
 *
 * All collaborators are mocked – no private fields or methods of {@link JsonSchema}
 * are accessed directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNode schemaNode;
    private JsonSchema jsonSchema;          // real instance created via reflection
    private JsonSchema spySchema;           // spy that allows us to stub getSubSchema

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

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
        // Nothing to clean up – all mocks are garbage‑collected.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> spySchema.getRefSchema(fragment),
                "Expected JsonSchemaException when anchor cannot be resolved");
        assertTrue(ex.getMessage().contains("Unable to find anchor"));
    }
}
```