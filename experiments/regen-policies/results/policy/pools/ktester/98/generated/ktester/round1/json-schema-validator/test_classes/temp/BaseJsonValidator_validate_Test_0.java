package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.i18n.DefaultMessageSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_validate_Test {

    /**
     * Mocks required for the {@link BaseJsonValidator} constructor.
     */
    @Mock
    SchemaLocation mockSchemaLocation;

    @Mock
    JsonNodePath mockEvaluationPath;

    @Mock
    JsonNode mockSchemaNode;

    @Mock
    JsonSchema mockParentSchema;

    @Mock
    ValidatorTypeCode mockValidatorType;

    @Mock
    ValidationContext mockValidationContext;

    /**
     * The concrete validator used in the tests.
     */
    private TestValidator validator;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     */
    private static class TestValidator extends BaseJsonValidator {

        /**
         * The set that the overridden {@code validate(...)} method will return.
         */
        private final Set<ValidationMessage> stubResult;

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, Set<ValidationMessage> stubResult) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.stubResult = stubResult;
        }

        /**
         * The method that the public {@code validate(...)} delegates to.
         */
        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
            // In a real validator this would contain the validation logic.
            // For the test we simply return the stubbed result.
            return stubResult;
        }

        /**
         * Required abstract members – they are not used by the test.
         */
        @Override
        protected JsonNodePath atRoot() {
            return new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public String getNodeFieldType() {
            return null;
        }
    }

    /* --------------------------------------------------------------------- */
    private static class RecordingValidator extends BaseJsonValidator {

        /**
         * Result that the overridden protected validate method will return.
         */
        private final Set<ValidationMessage> stubResult;

        /**
         * Captured arguments of the protected validate method.
         */
        JsonNode capturedNode;

        JsonNode capturedRootNode;

        JsonNodePath capturedInstanceLocation;

        /**
         * Behaviour switches for the abstract helper methods.
         */
        private boolean collectAnnotationsResult = false;

        private RuntimeException collectAnnotationsException = null;

        private RuntimeException atRootException = null;

        private JsonNodePath atRootPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        private RuntimeException protectedValidateException = null;

        RecordingValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, Set<ValidationMessage> stubResult) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.stubResult = stubResult;
        }

        /* ----------------------------------------------------------------- */
        /*  Overridden abstract members – not used in these tests            */
        /* ----------------------------------------------------------------- */
        @Override
        protected JsonNodePath atRoot() {
            if (atRootException != null) {
                throw atRootException;
            }
            return atRootPath;
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }

        @Override
        public String getNodeFieldType() {
            return null;
        }

        /* ----------------------------------------------------------------- */
        /*  Core delegation point – records arguments and can throw         */
        /* ----------------------------------------------------------------- */
        @Override
        protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
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
    private static final class SimpleMessage implements ValidationMessage {

        private final String message;

        SimpleMessage(String message) {
            this.message = message;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public String getPath() {
            return "";
        }

        @Override
        public String getType() {
            return "";
        }

        @Override
        public String getSchemaLocation() {
            return "";
        }

        @Override
        public String getKeyword() {
            return "";
        }

        @Override
        public String getErrorCode() {
            return "";
        }

        @Override
        public String toString() {
            return message;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a deterministic stub result.
        Set<ValidationMessage> stubResult = new HashSet<>();
        stubResult.add(new SimpleMessage("stub validation message"));
        // Instantiate the concrete validator with all mocked constructor arguments.
        validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, stubResult);
    }

    /* --------------------------------------------------------------------- */
    private RecordingValidator createValidator(Set<ValidationMessage> stubResult) {
        return new RecordingValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, stubResult);
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> jsonNodeProvider() {
        return Stream.of(Arguments.of(JsonNodeFactory.instance.textNode("any string"), "text‑node"), Arguments.of(JsonNodeFactory.instance.numberNode(42), "number‑node"), Arguments.of(JsonNodeFactory.instance.booleanNode(true), "boolean‑node"), Arguments.of(JsonNodeFactory.instance.arrayNode(), "empty‑array"), Arguments.of(JsonNodeFactory.instance.objectNode().set("inner", JsonNodeFactory.instance.textNode("value")), "object‑with‑inner"));
    }

    @Test
    void validate_ShouldDelegateToProtectedValidateAndReturnResult() {
        // GIVEN
        // real instance – no behaviour needed
        ExecutionContext executionContext = new ExecutionContext();
        // the node being validated (content is irrelevant)
        JsonNode nodeToValidate = mock(JsonNode.class);
        // WHEN
        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(1, result.size(), "Result set should contain exactly one message");
        ValidationMessage message = result.iterator().next();
        assertEquals("stub validation message", message.getMessage(), "The message returned must be the one supplied by the stub implementation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_NullExecutionContextAndNode_throwsNullPointerException() {
        RecordingValidator validator = createValidator(Collections.emptySet());
        assertThrows(NullPointerException.class, () -> validator.validate(null, null), "Both arguments null – NPE expected");
    }

    @Test
    void validate_NullExecutionContext_throwsNullPointerException() {
        RecordingValidator validator = createValidator(Collections.emptySet());
        JsonNode node = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class, () -> validator.validate(null, node), "Null ExecutionContext – NPE expected");
    }

    @Test
    void validate_NullNode_throwsNullPointerException() {
        RecordingValidator validator = createValidator(Collections.emptySet());
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, null), "Null JsonNode – NPE expected");
    }

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
    @Test
    void validate_CollectAnnotationsTrue_ReturnsResultWithAnnotationFlag() {
        Set<ValidationMessage> stub = Set.of(new SimpleMessage("annot‑true"));
        RecordingValidator validator = createValidator(stub);
        // simulate successful annotation collection
        validator.collectAnnotationsResult = true;
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
        // annotation collection disabled
        validator.collectAnnotationsResult = false;
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("annotated", false);
        Set<ValidationMessage> result = validator.validate(ctx, node);
        assertEquals(stub, result, "Result must be the stub set");
        assertFalse(validator.collectAnnotationsResult, "collectAnnotations should have returned false");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_ProtectedValidateThrowsRuntimeException_Propagates() {
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = createValidator(stub);
        validator.protectedValidateException = new RuntimeException("forced‑error");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.arrayNode();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(ctx, node), "RuntimeException should be propagated");
        assertEquals("forced‑error", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_CollectAnnotationsThrowsException_Propagates() {
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = createValidator(stub);
        validator.collectAnnotationsException = new IllegalStateException("annotation‑failure");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode().put("annot", true);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> validator.validate(ctx, node), "Exception from collectAnnotations must propagate");
        assertEquals("annotation‑failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_AtRootThrowsException_Propagates() {
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = createValidator(stub);
        validator.atRootException = new RuntimeException("atRoot‑failed");
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(ctx, node), "Exception from atRoot() must propagate");
        assertEquals("atRoot‑failed", ex.getMessage());
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
    @Test
    void validate_NullSchemaNode_ThrowsNullPointerException() {
        // Build a validator where the protected field `schemaNode` is deliberately null.
        RecordingValidator validator = new RecordingValidator(mockSchemaLocation, mockEvaluationPath, // <-- schemaNode is null
        null, mockParentSchema, mockValidatorType, mockValidationContext, Collections.emptySet());
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.objectNode();
        // The concrete BaseJsonValidator implementation accesses `schemaNode` in
        // various helper methods; the first dereference will trigger NPE.
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, node), "When schemaNode is null a NullPointerException is expected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_NullValidationContext_ThrowsNullPointerException() {
        RecordingValidator validator = new RecordingValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, // <-- validationContext is null
        null, Collections.emptySet());
        ExecutionContext ctx = new ExecutionContext();
        JsonNode node = JsonNodeFactory.instance.textNode("test");
        assertThrows(NullPointerException.class, () -> validator.validate(ctx, node), "When validationContext is null a NullPointerException is expected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorMismatch_ThrowsIllegalArgumentException() {
        // Build a minimal discriminator object that expects property "type" = "known"
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.put("propertyName", "type");
        discriminator.set("mapping", JsonNodeFactory.instance.objectNode().set("known", JsonNodeFactory.instance.objectNode()));
        // The test subclass will invoke the static checkDiscriminatorMatch method
        // with a mismatching value, causing the expected exception.
        Set<ValidationMessage> stub = Collections.emptySet();
        RecordingValidator validator = new RecordingValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, stub) {

            @Override
            protected Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation) {
                // Simulate the internal call that the real validator would perform.
                // The discriminator expects the value "known", but we pass "unknown".
                String discriminatorPropertyValue = node.get("type").asText();
                BaseJsonValidator.checkDiscriminatorMatch(new DiscriminatorContext(), discriminator, discriminatorPropertyValue, // jsonSchema argument not needed for this test
                null);
                return stub;
            }
        };
        ExecutionContext ctx = new ExecutionContext();
        ObjectNode instance = JsonNodeFactory.instance.objectNode().put("type", "unknown");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(ctx, instance), "Mismatching discriminator value must cause IllegalArgumentException");
        assertTrue(ex.getMessage().toLowerCase().contains("discriminator"), "Exception message should mention discriminator");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void registerAndMergeDiscriminator_DuplicateRegistration_ThrowsIllegalStateException() {
        // First registration – should succeed.
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = JsonNodeFactory.instance.objectNode();
        discriminator.put("propertyName", "type");
        discriminator.set("mapping", JsonNodeFactory.instance.objectNode().set("A", JsonNodeFactory.instance.objectNode()));
        JsonSchema schema = mock(JsonSchema.class);
        JsonNodePath location = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        // First call – no exception expected.
        assertDoesNotThrow(() -> BaseJsonValidator.registerAndMergeDiscriminator(context, discriminator, schema, location));
        // Second call with the same property – should raise IllegalStateException.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(context, discriminator, schema, location), "Duplicate discriminator registration must fail");
        assertTrue(ex.getMessage().contains("already registered"), "Exception message should mention duplicate registration");
    }
}
