
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.SpecVersion.VersionFlag;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchema_Test {

    private ValidationContext validationContext;

    private SchemaLocation schemaLocation;

    private JsonNodePath evaluationPath;

    private JsonSchema realSchema;

    private JsonSchema spySchema;

    private ObjectMapper mapper;

    private JsonNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // Minimal empty schema node.
        mapper = new ObjectMapper();
        schemaNode = mapper.createObjectNode();
        // Build a ValidationContext using the default meta‑schema and a fresh config.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);
        // Absolute IRI for the root schema.
        schemaLocation = new SchemaLocation(new AbsoluteIri("http://example.org/schema.json"));
        // Evaluation path – root with JSON_POINTER type.
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // Create a JsonSchema instance (package‑private factory method).
        realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        // Spy so that we can stub getSubSchema for the JSON‑Pointer test.
        spySchema = spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid interference between tests.
        validationContext.getSchemaResources().clear();
        validationContext.getDynamicAnchors().clear();
    }

    /* --------------------------------------------------------------
     *  Helper method to set private fields via reflection.
     * -------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> clazz = target.getClass();
        // Walk up the class hierarchy to find the field.
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new IllegalStateException("Field '" + fieldName + "' not found in class hierarchy");
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testGetRefSchemaWithJsonPointer() {
        // Fragment with JSON_POINTER type – should delegate to getSubSchema().
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema expectedSubSchema = mock(JsonSchema.class);
        doReturn(expectedSubSchema).when(spySchema).getSubSchema(fragment);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertSame(expectedSubSchema, result, "When the fragment is a JSON pointer the method must return the sub‑schema");
    }

    @Test
    void testGetRefSchemaWithAnchorFoundInResources() {
        // Anchor fragment (non‑JSON_POINTER) that exists in schemaResources.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("myAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#myAnchor";
        JsonSchema expectedAnchorSchema = mock(JsonSchema.class);
        validationContext.getSchemaResources().put(anchorKey, expectedAnchorSchema);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertSame(expectedAnchorSchema, result, "The method must return the schema stored in schemaResources for the anchor");
    }

    @Test
    void testGetRefSchemaWithAnchorFoundInDynamicAnchors() {
        // Anchor fragment that is only present in dynamicAnchors.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("dynAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#dynAnchor";
        JsonSchema expectedAnchorSchema = mock(JsonSchema.class);
        // Ensure the key is not in schemaResources.
        validationContext.getSchemaResources().remove(anchorKey);
        validationContext.getDynamicAnchors().put(anchorKey, expectedAnchorSchema);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertSame(expectedAnchorSchema, result, "If the anchor is not in schemaResources it must be looked up in dynamicAnchors");
    }

    @Test
    void testGetRefSchemaWithAnchorNotFoundThrowsException() {
        // Anchor fragment that cannot be resolved.
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("missingAnchor");
        String anchorKey = schemaLocation.getAbsoluteIri().toString() + "#missingAnchor";
        // Ensure both maps are empty for this key.
        validationContext.getSchemaResources().remove(anchorKey);
        validationContext.getDynamicAnchors().remove(anchorKey);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> spySchema.getRefSchema(fragment), "An unknown anchor must cause a JsonSchemaException");
        assertEquals("Unable to find anchor " + anchorKey, ex.getMessage(), "Exception message should contain the missing anchor identifier");
    }

    @Test
    void testGetRefSchemaJsonPointerSubSchemaReturnsNull() {
        // Stub getSubSchema to return null and verify the method propagates null.
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        doReturn(null).when(spySchema).getSubSchema(fragment);
        JsonSchema result = spySchema.getRefSchema(fragment);
        assertNull(result, "When getSubSchema returns null the method must propagate null");
        // Verify getSubSchema was called exactly once.
        verify(spySchema, times(1)).getSubSchema(fragment);
    }

    @Test
    void testGetRefSchemaWithNullFragmentThrowsNPE() {
        // Passing a null fragment should result in a NullPointerException.
        assertThrows(NullPointerException.class, () -> spySchema.getRefSchema(null), "Calling getRefSchema with a null fragment must throw NPE");
    }

    @Test
    void testGetRefSchemaWithNullSchemaLocationThrowsNPE() throws Exception {
        // Create a schema with a valid location then set it to null via reflection.
        JsonSchema schemaWithNullLocation = spy(realSchema);
        setPrivateField(schemaWithNullLocation, "schemaLocation", null);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        // The method should attempt to call getSchemaLocation().getAbsoluteIri()
        // and therefore throw NPE.
        assertThrows(NullPointerException.class, () -> schemaWithNullLocation.getRefSchema(fragment), "If schemaLocation is null a NPE should be thrown before any lookup");
    }

    @Test
    void testGetRefSchemaWithNullValidationContextThrowsNPE() throws Exception {
        // Set the validationContext field to null via reflection.
        JsonSchema schemaWithNullContext = spy(realSchema);
        setPrivateField(schemaWithNullContext, "validationContext", null);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        assertThrows(NullPointerException.class, () -> schemaWithNullContext.getRefSchema(fragment), "If validationContext is null a NPE should be thrown when accessing it");
    }

    @Test
    void testGetRefSchemaWithNullSchemaResourcesMapThrowsNPE() {
        // Mock ValidationContext to return null for getSchemaResources().
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getSchemaResources()).thenReturn(null);
        when(mockContext.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        JsonSchema schema = JsonSchema.from(mockContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "A null schemaResources map must cause a NPE on .get(anchor)");
    }

    @Test
    void testGetRefSchemaWithNullDynamicAnchorsMapThrowsNPE() {
        // Mock ValidationContext to return a valid map for resources but null for dynamicAnchors.
        ValidationContext mockContext = mock(ValidationContext.class);
        when(mockContext.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(mockContext.getDynamicAnchors()).thenReturn(null);
        JsonSchema schema = JsonSchema.from(mockContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonNodePath fragment = mock(JsonNodePath.class);
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        when(fragment.toString()).thenReturn("anyAnchor");
        assertThrows(NullPointerException.class, () -> schema.getRefSchema(fragment), "A null dynamicAnchors map must cause a NPE on fallback lookup");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:7: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:9: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:13: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:16: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:50: error: cannot find symbol
        spySchema = spy(realSchema);
                    ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:86: error: cannot find symbol
        JsonSchema expectedSubSchema = mock(JsonSchema.class);
                                       ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:87: error: cannot find symbol
        doReturn(expectedSubSchema).when(spySchema).getSubSchema(fragment);
        ^
  symbol:   method doReturn(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:95: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:96: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:97: error: cannot find symbol
        when(fragment.toString()).thenReturn("myAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:99: error: cannot find symbol
        JsonSchema expectedAnchorSchema = mock(JsonSchema.class);
                                          ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:108: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:109: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:110: error: cannot find symbol
        when(fragment.toString()).thenReturn("dynAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:112: error: cannot find symbol
        JsonSchema expectedAnchorSchema = mock(JsonSchema.class);
                                          ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:123: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:124: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:125: error: cannot find symbol
        when(fragment.toString()).thenReturn("missingAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:138: error: cannot find symbol
        doReturn(null).when(spySchema).getSubSchema(fragment);
        ^
  symbol:   method doReturn(<null>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:142: error: cannot find symbol
        verify(spySchema, times(1)).getSubSchema(fragment);
                          ^
  symbol:   method times(int)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:154: error: cannot find symbol
        JsonSchema schemaWithNullLocation = spy(realSchema);
                                            ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:156: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:157: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:158: error: cannot find symbol
        when(fragment.toString()).thenReturn("anyAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:167: error: cannot find symbol
        JsonSchema schemaWithNullContext = spy(realSchema);
                                           ^
  symbol:   method spy(JsonSchema)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:169: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:170: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:171: error: cannot find symbol
        when(fragment.toString()).thenReturn("anyAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:178: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:179: error: cannot find symbol
        when(mockContext.getSchemaResources()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:180: error: cannot find symbol
        when(mockContext.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:182: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:183: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:184: error: cannot find symbol
        when(fragment.toString()).thenReturn("anyAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:191: error: cannot find symbol
        ValidationContext mockContext = mock(ValidationContext.class);
                                        ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:192: error: cannot find symbol
        when(mockContext.getSchemaResources()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:193: error: cannot find symbol
        when(mockContext.getDynamicAnchors()).thenReturn(null);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:195: error: cannot find symbol
        JsonNodePath fragment = mock(JsonNodePath.class);
                                ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:196: error: cannot find symbol
        when(fragment.getPathType()).thenReturn(PathType.JSON_PATH);
        ^
  symbol:   method when(PathType)
  location: class JsonSchema_getRefSchema_Test
src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java:197: error: cannot find symbol
        when(fragment.toString()).thenReturn("anyAnchor");
        ^
  symbol:   method when(String)
  location: class JsonSchema_getRefSchema_Test
Note: src/test/java/com/networknt/schema/JsonSchema_getRefSchema_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
40 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\trelated functions:\n\t\tmethod `protected JsonNodePath atRoot()` returns `com.networknt.schema.JsonNodePath`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`, api document: \n     * Get the root path.\n     *\n     * @return The path.\n     ;\nclass `com.networknt.schema.PathType`:\n\tdependent fields:\n\t\tcom.networknt.schema.PathType com.networknt.schema.PathType.JSON_POINTER;\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.getRefSchema(JsonNodePath)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


