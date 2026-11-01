package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_collectAnnotations_Test {

    // -----------------------------------------------------------------------
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    ValidationContext validationContext;

    // any external resources for the purpose BaseJsonValidator_collectAnnotations_Testthese tests
    ExecutionContext executionContext;

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * The keyword can be set via constructor to drive the different test scenarios.
     */
    private static class TestValidator extends BaseJsonValidator {

        private final String keyword;

        // expose for test construction
        private final JsonNode schemaNode;

        TestValidator(String keyword, JsonNode schemaNode, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
            this.keyword = keyword;
            this.schemaNode = schemaNode;
        }

        @Override
        public String getKeyword() {
            return keyword;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // not relevant for these tests
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // initialise a fresh ExecutionContext before each test
        executionContext = new ExecutionContext();
    }

    /**
     * Calls the protected {@code collectAnnotations(ExecutionContext)} method via reflection.
     */
    private static boolean invokeCollectAnnotations(BaseJsonValidator validator, ExecutionContext ctx) throws Exception {
        Method m = BaseJsonValidator.class.getDeclaredMethod("collectAnnotations", ExecutionContext.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(validator, ctx);
    }

    // -----------------------------------------------------------------------
    static Stream<Arguments> nullExecutionContextProvider() {
        return Stream.of(Arguments.of(null));
    }

    static Stream<Arguments> disabledAnnotationsProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(false);
        return Stream.of(Arguments.of(ctx));
    }

    static Stream<Arguments> nullKeywordProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        return Stream.of(Arguments.of(ctx));
    }

    static Stream<Arguments> knownKeywordSuccessProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> list = new ArrayList<>();
        when(ctx.getAnnotations()).thenReturn(list);
        // instance node is required for location handling – a simple empty object node is enough
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("type", createSchemaNode("type", "string"), ctx));
    }

    static Stream<Arguments> unknownKeywordProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> list = new ArrayList<>();
        when(ctx.getAnnotations()).thenReturn(list);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("unknownKeyword", JsonNodeFactory.instance.objectNode(), ctx));
    }

    static Stream<Arguments> minimumKeywordSuccessProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> list = new ArrayList<>();
        when(ctx.getAnnotations()).thenReturn(list);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("minimum", createSchemaNode("minimum", 5), ctx));
    }

    static Stream<Arguments> missingValueProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> list = new ArrayList<>();
        when(ctx.getAnnotations()).thenReturn(list);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        // schemaNode does NOT contain "type"
        return Stream.of(Arguments.of("type", JsonNodeFactory.instance.objectNode(), ctx));
    }

    static Stream<Arguments> emptyKeywordProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> list = new ArrayList<>();
        when(ctx.getAnnotations()).thenReturn(list);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("", JsonNodeFactory.instance.objectNode(), ctx));
    }

    static Stream<Arguments> preExistingAnnotationsProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        JsonNodeAnnotation existing = mock(JsonNodeAnnotation.class);
        List<JsonNodeAnnotation> list = new ArrayList<>(Collections.singletonList(existing));
        when(ctx.getAnnotations()).thenReturn(list);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("type", createSchemaNode("type", "number"), ctx));
    }

    static Stream<Arguments> nullAnnotationsListProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        when(ctx.getAnnotations()).thenReturn(null);
        // capture the list that the validator sets via setAnnotations(...)
        doAnswer(invocation -> {
            List<JsonNodeAnnotation> newList = invocation.getArgument(0);
            // replace the null list with a mutable one for later verification
            when(ctx.getAnnotations()).thenReturn(newList);
            return null;
        }).when(ctx).setAnnotations(anyList());
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("type", createSchemaNode("type", "boolean"), ctx));
    }

    static Stream<Arguments> suppressSubSchemaRetrievalProvider() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> list = new ArrayList<>();
        when(ctx.getAnnotations()).thenReturn(list);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        // validator with suppressSubSchemaRetrieval = true (set via reflection after construction)
        TestValidator validator = new TestValidator("format", createSchemaNode("format", "email"), mock(SchemaLocation.class), mock(JsonNodePath.class), mock(JsonSchema.class), mock(ValidatorTypeCode.class), mock(ValidationContext.class));
        Field f = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
        f.setAccessible(true);
        f.setBoolean(validator, true);
        return Stream.of(Arguments.of(validator, ctx));
    }

    static Stream<Arguments> nullSchemaNodeProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of((JsonNode) null, ctx));
    }

    static Stream<Arguments> nullInstanceNodeProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        when(ctx.getInstanceNode()).thenReturn(null);
        return Stream.of(Arguments.of("type", createSchemaNode("type", "string"), ctx));
    }

    static Stream<Arguments> unmodifiableAnnotationsProvider() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        List<JsonNodeAnnotation> unmod = Collections.unmodifiableList(new ArrayList<>());
        when(ctx.getAnnotations()).thenReturn(unmod);
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        return Stream.of(Arguments.of("type", createSchemaNode("type", "string"), ctx));
    }

    // -----------------------------------------------------------------------
    private static ObjectNode createSchemaNode(String field, Object value) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (value instanceof String) {
            node.put(field, (String) value);
        } else if (value instanceof Integer) {
            node.put(field, (Integer) value);
        } else if (value instanceof Double) {
            node.put(field, (Double) value);
        }
        return node;
    }

    // -----------------------------------------------------------------------
    @Test
    void collectAnnotations_delegatesAndReturnsTrue() {
        // Arrange – create a spy so we can stub the protected overload
        TestValidator realValidator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        BaseJsonValidator spyValidator = Mockito.spy(realValidator);
        // Stub the protected method to return true regardless BaseJsonValidator_collectAnnotations_Testthe ExecutionContext
        doReturn(true).when(spyValidator).collectAnnotations(any(ExecutionContext.class), eq("testKeyword"));
        // Act
        boolean result = spyValidator.collectAnnotations(executionContext);
        // Assert
        assertTrue(result, "collectAnnotations should return true when the protected overload returns true");
        // Verify that the overload was called exactly once with the expected arguments
        verify(spyValidator, times(1)).collectAnnotations(eq(executionContext), eq("testKeyword"));
    }

    @Test
    void collectAnnotations_delegatesAndReturnsFalse() {
        // Arrange
        TestValidator realValidator = new TestValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        BaseJsonValidator spyValidator = Mockito.spy(realValidator);
        // Stub the protected method to return false
        doReturn(false).when(spyValidator).collectAnnotations(any(ExecutionContext.class), eq("testKeyword"));
        // Act
        boolean result = spyValidator.collectAnnotations(executionContext);
        // Assert
        assertFalse(result, "collectAnnotations should return false when the protected overload returns false");
        verify(spyValidator, times(1)).collectAnnotations(eq(executionContext), eq("testKeyword"));
    }

    @ParameterizedTest
    @MethodSource("nullExecutionContextProvider")
    void collectAnnotations_nullExecutionContext_returnsFalse(ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator("type", JsonNodeFactory.instance.objectNode(), schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertFalse(result, "Method should return false when ExecutionContext is null");
    }

    @ParameterizedTest
    @MethodSource("disabledAnnotationsProvider")
    void collectAnnotations_collectionDisabled_returnsFalse(ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator("type", createSchemaNode("type", "string"), schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertFalse(result, "When executionContext.isCollectAnnotations() is false, method must return false");
    }

    @ParameterizedTest
    @MethodSource("nullKeywordProvider")
    void collectAnnotations_nullKeyword_returnsFalse(ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(null, createSchemaNode("type", "string"), schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertFalse(result, "Null keyword should cause method to return false");
    }

    @ParameterizedTest
    @MethodSource("knownKeywordSuccessProvider")
    void collectAnnotations_knownKeyword_success(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertTrue(result, "Known keyword with proper schema should return true");
        List<JsonNodeAnnotation> list = ctx.getAnnotations();
        assertEquals(1, list.size(), "One annotation should have been added");
        // Basic sanity – the stored annotation must have the same keyword
        assertEquals(keyword, list.get(0).getKeyword(), "Stored annotation keyword must match");
    }

    @ParameterizedTest
    @MethodSource("unknownKeywordProvider")
    void collectAnnotations_unknownKeyword_returnsFalse(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertFalse(result, "Unknown keyword should result in false");
        assertTrue(ctx.getAnnotations().isEmpty(), "No annotation should be added for unknown keyword");
    }

    @Test
    void collectAnnotations_customizerThrows_propagatesException() throws Exception {
        // spy validator to force putAnnotation to throw
        TestValidator real = new TestValidator("type", createSchemaNode("type", "string"), schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        BaseJsonValidator spy = Mockito.spy(real);
        doThrow(new RuntimeException("customizer failure")).when(spy).putAnnotation(any(ExecutionContext.class), any());
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isCollectAnnotations()).thenReturn(true);
        when(ctx.getAnnotations()).thenReturn(new ArrayList<>());
        when(ctx.getInstanceNode()).thenReturn(JsonNodeFactory.instance.objectNode());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeCollectAnnotations(spy, ctx), "Exception from customizer must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("minimumKeywordSuccessProvider")
    void collectAnnotations_minimumKeyword_success(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertTrue(result, "Keyword \"minimum\" with value should return true");
        assertEquals(1, ctx.getAnnotations().size(), "Annotation list should contain one entry");
        assertEquals(keyword, ctx.getAnnotations().get(0).getKeyword());
    }

    @ParameterizedTest
    @MethodSource("missingValueProvider")
    void collectAnnotations_missingSchemaValue_returnsFalse(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertFalse(result, "When schemaNode lacks the required property, method should return false");
        assertTrue(ctx.getAnnotations().isEmpty(), "No annotation should be added");
    }

    @ParameterizedTest
    @MethodSource("emptyKeywordProvider")
    void collectAnnotations_emptyKeyword_returnsFalse(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertFalse(result, "Empty keyword string should be treated as unknown");
        assertTrue(ctx.getAnnotations().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("preExistingAnnotationsProvider")
    void collectAnnotations_preExistingAnnotations_addsAnother(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertTrue(result, "Method should succeed even when annotations already exist");
        assertEquals(2, ctx.getAnnotations().size(), "A new annotation should be added (total 2)");
    }

    @ParameterizedTest
    @MethodSource("nullAnnotationsListProvider")
    void collectAnnotations_nullAnnotationsList_initialisesList(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertTrue(result, "Method should succeed and initialise a null annotations list");
        assertNotNull(ctx.getAnnotations(), "Annotations list must be initialised");
        assertEquals(1, ctx.getAnnotations().size());
    }

    @ParameterizedTest
    @MethodSource("suppressSubSchemaRetrievalProvider")
    void collectAnnotations_suppressSubSchemaRetrievalDoesNotAffectResult(TestValidator validator, ExecutionContext ctx) throws Exception {
        // validator already has suppressSubSchemaRetrieval = true
        boolean result = invokeCollectAnnotations(validator, ctx);
        assertTrue(result, "Annotation collection must succeed regardless of suppressSubSchemaRetrieval flag");
        assertEquals(1, ctx.getAnnotations().size());
    }

    @ParameterizedTest
    @MethodSource("nullSchemaNodeProvider")
    void collectAnnotations_nullSchemaNode_throwsNPE(JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator("type", schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        assertThrows(NullPointerException.class, () -> invokeCollectAnnotations(validator, ctx), "Null schemaNode should cause NPE");
    }

    @ParameterizedTest
    @MethodSource("nullInstanceNodeProvider")
    void collectAnnotations_nullInstanceNode_throwsNPE(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        assertThrows(NullPointerException.class, () -> invokeCollectAnnotations(validator, ctx), "Null instance node should cause NPE");
    }

    @ParameterizedTest
    @MethodSource("unmodifiableAnnotationsProvider")
    void collectAnnotations_unmodifiableAnnotations_throwsUnsupportedOperationException(String keyword, JsonNode schemaNode, ExecutionContext ctx) throws Exception {
        TestValidator validator = new TestValidator(keyword, schemaNode, schemaLocation, evaluationPath, parentSchema, validatorType, validationContext);
        assertThrows(UnsupportedOperationException.class, () -> invokeCollectAnnotations(validator, ctx), "Adding to an unmodifiable annotations list must throw");
    }
}
