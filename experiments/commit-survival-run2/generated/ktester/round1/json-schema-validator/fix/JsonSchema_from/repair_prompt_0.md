
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    // Common test data
    private static final String SCHEMA_PATH = "$.definitions.test";

    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");

    @Mock
    ValidationContext // mocked dependency
    validationContext;

    @Mock
    JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

    @Mock
    JsonSchema // parent schema (can be null in tests)
    parentSchema;

    @Mock
    SchemaValidatorsConfig // configuration object inside ValidationContext
    config;

    @Mock
    ValidationContext.DiscriminatorContext // for OpenAPI3 style discriminators
    discriminatorContext;

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing that is common to most tests
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to avoid cross‑test interference
        clearInvocations(validationContext, metaSchema, parentSchema, config, discriminatorContext);
    }

    /**
     * Retrieves a private field value from the given {@link JsonSchema} instance using reflection.
     *
     * @param schema    the JsonSchema instance
     * @param fieldName the name of the private field
     * @return the field value (may be {@code null})
     */
    private static Object getPrivateField(JsonSchema schema, String fieldName) {
        try {
            // The field may be declared in JsonSchema or a superclass (BaseJsonValidator)
            Class<?> cls = schema.getClass();
            while (cls != null) {
                try {
                    Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(schema);
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            fail("Field '" + fieldName + "' not found in JsonSchema hierarchy");
            // unreachable
            return null;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Convenience wrapper to obtain the {@code schemaPath} stored inside the JsonSchema.
     *
     * @param schema the JsonSchema instance
     * @return the schemaPath string
     */
    private static String getSchemaPath(JsonSchema schema) {
        return (String) getPrivateField(schema, "schemaPath");
    }

    /**
     * Convenience wrapper to obtain the {@code suppressSubSchemaRetrieval} flag stored inside the JsonSchema.
     *
     * @param schema the JsonSchema instance
     * @return the flag value
     */
    private static boolean getSuppressFlag(JsonSchema schema) {
        return (boolean) getPrivateField(schema, "suppressSubSchemaRetrieval");
    }

    /**
     * Factory method that forwards directly to the package‑private {@code JsonSchema.from(...)}.
     */
    private static JsonSchema createSchema(ValidationContext vc, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent, boolean suppress) {
        return JsonSchema.from(vc, schemaPath, currentUri, schemaNode, parent, suppress);
    }

    private static ValidationContext newValidationContext() {
        // default constructor – sufficient for our tests
        return new ValidationContext();
    }

    /**
     * Verify that the static {@code from} method creates a non‑null {@link JsonSchema}
     * instance when the {@link ValidationContext} does not provide a {@link SchemaValidatorsConfig}
     * (i.e. {@code getConfig()} returns {@code null}).
     */
    @Test
    void from_WithNullConfig_ReturnsJsonSchema() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(null);
        JsonNode schemaNode = mock(JsonNode.class);
        // Act
        JsonSchema result = JsonSchema.from(validationContext, SCHEMA_PATH, CURRENT_URI, schemaNode, parentSchema, false);
        // Assert
        assertNotNull(result, "JsonSchema.from should never return null");
        // No further interaction verification is required – the constructor should simply complete.
    }

    /**
     * Verify that when a non‑null {@link SchemaValidatorsConfig} is supplied,
     * the constructor queries the configuration for keyword walk listeners.
     */
    @Test
    void from_WithNonNullConfig_InvokesKeywordWalkListenerSetup() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        JsonNode schemaNode = mock(JsonNode.class);
        // Act
        JsonSchema result = JsonSchema.from(validationContext, SCHEMA_PATH, CURRENT_URI, schemaNode, parentSchema, false);
        // Assert
        assertNotNull(result, "JsonSchema.from should produce a JsonSchema instance");
        // Verify that the config was consulted for the keyword‑listener map
        verify(config, times(1)).getKeywordWalkListenersMap();
        // Since discriminators are disabled, no further discriminator interaction should happen
        verify(config, never()).isOpenAPI3StyleDiscriminators();
    }

    /**
     * Verify the OpenAPI‑3 style discriminator registration path.
     * When the configuration enables discriminators and the schema node contains a
     * {@code discriminator} property, the {@link ValidationContext.DiscriminatorContext}
     * should be asked to register it.
     */
    @Test
    void from_WithOpenApiDiscriminators_RegistersDiscriminator() {
        // Arrange
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discriminatorContext);
        // Build a minimal ObjectNode that contains a "discriminator" field
        ObjectNode schemaNode = new ObjectNode(JsonNodeFactory.instance);
        ObjectNode discriminatorNode = new ObjectNode(JsonNodeFactory.instance);
        discriminatorNode.put("propertyName", "type");
        schemaNode.set("discriminator", discriminatorNode);
        // Act
        JsonSchema result = JsonSchema.from(validationContext, SCHEMA_PATH, CURRENT_URI, schemaNode, parentSchema, false);
        // Assert
        assertNotNull(result, "JsonSchema.from should still return a JsonSchema instance");
        // Verify that the discriminator registration was attempted
        verify(discriminatorContext, times(1)).registerDiscriminator(eq(SCHEMA_PATH), eq(discriminatorNode));
    }

    @Test
    void from_StandardParameters_CreatesInstance() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/properties/name";
        URI uri = URI.create("http://example.com/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = createSchema(vc, schemaPath, uri, node, null, false);
        assertNotNull(schema, "Factory should return a non‑null JsonSchema");
        assertEquals(uri, schema.getCurrentUri(), "Current URI must be stored unchanged");
        assertEquals(schemaPath, getSchemaPath(schema), "Schema path must be stored unchanged");
        assertFalse(getSuppressFlag(schema), "Suppress flag should be false");
    }

    @Test
    void from_NullParentAndSuppressTrue_CreatesInstance() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#";
        URI uri = URI.create("file:/tmp/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();
        JsonSchema schema = createSchema(vc, schemaPath, uri, node, null, true);
        assertNotNull(schema);
        assertTrue(getSuppressFlag(schema), "Suppress flag must be true");
        assertNull(getPrivateField(schema, "parentSchema"), "Parent should be null");
    }

    @Test
    void from_EmptyPathAndNullUri_StoresValuesCorrectly() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "";
        URI uri = null;
        JsonNode node = JsonNodeFactory.instance.objectNode();
        // a parent schema with normal values
        JsonSchema parent = createSchema(newValidationContext(), "#/root", URI.create("http://example.com"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema schema = createSchema(vc, schemaPath, uri, node, parent, false);
        assertNotNull(schema);
        assertNull(schema.getCurrentUri(), "Current URI should be null");
        assertEquals("", getSchemaPath(schema), "Schema path should be the empty string");
    }

    @Test
    void from_NullValidationContext_ThrowsNPE() {
        String schemaPath = "#/properties/id";
        URI uri = URI.create("http://example.com/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();
        Executable ctor = () -> createSchema(null, schemaPath, uri, node, null, false);
        assertThrows(NullPointerException.class, ctor);
    }

    @Test
    void from_NullSchemaNode_ThrowsNPE() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/properties/age";
        URI uri = URI.create("http://example.com/person.json");
        Executable ctor = () -> createSchema(vc, schemaPath, uri, null, null, false);
        assertThrows(NullPointerException.class, ctor);
    }

    @Test
    void from_RelativeUri_StoresUriUnmodified() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/components/schemas/Address";
        // ends with '/' – treated as base
        URI uri = URI.create("http://example.com/api/v1/");
        JsonSchema schema = createSchema(vc, schemaPath, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertEquals(uri, schema.getCurrentUri());
        assertEquals(schemaPath, getSchemaPath(schema));
    }

    @Test
    void from_SuppressTrueWithParent_StoresAllValues() {
        ValidationContext vc = newValidationContext();
        JsonSchema parent = createSchema(vc, "#", URI.create("file:/opt/schemas/root.json"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema schema = createSchema(vc, "#/properties/email", URI.create("file:/opt/schemas/user.json"), JsonNodeFactory.instance.objectNode(), parent, true);
        assertNotNull(schema);
        assertTrue(getSuppressFlag(schema));
        assertSame(parent, getPrivateField(schema, "parentSchema"));
    }

    @Test
    void from_EscapedSchemaPath_PreservesEscapes() {
        ValidationContext vc = newValidationContext();
        String escapedPath = "#/properties/~1special~0name";
        URI uri = URI.create("http://example.com/complex.json");
        JsonSchema schema = createSchema(vc, escapedPath, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertEquals(escapedPath, getSchemaPath(schema), "Escaped characters must not be decoded or altered");
    }

    @Test
    void from_UriWithQueryAndFragment_StoresExactly() {
        ValidationContext vc = newValidationContext();
        String path = "#/definitions/item";
        URI uri = URI.create("http://example.com/api?version=2#section");
        JsonSchema schema = createSchema(vc, path, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertEquals(uri, schema.getCurrentUri());
        assertEquals(path, getSchemaPath(schema));
    }

    @Test
    void from_MultiLevelParent_FindAncestorReturnsRoot() {
        ValidationContext vcRoot = newValidationContext();
        JsonSchema root = createSchema(vcRoot, "#", URI.create("http://example.com/root.json"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema middle = createSchema(vcRoot, "#/properties/middle", URI.create("http://example.com/middle.json"), JsonNodeFactory.instance.objectNode(), root, false);
        JsonSchema child = createSchema(vcRoot, "#/properties/inner", URI.create("http://example.com/outer.json"), JsonNodeFactory.instance.objectNode(), middle, false);
        JsonSchema ancestor = child.findAncestor();
        assertNotNull(ancestor);
        assertEquals("#", getSchemaPath(ancestor), "Ancestor should be the root schema with path \"#\"");
    }

    @Test
    void from_NullSchemaPath_ThrowsNPE() {
        ValidationContext vc = newValidationContext();
        URI uri = URI.create("http://example.com/schema.json");
        Executable ctor = () -> createSchema(vc, null, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertThrows(NullPointerException.class, ctor);
    }

    @Test
    void from_InvalidSchemaPathFormat_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        // missing leading '#'
        String badPath = "properties/name";
        Executable ctor = () -> createSchema(vc, badPath, URI.create("http://example.com/schema.json"), JsonNodeFactory.instance.objectNode(), null, false);
        assertThrows(IllegalArgumentException.class, ctor);
    }

    @Test
    void from_NonObjectSchemaNode_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        String path = "#/definitions/list";
        Executable ctor = () -> createSchema(vc, path, URI.create("http://example.com/schema.json"), JsonNodeFactory.instance.arrayNode(), null, false);
        assertThrows(IllegalArgumentException.class, ctor);
    }

    @Test
    void from_ParentWithDifferentValidationContext_ThrowsIllegalStateException() {
        ValidationContext vcChild = newValidationContext();
        // distinct instance
        ValidationContext vcParent = newValidationContext();
        JsonSchema parent = createSchema(vcParent, "#", URI.create("http://example.com/parent.json"), JsonNodeFactory.instance.objectNode(), null, false);
        Executable ctor = () -> createSchema(vcChild, "#/properties/child", URI.create("http://example.com/child.json"), JsonNodeFactory.instance.objectNode(), parent, false);
        assertThrows(IllegalStateException.class, ctor);
    }

    @Test
    void from_SelfReferentialParent_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        JsonSchema selfParent = JsonSchema.from(vc, "#", URI.create("http://example.com/self.json"), JsonNodeFactory.instance.objectNode(), null, false);
        Executable ctor = () -> createSchema(vc, "#/properties/self", URI.create("http://example.com/self.json"), JsonNodeFactory.instance.objectNode(), // parent points to the same instance that will be returned by from()
        selfParent, false);
        // The implementation should detect the circular reference and reject it.
        assertThrows(IllegalArgumentException.class, ctor);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:8: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:9: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:14: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:18: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:19: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:21: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:29: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:41: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:45: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:52: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:58: error: cannot find symbol
        clearInvocations(validationContext, metaSchema, parentSchema, config, discriminatorContext);
        ^
  symbol:   method clearInvocations(ValidationContext,JsonMetaSchema,JsonSchema,SchemaValidatorsConfig,DiscriminatorContext)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:118: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
        return new ValidationContext();
               ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    no arguments
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:129: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:130: error: cannot find symbol
        JsonNode schemaNode = mock(JsonNode.class);
                              ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:145: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:146: error: cannot find symbol
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(Map<String,List<JsonSchemaWalkListener>>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:147: error: cannot find symbol
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:148: error: cannot find symbol
        JsonNode schemaNode = mock(JsonNode.class);
                              ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:154: error: cannot find symbol
        verify(config, times(1)).getKeywordWalkListenersMap();
                       ^
  symbol:   method times(int)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:156: error: cannot find symbol
        verify(config, never()).isOpenAPI3StyleDiscriminators();
                       ^
  symbol:   method never()
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:168: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:169: error: cannot find symbol
        when(config.getKeywordWalkListenersMap()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(Map<String,List<JsonSchemaWalkListener>>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:170: error: cannot find symbol
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:171: error: cannot find symbol
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discriminatorContext);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:182: error: cannot find symbol
        verify(discriminatorContext, times(1)).registerDiscriminator(eq(SCHEMA_PATH), eq(discriminatorNode));
                                     ^
  symbol:   method times(int)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:182: error: cannot find symbol
        verify(discriminatorContext, times(1)).registerDiscriminator(eq(SCHEMA_PATH), eq(discriminatorNode));
                                                                     ^
  symbol:   method eq(String)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:182: error: cannot find symbol
        verify(discriminatorContext, times(1)).registerDiscriminator(eq(SCHEMA_PATH), eq(discriminatorNode));
                                                                                      ^
  symbol:   method eq(ObjectNode)
  location: class JsonSchema_from_Test
29 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)`, api document: \n     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.\n     ;\n\t\tmethod ` static JsonSchema from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


