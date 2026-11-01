```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JsonSchema#getRefSchema(JsonNodePath)}.
 *
 * <p>These tests avoid the use of external mocking frameworks.  Simple
 * concrete subclasses are used to provide the behaviour that would normally be
 * supplied by Mockito.</p>
 */
class JsonSchema_getRefSchema_Test {

    /** Simple {@link ValidationContext} that exposes mutable maps. */
    private static class TestValidationContext extends ValidationContext {
        private final Map<String, JsonSchema> schemaResources = new HashMap<>();
        private final Map<String, JsonSchema> dynamicAnchors = new HashMap<>();

        @Override
        public Map<String, JsonSchema> getSchemaResources() {
            return schemaResources;
        }

        @Override
        public Map<String, JsonSchema> getDynamicAnchors() {
            return dynamicAnchors;
        }
    }

    /** Simple {@link SchemaLocation} that returns a configurable base IRI. */
    private static class TestSchemaLocation extends SchemaLocation {
        private final URI absoluteIri;

        TestSchemaLocation(URI absoluteIri) {
            this.absoluteIri = absoluteIri;
        }

        @Override
        public URI getAbsoluteIri() {
            return absoluteIri;
        }
    }

    /** Simple {@link JsonNodePath} that allows us to control the path type and its string
     * representation. */
    private static class TestJsonNodePath extends JsonNodePath {
        private final PathType type;
        private final String pathString;

        TestJsonNodePath(PathType type, String pathString) {
            super(type);
            this.type = type;
            this.pathString = pathString;
        }

        @Override
        public PathType getPathType() {
            return type;
        }

        @Override
        public String toString() {
            return pathString;
        }
    }

    private TestValidationContext validationContext;
    private TestSchemaLocation schemaLocation;
    private JsonNode schemaNode; // not used by the tests – can be null
    private JsonSchema jsonSchema; // real instance created via reflection

    @BeforeEach
    void setupBeforeEach() throws Exception {
        validationContext = new TestValidationContext();
        // base IRI is null – this matches the original test set‑up
        schemaLocation = new TestSchemaLocation(null);
        schemaNode = null; // the schema node itself is irrelevant for the getRefSchema tests

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
                new JsonNodePath(PathType.JSON_POINTER), // evaluationPath – not used
                schemaNode,
                null,
                false);
    }

    /** Helper that creates a {@link JsonSchema} instance via reflection. */
    private JsonSchema createJsonSchema(ValidationContext ctx, SchemaLocation loc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(ctx,
                loc,
                new JsonNodePath(PathType.JSON_POINTER), // evaluationPath – not used
                schemaNode,
                null,
                false);
    }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInSchemaResources() {
        TestJsonNodePath fragment = new TestJsonNodePath(null, "myAnchor");
        String anchorKey = "#" + "myAnchor";
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);

        JsonSchema actual = jsonSchema.getRefSchema(fragment);
        assertSame(expected, actual, "Expected lookup in schemaResources for anchor");
    }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInDynamicAnchors() {
        TestJsonNodePath fragment = new TestJsonNodePath(null, "dynamicAnchor");
        String anchorKey = "#" + "dynamicAnchor";
        JsonSchema expected = mock(JsonSchema.class);

        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().put(anchorKey, expected);

        JsonSchema actual = jsonSchema.getRefSchema(fragment);
        assertSame(expected, actual, "Expected lookup in dynamicAnchors when not present in schemaResources");
    }

    @Test
    void getRefSchema_whenAnchorNotFound_throwsJsonSchemaException() {
        TestJsonNodePath fragment = new TestJsonNodePath(null, "missing");
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().clear();

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> jsonSchema.getRefSchema(fragment),
                "Expected JsonSchemaException when anchor cannot be resolved");
        assertTrue(ex.getMessage().contains("Unable to find anchor"));
    }

    @Test
    void getRefSchema_anchorWithBaseIri_foundInSchemaResources() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/defs/foo");
        String anchorKey = "http://example.com/schema.json#/defs/foo";
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);

        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(expected, actual, "Should resolve anchor with base IRI from schemaResources");
    }

    @Test
    void getRefSchema_anchorWithBaseIri_foundInDynamicAnchors() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/defs/bar");
        String anchorKey = "http://example.com/schema.json#/defs/bar";
        JsonSchema expected = mock(JsonSchema.class);

        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().put(anchorKey, expected);

        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(expected, actual, "Should resolve anchor with base IRI from dynamicAnchors");
    }

    @Test
    void getRefSchema_anchorPrecedence_schemaResourcesOverDynamicAnchors() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/common");
        String anchorKey = "http://example.com/schema.json#/common";

        JsonSchema fromResources = mock(JsonSchema.class);
        JsonSchema fromDynamic = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, fromResources);
        validationContext.getDynamicAnchors().put(anchorKey, fromDynamic);

        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(fromResources, actual,
                "schemaResources must win over dynamicAnchors when both contain the key");
    }

    @Test
    void getRefSchema_anchorFallbackWhenSchemaResourcesEntryNull() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/fallback");
        String anchorKey = "http://example.com/schema.json#/fallback";

        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, null);
        validationContext.getDynamicAnchors().put(anchorKey, expected);

        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(expected, actual,
                "Should continue searching dynamicAnchors when schemaResources entry is null");
    }

    @Test
    void getRefSchema_anchorWithEmptyFragmentString() {
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "");
        String anchorKey = "#";
        JsonSchema expected = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expected);

        JsonSchema actual = jsonSchema.getRefSchema(fragment);
        assertSame(expected, actual, "Empty fragment should produce anchor \"#\" and be resolved");
    }

    @Test
    void getRefSchema_nullFragment_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.getRefSchema(null),
                "Calling getRefSchema with null should throw NPE");
    }

    @Test
    void getRefSchema_nullSchemaLocation_throwsNullPointerException() throws Exception {
        JsonSchema schema = createJsonSchema(validationContext, null);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/foo");
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(fragment),
                "Null schemaLocation should cause NPE when resolving anchor");
    }

    @Test
    void getRefSchema_nullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema schema = createJsonSchema(null, schemaLocation);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/bar");
        assertThrows(NullPointerException.class,
                () -> schema.getRefSchema(fragment),
                "Null validationContext should cause NPE when accessing its maps");
    }

    @Test
    void getRefSchema_nullSchemaResourcesMap_throwsNullPointerException() {
        when(validationContext.getSchemaResources()).thenReturn(null);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/missing");
        assertThrows(NullPointerException.class,
                () -> jsonSchema.getRefSchema(fragment),
                "Null schemaResources map should cause NPE before checking dynamicAnchors");
    }

    @Test
    void getRefSchema_nullDynamicAnchorsMap_throwsNullPointerException() {
        when(validationContext.getDynamicAnchors()).thenReturn(null);
        TestJsonNodePath fragment = new TestJsonNodePath(PathType.ANCHOR, "/missing");
        assertThrows(NullPointerException.class,
                () -> jsonSchema.getRefSchema(fragment),
                "Null dynamicAnchors map should cause NPE after schemaResources miss");
    }

    @Test
    void getRefSchema_fragmentToStringThrowsException_propagatesException() {
        JsonNodePath fragment = new JsonNodePath(PathType.ANCHOR) {
            @Override
            public String toString() {
                throw new RuntimeException("toString failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.getRefSchema(fragment),
                "Exception from fragment.toString() must propagate");
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void getRefSchema_fragmentGetPathTypeThrowsException_propagatesException() {
        JsonNodePath fragment = new JsonNodePath(PathType.ANCHOR) {
            @Override
            public PathType getPathType() {
                throw new IllegalStateException("path type failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> jsonSchema.getRefSchema(fragment),
                "Exception from fragment.getPathType() must propagate");
        assertEquals("path type failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------
     * Helper method to create a Mockito‑like mock for {@link JsonSchema}.
     * The real library is not available, so we use a very small stub that
     * simply extends the class without adding behaviour.  The test only
     * needs an instance to be stored in the lookup maps.
     * -------------------------------------------------------------------- */
    private static JsonSchema mock(JsonSchema ignored) {
        return ignored; // the argument itself is already a mock‑like stub
    }
}
```