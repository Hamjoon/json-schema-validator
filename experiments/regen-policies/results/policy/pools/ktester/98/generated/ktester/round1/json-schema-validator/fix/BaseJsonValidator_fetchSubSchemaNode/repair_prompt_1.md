
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.Collections;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import java.util.Set;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /**
     * The validator under test – a concrete subclass that makes the method public.
     */
    private TestValidator validator;

    /**
     * Mocked validation context – used to stub the creation of a sub‑schema.
     */
    @Mock
    private ValidationContext validationContext;

    /**
     * A dummy schema node – the actual content is irrelevant for the current tests.
     */
    @Mock
    private JsonNode schemaNode;

    /**
     * A dummy parent schema – not used by the method under test.
     */
    @Mock
    private JsonSchema parentSchema;

    /**
     * Dummy objects required by the BaseJsonValidator constructor.
     */
    @Mock
    private SchemaLocation schemaLocation;

    @Mock
    private JsonNodePath evaluationPath;

    @Mock
    private ValidatorTypeCode validatorType;

    /**
     * A mock sub‑schema that will be returned by the stubbed ValidationContext.
     */
    @Mock
    private JsonSchema subSchema;

    /**
     * Concrete subclass of {@link BaseJsonValidator} that simply forwards the
     * protected {@code fetchSubSchemaNode} method as public so that it can be
     * exercised from the test suite.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            // The super‑constructor that accepts the full list of arguments is used
            // because it allows us to set the {@code suppressSubSchemaRetrieval}
            // flag directly.
            super(suppressSubSchemaRetrieval, schemaNode, validationContext, /* from ValidationMessageHandler */
            // errorMessageType
            null, // errorMessageKeyword
            null, // messageSource
            null, // keyword
            null, parentSchema, schemaLocation, evaluationPath, // evaluationParentSchema
            null, // errorMessage map
            null);
        }

        /**
         * Expose the protected method for testing.
         */
        JsonSchema fetchSubSchemaNodePublic(ValidationContext vc) {
            return super.fetchSubSchemaNode(vc);
        }

        // The abstract methods from JsonValidator are not needed for these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Initialise Mockito annotations before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test that when {@code suppressSubSchemaRetrieval} is {@code true} the method returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressed() {
        // given – a validator with suppress flag set to true
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext, /* suppressSubSchemaRetrieval = */
        true);
        // when
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        // then
        assertNull(result, "When sub‑schema retrieval is suppressed the result must be null");
    }

    /**
     * Test that when {@code suppressSubSchemaRetrieval} is {@code false} the method delegates to the
     *  ValidationContext and returns the created {@link JsonSchema}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnSubSchema_WhenNotSuppressed() {
        // given – stub the ValidationContext to return a known JsonSchema instance
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(subSchema);
        validator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext, /* suppressSubSchemaRetrieval = */
        false);
        // when
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        // then
        assertNotNull(result, "Result must not be null when retrieval is not suppressed");
        assertSame(subSchema, result, "The returned schema should be the one created by ValidationContext");
    }

    /**
     * Suppressed flag overrides any non‑null schema node – still returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressedEvenIfSchemaNodeIsObject() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("$ref", "#/definitions/foo");
        validator = new TestValidator(schemaLocation, evaluationPath, objectNode, parentSchema, validatorType, validationContext, true);
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "Even with a valid schema node, suppression must force a null result");
    }

    /**
     * When {@code suppressSubSchemaRetrieval} is {@code false} but the schema node is {@code null},
     *  the private {@code obtainSubSchemaNode} implementation returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNull() {
        validator = new TestValidator(schemaLocation, evaluationPath, null, parentSchema, validatorType, validationContext, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "A null schema node should lead to a null sub‑schema");
    }

    /**
     * A non‑object node (e.g., a textual node) is treated as “not a schema” and yields {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNonObject() {
        TextNode textNode = new TextNode("just a string");
        validator = new TestValidator(schemaLocation, evaluationPath, textNode, parentSchema, validatorType, validationContext, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "A non‑object schema node must result in null");
    }

    /**
     * An array node is illegal as a schema definition – the method must propagate {@link IllegalArgumentException}.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenSchemaNodeIsArray() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(1).add(2);
        validator = new TestValidator(schemaLocation, evaluationPath, arrayNode, parentSchema, validatorType, validationContext, false);
        assertThrows(IllegalArgumentException.class, () -> validator.fetchSubSchemaNodePublic(validationContext), "ArrayNode as a schema definition should cause IllegalArgumentException");
    }

    /**
     * An object node that contains a $ref together with another keyword violates the $ref rule
     *  and must cause {@link IllegalArgumentException}.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenRefWithOtherKeywords() {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("$ref", "#/definitions/foo");
        // illegal combination
        obj.put("type", "string");
        validator = new TestValidator(schemaLocation, evaluationPath, obj, parentSchema, validatorType, validationContext, false);
        assertThrows(IllegalArgumentException.class, () -> validator.fetchSubSchemaNodePublic(validationContext), "ObjectNode with $ref plus other keywords must cause IllegalArgumentException");
    }

    /**
     * When the supplied {@code ValidationContext} is {@code null} a {@link NullPointerException}
     *  must be thrown immediately.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowNullPointerException_WhenValidationContextIsNull() {
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class), parentSchema, validatorType, null, false);
        assertThrows(NullPointerException.class, () -> validator.fetchSubSchemaNodePublic(null), "Passing a null ValidationContext must result in NPE");
    }

    /**
     * If {@code ValidationContext.newSchema} throws a {@link RuntimeException},
     *  the exception must propagate unchanged.
     */
    @Test
    void fetchSubSchemaNode_ShouldPropagateRuntimeException_FromValidationContext() {
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class), parentSchema, validatorType, validationContext, false);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.fetchSubSchemaNodePublic(validationContext));
        assertEquals("boom", ex.getMessage(), "RuntimeException should be propagated unchanged");
    }

    /**
     * If {@code ValidationContext.newSchema} throws a {@link JsonSchemaException},
     *  the method must let it bubble up.
     */
    @Test
    void fetchSubSchemaNode_ShouldPropagateJsonSchemaException() {
        JsonSchemaException jse = new JsonSchemaException("invalid schema");
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(jse);
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class), parentSchema, validatorType, validationContext, false);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> validator.fetchSubSchemaNodePublic(validationContext));
        assertSame(jse, ex, "The same JsonSchemaException instance must be propagated");
    }

    /**
     * When a valid object node (without $ref) is supplied, the method should return a non‑null
     *  {@link JsonSchema} whose {@code getSchemaNode()} equals the original node.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnValidSchema_WhenObjectNodeIsValid() {
        // Prepare a simple object schema node.
        ObjectNode schemaObj = JsonNodeFactory.instance.objectNode();
        schemaObj.put("type", "string");
        // Stub the ValidationContext to return a concrete JsonSchema implementation.
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(schemaObj), any(JsonSchema.class))).thenAnswer(invocation -> {
            // Return a minimal anonymous JsonSchema that simply returns the node we passed.
            JsonNode passedNode = invocation.getArgument(2);
            return new JsonSchema() {

                @Override
                public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                    return Collections.emptySet();
                }

                @Override
                public JsonNode getSchemaNode() {
                    return passedNode;
                }

                @Override
                public SchemaLocation getSchemaLocation() {
                    return null;
                }

                @Override
                public JsonSchema getParentSchema() {
                    return null;
                }

                @Override
                public JsonSchema getEvaluationParentSchema() {
                    return null;
                }

                @Override
                public JsonNodePath getEvaluationPath() {
                    return null;
                }

                @Override
                public String getKeyword() {
                    return null;
                }

                @Override
                public String toString() {
                    return "AnonymousJsonSchema";
                }
            };
        });
        validator = new TestValidator(schemaLocation, evaluationPath, schemaObj, parentSchema, validatorType, validationContext, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNotNull(result, "A valid object node must produce a non‑null JsonSchema");
        assertSame(schemaObj, result.getSchemaNode(), "The returned schema's node should be the original schema node");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:6: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:7: error: package org.mockito does not exist
import org.mockito.MockitoAnnotations;
                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:10: error: package org.mockito does not exist
import static org.mockito.ArgumentMatchers.*;
                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:11: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:15: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:18: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:31: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:49: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:52: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:55: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:61: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:69: error: TestValidator is not abstract and does not override abstract method validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath) in JsonValidator
    private static class TestValidator extends BaseJsonValidator {
                   ^
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:104: error: cannot find symbol
        MockitoAnnotations.openMocks(this);
        ^
  symbol:   variable MockitoAnnotations
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:128: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(subSchema);
                                         ^
  symbol:   method any(Class<SchemaLocation>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:128: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(subSchema);
                                                                    ^
  symbol:   method any(Class<JsonNodePath>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:128: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(subSchema);
                                                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:128: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(subSchema);
                                                                                                                  ^
  symbol:   method any(Class<JsonSchema>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:203: error: cannot find symbol
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class), parentSchema, validatorType, null, false);
                                                                      ^
  symbol:   method mock(Class<JsonNode>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:213: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
                                         ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:213: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
                                                ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:213: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
                                                       ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:213: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
                                                              ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:214: error: cannot find symbol
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class), parentSchema, validatorType, validationContext, false);
                                                                      ^
  symbol:   method mock(Class<JsonNode>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:226: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(jse);
                                         ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:226: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(jse);
                                                ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:226: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(jse);
                                                       ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:226: error: cannot find symbol
        when(validationContext.newSchema(any(), any(), any(), any())).thenThrow(jse);
                                                              ^
  symbol:   method any()
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:227: error: cannot find symbol
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class), parentSchema, validatorType, validationContext, false);
                                                                      ^
  symbol:   method mock(Class<JsonNode>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:242: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(schemaObj), any(JsonSchema.class))).thenAnswer(invocation -> {
                                         ^
  symbol:   method any(Class<SchemaLocation>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:242: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(schemaObj), any(JsonSchema.class))).thenAnswer(invocation -> {
                                                                    ^
  symbol:   method any(Class<JsonNodePath>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:242: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(schemaObj), any(JsonSchema.class))).thenAnswer(invocation -> {
                                                                                             ^
  symbol:   method eq(ObjectNode)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:242: error: cannot find symbol
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), eq(schemaObj), any(JsonSchema.class))).thenAnswer(invocation -> {
                                                                                                            ^
  symbol:   method any(Class<JsonSchema>)
  location: class BaseJsonValidator_fetchSubSchemaNode_Test
src/test/java/com/networknt/schema/BaseJsonValidator_fetchSubSchemaNode_Test.java:245: error: no suitable constructor found for JsonSchema(no arguments)
            return new JsonSchema() {
                   ^
    constructor JsonSchema.JsonSchema(ValidationContext,SchemaLocation,JsonNodePath,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(List<JsonValidator>,boolean,boolean,TypeValidator,String,boolean,JsonNode,ValidationContext,ErrorMessageType,String,MessageSource,Keyword,JsonSchema,SchemaLocation,JsonNodePath,JsonSchema,Map<String,String>) is not applicable
      (actual and formal argument lists differ in length)
39 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validateUnknownFormat(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`, api document: \r\n     * When the Format-Assertion vocabulary is specified, implementations MUST fail upon encountering unknown formats.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @param node the node\r\n     * @param rootNode the root node\r\n     * @param instanceLocation the instance location\r\n     * @return the messages\r\n     ;\n\t\tmethod `protected Object getAnnotationValue()` returns `java.lang.Object`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`, api document: \r\n     * Gets the annotation value.\r\n     * \r\n     * @return the annotation value\r\n     ;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.fetchSubSchemaNode(ValidationContext)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


