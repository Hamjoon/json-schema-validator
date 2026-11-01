
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
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
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.extension.ExtendWith;

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:4: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:37: error: cannot find symbol
        validationContext = mock(ValidationContext.class);
                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:40: error: cannot find symbol
        when(validationContext.getSchemaResources()).thenReturn(new HashMap<>());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:41: error: cannot find symbol
        when(validationContext.getDynamicAnchors()).thenReturn(new HashMap<>());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:42: error: cannot find symbol
        schemaLocation = mock(SchemaLocation.class);
                         ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:45: error: cannot find symbol
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:46: error: cannot find symbol
        schemaNode = mock(JsonNode.class);
                     ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:56: error: cannot find symbol
        spySchema = spy(jsonSchema);
                    ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:72: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:73: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:74: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:76: error: cannot find symbol
        doReturn(expected).when(spySchema).getSubSchema(fragment);
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:81: error: cannot find symbol
        verify(spySchema, times(1)).getSubSchema(fragment);
                          ^
  symbol:   method times(int)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:87: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:88: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(null);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:89: error: cannot find symbol
        when(fragment.toString()).thenReturn("myAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:92: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:104: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:105: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(null);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:106: error: cannot find symbol
        when(fragment.toString()).thenReturn("dynamicAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:108: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:121: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:122: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(null);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:123: error: cannot find symbol
        when(fragment.toString()).thenReturn("missing");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:135: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:136: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_POINTER);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:137: error: cannot find symbol
        doReturn(null).when(spySchema).getSubSchema(fragment);
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:142: error: cannot find symbol
        verify(spySchema, times(1)).getSubSchema(fragment);
                          ^
  symbol:   method times(int)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:148: error: cannot find symbol
        SchemaLocation locWithIri = mock(SchemaLocation.class);
                                    ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:149: error: cannot find symbol
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:151: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:152: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:153: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:153: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:154: error: cannot find symbol
        when(fragment.toString()).thenReturn("/defs/foo");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:156: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:167: error: cannot find symbol
        SchemaLocation locWithIri = mock(SchemaLocation.class);
                                    ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:168: error: cannot find symbol
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:170: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:171: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:172: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:172: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:173: error: cannot find symbol
        when(fragment.toString()).thenReturn("/defs/bar");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:175: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:188: error: cannot find symbol
        SchemaLocation locWithIri = mock(SchemaLocation.class);
                                    ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:189: error: cannot find symbol
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:191: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:192: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:193: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:193: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:194: error: cannot find symbol
        when(fragment.toString()).thenReturn("/common");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:196: error: cannot find symbol
        JsonSchema fromResources = mock(JsonSchema.class);
                                   ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:197: error: cannot find symbol
        JsonSchema fromDynamic = mock(JsonSchema.class);
                                 ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:209: error: cannot find symbol
        SchemaLocation locWithIri = mock(SchemaLocation.class);
                                    ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:210: error: cannot find symbol
        when(locWithIri.getAbsoluteIri()).thenReturn(URI.create("http://example.com/schema.json"));
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:212: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:213: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:214: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:214: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:215: error: cannot find symbol
        when(fragment.toString()).thenReturn("/fallback");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:217: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:230: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:231: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:231: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:232: error: cannot find symbol
        when(fragment.toString()).thenReturn("");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:235: error: cannot find symbol
        JsonSchema expected = mock(JsonSchema.class);
                              ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:252: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:253: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:254: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:254: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:255: error: cannot find symbol
        when(fragment.toString()).thenReturn("/foo");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:264: error: cannot find symbol
        JsonSchema spy = spy(schema);
                         ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:265: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:266: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:266: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:267: error: cannot find symbol
        when(fragment.toString()).thenReturn("/bar");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:274: error: cannot find symbol
        when(validationContext.getSchemaResources()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:276: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:277: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:277: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:278: error: cannot find symbol
        when(fragment.toString()).thenReturn("/missing");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:285: error: cannot find symbol
        when(validationContext.getDynamicAnchors()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:286: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:287: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:287: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:288: error: cannot find symbol
        when(fragment.toString()).thenReturn("/missing");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:294: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:295: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
                                                        ^
  symbol:   variable ANCHOR
  location: class PathType
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:295: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.ANCHOR);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:296: error: cannot find symbol
        when(fragment.toString()).thenThrow(new RuntimeException("toString failure"));
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:303: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:304: error: cannot find symbol
        when(fragment.getPathType()).thenThrow(new IllegalStateException("path type failure"));
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
95 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\trelated functions:\n\t\tmethod `protected JsonNodePath atRoot()` returns `com.networknt.schema.JsonNodePath`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`, api document: \n     * Get the root path.\n     *\n     * @return The path.\n     ;\n\t\tmethod `private static void checkForExplicitDiscriminatorMappingMatch(DiscriminatorContext, String, JsonNode, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\n\t\tmethod `private static boolean noExplicitDiscriminatorKeyOverride(JsonNode, JsonSchema)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.PathType`:\n\tdependent fields:\n\t\tcom.networknt.schema.PathType com.networknt.schema.PathType.JSON_POINTER;\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


