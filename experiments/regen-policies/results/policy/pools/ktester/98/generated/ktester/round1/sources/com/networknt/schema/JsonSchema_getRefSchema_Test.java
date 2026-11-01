package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.fasterxml.jackson.databind.JsonNode;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;

    private SchemaLocation schemaLocation;

    private JsonNode schemaNode;

    // real instance created via reflection
    private JsonSchema jsonSchema;

    // spy that allows us to stub getSubSchema
    private JsonSchema spySchema;

    /**
     * Test subclass of {@link JsonSchema} that lets us stub {@code getSubSchema}.
     */
    private static class TestJsonSchema extends JsonSchema {

        private JsonSchema subSchemaResult;

        private JsonNodePath capturedFragment;

        private int callCount = 0;

        TestJsonSchema(ValidationContext ctx, SchemaLocation loc, JsonNodePath evalPath, JsonNode schemaNode, JsonSchema parent, boolean something) throws Exception {
//             super(ctx, loc, evalPath, schemaNode, parent, something);
        }

        void setSubSchemaResult(JsonSchema result) {
            this.subSchemaResult = result;
        }

        JsonNodePath getCapturedFragment() {
            return capturedFragment;
        }

        int getCallCount() {
            return callCount;
        }

//         @Override
//         protected JsonSchema getSubSchema(JsonNodePath fragment) {
//             callCount++;
//             capturedFragment = fragment;
//             return subSchemaResult;
//         }
    }

    /**
     * Simple {@link JsonNodePath} that allows us to control the path type and its
     * string representation.
     */
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

    /**
     * Simple {@link ValidationContext} exposing mutable maps.
     */
    private static class TestValidationContext extends ValidationContext {

        private final ConcurrentMap<String, JsonSchema> schemaResources = new ConcurrentHashMap<>();

        private final ConcurrentMap<String, JsonSchema> dynamicAnchors = new ConcurrentHashMap<>();

        TestValidationContext() {
            // The three‑argument constructor is the most common one.
            super(null, null, null);
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getSchemaResources() {
            return schemaResources;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getDynamicAnchors() {
            return dynamicAnchors;
        }
    }

    /**
     * ValidationContext that deliberately returns {@code null} maps.
     */
    private static class NullResourcesValidationContext extends ValidationContext {

        NullResourcesValidationContext() {
            super(null, null, null);
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getSchemaResources() {
            return null;
        }

        @Override
        public ConcurrentMap<String, JsonSchema> getDynamicAnchors() {
            return null;
        }
    }

    /**
     * Simple {@link SchemaLocation} that can carry a base IRI.
     */
    private static class TestSchemaLocation extends SchemaLocation {

        private final AbsoluteIri absoluteIri;

        TestSchemaLocation(URI uri) {
            super(uri == null ? null : AbsoluteIri.of(uri.toString()));
            this.absoluteIri = uri == null ? null : AbsoluteIri.of(uri.toString());
        }
    }

    /**
     * Simple {@link JsonNodePath} that lets us control the returned {@link PathType}.
     */
    private static class SimpleJsonNodePath extends JsonNodePath {

        private final PathType type;

        SimpleJsonNodePath(PathType type) {
            super(type);
            this.type = type;
        }

        @Override
        public PathType getPathType() {
            return type;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        validationContext = new TestValidationContext();
        // base IRI is null – this matches the original test set‑up
        schemaLocation = new TestSchemaLocation(null);
        // the schema node itself is irrelevant for the getRefSchema tests
        schemaNode = null;
        // create the “real” schema instance via reflection
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(validationContext, schemaLocation, new JsonNodePath(PathType.JSON_POINTER), schemaNode, null, false);
    }

    /* ------------------------------------------------------------------------
     *  Helper method to create a JsonSchema instance via reflection.
     * ------------------------------------------------------------------------ */
    private JsonSchema createJsonSchema(ValidationContext ctx, SchemaLocation loc) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(ctx, loc, new JsonNodePath(PathType.JSON_POINTER), schemaNode, null, false);
    }

    /* --------------------------------------------------------------------
     * Helper method to create a Mockito‑like mock for {@link JsonSchema}.
     * The real library is not available, so we use a very small stub that
     * simply extends the class without adding behaviour.  The test only
     * needs an instance to be stored in the lookup maps.
     * -------------------------------------------------------------------- */
//     private static JsonSchema mock(JsonSchema ignored) {
//         return instance;
//     }

//     @Test
//     void getRefSchema_whenFragmentIsJsonPointer_delegatesToGetSubSchema() {
//         // Arrange – a fragment whose type is JSON_POINTER
//         JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_POINTER);
//         JsonSchema expected = mock(createJsonSchema(validationContext, schemaLocation));
//         spySchema.setSubSchemaResult(expected);
//         // Act
//         JsonSchema actual = spySchema.getRefSchema(fragment);
//         // Assert
//         assertSame(expected, actual, "Expected delegation to getSubSchema for JSON‑Pointer fragments");
//         assertEquals(1, spySchema.getCallCount(), "getSubSchema should be called exactly once");
//         assertSame(fragment, spySchema.getCapturedFragment(), "Fragment passed to getSubSchema should be the same");
//     }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInSchemaResources() {
        TestJsonNodePath fragment = new TestJsonNodePath(null, "myAnchor");
        String anchorKey = "#" + "myAnchor";
        JsonSchema expected = mock(createJsonSchema(validationContext, schemaLocation));
        validationContext.getSchemaResources().put(anchorKey, expected);
        JsonSchema actual = jsonSchema.getRefSchema(fragment);
        assertSame(expected, actual, "Expected lookup in schemaResources for anchor");
    }

    @Test
    void getRefSchema_whenFragmentIsAnchor_foundInDynamicAnchors() {
        TestJsonNodePath fragment = new TestJsonNodePath(null, "dynamicAnchor");
        String anchorKey = "#" + "dynamicAnchor";
        JsonSchema expected = mock(createJsonSchema(validationContext, schemaLocation));
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
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> jsonSchema.getRefSchema(fragment), "Expected JsonSchemaException when anchor cannot be resolved");
        assertTrue(ex.getMessage().contains("Unable to find anchor"));
    }

//     @Test
//     void getRefSchema_whenJsonPointerReturnsNull_propagatesNull() {
//         // Arrange
//         JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_POINTER);
//         spySchema.setSubSchemaResult(null);
//         // Act
//         JsonSchema actual = spySchema.getRefSchema(fragment);
//         // Assert
//         assertNull(actual, "Null result from getSubSchema must be propagated");
//         assertEquals(1, spySchema.getCallCount(), "getSubSchema should be called exactly once");
//     }

    @Test
    void getRefSchema_anchorWithBaseIri_foundInSchemaResources() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/defs/foo");
        String anchorKey = "http://example.com/schema.json#/defs/foo";
        JsonSchema expected = mock(createJsonSchema(validationContext, locWithIri));
        validationContext.getSchemaResources().put(anchorKey, expected);
        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(expected, actual, "Should resolve anchor with base IRI from schemaResources");
    }

    @Test
    void getRefSchema_anchorWithBaseIri_foundInDynamicAnchors() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/defs/bar");
        String anchorKey = "http://example.com/schema.json#/defs/bar";
        JsonSchema expected = mock(createJsonSchema(validationContext, locWithIri));
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().put(anchorKey, expected);
        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(expected, actual, "Should resolve anchor with base IRI from dynamicAnchors");
    }

    @Test
    void getRefSchema_anchorPrecedence_schemaResourcesOverDynamicAnchors() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/common");
        String anchorKey = "http://example.com/schema.json#/common";
        JsonSchema fromResources = mock(createJsonSchema(validationContext, locWithIri));
        JsonSchema fromDynamic = mock(createJsonSchema(validationContext, locWithIri));
        validationContext.getSchemaResources().put(anchorKey, fromResources);
        validationContext.getDynamicAnchors().put(anchorKey, fromDynamic);
        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(fromResources, actual, "schemaResources must win over dynamicAnchors when both contain the key");
    }

    @Test
    void getRefSchema_anchorFallbackWhenSchemaResourcesEntryNull() throws Exception {
        TestSchemaLocation locWithIri = new TestSchemaLocation(URI.create("http://example.com/schema.json"));
        JsonSchema schema = createJsonSchema(validationContext, locWithIri);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/fallback");
        String anchorKey = "http://example.com/schema.json#/fallback";
        JsonSchema expected = mock(createJsonSchema(validationContext, locWithIri));
        validationContext.getSchemaResources().put(anchorKey, null);
        validationContext.getDynamicAnchors().put(anchorKey, expected);
        JsonSchema actual = schema.getRefSchema(fragment);
        assertSame(expected, actual, "Should continue searching dynamicAnchors when schemaResources entry is null");
    }

    @Test
    void getRefSchema_anchorWithEmptyFragmentString() {
        TestJsonNodePath fragment = new TestJsonNodePath(null, "");
        String anchorKey = "#";
        JsonSchema expected = mock(createJsonSchema(validationContext, schemaLocation));
        validationContext.getSchemaResources().put(anchorKey, expected);
        JsonSchema actual = jsonSchema.getRefSchema(fragment);
        assertSame(expected, actual, "Empty fragment should produce anchor \"#\" and be resolved");
    }

    @Test
    void getRefSchema_nullFragment_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> jsonSchema.getRefSchema(null), "Calling getRefSchema with null should throw NPE");
    }

    @Test
    void getRefSchema_nullSchemaLocation_throwsNullPointerException() throws Exception {
        JsonSchema schema = createJsonSchema(validationContext, null);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/foo");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "Null schemaLocation should cause NPE when resolving anchor");
    }

    @Test
    void getRefSchema_nullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema schema = createJsonSchema(null, schemaLocation);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/bar");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "Null validationContext should cause NPE when accessing its maps");
    }

    @Test
    void getRefSchema_nullSchemaResourcesMap_throwsNullPointerException() {
        ValidationContext ctx = new NullResourcesValidationContext();
        JsonSchema schema = createJsonSchema(ctx, schemaLocation);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/missing");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "Null schemaResources map should cause NPE before checking dynamicAnchors");
    }

    @Test
    void getRefSchema_nullDynamicAnchorsMap_throwsNullPointerException() {
        // schemaResources will miss, then dynamicAnchors is null
        ValidationContext ctx = new NullResourcesValidationContext() {

            @Override
            public ConcurrentMap<String, JsonSchema> getSchemaResources() {
                // empty but non‑null
                return new ConcurrentHashMap<>();
            }
        };
        JsonSchema schema = createJsonSchema(ctx, schemaLocation);
        TestJsonNodePath fragment = new TestJsonNodePath(null, "/missing");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "Null dynamicAnchors map should cause NPE after schemaResources miss");
    }

    @Test
    void getRefSchema_fragmentToStringThrowsException_propagatesException() {
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER) {

            @Override
            public String toString() {
                throw new RuntimeException("toString failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.getRefSchema(fragment), "Exception from fragment.toString() must propagate");
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void getRefSchema_fragmentGetPathTypeThrowsException_propagatesException() {
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER) {

            @Override
            public PathType getPathType() {
                throw new IllegalStateException("path type failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jsonSchema.getRefSchema(fragment), "Exception from fragment.getPathType() must propagate");
        assertEquals("path type failure", ex.getMessage());
    }
}