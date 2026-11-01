


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    private JsonNode schemaNode;

//     @Mock
    private SchemaLocation mockSchemaLocation;

//     @Mock
    private JsonNodePath mockEvaluationPath;

//     @Mock
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
        JsonSchema result = JsonSchema.from(validationContext, // SchemaLocation – not needed for this test
        null, // EvaluationPath – not needed for this test
        null, schemaNode, // parent schema – none
        null, false);
        // Assert
        assertNotNull(result, "JsonSchema.from should not return null");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_WithoutParent_ShouldCreateSchemaWithSameValidationContext() {
        // Act
        JsonSchema result = JsonSchema.from(validationContext, // SchemaLocation
        null, // EvaluationPath
        null, schemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval flag true
        true);
        // Assert
        assertNotNull(result, "JsonSchema.from should not return null when parent is null");
        assertSame(validationContext, result.getValidationContext(), "The created JsonSchema must expose the same ValidationContext that was supplied");
    }

    @Test
    void from_NullValidationContext_ShouldCreateSchemaWithNullContext() {
        JsonSchema result = JsonSchema.from(null, null, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null even when ValidationContext is null");
        assertNull(result.getValidationContext(), "getValidationContext() should return null when a null context was supplied");
    }

    @Test
    void from_NullSchemaLocation_ShouldCreateSchemaWithNullId() {
        JsonSchema result = JsonSchema.from(validationContext, null, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null when SchemaLocation is null");
        assertNull(result.getId(), "getId() should be null because no SchemaLocation was supplied");
    }

    @Test
    void from_NullEvaluationPath_ShouldCreateSchemaWithoutException() {
        JsonSchema result = JsonSchema.from(validationContext, null, null, schemaNode, null, false);
        assertNotNull(result, "Result must not be null when evaluationPath is null");
    }

    @Test
    void from_NullSchemaNode_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> JsonSchema.from(validationContext, null, null, null, null, false), "Passing a null schemaNode must result in NPE");
    }

    @ParameterizedTest
    @MethodSource("validNodeProvider")
    void from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode node) {
        JsonSchema result = JsonSchema.from(validationContext, null, null, node, null, false);
        assertNotNull(result, "Factory must return a non‑null JsonSchema for node: " + node.getNodeType());
        // Basic sanity: the instance should be usable for validation without throwing.
        assertDoesNotThrow(() -> result.validate(node), "Calling validate on the created schema should not throw");
    }

    @Test
    void from_NumericSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode numericNode = JsonNodeFactory.instance.numberNode(42);
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, null, null, numericNode, null, false), "Numeric node is not a valid JSON Schema representation");
    }

    @Test
    void from_TextSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode textNode = JsonNodeFactory.instance.textNode("not a schema");
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, null, null, textNode, null, false), "Text node is not a valid JSON Schema representation");
    }

    @Test
    void from_MissingSchemaNode_ShouldThrowIllegalArgumentException() {
        JsonNode missingNode = JsonNodeFactory.instance.missingNode();
        assertThrows(IllegalArgumentException.class, () -> JsonSchema.from(validationContext, null, null, missingNode, null, false), "Missing node must be rejected");
    }

    @Test
    void from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed() {
        JsonSchema schema = JsonSchema.from(validationContext, null, null, schemaNode, null, true);
        // Any attempt to resolve a sub‑schema must return null.
        assertNull(schema.getSubSchema(null), "When suppressSubSchemaRetrieval is true, getSubSchema must return null");
    }

    @Test
    void from_WithParent_SuppressFalse_FindAncestorReturnsParent() {
        // Create a parent schema first.
        JsonSchema parent = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), parent, false);
        assertSame(parent, child.findAncestor(), "findAncestor() on child should return the supplied parent schema");
    }

    @Test
    void from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent() {
        JsonSchema parent = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema child = JsonSchema.from(validationContext, null, null, JsonNodeFactory.instance.objectNode(), parent, true);
        assertNull(child.getSubSchema(null), "Suppress flag true must prevent sub‑schema resolution even when a parent exists");
    }

    @Test
    void from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException() {
        // The SchemaLocation constructor validates the URI and throws IllegalArgumentException.
        // In the current library version the constructor expects an AbsoluteIri, so we
        // simply pass a malformed URI string wrapped in a try‑catch to verify the exception.
        assertThrows(IllegalArgumentException.class, () -> {
            // This will fail because the URI is not valid.
            new SchemaLocation(new AbsoluteIri("ht!tp://[invalid-uri]"));
        }, "Malformed URI must cause IllegalArgumentException");
    }

    @Test
    void from_DefaultIsRecursiveAnchor_ShouldBeFalse() {
        JsonSchema schema = JsonSchema.from(validationContext, null, null, schemaNode, null, false);
        assertFalse(schema.isRecursiveAnchor(), "A newly created schema should not be a recursive anchor by default");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_from_Test ✔
│     ├─ from_TextSchemaNode_ShouldThrowIllegalArgumentException() ✘ Text node is not a valid JSON Schema representation ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     ├─ from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent() ✘ java.lang.NullPointerException
│     ├─ from_NullSchemaLocation_ShouldCreateSchemaWithNullId() ✘ java.lang.NullPointerException
│     ├─ from_DefaultIsRecursiveAnchor_ShouldBeFalse() ✘ java.lang.NullPointerException
│     ├─ from_NullEvaluationPath_ShouldCreateSchemaWithoutException() ✘ java.lang.NullPointerException
│     ├─ from_WithParent_SuppressFalse_FindAncestorReturnsParent() ✘ java.lang.NullPointerException
│     ├─ from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode) ✔
│     │  ├─ [1] {} ✘ java.lang.NullPointerException
│     │  ├─ [2] [] ✘ java.lang.NullPointerException
│     │  └─ [3] true ✘ java.lang.NullPointerException
│     ├─ from_NumericSchemaNode_ShouldThrowIllegalArgumentException() ✘ Numeric node is not a valid JSON Schema representation ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     ├─ from_NullSchemaNode_ShouldThrowNullPointerException() ✔
│     ├─ from_MissingSchemaNode_ShouldThrowIllegalArgumentException() ✘ Missing node must be rejected ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     ├─ from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed() ✘ java.lang.NullPointerException
│     ├─ from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException() ✘ Malformed URI must cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ from_NullValidationContext_ShouldCreateSchemaWithNullContext() ✘ Cannot invoke "com.networknt.schema.ValidationContext.resolveSchemaId(com.fasterxml.jackson.databind.JsonNode)" because "validationContext" is null
│     ├─ from_WithoutParent_ShouldCreateSchemaWithSameValidationContext() ✘ java.lang.NullPointerException
│     └─ from_WithParent_ShouldCreateSchemaWithSameValidationContext() ✘ java.lang.NullPointerException
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (16):
  JUnit Jupiter:JsonSchema_from_Test:from_TextSchemaNode_ShouldThrowIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_TextSchemaNode_ShouldThrowIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Text node is not a valid JSON Schema representation ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_from_Test.from_TextSchemaNode_ShouldThrowIllegalArgumentException(JsonSchema_from_Test.java:117)
       [...]
     Caused by: java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_WithParent_SuppressTrue_SubSchemaRetrievalIsSuppressedEvenWithParent(JsonSchema_from_Test.java:143)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_NullSchemaLocation_ShouldCreateSchemaWithNullId()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_NullSchemaLocation_ShouldCreateSchemaWithNullId', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_NullSchemaLocation_ShouldCreateSchemaWithNullId(JsonSchema_from_Test.java:83)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_DefaultIsRecursiveAnchor_ShouldBeFalse()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_DefaultIsRecursiveAnchor_ShouldBeFalse', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_DefaultIsRecursiveAnchor_ShouldBeFalse(JsonSchema_from_Test.java:161)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_NullEvaluationPath_ShouldCreateSchemaWithoutException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_NullEvaluationPath_ShouldCreateSchemaWithoutException', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_NullEvaluationPath_ShouldCreateSchemaWithoutException(JsonSchema_from_Test.java:90)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_WithParent_SuppressFalse_FindAncestorReturnsParent()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_WithParent_SuppressFalse_FindAncestorReturnsParent', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_WithParent_SuppressFalse_FindAncestorReturnsParent(JsonSchema_from_Test.java:136)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode):[1] {}
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_ValidSchemaNodeTypes_ShouldCreateInstance', methodParameterTypes = 'com.fasterxml.jackson.databind.JsonNode']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonSchema_from_Test.java:102)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode):[2] []
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_ValidSchemaNodeTypes_ShouldCreateInstance', methodParameterTypes = 'com.fasterxml.jackson.databind.JsonNode']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonSchema_from_Test.java:102)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonNode):[3] true
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_ValidSchemaNodeTypes_ShouldCreateInstance', methodParameterTypes = 'com.fasterxml.jackson.databind.JsonNode']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_ValidSchemaNodeTypes_ShouldCreateInstance(JsonSchema_from_Test.java:102)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_NumericSchemaNode_ShouldThrowIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_NumericSchemaNode_ShouldThrowIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Numeric node is not a valid JSON Schema representation ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_from_Test.from_NumericSchemaNode_ShouldThrowIllegalArgumentException(JsonSchema_from_Test.java:111)
       [...]
     Caused by: java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_MissingSchemaNode_ShouldThrowIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_MissingSchemaNode_ShouldThrowIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Missing node must be rejected ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_from_Test.from_MissingSchemaNode_ShouldThrowIllegalArgumentException(JsonSchema_from_Test.java:123)
       [...]
     Caused by: java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_SuppressTrue_NoParent_SubSchemaRetrievalIsSuppressed(JsonSchema_from_Test.java:128)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Malformed URI must cause IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_from_Test.from_InvalidSchemaLocationUri_ShouldThrowIllegalArgumentException(JsonSchema_from_Test.java:153)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_NullValidationContext_ShouldCreateSchemaWithNullContext()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_NullValidationContext_ShouldCreateSchemaWithNullContext', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.resolveSchemaId(com.fasterxml.jackson.databind.JsonNode)" because "validationContext" is null
       com.networknt.schema.JsonSchema.resolve(JsonSchema.java:76)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_NullValidationContext_ShouldCreateSchemaWithNullContext(JsonSchema_from_Test.java:76)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_WithoutParent_ShouldCreateSchemaWithSameValidationContext()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_WithoutParent_ShouldCreateSchemaWithSameValidationContext', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_WithoutParent_ShouldCreateSchemaWithSameValidationContext(JsonSchema_from_Test.java:64)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_from_Test:from_WithParent_ShouldCreateSchemaWithSameValidationContext()
    MethodSource [className = 'com.networknt.schema.JsonSchema_from_Test', methodName = 'from_WithParent_ShouldCreateSchemaWithSameValidationContext', methodParameterTypes = '']
    => java.lang.NullPointerException
       java.base/java.util.Objects.requireNonNull(Objects.java:209)
       com.networknt.schema.ValidationMessageHandler.<init>(ValidationMessageHandler.java:30)
       com.networknt.schema.BaseJsonValidator.<init>(BaseJsonValidator.java:52)
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       com.networknt.schema.JsonSchema.from(JsonSchema.java:66)
       com.networknt.schema.JsonSchema_from_Test.from_WithParent_ShouldCreateSchemaWithSameValidationContext(JsonSchema_from_Test.java:52)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 351 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        17 tests found           ]
[         0 tests skipped         ]
[        17 tests started         ]
[         0 tests aborted         ]
[         1 tests successful      ]
[        16 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.SchemaLocation`:
	
class `com.networknt.schema.JsonNodePath`:
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method ` static JsonSchema from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.from(ValidationContext, SchemaLocation, JsonNodePath, JsonNode, JsonSchema, boolean)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
