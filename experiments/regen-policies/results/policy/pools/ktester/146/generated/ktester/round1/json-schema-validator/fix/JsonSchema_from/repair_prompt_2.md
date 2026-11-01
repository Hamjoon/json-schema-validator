
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.net.URI;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    /**
     * Shared ObjectMapper for creating JsonNode instances.
     */
    private static ObjectMapper mapper;

    /**
     * ValidationContext used by all test cases.
     */
    private ValidationContext validationContext;

    /**
     * Shared ObjectMapper for creating {@link JsonNode} instances.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Minimal dummy implementation of {@link SchemaLocation}.
     */
    private static final class DummySchemaLocation implements SchemaLocation {

        @Override
        public String toString() {
            return "mocked-schema-location";
        }

        @Override
        public URI getAbsoluteIri() {
            return null;
        }
    }

    /**
     * Minimal dummy implementation of {@link JsonNodePath}.
     */
    private static final class DummyJsonNodePath implements JsonNodePath {
        // No behaviour required for the current tests.
    }

    /**
     * Minimal dummy implementation of {@link JsonSchema} used only as a parent placeholder.
     */
    private static final class DummyParentSchema extends JsonSchema {

        DummyParentSchema() {
            super(null, null, null, null, null, false);
        }
    }

    /**
     * Simple {@link SchemaLocation} implementation used in tests.
     */
    private static final class SimpleSchemaLocation extends SchemaLocation {

        SimpleSchemaLocation() {
            super("dummy-location");
        }
    }

    /**
     * Simple {@link JsonNodePath} implementation used in tests (no behaviour needed).
     */
    private static final class SimpleJsonNodePath extends JsonNodePath {
        // No additional behaviour required for the current test scenarios.
    }

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Build a minimal but functional ValidationContext (no external resources needed).
        // library provides a static instance
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal but functional ValidationContext (no external resources needed).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012Instance();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config, new java.util.concurrent.ConcurrentHashMap<>(), new java.util.concurrent.ConcurrentHashMap<>(), new java.util.concurrent.ConcurrentHashMap<>());
        // Re‑initialise the context before each test to avoid cross‑test contamination.
        initValidationContext();
    }

    /**
     * Creates a mocked {@link SchemaLocation} that safely returns a string.
     */
    private static SchemaLocation mockSchemaLocation() {
        SchemaLocation location = mock(SchemaLocation.class);
        when(location.toString()).thenReturn("mocked-schema-location");
        when(location.getAbsoluteIri()).thenReturn(null);
        return location;
    }

    /**
     * Creates a mocked {@link JsonNodePath}.
     */
    private static JsonNodePath mockJsonNodePath() {
        return mock(JsonNodePath.class);
    }

    /**
     * Creates a mocked parent {@link JsonSchema}.
     */
    private static JsonSchema mockParentSchema() {
        return mock(JsonSchema.class);
    }

    /**
     * Supplies the data for {@link #successfulCreation(...)}.
     */
    private static Stream<Arguments> successfulCreationProvider() {
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        JsonNode emptyArray = new ArrayNode(JsonNodeFactory.instance);
        JsonNode trueBoolean = BooleanNode.TRUE;
        JsonNode textNode = new TextNode("example");
        JsonSchema parent = createParentSchema();
        return Stream.of(// all non‑null arguments, suppress = false
        Arguments.of("All arguments non‑null, suppress=false", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, parent, false), // all non‑null arguments, suppress = true
        Arguments.of("All arguments non‑null, suppress=true", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, parent, true), // optional nulls (location, path, parent) – still succeeds
        Arguments.of("Optional nulls (location, path, parent)", validationContext, null, null, emptyObject, null, false), // various JsonNode implementations
        Arguments.of("ArrayNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyArray, parent, false), Arguments.of("BooleanNode (true) as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), trueBoolean, null, false), Arguments.of("TextNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), textNode, parent, true));
    }

    private static Stream<Arguments> nullMandatoryProvider() {
        JsonNode dummyNode = new ObjectNode(JsonNodeFactory.instance);
        JsonSchema parent = createParentSchema();
        return Stream.of(Arguments.of("validationContext is null", null, new SimpleSchemaLocation(), new SimpleJsonNodePath(), dummyNode, parent, false), Arguments.of("schemaNode is null", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), null, parent, false));
    }

    /**
     * Helper to create a minimal parent {@link JsonSchema} instance.
     */
    private static JsonSchema createParentSchema() {
        // An empty object schema is sufficient as a parent placeholder.
        JsonNode emptyObject = new ObjectNode(JsonNodeFactory.instance);
        return JsonSchema.from(validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, null, false);
    }

    /**
     * Verifies that {@code JsonSchema.from(...)} creates a usable {@link JsonSchema}
     * instance with the supplied arguments.
     */
    @Test
    void fromCreatesJsonSchemaInstance() throws Exception {
        JsonNode schemaNode = MAPPER.readTree("{}");
        SchemaLocation schemaLocation = new SimpleSchemaLocation();
        JsonNodePath evaluationPath = new SimpleJsonNodePath();
        JsonSchema result = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(), "The ValidationContext inside the created JsonSchema must be the one supplied");
        assertNotNull(result.getValidators(), "Validators list should be initialised (may be empty)");
        assertTrue(result.getValidators().isEmpty(), "An empty schema should result in an empty validators list");
    }

    /**
     * Parameterized test that verifies successful creation of {@link JsonSchema}
     * for a wide range of argument combinations where no exception is expected.
     *
     * The test data is supplied by {@link #successfulCreationProvider()}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulCreationProvider")
    void successfulCreation(String description, ValidationContext ctx, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        JsonSchema result = JsonSchema.from(ctx, schemaLocation, evaluationPath, schemaNode, parent, suppressSubSchemaRetrieval);
        assertNotNull(result, description + " – result must not be null");
        assertSame(ctx, result.getValidationContext(), description + " – ValidationContext must be the one supplied");
        assertNotNull(result.getValidators(), description + " – validators list must be initialised");
    }

    /**
     * Tests that a {@link NullPointerException} is thrown when mandatory parameters
     * are {@code null}.  Two distinct scenarios are covered.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullMandatoryProvider")
    void nullMandatoryParameters(String description, ValidationContext ctx, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppress) {
        assertThrows(NullPointerException.class, () -> JsonSchema.from(ctx, schemaLocation, evaluationPath, schemaNode, parent, suppress), description);
    }

    /**
     * Tests that a malformed {@code $ref} containing an illegal percent‑encoding
     * triggers an {@link UnsupportedEncodingException}.
     *
     * The schema node is built as {@code {"$ref":"http://example.com/invalid%ZZschema.json"}}.
     * The constructor of {@link JsonSchema} attempts to URL‑decode the reference,
     * which should raise the exception.
     */
    @Test
    void malformedRefThrowsUnsupportedEncodingException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "http://example.com/invalid%ZZschema.json");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(UnsupportedEncodingException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, createParentSchema(), false), "Malformed $ref should cause UnsupportedEncodingException");
    }

    /**
     * Tests that a {@code $ref} pointing to a non‑existent external resource
     * results in an {@link IOException}.
     *
     * The actual IOException is thrown by the internal schema loading logic.
     */
    @Test
    void nonExistentRefThrowsIOException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "http://nonexistent.example.com/schema.json");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(IOException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, createParentSchema(), false), "Non‑existent $ref should cause IOException");
    }

    /**
     * Tests that providing a primitive numeric {@link JsonNode} (e.g., {@code IntNode})
     * leads to an {@link IllegalArgumentException}.
     *
     * The constructor expects a schema node that can represent a JSON‑Schema
     * (object, array or boolean).  Supplying a scalar numeric node is illegal.
     */
    @Test
    void primitiveNumericNodeThrowsIllegalArgumentException() throws Exception {
        JsonNode numericNode = new IntNode(123);
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, location, path, numericNode, createParentSchema(), false), "Numeric schema node should cause IllegalArgumentException");
    }

    /**
     * Tests that a circular reference (self‑referencing {@code $ref}) results in a
     * {@link JsonSchemaException}.
     *
     * The schema node contains {@code {"$ref":"#"}} which points to itself.
     */
    @Test
    void circularReferenceThrowsJsonSchemaException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "#");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(JsonSchemaException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, createParentSchema(), false), "Self‑referencing $ref should cause JsonSchemaException");
    }

    /**
     * Tests that when a {@code $ref} needs to be resolved against a parent schema
     * but the {@code parent} argument is {@code null}, an {@link IllegalStateException}
     * is thrown.
     *
     * The schema node contains a relative reference that requires a parent for resolution.
     */
    @Test
    void refRequiresParentButParentIsNullThrowsIllegalStateException() throws Exception {
        ObjectNode schemaNode = MAPPER.createObjectNode().put("$ref", "#/definitions/foo");
        SchemaLocation location = new SimpleSchemaLocation();
        JsonNodePath path = new SimpleJsonNodePath();
        assertThrows(IllegalStateException.class, () -> JsonSchema.from(validationContext, location, path, schemaNode, null, false), "Missing parent for $ref resolution should cause IllegalStateException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:18: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:43: error: interface expected here
    private static final class DummySchemaLocation implements SchemaLocation {
                                                              ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:59: error: interface expected here
    private static final class DummyJsonNodePath implements JsonNodePath {
                                                            ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:22: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:50: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:69: error: constructor JsonSchema in class JsonSchema cannot be applied to given types;
            super(null, null, null, null, null, false);
            ^
  required: ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean
  found:    <null>,<null>,<null>,<null>,<null>,boolean
  reason: JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) has private access in JsonSchema
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:79: error: incompatible types: String cannot be converted to AbsoluteIri
            super("dummy-location");
                  ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:86: error: no suitable constructor found for JsonNodePath(no arguments)
    private static final class SimpleJsonNodePath extends JsonNodePath {
                         ^
    constructor JsonNodePath.JsonNodePath(PathType) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,String) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonNodePath.JsonNodePath(JsonNodePath,int) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:98: error: non-static variable validationContext cannot be referenced from a static context
        validationContext = new ValidationContext(metaSchema, factory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:99: error: variable metaSchema is already defined in method setupBeforeAll()
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
                       ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:100: error: variable factory is already defined in method setupBeforeAll()
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
                          ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:101: error: variable config is already defined in method setupBeforeAll()
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
                               ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:102: error: non-static variable validationContext cannot be referenced from a static context
        validationContext = new ValidationContext(metaSchema, factory, config, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:108: error: cannot find symbol
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012Instance();
                                                  ^
  symbol:   method getV202012Instance()
  location: class JsonMetaSchema
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:113: error: cannot find symbol
        initValidationContext();
        ^
  symbol:   method initValidationContext()
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:120: error: cannot find symbol
        SchemaLocation location = mock(SchemaLocation.class);
                                  ^
  symbol:   method mock(Class<SchemaLocation>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:121: error: cannot find symbol
        when(location.toString()).thenReturn("mocked-schema-location");
        ^
  symbol:   method when(String)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:122: error: cannot find symbol
        when(location.getAbsoluteIri()).thenReturn(null);
        ^
  symbol:   method when(AbsoluteIri)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:130: error: cannot find symbol
        return mock(JsonNodePath.class);
               ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:137: error: cannot find symbol
        return mock(JsonSchema.class);
               ^
  symbol:   method mock(Class<JsonSchema>)
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:150: error: non-static variable validationContext cannot be referenced from a static context
        Arguments.of("All arguments non‑null, suppress=false", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, parent, false), // all non‑null arguments, suppress = true
                                                               ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:151: error: non-static variable validationContext cannot be referenced from a static context
        Arguments.of("All arguments non‑null, suppress=true", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, parent, true), // optional nulls (location, path, parent) – still succeeds
                                                              ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:152: error: non-static variable validationContext cannot be referenced from a static context
        Arguments.of("Optional nulls (location, path, parent)", validationContext, null, null, emptyObject, null, false), // various JsonNode implementations
                                                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:153: error: non-static variable validationContext cannot be referenced from a static context
        Arguments.of("ArrayNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyArray, parent, false), Arguments.of("BooleanNode (true) as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), trueBoolean, null, false), Arguments.of("TextNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), textNode, parent, true));
                                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:153: error: non-static variable validationContext cannot be referenced from a static context
        Arguments.of("ArrayNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyArray, parent, false), Arguments.of("BooleanNode (true) as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), trueBoolean, null, false), Arguments.of("TextNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), textNode, parent, true));
                                                                                                                                                                                                      ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:153: error: non-static variable validationContext cannot be referenced from a static context
        Arguments.of("ArrayNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyArray, parent, false), Arguments.of("BooleanNode (true) as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), trueBoolean, null, false), Arguments.of("TextNode as schemaNode", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), textNode, parent, true));
                                                                                                                                                                                                                                                                                                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:159: error: non-static variable validationContext cannot be referenced from a static context
        return Stream.of(Arguments.of("validationContext is null", null, new SimpleSchemaLocation(), new SimpleJsonNodePath(), dummyNode, parent, false), Arguments.of("schemaNode is null", validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), null, parent, false));
                                                                                                                                                                                             ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:168: error: non-static variable validationContext cannot be referenced from a static context
        return JsonSchema.from(validationContext, new SimpleSchemaLocation(), new SimpleJsonNodePath(), emptyObject, null, false);
                               ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
28 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.SchemaLocation`:\n\t\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod ` static JsonSchema from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


