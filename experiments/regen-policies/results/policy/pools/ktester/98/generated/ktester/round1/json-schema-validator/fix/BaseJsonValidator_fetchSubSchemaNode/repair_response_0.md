```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.Set;

/**
 * Unit tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * <p>The original test suite used Mockito for stubbing, but the execution
 * environment does not provide the Mockito library.  All mocking has been
 * replaced with lightweight hand‑written stubs and dynamic proxies.</p>
 */
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** The validator under test – a concrete subclass that makes the method public. */
    private TestValidator validator;

    /** Dummy objects required by the {@link BaseJsonValidator} constructor. */
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private JsonSchema parentSchema;
    private ValidatorTypeCode validatorType;

    /** A simple {@link JsonSchema} implementation used in the tests. */
    private static class DummyJsonSchema extends JsonSchema {
        private final JsonNode node;

        DummyJsonSchema(JsonNode node) {
            // Use the simplest constructor of JsonSchema.
            super(null, null, null, node, null, false);
            this.node = node;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNode() {
            return node;
        }

        @Override public SchemaLocation getSchemaLocation() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public JsonSchema getEvaluationParentSchema() { return null; }
        @Override public JsonNodePath getEvaluationPath() { return null; }
        @Override public String getKeyword() { return null; }

        @Override
        public String toString() {
            return "DummyJsonSchema";
        }
    }

    /**
     * Concrete subclass of {@link BaseJsonValidator} that simply forwards the
     * protected {@code fetchSubSchemaNode} method as public so that it can be
     * exercised from the test suite.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            // The super‑constructor that accepts the full list of arguments is used
            // because it allows us to set the {@code suppressSubSchemaRetrieval}
            // flag directly.
            super(suppressSubSchemaRetrieval,
                  schemaNode,
                  validationContext,
                  null, // errorMessageType
                  null, // errorMessageKeyword
                  null, // messageSource
                  null, // keyword
                  null,
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  null, // errorMessage map
                  null);
        }

        /** Expose the protected method for testing. */
        JsonSchema fetchSubSchemaNodePublic(ValidationContext vc) {
            return super.fetchSubSchemaNode(vc);
        }

        // The abstract method signature in JsonValidator has four parameters.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                JsonNode node,
                                                JsonNode rootNode,
                                                JsonNodePath instanceLocation) {
            return Collections.emptySet();
        }
    }

    /** Initialise the dummy objects before each test. */
    @BeforeEach
    void setupBeforeEach() {
        schemaLocation = null;
        evaluationPath = null;
        parentSchema = null;
        validatorType = null;
    }

    /**
     * Test that when {@code suppressSubSchemaRetrieval} is {@code true}
     * the method returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressed() {
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, stubContextReturning(null), true);
        JsonSchema result = validator.fetchSubSchemaNodePublic(stubContextReturning(null));
        Assertions.assertNull(result, "When sub‑schema retrieval is suppressed the result must be null");
    }

    /**
     * Test that when {@code suppressSubSchemaRetrieval} is {@code false}
     * the method delegates to the {@link ValidationContext} and returns the
     * created {@link JsonSchema}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnSubSchema_WhenNotSuppressed() {
        JsonSchema subSchema = new DummyJsonSchema(null);
        ValidationContext ctx = stubContextReturning(subSchema);
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, ctx, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(ctx);
        Assertions.assertNotNull(result, "Result must not be null when retrieval is not suppressed");
        Assertions.assertSame(subSchema, result, "The returned schema should be the one created by ValidationContext");
    }

    /**
     * Suppressed flag overrides any non‑null schema node – still returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressedEvenIfSchemaNodeIsObject() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("$ref", "#/definitions/foo");
        validator = new TestValidator(schemaLocation, evaluationPath, objectNode,
                parentSchema, validatorType, stubContextReturning(null), true);
        JsonSchema result = validator.fetchSubSchemaNodePublic(stubContextReturning(null));
        Assertions.assertNull(result, "Even with a valid schema node, suppression must force a null result");
    }

    /**
     * When {@code suppressSubSchemaRetrieval} is {@code false} but the schema node is {@code null},
     * the private {@code obtainSubSchemaNode} implementation returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNull() {
        validator = new TestValidator(schemaLocation, evaluationPath, null,
                parentSchema, validatorType, stubContextReturning(null), false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(stubContextReturning(null));
        Assertions.assertNull(result, "A null schema node should lead to a null sub‑schema");
    }

    /**
     * A non‑object node (e.g., a textual node) is treated as “not a schema” and yields {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNonObject() {
        TextNode textNode = new TextNode("just a string");
        validator = new TestValidator(schemaLocation, evaluationPath, textNode,
                parentSchema, validatorType, stubContextReturning(null), false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(stubContextReturning(null));
        Assertions.assertNull(result, "A non‑object schema node must result in null");
    }

    /**
     * An array node is illegal as a schema definition – the method must propagate {@link IllegalArgumentException}.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenSchemaNodeIsArray() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(1).add(2);
        validator = new TestValidator(schemaLocation, evaluationPath, arrayNode,
                parentSchema, validatorType, stubContextReturning(null), false);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> validator.fetchSubSchemaNodePublic(stubContextReturning(null)),
                "ArrayNode as a schema definition should cause IllegalArgumentException");
    }

    /**
     * An object node that contains a $ref together with another keyword violates the $ref rule
     * and must cause {@link IllegalArgumentException}.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenRefWithOtherKeywords() {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("$ref", "#/definitions/foo");
        obj.put("type", "string"); // illegal combination
        validator = new TestValidator(schemaLocation, evaluationPath, obj,
                parentSchema, validatorType, stubContextReturning(null), false);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> validator.fetchSubSchemaNodePublic(stubContextReturning(null)),
                "ObjectNode with $ref plus other keywords must cause IllegalArgumentException");
    }

    /**
     * When the supplied {@code ValidationContext} is {@code null} a {@link NullPointerException}
     * must be thrown immediately.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowNullPointerException_WhenValidationContextIsNull() {
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, null, false);
        Assertions.assertThrows(NullPointerException.class,
                () -> validator.fetchSubSchemaNodePublic(null),
                "Passing a null ValidationContext must result in NPE");
    }

    /**
     * If {@code ValidationContext.newSchema} throws a {@link RuntimeException},
     * the exception must propagate unchanged.
     */
    @Test
    void fetchSubSchemaNode_ShouldPropagateRuntimeException_FromValidationContext() {
        ValidationContext ctx = stubContextThrowing(new RuntimeException("boom"));
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, ctx, false);
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class,
                () -> validator.fetchSubSchemaNodePublic(ctx));
        Assertions.assertEquals("boom", ex.getMessage(),
                "RuntimeException should be propagated unchanged");
    }

    /**
     * If {@code ValidationContext.newSchema} throws a {@link JsonSchemaException},
     * the method must let it bubble up.
     */
    @Test
    void fetchSubSchemaNode_ShouldPropagateJsonSchemaException() {
        JsonSchemaException jse = new JsonSchemaException("invalid schema");
        ValidationContext ctx = stubContextThrowing(jse);
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, ctx, false);
        JsonSchemaException ex = Assertions.assertThrows(JsonSchemaException.class,
                () -> validator.fetchSubSchemaNodePublic(ctx));
        Assertions.assertSame(jse, ex,
                "The same JsonSchemaException instance must be propagated");
    }

    /**
     * When a valid object node (without $ref) is supplied, the method should return a non‑null
     * {@link JsonSchema} whose {@code getSchemaNode()} equals the original node.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnValidSchema_WhenObjectNodeIsValid() {
        ObjectNode schemaObj = JsonNodeFactory.instance.objectNode();
        schemaObj.put("type", "string");

        // Create a ValidationContext that returns a DummyJsonSchema wrapping the node.
        ValidationContext ctx = (ValidationContext) Proxy.newProxyInstance(
                ValidationContext.class.getClassLoader(),
                new Class[]{ValidationContext.class},
                (proxy, method, args) -> {
                    if ("newSchema".equals(method.getName())) {
                        return new DummyJsonSchema(schemaObj);
                    }
                    return null;
                });

        validator = new TestValidator(schemaLocation, evaluationPath, schemaObj,
                parentSchema, validatorType, ctx, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(ctx);
        Assertions.assertNotNull(result, "A valid object node must produce a non‑null JsonSchema");
        Assertions.assertSame(schemaObj, result.getSchemaNode(),
                "The returned schema's node should be the original schema node");
    }

    /* --------------------------------------------------------------------- *
     * Helper methods to create simple stubs / proxies for ValidationContext *
     * --------------------------------------------------------------------- */

    /** Returns a simple {@link JsonNode} mock – just a new empty ObjectNode. */
    private JsonNode mock(JsonNode.class) {
        return JsonNodeFactory.instance.objectNode();
    }

    /** Creates a stub {@link ValidationContext} that returns the supplied schema. */
    private ValidationContext stubContextReturning(JsonSchema toReturn) {
        return (ValidationContext) Proxy.newProxyInstance(
                ValidationContext.class.getClassLoader(),
                new Class[]{ValidationContext.class},
                (proxy, method, args) -> {
                    if ("newSchema".equals(method.getName())) {
                        return toReturn;
                    }
                    return null;
                });
    }

    /** Creates a stub {@link ValidationContext} that throws the supplied exception from {@code newSchema}. */
    private ValidationContext stubContextThrowing(RuntimeException ex) {
        return (ValidationContext) Proxy.newProxyInstance(
                ValidationContext.class.getClassLoader(),
                new Class[]{ValidationContext.class},
                (proxy, method, args) -> {
                    if ("newSchema".equals(method.getName())) {
                        throw ex;
                    }
                    return null;
                });
    }

    /** Overload for checked {@link JsonSchemaException}. */
    private ValidationContext stubContextThrowing(JsonSchemaException ex) {
        return (ValidationContext) Proxy.newProxyInstance(
                ValidationContext.class.getClassLoader(),
                new Class[]{ValidationContext.class},
                (proxy, method, args) -> {
                    if ("newSchema".equals(method.getName())) {
                        throw ex;
                    }
                    return null;
                });
    }
}
```