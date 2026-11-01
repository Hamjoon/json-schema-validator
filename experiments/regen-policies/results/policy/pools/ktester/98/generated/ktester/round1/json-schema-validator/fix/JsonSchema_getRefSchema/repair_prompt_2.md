
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;

    private SchemaLocation schemaLocation;

    private JsonNode schemaNode;

    // real instance created via reflection
    private JsonSchema jsonSchema;

    // spy that allows us to stub getSubSchema
    private JsonSchema spySchema;

    /**
     * Simple {@link JsonNodePath} that allows us to control the path type and its string
     * representation.
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
            super(uri == null ? null : AbsoluteIri.of(uri));
            this.absoluteIri = uri == null ? null : AbsoluteIri.of(uri);
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

    /**
     * Test subclass of {@link JsonSchema} that lets us stub {@code getSubSchema}.
     */
    private static class TestJsonSchema extends JsonSchema {

        private JsonSchema subSchemaResult;

        private JsonNodePath capturedFragment;

        private int callCount = 0;

        TestJsonSchema(ValidationContext ctx, SchemaLocation loc, JsonNodePath evalPath, JsonNode schemaNode, JsonSchema parent, boolean something) throws Exception {
            super(ctx, loc, evalPath, schemaNode, parent, something);
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

        @Override
        protected JsonSchema getSubSchema(JsonNodePath fragment) {
            callCount++;
            capturedFragment = fragment;
            return subSchemaResult;
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
        // create a spy‑like instance that we can control
        spySchema = new TestJsonSchema(validationContext, schemaLocation, new JsonNodePath(PathType.JSON_POINTER), null, null, false);
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
    private static JsonSchema mock(JsonSchema ignored) {
        return instance;
    }

    @Test
    void getRefSchema_whenFragmentIsJsonPointer_delegatesToGetSubSchema() {
        // Arrange – a fragment whose type is JSON_POINTER
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_POINTER);
        JsonSchema expected = mock(createJsonSchema(validationContext, schemaLocation));
        spySchema.setSubSchemaResult(expected);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertSame(expected, actual, "Expected delegation to getSubSchema for JSON‑Pointer fragments");
        assertEquals(1, spySchema.getCallCount(), "getSubSchema should be called exactly once");
        assertSame(fragment, spySchema.getCapturedFragment(), "Fragment passed to getSubSchema should be the same");
    }

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

    @Test
    void getRefSchema_whenJsonPointerReturnsNull_propagatesNull() {
        // Arrange
        JsonNodePath fragment = new SimpleJsonNodePath(PathType.JSON_POINTER);
        spySchema.setSubSchemaResult(null);
        // Act
        JsonSchema actual = spySchema.getRefSchema(fragment);
        // Assert
        assertNull(actual, "Null result from getSubSchema must be propagated");
        assertEquals(1, spySchema.getCallCount(), "getSubSchema should be called exactly once");
    }

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
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:109: error: incompatible types: URI cannot be converted to String
            super(uri == null ? null : AbsoluteIri.of(uri));
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:110: error: incompatible types: URI cannot be converted to String
            this.absoluteIri = uri == null ? null : AbsoluteIri.of(uri);
                                                                   ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:144: error: constructor JsonSchema in class JsonSchema cannot be applied to given types;
            super(ctx, loc, evalPath, schemaNode, parent, something);
            ^
  required: ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean
  found:    ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean
  reason: JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) has private access in JsonSchema
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:160: error: getSubSchema(JsonNodePath) in TestJsonSchema cannot override getSubSchema(JsonNodePath) in JsonSchema
        protected JsonSchema getSubSchema(JsonNodePath fragment) {
                             ^
  attempting to assign weaker access privileges; was public
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:198: error: cannot find symbol
        return instance;
               ^
  symbol:   variable instance
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:206: error: cannot find symbol
        spySchema.setSubSchemaResult(expected);
                 ^
  symbol:   method setSubSchemaResult(JsonSchema)
  location: variable spySchema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:211: error: cannot find symbol
        assertEquals(1, spySchema.getCallCount(), "getSubSchema should be called exactly once");
                                 ^
  symbol:   method getCallCount()
  location: variable spySchema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:212: error: cannot find symbol
        assertSame(fragment, spySchema.getCapturedFragment(), "Fragment passed to getSubSchema should be the same");
                                      ^
  symbol:   method getCapturedFragment()
  location: variable spySchema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:249: error: cannot find symbol
        spySchema.setSubSchemaResult(null);
                 ^
  symbol:   method setSubSchemaResult(<null>)
  location: variable spySchema of type JsonSchema
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:254: error: cannot find symbol
        assertEquals(1, spySchema.getCallCount(), "getSubSchema should be called exactly once");
                                 ^
  symbol:   method getCallCount()
  location: variable spySchema of type JsonSchema
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
12 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\trelated functions:\n\t\tmethod `protected JsonNodePath atRoot()` returns `com.networknt.schema.JsonNodePath`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`, api document: \n     * Get the root path.\n     *\n     * @return The path.\n     ;\n\t\tmethod `private static void checkForExplicitDiscriminatorMappingMatch(DiscriminatorContext, String, JsonNode, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\n\t\tmethod `private static boolean noExplicitDiscriminatorKeyOverride(JsonNode, JsonSchema)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.PathType`:\n\tdependent fields:\n\t\tcom.networknt.schema.PathType com.networknt.schema.PathType.JSON_POINTER;\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


