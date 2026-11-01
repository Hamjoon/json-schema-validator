
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    private JsonNode schemaNode;

    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonSchema mockParentSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Build a minimal ValidationContext – we use the default meta‑schema
        // (draft‑2020‑12) and a default JsonSchemaFactory.
        JsonMetaSchema metaSchema = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        validationContext = new ValidationContext(metaSchema, factory, config);
        // A tiny schema node – an empty object is sufficient for construction.
        schemaNode = MAPPER.createObjectNode();
    }

    private static Stream<JsonNode> validNodeProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return Stream.of(f.objectNode(), f.arrayNode(), f.booleanNode(true));
    }

    @Test
    void from_WithParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, schemaNode, mockParentSchema, false);
        // Assert
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_WithoutParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, schemaNode, // no parent schema
        null, // test the suppressSubSchemaRetrieval flag as true
        true);
        // Assert
        assertNotNull(result, "JsonSchema.from should not return null when parent is null");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_NullValidationContext_ShouldCreateSchemaWithNullContext() {
        JsonSchema result = JsonSchema.from(null, mockSchemaLocation, mockEvaluationPath, schemaNode, null, false);
        assertNotNull(result, "Result must not be null even when ValidationContext is null");
        assertNull(result.getValidationContext(), "getValidationContext() should return null when a null context was supplied");
    }

    @Test
    void from_NullSchemaLocation_ShouldCreateSchemaWithNullId() {
        JsonSchema result = JsonSchema.from(validationContext, null, mockEvaluationPath, schemaNode, null, false);
        assertNotNull(result, "Result must not be null when SchemaLocation is null");
        assertNull(result.getId(), "getId() should be null because no SchemaLocation was supplied");
    }

    @Test
    void from_NullEvaluationPath_ShouldCreateSchemaWithoutException() {
        JsonSchema result = JsonSchema.from(validationContext, mockSchemaLocation, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null when evaluationPath is null");
    }

    @Test
    void from_NullSchemaNode_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, null, null, false), "Passing a null schemaNode must result in NPE");
    }

    @ParameterizedTest
    @MethodSource("validNodeProvider")
    void from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode node) {
        JsonSchema result = JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, node, null, false);
        assertNotNull(result, "Factory must return a non‑null JsonSchema for node: " + node.getNodeType());
        // Basic sanity: the instance should be usable for validation without throwing.
        assertDoesNotThrow(() -> result.validate(node), "Calling validate on the created schema should not throw");
    }

    @Test
    void from_NumericSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode numericNode = JsonNodeFactory.instance.numberNode(42);
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, numericNode, null, false), "Numeric node is not a valid JSON Schema representation");
    }

    @Test
    void from_TextSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode textNode = JsonNodeFactory.instance.textNode("not a schema");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, textNode, null, false), "Text node is not a valid JSON Schema representation");
    }

    @Test
    void from_MissingSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode missingNode = JsonNodeFactory.instance.missingNode();
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, missingNode, null, false), "Missing node must be rejected");
    }

    @Test
    void from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed() {
        JsonSchema schema = JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, schemaNode, null, true);
        // Any attempt to resolve a sub‑schema must return null.
        JsonNodePath anyPath = new JsonNodePath("#/any");
        assertNull(schema.getSubSchema(anyPath), "When suppressSubSchemaRetrieval is true, getSubSchema must return null");
    }

    @Test
    void from_WithParent_SuppressFalse_FindAncestorReturnsParent() {
        // Create a parent schema first.
        JsonSchema parent = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/root"), new JsonNodePath("$"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/child"), new JsonNodePath("#child"), JsonNodeFactory.instance.objectNode(), parent, false);
        assertSame(parent, child.findAncestor(), "findAncestor() on child should return the supplied parent schema");
    }

    @Test
    void from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent() {
        JsonSchema parent = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/root"), new JsonNodePath("$"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/child"), new JsonNodePath("#child"), JsonNodeFactory.instance.objectNode(), parent, true);
        JsonNodePath anyPath = new JsonNodePath("#/any");
        assertNull(child.getSubSchema(anyPath), "Suppress flag true must prevent sub‑schema resolution even when a parent exists");
    }

    @Test
    void from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException() {
        // The SchemaLocation constructor validates the URI and throws IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> {
            SchemaLocation badLocation = new SchemaLocation("ht!tp://[invalid-uri]");
            JsonSchema.from(validationContext, badLocation, mockEvaluationPath, schemaNode, null, false);
        }, "Malformed URI must cause IllegalArgumentException");
    }

    @Test
    void from_DefaultIsRecursiveAnchor_ShouldBeFalse() {
        JsonSchema schema = JsonSchema.from(validationContext, mockSchemaLocation, mockEvaluationPath, schemaNode, null, false);
        assertFalse(schema.isRecursiveAnchor(), "A newly created schema should not be a recursive anchor by default");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:9: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_from_Test
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:129: error: incompatible types: String cannot be converted to PathType
        JsonNodePath anyPath = new JsonNodePath("#/any");
                                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:136: error: incompatible types: String cannot be converted to AbsoluteIri
        JsonSchema parent = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/root"), new JsonNodePath("$"), JsonNodeFactory.instance.objectNode(), null, false);
                                                                                  ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:136: error: incompatible types: String cannot be converted to PathType
        JsonSchema parent = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/root"), new JsonNodePath("$"), JsonNodeFactory.instance.objectNode(), null, false);
                                                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:137: error: incompatible types: String cannot be converted to AbsoluteIri
        JsonSchema child = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/child"), new JsonNodePath("#child"), JsonNodeFactory.instance.objectNode(), parent, false);
                                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:137: error: incompatible types: String cannot be converted to PathType
        JsonSchema child = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/child"), new JsonNodePath("#child"), JsonNodeFactory.instance.objectNode(), parent, false);
                                                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:143: error: incompatible types: String cannot be converted to AbsoluteIri
        JsonSchema parent = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/root"), new JsonNodePath("$"), JsonNodeFactory.instance.objectNode(), null, false);
                                                                                  ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:143: error: incompatible types: String cannot be converted to PathType
        JsonSchema parent = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/root"), new JsonNodePath("$"), JsonNodeFactory.instance.objectNode(), null, false);
                                                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:144: error: incompatible types: String cannot be converted to AbsoluteIri
        JsonSchema child = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/child"), new JsonNodePath("#child"), JsonNodeFactory.instance.objectNode(), parent, true);
                                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:144: error: incompatible types: String cannot be converted to PathType
        JsonSchema child = JsonSchema.from(validationContext, new SchemaLocation("http://example.com/child"), new JsonNodePath("#child"), JsonNodeFactory.instance.objectNode(), parent, true);
                                                                                                                               ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:145: error: incompatible types: String cannot be converted to PathType
        JsonNodePath anyPath = new JsonNodePath("#/any");
                                                ^
src/test/java/com/networknt/schema/JsonSchema_from_Test.java:153: error: incompatible types: String cannot be converted to AbsoluteIri
            SchemaLocation badLocation = new SchemaLocation("ht!tp://[invalid-uri]");
                                                            ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
19 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.SchemaLocation`:\n\t\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod ` static JsonSchema from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


