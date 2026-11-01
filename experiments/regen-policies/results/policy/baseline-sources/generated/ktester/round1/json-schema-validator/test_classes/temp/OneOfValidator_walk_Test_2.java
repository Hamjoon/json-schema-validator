package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

/**
 * Tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OneOfValidator walk() behaviour")
class OneOfValidator_walk_Test {

    /** shared test utilities */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** objects created per test */
    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private JsonNode schemaNode;
    private OneOfValidator validator;
    private ExecutionContext execContext;
    private JsonNode dataNode;
    private JsonNode rootNode;
    private String at;

    /** two child schemas that will be injected into the validator (recording) */
    private RecordingJsonSchema childSchema0;
    private RecordingJsonSchema childSchema1;

    /** mock child schemas used for verification */
    private JsonSchema mockChildSchema0;
    private JsonSchema mockChildSchema1;

    /** a simple ValidationContext stub – it is never consulted because we inject schemas directly */
    private ValidationContext dummyContext;

    /** Helper validator that allows us to stub the {@code validate} method. */
    private static class StubValidateOneOfValidator extends OneOfValidator {

        private Set<ValidationMessage> stubResult = Collections.emptySet();

        StubValidateOneOfValidator(String path, JsonNode schemaNode, JsonSchema parent, ValidationContext ctx) {
            super(path, schemaNode, parent, ctx);
        }

        void setStubResult(Set<ValidationMessage> result) {
            this.stubResult = result;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            return stubResult;
        }
    }

    /** Minimal JsonSchema implementation that records whether {@code walk} was called. */
    private static class RecordingJsonSchema extends JsonSchema {

        private boolean walked = false;

        RecordingJsonSchema() {
            // protected constructor of JsonSchema expects a ValidationContext, a schema node and a parent schema.
            super(null, null, null);
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node,
                                          JsonNode rootNode, String at, boolean shouldValidateSchema) {
            walked = true;
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node,
                                              JsonNode rootNode, String at) {
            return Collections.emptySet();
        }

        boolean wasWalked() {
            return walked;
        }

        void reset() {
            walked = false;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- mocks & stubs -------------------------------------------------
        validationContext = mock(ValidationContext.class);
        parentSchema = mock(JsonSchema.class);
        execContext = new ExecutionContext();
        dataNode = MAPPER.createObjectNode();
        rootNode = MAPPER.createObjectNode();
        at = "#";

        // ----- build a schemaNode that contains two child schemas ------------
        ArrayNode arrayNode = MAPPER.createArrayNode();
        ObjectNode childDef0 = MAPPER.createObjectNode();
        childDef0.put("type", "string");
        arrayNode.add(childDef0);
        ObjectNode childDef1 = MAPPER.createObjectNode();
        childDef1.put("type", "integer");
        arrayNode.add(childDef1);
        schemaNode = arrayNode;

        // ----- create mock child schemas and stub ValidationContext ------------
        mockChildSchema0 = mock(JsonSchema.class);
        mockChildSchema1 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), eq(childDef0), eq(parentSchema))).thenReturn(mockChildSchema0);
        when(validationContext.newSchema(anyString(), eq(childDef1), eq(parentSchema))).thenReturn(mockChildSchema1);

        // ----- construct the OneOfValidator (populates its internal list) ---
        validator = new OneOfValidator("testPath", schemaNode, parentSchema, validationContext);

        // ----- create recording child schemas ---------------------------------
        childSchema0 = new RecordingJsonSchema();
        childSchema1 = new RecordingJsonSchema();

        // ----- inject both mock and recording schemas into the private 'schemas' field ----------
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        List<JsonSchema> combined = new ArrayList<>();
        combined.addAll(Arrays.asList(mockChildSchema0, mockChildSchema1));
        combined.addAll(Arrays.asList(childSchema0, childSchema1));
        schemasField.set(validator, combined);

        // ----- dummy context for tests that need it ---------------------------
        dummyContext = new ValidationContext(null, null, null, null) {
        };
    }

    private static Stream<Set<ValidationMessage>> provideValidateResults() {
        ValidationMessage dummyMsg = new ValidationMessage("dummy");
        return Stream.of(
                Collections.singleton(dummyMsg), // non‑empty
                Collections.emptySet()          // empty
        );
    }

    @Test
    void walk_shouldValidateSchemaTrue_delegatesToValidate() {
        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = new HashSet<>();
        expected.add(mockMessage);
        OneOfValidator spyValidator = spy(validator);
        doReturn(expected).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> actual = spyValidator.walk(execContext, dataNode, rootNode, at, true);

        assertSame(expected, actual, "walk should return the set produced by validate");
        verify(spyValidator, times(1)).validate(execContext, dataNode, rootNode, at);
    }

    @Test
    void walk_shouldValidateSchemaFalse_walksChildSchemas() {
        JsonSchema cs0 = validationContext.newSchema("testPath/0", ((ArrayNode) schemaNode).get(0), parentSchema);
        JsonSchema cs1 = validationContext.newSchema("testPath/1", ((ArrayNode) schemaNode).get(1), parentSchema);

        Set<ValidationMessage> result = validator.walk(execContext, dataNode, rootNode, at, false);

        assertTrue(result.isEmpty(), "Result set must be empty when shouldValidateSchema is false");
        verify(cs0, times(1)).walk(execContext, dataNode, rootNode, at, false);
        verify(cs1, times(1)).walk(execContext, dataNode, rootNode, at, false);

        OneOfValidator spyValidator = spy(validator);
        spyValidator.walk(execContext, dataNode, rootNode, at, false);
        verify(spyValidator, never()).validate(any(), any(), any(), anyString());
    }

    @Test
    void walk_shouldValidateSchemaTrue_emptyResult() {
        OneOfValidator spyValidator = spy(validator);
        doReturn(Collections.emptySet()).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = spyValidator.walk(execContext, dataNode, rootNode, at, true);

        assertTrue(result.isEmpty(), "walk should return an empty set when validate returns empty");
        verify(spyValidator, times(1)).validate(execContext, dataNode, rootNode, at);
    }

    @Test
    void walk_shouldValidateSchemaFalse_noChildSchemas_returnsEmptySet() {
        ArrayNode emptyArray = MAPPER.createArrayNode();
        OneOfValidator emptyValidator = new OneOfValidator("emptyPath", emptyArray, parentSchema, validationContext);
        Set<ValidationMessage> result = emptyValidator.walk(execContext, dataNode, rootNode, at, false);
        assertTrue(result.isEmpty(), "Result must be empty when there are no child schemas");
    }

    @Test
    void walk_shouldValidateSchemaFalse_multipleChildSchemas() {
        JsonSchema cs0 = validationContext.newSchema("testPath/0", ((ArrayNode) schemaNode).get(0), parentSchema);
        JsonSchema cs1 = validationContext.newSchema("testPath/1", ((ArrayNode) schemaNode).get(1), parentSchema);
        Set<ValidationMessage> result = validator.walk(execContext, dataNode, rootNode, at, false);
        assertTrue(result.isEmpty(), "Result set must be empty for shouldValidateSchema == false");
        verify(cs0, times(1)).walk(execContext, dataNode, rootNode, at, false);
        verify(cs1, times(1)).walk(execContext, dataNode, rootNode, at, false);

        OneOfValidator spyValidator = spy(validator);
        spyValidator.walk(execContext, dataNode, rootNode, at, false);
        verify(spyValidator, never()).validate(any(), any(), any(), anyString());
    }

    @Test
    void walk_childSchemaThrowsRuntimeException_propagates() {
        ObjectNode child = MAPPER.createObjectNode();
        child.put("type", "string");
        ArrayNode singleArray = MAPPER.createArrayNode().add(child);
        JsonSchema throwingChild = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), eq(child), eq(parentSchema))).thenReturn(throwingChild);
        doThrow(new RuntimeException("child failure")).when(throwingChild)
                .walk(execContext, dataNode, rootNode, at, false);
        OneOfValidator validatorWithThrowingChild = new OneOfValidator("path/withThrow", singleArray, parentSchema, validationContext);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validatorWithThrowingChild.walk(execContext, dataNode, rootNode, at, false),
                "Exception from child schema should propagate");
        assertEquals("child failure", ex.getMessage());
    }

    @Test
    void walk_shouldValidateSchemaTrue_nullNode_propagatesException() {
        OneOfValidator spyValidator = spy(validator);
        doThrow(new NullPointerException("node is null"))
                .when(spyValidator).validate(eq(execContext), isNull(), any(), anyString());
        assertThrows(NullPointerException.class,
                () -> spyValidator.walk(execContext, null, rootNode, at, true),
                "NullPointerException should be propagated when node is null");
    }

    @Test
    void walk_shouldValidateSchemaTrue_nullExecutionContext_propagatesException() {
        OneOfValidator spyValidator = spy(validator);
        doThrow(new NullPointerException("executionContext is null"))
                .when(spyValidator).validate(isNull(), any(), any(), anyString());
        assertThrows(NullPointerException.class,
                () -> spyValidator.walk(null, dataNode, rootNode, at, true),
                "NullPointerException should be propagated when ExecutionContext is null");
    }

    @ParameterizedTest
    @MethodSource("provideValidateResults")
    void walk_shouldValidateSchemaTrue_parameterized(Set<ValidationMessage> stubResult) {
        OneOfValidator spyValidator = spy(validator);
        doReturn(stubResult).when(spyValidator).validate(any(), any(), any(), anyString());
        Set<ValidationMessage> actual = spyValidator.walk(execContext, dataNode, rootNode, at, true);
        assertEquals(stubResult, actual, "walk must return exactly what validate() returns");
    }

    @Test
    @DisplayName("walk should delegate to validate when shouldValidateSchema == true")
    void walk_delegatesToValidateWhenFlagTrue() {
        StubValidateOneOfValidator stubValidator = new StubValidateOneOfValidator("testPath",
                MAPPER.createArrayNode(), null, dummyContext);
        Set<ValidationMessage> expected = new HashSet<>();
        expected.add(new ValidationMessage("dummy"));
        stubValidator.setStubResult(expected);
        Set<ValidationMessage> actual = stubValidator.walk(execContext, dataNode, rootNode, at, true);
        assertSame(expected, actual, "walk must return exactly what validate() returns");
    }

    @Test
    @DisplayName("walk should return empty set when shouldValidateSchema == true and validate returns empty")
    void walk_trueFlagEmptyResult() {
        StubValidateOneOfValidator stubValidator = new StubValidateOneOfValidator("testPath",
                MAPPER.createArrayNode(), null, dummyContext);
        stubValidator.setStubResult(Collections.emptySet());
        Set<ValidationMessage> result = stubValidator.walk(execContext, dataNode, rootNode, at, true);
        assertTrue(result.isEmpty(), "Result should be empty when validate returns empty");
    }

    @Test
    @DisplayName("walk should invoke each child schema when shouldValidateSchema == false")
    void walk_falseFlagInvokesChildren() {
        assertFalse(childSchema0.wasWalked(), "child 0 not walked yet");
        assertFalse(childSchema1.wasWalked(), "child 1 not walked yet");
        Set<ValidationMessage> result = validator.walk(execContext, dataNode, rootNode, at, false);
        assertTrue(result.isEmpty(), "Result set must be empty when shouldValidateSchema is false");
        assertTrue(childSchema0.wasWalked(), "child 0 walk should have been called");
        assertTrue(childSchema1.wasWalked(), "child 1 walk should have been called");
    }

    @Test
    @DisplayName("walk with no child schemas returns empty set")
    void walk_falseFlagNoChildren() throws Exception {
        OneOfValidator emptyValidator = new OneOfValidator("emptyPath",
                MAPPER.createArrayNode(), null, dummyContext);
        Set<ValidationMessage> result = emptyValidator.walk(execContext, dataNode, rootNode, at, false);
        assertTrue(result.isEmpty(), "Result must be empty when there are no child schemas");
    }

    @Test
    @DisplayName("walk propagates RuntimeException thrown by a child schema")
    void walk_propagatesExceptionFromChild() throws Exception {
        RecordingJsonSchema throwingChild = new RecordingJsonSchema() {
            @Override
            public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node,
                                                JsonNode rootNode, String at, boolean shouldValidateSchema) {
                throw new RuntimeException("child failure");
            }
        };
        OneOfValidator validatorWithThrow = new OneOfValidator("path",
                MAPPER.createArrayNode().add(MAPPER.createObjectNode().put("type", "string")),
                null, dummyContext);
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validatorWithThrow, Collections.singletonList(throwingChild));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validatorWithThrow.walk(execContext, dataNode, rootNode, at, false));
        assertEquals("child failure", ex.getMessage());
    }

    @Test
    @DisplayName("walk forwards NullPointerException when node is null and flag true")
    void walk_nullNodePropagatesException() {
        StubValidateOneOfValidator stubValidator = new StubValidateOneOfValidator("testPath",
                MAPPER.createArrayNode(), null, dummyContext) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node,
                                                     JsonNode rootNode, String at) {
                if (node == null) {
                    throw new NullPointerException("node is null");
                }
                return super.validate(ctx, node, rootNode, at);
            }
        };
        assertThrows(NullPointerException.class,
                () -> stubValidator.walk(execContext, null, rootNode, at, true));
    }

    @Test
    @DisplayName("walk forwards NullPointerException when ExecutionContext is null and flag true")
    void walk_nullExecutionContextPropagatesException() {
        StubValidateOneOfValidator stubValidator = new StubValidateOneOfValidator("testPath",
                MAPPER.createArrayNode(), null, dummyContext) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node,
                                                     JsonNode rootNode, String at) {
                if (ctx == null) {
                    throw new NullPointerException("executionContext is null");
                }
                return super.validate(ctx, node, rootNode, at);
            }
        };
        assertThrows(NullPointerException.class,
                () -> stubValidator.walk(null, dataNode, rootNode, at, true));
    }

    @ParameterizedTest
    @MethodSource("provideValidateResults")
    @DisplayName("walk returns exactly what validate() returns (parameterized)")
    void walk_parameterizedValidateResult(Set<ValidationMessage> stubResult) {
        StubValidateOneOfValidator stubValidator = new StubValidateOneOfValidator("testPath",
                MAPPER.createArrayNode(), null, dummyContext);
        stubValidator.setStubResult(stubResult);
        Set<ValidationMessage> actual = stubValidator.walk(execContext, dataNode, rootNode, at, true);
        assertEquals(stubResult, actual);
    }
}