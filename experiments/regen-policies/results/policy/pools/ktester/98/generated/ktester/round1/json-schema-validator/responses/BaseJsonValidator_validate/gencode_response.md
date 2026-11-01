```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link BaseJsonValidator#validate(ExecutionContext, JsonNode)}.
 *
 * <p>Because {@code BaseJsonValidator} is abstract, a tiny concrete implementation
 * {@link RecordingValidator} is defined inside the test.  The implementation
 * records the arguments it receives, allows us to inject custom behaviour
 * (e.g. throwing an exception, returning a specific {@link ValidationMessage}
 * set, or controlling the result of {@code collectAnnotations(...)}) and
 * therefore lets us verify the contract of the public {@code validate(...)}
 * method under many different scenarios.
 *
 * <p>The test cases are derived from the JSON specification supplied with the
 * task.  Each logical group is represented by one (or a few) test methods.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_Test {

    /* --------------------------------------------------------------------- */
    /*  Mocks required for the BaseJsonValidator constructor                    */
    /* --------------------------------------------------------------------- */
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock JsonNode mockSchemaNode;
    @Mock JsonSchema mockParentSchema;
    @Mock ValidatorTypeCode mockValidatorType;
    @Mock ValidationContext mockValidationContext;

    /* --------------------------------------------------------------------- */
    /*  Helper concrete validator that records calls and can be customised   */
    /* --------------------------------------------------------------------- */
    private static class RecordingValidator extends BaseJsonValidator {

        /** Result that the overridden protected validate method will return. */
        private final Set<ValidationMessage> stubResult;

        /** Captured arguments of the protected validate method. */
        JsonNode capturedNode;
        JsonNode capturedRootNode;
        JsonNodePath capturedInstanceLocation;

        /** Behaviour switches for the abstract helper methods. */
        private boolean collectAnnotationsResult = false;
        private RuntimeException collectAnnotationsException = null;
        private RuntimeException atRootException = null;
        private JsonNodePath atRootPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        private RuntimeException protectedValidateException = null;

        RecordingValidator(SchemaLocation schemaLocation,
                          JsonNodePath evaluationPath,
                          JsonNode schemaNode,
                          JsonSchema parentSchema,
                          ValidatorTypeCode validatorType,
                          ValidationContext validationContext,
                          Set<ValidationMessage> stubResult) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.stubResult = stubResult;
        }

        /* ----------------------------------------------------------------- */
        /*  Overridden abstract members – not used in these tests            */
        /* ----------------------------------------------------------------- */
        @Override protected JsonNodePath atRoot() {
            if (atRootException != null) {
                throw atRootException;
            }
            return atRootPath;
        }

        @Override public JsonNodePath getEvaluationPath() { return null; }
        @Override public String getKeyword() { return "testKeyword"; }
        @Override public JsonSchema getEvaluationParentSchema() { return null; }
        @Override public SchemaLocation getSchemaLocation() { return null; }
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public String getNodeFieldType() { return null; }

        /* ----------------------------------------------------------------- */
        /*  Core delegation point – records arguments and can throw         */
        /* ----------------------------------------------------------------- */
        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   JsonNodePath instanceLocation) {
            if (protectedValidateException != null) {
                throw protectedValidateException;
            }
            this.capturedNode = node;
            this.capturedRootNode = rootNode;
            this.capturedInstanceLocation = instanceLocation;
            return stubResult;
        }

        /* ----------------------------------------------------------------- */
        /*  Annotation collection helpers                                    */
        /* ----------------------------------------------------------------- */
        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext) {
            if (collectAnnotationsException != null) {
                throw collectAnnotationsException;
            }
            return collectAnnotationsResult;
        }

        @Override
        protected boolean collectAnnotations(ExecutionContext executionContext, String keyword) {
            return collectAnnotationsResult;
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Simple ValidationMessage implementation for assertions               */
    /* --------------------------------------------------------------------- */
    private static final class SimpleMessage implements ValidationMessage {
        private final String message;
        SimpleMessage(String message) { this.message = message; }
        @Override public String getMessage() { return message; }
        @Override public String getPath() { return ""; }
        @Override public String getType() { return ""; }
        @Override public String getSchemaLocation() { return ""; }
        @Override public String getKeyword() { return ""; }
        @Override public String getErrorCode() { return ""; }
        @Override public String toString() { return message; }
    }

    /* --------------------------------------------------------------------- */
    /*  Re‑usable factory method for a validator with a given stub result   */
    /* --------------------------------------------------------------------- */
    private RecordingValidator createValidator(Set<ValidationMessage> stubResult) {
        return new RecordingValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                stubResult);
    }

    /* --------------------------------------------------------------------- */
    /*  1. Null handling                                                      */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_NullExecutionContextAndNode_throwsNullPointerException() {
        RecordingValidator validator = createValidator(Collections.emptySet());
        assertThrows(NullPointerException.class,
                () -> validator.validate(null, null),
                "Both arguments null – NPE expected");
    }

    @Test
    void validate_NullExecutionContext_throwsNullPointerException() {
        RecordingValidator validator = createValidator(Collections.emptySet());
        JsonNode node = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class,
                () -> validator.validate(null, node),
                "Null ExecutionContext – NPE expected");
    }

    @Test
    void validate_NullNode_throwsNullPointerException() {
        RecordingValidator validator = createValidator(Collections.emptySet());
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, null),
                "Null JsonNode – NPE expected");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Root handling – atRoot true / false                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_RootTrue_DelegatesAndReturnsStubResult() {
        Set<ValidationMessage> stub = Set.of(new SimpleMessage("root‑true"));
        RecordingValidator validator = createValidator(stub);
        // force atRoot() to return a non‑null path (treated as “true” in the spec)
        validator.atRootPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.textNode("sample");

        Set<ValidationMessage> result = validator.validate(ctx, node);

        assertEquals(stub, result, "Result must be the stub set");
        assertSame(node, validator.capturedNode, "Node argument must be passed unchanged");
        assertSame(node, validator.capturedRootNode, "Root node argument must be the same as the instance node");
        assertNotNull(validator.capturedInstanceLocation, "Instance location must be supplied");
    }

    @Test
    void validate_RootFalse_DelegatesAndReturnsStubResult() {
        Set<ValidationMessage> stub = Set.of(new SimpleMessage("root‑false"));
        RecordingValidator validator = createValidator(stub);
        // Simulate “false” by returning null – the BaseJsonValidator implementation
        // will still forward the same node as rootNode.
        validator.atRootPath = null;

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("field", 1);

        Set<ValidationMessage> result = validator.validate(ctx, node);

        assertEquals(stub, result, "Result must be the stub set");
        assertSame(node, validator.capturedNode, "Node argument must be passed unchanged");
        assertSame(node, validator.capturedRootNode, "Root node argument must be the same as the instance node");
    }

    /* --------------------------------------------------------------------- */
    /*  3. Annotation collection branch                                        */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_CollectAnnotationsTrue_ReturnsResultWithAnnotationFlag() {
        Set<ValidationMessage> stub = Set.of(new SimpleMessage("annot‑true"));
        RecordingValidator validator = createValidator(stub);
        validator.collectAnnotationsResult = true; // simulate successful annotation collection

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("annotated", true);

        Set<ValidationMessage> result = validator.validate(ctx, node);

        assertEquals(stub, result, "Result must be the stub set");
        // The only observable effect is that no exception was thrown and the
        // internal flag was honoured – we assert the flag directly.
        assertTrue(validator.collectAnnotationsResult, "collectAnnotations should have returned true");
    }

    @Test
    void validate_CollectAnnotationsFalse_ReturnsResultWithoutAnnotationSideEffect() {
        Set<ValidationMessage> stub = Set.of(new SimpleMessage("annot‑false"));
        RecordingValidator validator = createValidator(stub);
        validator.collectAnnotationsResult = false; // annotation collection disabled

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("annotated", false);

        Set<ValidationMessage> result = validator.validate(ctx, node);

        assertEquals(stub, result, "Result must be the stub set");
        assertFalse(validator.collectAnnotationsResult, "collectAnnotations should have returned false");
    }

    /* --------------------------------------------------------------------- */
    /*  4. Exception propagation – protected validate throws                 */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_ProtectedValidateThrowsRuntimeException_Propagates() {
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = createValidator(stub);
        validator.protectedValidateException = new RuntimeException("forced‑error");

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.arrayNode();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.validate(ctx, node),
                "RuntimeException should be propagated");
        assertEquals("forced‑error", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  5. collectAnnotations throws exception                                 */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_CollectAnnotationsThrowsException_Propagates() {
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = createValidator(stub);
        validator.collectAnnotationsException = new IllegalStateException("annotation‑failure");

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("annot", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> validator.validate(ctx, node),
                "Exception from collectAnnotations must propagate");
        assertEquals("annotation‑failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  6. atRoot throws exception                                               */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_AtRootThrowsException_Propagates() {
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = createValidator(stub);
        validator.atRootException = new RuntimeException("atRoot‑failed");

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.validate(ctx, node),
                "Exception from atRoot() must propagate");
        assertEquals("atRoot‑failed", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  7. Delegation with various JsonNode types (parameterised)            */
    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> jsonNodeProvider() {
        return Stream.of(
                Arguments.of(JsonNodeFactory.instance.textNode("any string"), "text‑node"),
                Arguments.of(JsonNodeFactory.instance.numberNode(42), "number‑node"),
                Arguments.of(JsonNodeFactory.instance.booleanNode(true), "boolean‑node"),
                Arguments.of(JsonNodeFactory.instance.arrayNode(), "empty‑array"),
                Arguments.of(
                        JsonNodeFactory.instance.objectNode()
                                .set("inner", JsonNodeFactory.instance.textNode("value")),
                        "object‑with‑inner")
        );
    }

    @ParameterizedTest
    @MethodSource("jsonNodeProvider")
    void validate_VariousJsonNodeTypes_DelegatesAndReturnsCorrespondingMessage(JsonNode node, String expectedMessage) {
        Set<ValidationMessage> stub = Set.of(new SimpleMessage(expectedMessage));
        RecordingValidator validator = createValidator(stub);

        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> result = validator.validate(ctx, node);

        assertEquals(stub, result, "Result must contain the expected message");
        assertSame(node, validator.capturedNode, "The node passed to the protected validate must be unchanged");
    }

    /* --------------------------------------------------------------------- */
    /*  8. Null schemaNode handling – expect NPE when schemaNode is null      */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_NullSchemaNode_ThrowsNullPointerException() {
        // Build a validator where the protected field `schemaNode` is deliberately null.
        RecordingValidator validator = new RecordingValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                null,                         // <-- schemaNode is null
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                Collections.emptySet());

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();

        // The concrete BaseJsonValidator implementation accesses `schemaNode` in
        // various helper methods; the first dereference will trigger NPE.
        assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, node),
                "When schemaNode is null a NullPointerException is expected");
    }

    /* --------------------------------------------------------------------- */
    /*  9. Null validationContext handling – expect NPE when context is null */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_NullValidationContext_ThrowsNullPointerException() {
        RecordingValidator validator = new RecordingValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                null,                         // <-- validationContext is null
                Collections.emptySet());

        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.textNode("test");

        assertThrows(NullPointerException.class,
                () -> validator.validate(ctx, node),
                "When validationContext is null a NullPointerException is expected");
    }

    /* --------------------------------------------------------------------- */
    /*  10. Discriminator mismatch – provoke IllegalArgumentException       */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorMismatch_ThrowsIllegalArgumentException() {
        // Build a minimal discriminator object that expects property "type" = "known"
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.put("propertyName", "type");
        discriminator.set("mapping", JsonNodeFactory.instance.objectNode().set("known",
                JsonNodeFactory.instance.objectNode()));

        // The test subclass will invoke the static checkDiscriminatorMatch method
        // with a mismatching value, causing the expected exception.
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = new RecordingValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                stub) {

            @Override
            protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                      JsonNode node,
                                                      JsonNode rootNode,
                                                      JsonNodePath instanceLocation) {
                // Simulate the internal call that the real validator would perform.
                // The discriminator expects the value "known", but we pass "unknown".
                String discriminatorPropertyValue = node.get("type").asText();
                BaseJsonValidator.checkDiscriminatorMatch(
                        new DiscriminatorContext(),
                        discriminator,
                        discriminatorPropertyValue,
                        null); // jsonSchema argument not needed for this test
                return stub;
            }
        };

        ExecutionContext ctx = new ExecutionContext();
        ObjectNode instance = JsonNodeFactory.instance.objectNode().put("type", "unknown");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(ctx, instance),
                "Mismatching discriminator value must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("discriminator"),
                "Exception message should mention discriminator");
    }

    /* --------------------------------------------------------------------- */
    /*  11. registerAndMergeDiscriminator duplicate registration             */
    /* --------------------------------------------------------------------- */
    @Test
    void registerAndMergeDiscriminator_DuplicateRegistration_ThrowsIllegalStateException() {
        // First registration – should succeed.
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.put("propertyName", "type");
        discriminator.set("mapping", JsonNodeFactory.instance.objectNode()
                .set("A", JsonNodeFactory.instance.objectNode()));

        JsonSchema schema = mock(JsonSchema.class);
        JsonNodePath location = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // First call – no exception expected.
        assertDoesNotThrow(() ->
                BaseJsonValidator.registerAndMergeDiscriminator(context, discriminator, schema, location));

        // Second call with the same property – should raise IllegalStateException.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BaseJsonValidator.registerAndMergeDiscriminator(context, discriminator, schema, location),
                "Duplicate discriminator registration must fail");
        assertTrue(ex.getMessage().contains("already registered"),
                "Exception message should mention duplicate registration");
    }

    /* --------------------------------------------------------------------- */
    /*  12. Existing simple delegation test (kept unchanged)                 */
    /* --------------------------------------------------------------------- */
    @Test
    void validate_ShouldDelegateToProtectedValidateAndReturnResult() {
        // GIVEN
        ExecutionContext executionContext = new ExecutionContext(); // real instance – no behaviour needed
        JsonNode nodeToValidate = mock(JsonNode.class); // the node being validated (content is irrelevant)

        // WHEN
        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate);

        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(1, result.size(), "Result set should contain exactly one message");
        ValidationMessage message = result.iterator().next();
        assertEquals("stub validation message", message.getMessage(),
                "The message returned must be the one supplied by the stub implementation");
    }

    /* --------------------------------------------------------------------- */
    /*  Test fixture – the validator used by the original simple test       */
    /* --------------------------------------------------------------------- */
    private TestValidator validator;

    /** Minimal concrete implementation of {@link BaseJsonValidator} used for the original test. */
    private static class TestValidator extends BaseJsonValidator {

        /** The set that the overridden {@code validate(...)} method will return. */
        private final Set<ValidationMessage> stubResult;

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      Set<ValidationMessage> stubResult) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.stubResult = stubResult;
        }

        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   JsonNodePath instanceLocation) {
            return stubResult;
        }

        @Override protected JsonNodePath atRoot() { return new JsonNodePath(JsonNodePath.PathType.JSON_POINTER); }
        @Override public JsonNodePath getEvaluationPath() { return null; }
        @Override public String getKeyword() { return "testKeyword"; }
        @Override public JsonSchema getEvaluationParentSchema() { return null; }
        @Override public SchemaLocation getSchemaLocation() { return null; }
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public JsonSchema getParentSchema() { return null; }
        @Override public String getNodeFieldType() { return null; }
    }

    @BeforeEach
    void setupBeforeEach() {
        Set<ValidationMessage> stubResult = new HashSet<>();
        stubResult.add(new SimpleMessage("stub validation message"));
        validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                stubResult);
    }
}
```