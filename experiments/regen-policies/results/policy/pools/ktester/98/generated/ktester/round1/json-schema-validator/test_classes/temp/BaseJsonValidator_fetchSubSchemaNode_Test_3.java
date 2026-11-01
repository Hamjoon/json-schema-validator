package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.Collections;
import java.util.Set;
import java.lang.reflect.Constructor;

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
     * Minimal {@link JsonSchema} implementation used in the tests.
     */
    private static class DummyJsonSchema extends JsonSchema {

        DummyJsonSchema(ValidationContext vc, SchemaLocation sl, JsonNodePath path, JsonNode node, JsonSchema parent, boolean something) {
            super(vc, sl, path, node, parent, something);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    /**
     * {@link JsonSchema} that simply returns the node it was created with.
     */
    private static class CapturingJsonSchema extends JsonSchema {

        private final JsonNode capturedNode;

        CapturingJsonSchema(ValidationContext vc, SchemaLocation sl, JsonNodePath path, JsonNode node, JsonSchema parent) {
            super(vc, sl, path, node, parent, false);
            this.capturedNode = node;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNode() {
            return capturedNode;
        }
    }

    /**
     * Concrete subclass that simply forwards the protected method as public.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            // BaseJsonValidator does not have a constructor that takes the suppress flag,
            // so we call the closest constructor and then set the flag directly.
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.suppressSubSchemaRetrieval = suppressSubSchemaRetrieval;
        }

        /**
         * Expose the protected method for testing.
         */
        JsonSchema fetchSubSchemaNodePublic(ValidationContext vc) {
            return super.fetchSubSchemaNode(vc);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return Collections.emptySet();
        }
    }

    /**
     * Simple {@link ValidationContext} implementation that can be configured to
     * return a specific {@link JsonSchema} or to throw an exception.
     */
    private static class SimpleValidationContext extends ValidationContext {

        private final JsonSchema toReturn;

        private final RuntimeException toThrowRuntime;

        private final JsonSchemaException toThrowJse;

        SimpleValidationContext(JsonSchema toReturn) {
            super(null, null, null);
            this.toReturn = toReturn;
            this.toThrowRuntime = null;
            this.toThrowJse = null;
        }

        SimpleValidationContext(RuntimeException ex) {
            super(null, null, null);
            this.toReturn = null;
            this.toThrowRuntime = ex;
            this.toThrowJse = null;
        }

        SimpleValidationContext(JsonSchemaException ex) {
            super(null, null, null);
            this.toReturn = null;
            this.toThrowRuntime = null;
            this.toThrowJse = ex;
        }

        @Override
        public JsonSchema newSchema(SchemaLocation sl, JsonNodePath path, JsonNode node, JsonSchema parent) {
            if (toThrowRuntime != null) {
                throw toThrowRuntime;
            }
            if (toThrowJse != null) {
                throw toThrowJse;
            }
            if (toReturn != null) {
                return toReturn;
            }
            // Create a concrete JsonSchema instance via reflection.
            try {
                Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
                ctor.setAccessible(true);
                return ctor.newInstance(this, sl, path, node, parent, false);
            } catch (Exception e) {
                // Wrap any reflection problem into a RuntimeException – this should never happen in the test.
                throw new RuntimeException(e);
            }
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
        TestValidator validator = new TestValidator(null, null, null, null, null, new SimpleValidationContext((JsonSchema) null), true);
        JsonSchema result = validator.fetchSubSchemaNodePublic(new SimpleValidationContext((JsonSchema) null));
        Assertions.assertNull(result, "When sub‑schema retrieval is suppressed the result must be null");
    }

    /**
     * Test that when {@code suppressSubSchemaRetrieval} is {@code false} the method delegates to the
     *  ValidationContext and returns the created {@link JsonSchema}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnSubSchema_WhenNotSuppressed() {
        JsonSchema expected = new SimpleValidationContext((JsonSchema) null).newSchema(null, null, null, null);
        SimpleValidationContext ctx = new SimpleValidationContext(expected);
        TestValidator validator = new TestValidator(null, null, null, null, null, ctx, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(ctx);
        Assertions.assertNotNull(result, "Result must not be null when retrieval is not suppressed");
        Assertions.assertSame(expected, result, "The returned schema should be the one created by ValidationContext");
    }

    /**
     * Suppressed flag overrides any non‑null schema node – still returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressedEvenIfSchemaNodeIsObject() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("$ref", "#/definitions/foo");
        TestValidator validator = new TestValidator(null, null, objectNode, null, null, new SimpleValidationContext((JsonSchema) null), true);
        JsonSchema result = validator.fetchSubSchemaNodePublic(new SimpleValidationContext((JsonSchema) null));
        Assertions.assertNull(result, "Even with a valid schema node, suppression must force a null result");
    }

    /**
     * When {@code suppressSubSchemaRetrieval} is {@code false} but the schema node is {@code null},
     *  the private {@code obtainSubSchemaNode} implementation returns {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNull() {
        TestValidator validator = new TestValidator(null, null, null, null, null, new SimpleValidationContext((JsonSchema) null), false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(new SimpleValidationContext((JsonSchema) null));
        Assertions.assertNull(result, "A null schema node should lead to a null sub‑schema");
    }

    /**
     * A non‑object node (e.g., a textual node) is treated as “not a schema” and yields {@code null}.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNonObject() {
        TextNode textNode = new TextNode("just a string");
        TestValidator validator = new TestValidator(null, null, textNode, null, null, new SimpleValidationContext((JsonSchema) null), false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(new SimpleValidationContext((JsonSchema) null));
        Assertions.assertNull(result, "A non‑object schema node must result in null");
    }

    /**
     * An array node is illegal as a schema definition – the method must propagate {@link IllegalArgumentException}.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenSchemaNodeIsArray() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(1).add(2);
        TestValidator validator = new TestValidator(null, null, arrayNode, null, null, new SimpleValidationContext((JsonSchema) null), false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.fetchSubSchemaNodePublic(new SimpleValidationContext((JsonSchema) null)), "ArrayNode as a schema definition should cause IllegalArgumentException");
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
        TestValidator validator = new TestValidator(null, null, obj, null, null, new SimpleValidationContext((JsonSchema) null), false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> validator.fetchSubSchemaNodePublic(new SimpleValidationContext((JsonSchema) null)), "ObjectNode with $ref plus other keywords must cause IllegalArgumentException");
    }

    /**
     * When the supplied {@code ValidationContext} is {@code null} a {@link NullPointerException}
     *  must be thrown immediately.
     */
    @Test
    void fetchSubSchemaNode_ShouldThrowNullPointerException_WhenValidationContextIsNull() {
        TestValidator validator = new TestValidator(null, null, JsonNodeFactory.instance.objectNode(), null, null, null, false);
        Assertions.assertThrows(NullPointerException.class, () -> validator.fetchSubSchemaNodePublic(null), "Passing a null ValidationContext must result in NPE");
    }

    /**
     * If {@code ValidationContext.newSchema} throws a {@link RuntimeException},
     *  the exception must propagate unchanged.
     */
    @Test
    void fetchSubSchemaNode_ShouldPropagateRuntimeException_FromValidationContext() {
        SimpleValidationContext ctx = new SimpleValidationContext(new RuntimeException("boom"));
        TestValidator validator = new TestValidator(null, null, JsonNodeFactory.instance.objectNode(), null, null, ctx, false);
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () -> validator.fetchSubSchemaNodePublic(ctx));
        Assertions.assertEquals("boom", ex.getMessage(), "RuntimeException should be propagated unchanged");
    }

    /**
     * If {@code ValidationContext.newSchema} throws a {@link JsonSchemaException},
     *  the method must let it bubble up.
     */
    @Test
    void fetchSubSchemaNode_ShouldPropagateJsonSchemaException() {
        JsonSchemaException jse = new JsonSchemaException("invalid schema");
        SimpleValidationContext ctx = new SimpleValidationContext(jse);
        TestValidator validator = new TestValidator(null, null, JsonNodeFactory.instance.objectNode(), null, null, ctx, false);
        JsonSchemaException ex = Assertions.assertThrows(JsonSchemaException.class, () -> validator.fetchSubSchemaNodePublic(ctx));
        Assertions.assertSame(jse, ex, "The same JsonSchemaException instance must be propagated");
    }

    /**
     * When a valid object node (without $ref) is supplied, the method should return a non‑null
     *  {@link JsonSchema} whose {@code getSchemaNode()} equals the original node.
     */
    @Test
    void fetchSubSchemaNode_ShouldReturnValidSchema_WhenObjectNodeIsValid() {
        ObjectNode schemaObj = JsonNodeFactory.instance.objectNode();
        schemaObj.put("type", "string");
        ValidationContext ctx = new ValidationContext(null, null, null) {

            @Override
            public JsonSchema newSchema(SchemaLocation sl, JsonNodePath path, JsonNode node, JsonSchema parent) {
                // Use reflection to create a concrete JsonSchema that simply wraps the node.
                try {
                    Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
                    ctor.setAccessible(true);
                    return ctor.newInstance(this, sl, path, node, parent, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        TestValidator validator = new TestValidator(null, null, schemaObj, null, null, ctx, false);
        JsonSchema result = validator.fetchSubSchemaNodePublic(ctx);
        Assertions.assertNotNull(result, "A valid object node must produce a non‑null JsonSchema");
        Assertions.assertSame(schemaObj, result.getSchemaNode(), "The returned schema's node should be the original schema node");
    }
}
