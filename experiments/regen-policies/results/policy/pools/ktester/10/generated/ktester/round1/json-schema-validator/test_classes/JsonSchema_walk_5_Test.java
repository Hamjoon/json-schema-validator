package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.walk.WalkListenerRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_5_Test {

    /**
     * Mocks and test fixtures **
     */
    @Mock
    ExecutionContext mockExecutionContext;

    @Mock
    CollectorContext mockCollectorContext;

    @Mock
    JsonNode mockNode;

    @Mock
    JsonNode mockRootNode;

    @Mock
    JsonNodePath mockInstanceLocation;

    @Mock
    JsonValidator mockValidator;

    @Mock
    WalkListenerRunner mockWalkListenerRunner;

    @Mock
    JsonSchema // can be null as well
    parentSchema;

    JsonSchema schemaUnderTest;

    ValidationContext validationContext;

    SchemaLocation schemaLocation;

    JsonNodePath evaluationPath;

    // a simple empty schema node
    ObjectNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        // ----- Build minimal required objects -----
        // a very small (empty) schema node – sufficient for construction
        schemaNode = JsonMapperFactory.getInstance().createObjectNode();
        // ValidationContext – uses a default meta‑schema and factory; config can be default
        // any supported version
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        validationContext = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig());
        // SchemaLocation – we can use a dummy absolute IRI (null is acceptable for tests)
        schemaLocation = new SchemaLocation(null);
        // Evaluation path – root path (type does not matter for the test)
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // ----- Create the JsonSchema instance -----
        // Use the private constructor via reflection because it is not public.
        // The class is in the same package as the test, so we can invoke it directly.
        schemaUnderTest = new JsonSchema(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        // ----- Inject mocks -----
        // The field `keywordWalkListenerRunner` has package‑private visibility,
        // therefore we can set it directly.
        schemaUnderTest.keywordWalkListenerRunner = mockWalkListenerRunner;
        // Stub the ExecutionContext to return a CollectorContext
        when(mockExecutionContext.getCollectorContext()).thenReturn(mockCollectorContext);
        // Stub the WalkListenerRunner to allow the walk to proceed
        when(mockWalkListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), any(JsonNodePath.class), any(JsonNodePath.class), any(JsonSchema.class), any(ValidationContext.class), any(JsonSchemaFactory.class))).thenReturn(true);
        // post‑walk listener does not affect the result
        doNothing().when(mockWalkListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), any(JsonNodePath.class), any(JsonNodePath.class), any(JsonSchema.class), any(ValidationContext.class), any(JsonSchemaFactory.class), anySet());
        // ----- Prepare a mock validator -----
        // The validator must provide an evaluation path and a walk implementation.
        JsonNodePath validatorEvalPath = new JsonNodePath(PathType.JSON_POINTER);
        when(mockValidator.getEvaluationPath()).thenReturn(validatorEvalPath);
        // For simplicity we let the validator return a single ValidationMessage.
        ValidationMessage sampleMessage = ValidationMessage.of("sample", "sample message");
        Set<ValidationMessage> validatorErrors = new LinkedHashSet<>(Collections.singleton(sampleMessage));
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorErrors);
        // Stub JsonSchema#getValidators() to return a list containing our mock validator.
        // Because getValidators() is a concrete method that lazily loads validators,
        // we replace the internal list via a spy.
        JsonSchema spySchema = spy(schemaUnderTest);
        doReturn(Collections.singletonList(mockValidator)).when(spySchema).getValidators();
        schemaUnderTest = spySchema;
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the test clean.
        schemaUnderTest = null;
        mockExecutionContext = null;
        mockCollectorContext = null;
        mockNode = null;
        mockRootNode = null;
        mockInstanceLocation = null;
        mockValidator = null;
        mockWalkListenerRunner = null;
        parentSchema = null;
        validationContext = null;
        schemaLocation = null;
        evaluationPath = null;
        schemaNode = null;
    }

    @Test
    void walk_ShouldReturnValidatorErrors_WhenPreWalkListenerAllowsExecution() {
        // GIVEN – validator already set up in @BeforeEach to return one message
        ValidationMessage sampleMessage = ValidationMessage.of("sample", "sample message");
        Set<ValidationMessage> validatorErrors = new LinkedHashSet<>(Collections.singleton(sampleMessage));
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorErrors);
        // WHEN
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("sample", msg.getCode());
        assertEquals("sample message", msg.getMessage());
        verify(mockWalkListenerRunner, times(1)).runPreWalkListeners(eq(mockExecutionContext), anyString(), eq(mockNode), eq(mockRootNode), eq(mockInstanceLocation), any(), any(), eq(schemaUnderTest.schemaNode), eq(schemaUnderTest.parentSchema), eq(schemaUnderTest.validationContext), eq(schemaUnderTest.validationContext.getJsonSchemaFactory()));
        verify(mockWalkListenerRunner, times(1)).runPostWalkListeners(eq(mockExecutionContext), anyString(), eq(mockNode), eq(mockRootNode), eq(mockInstanceLocation), any(), any(), eq(schemaUnderTest.schemaNode), eq(schemaUnderTest.parentSchema), eq(schemaUnderTest.validationContext), eq(schemaUnderTest.validationContext.getJsonSchemaFactory()), eq(result));
    }

    @Test
    void walk_ShouldSkipValidator_WhenPreWalkListenerReturnsFalse() {
        // GIVEN – make the pre‑walk listener deny execution
        when(mockWalkListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), any(JsonNodePath.class), any(JsonNodePath.class), any(JsonSchema.class), any(ValidationContext.class), any(JsonSchemaFactory.class))).thenReturn(false);
        // WHEN
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        // THEN – no validator should have been invoked and the result set is empty
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when pre‑walk listener skips execution");
        verify(mockValidator, never()).walk(any(), any(), any(), any(), anyBoolean());
        // Post‑walk listener is still expected to be called
        verify(mockWalkListenerRunner, times(1)).runPostWalkListeners(eq(mockExecutionContext), anyString(), eq(mockNode), eq(mockRootNode), eq(mockInstanceLocation), any(JsonNodePath.class), any(JsonNodePath.class), eq(schemaUnderTest.schemaNode), eq(schemaUnderTest.parentSchema), eq(schemaUnderTest.validationContext), eq(schemaUnderTest.validationContext.getJsonSchemaFactory()), eq(result));
    }

    /**
     * 3. Pre‑walk listener proceeds but validator returns {@code null} **
     */
    @Test
    void walk_ShouldMergeScopes_WhenValidatorReturnsNull() {
        // make validator return null
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(null);
        // mock scopes
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // parent scope should be merged with child scope
        verify(parentScope, times(1)).mergeWith(childScope);
        // no errors added, so evaluated items/properties are untouched
        verify(mockCollectorContext, never()).getEvaluatedItems();
        verify(mockCollectorContext, never()).getEvaluatedProperties();
    }

    /**
     * 4. Validator returns empty set – scopes merged, no errors **
     */
    @Test
    void walk_ShouldMergeScopes_WhenValidatorReturnsEmptySet() {
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertTrue(result.isEmpty());
        verify(parentScope, times(1)).mergeWith(childScope);
    }

    /**
     * 5. Validator returns errors – errors added, no scope merge **
     */
    @Test
    void walk_ShouldAddErrorsAndSkipScopeMerge_WhenValidatorReturnsErrors() {
        ValidationMessage vm = ValidationMessage.of("err", "error");
        Set<ValidationMessage> errors = new LinkedHashSet<>(Collections.singleton(vm));
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(errors);
        // scopes – should NOT be merged
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertEquals(1, result.size());
        assertTrue(result.contains(vm));
        verify(parentScope, never()).mergeWith(any());
    }

    /**
     * 6. Items‑type validator with errors – evaluated items are copied **
     */
    @Test
    void walk_ShouldCopyEvaluatedItems_WhenItemsValidatorReturnsErrors() {
        // Use a mock of ItemsValidator (instanceof check passes)
        ItemsValidator itemsValidator = mock(ItemsValidator.class);
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER);
        when(itemsValidator.getEvaluationPath()).thenReturn(path);
        ValidationMessage vm = ValidationMessage.of("itm", "item error");
        Set<ValidationMessage> errors = Collections.singleton(vm);
        when(itemsValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(errors);
        // replace validator list
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Collections.singletonList(itemsValidator)).when(spy).getValidators();
        schemaUnderTest = spy;
        // mock scopes and evaluated items
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<String> childItems = new HashSet<>(Arrays.asList("item1", "item2"));
        when(childScope.getEvaluatedItems()).thenReturn(childItems);
        Set<String> collectorItems = new HashSet<>();
        when(mockCollectorContext.getEvaluatedItems()).thenReturn(collectorItems);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertEquals(1, result.size());
        // evaluated items from child scope must be added to collector context
        assertTrue(collectorItems.containsAll(childItems));
        verify(mockCollectorContext, times(1)).getEvaluatedItems();
    }

    /**
     * 7. Properties‑type validator with errors – evaluated properties are copied **
     */
    @Test
    void walk_ShouldCopyEvaluatedProperties_WhenPropertiesValidatorReturnsErrors() {
        PropertiesValidator propValidator = mock(PropertiesValidator.class);
        JsonNodePath path = new JsonNodePath(PathType.JSON_POINTER);
        when(propValidator.getEvaluationPath()).thenReturn(path);
        ValidationMessage vm = ValidationMessage.of("prop", "property error");
        Set<ValidationMessage> errors = Collections.singleton(vm);
        when(propValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(errors);
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Collections.singletonList(propValidator)).when(spy).getValidators();
        schemaUnderTest = spy;
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<String> childProps = new HashSet<>(Arrays.asList("a", "b"));
        when(childScope.getEvaluatedProperties()).thenReturn(childProps);
        Set<String> collectorProps = new HashSet<>();
        when(mockCollectorContext.getEvaluatedProperties()).thenReturn(collectorProps);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertEquals(1, result.size());
        assertTrue(collectorProps.containsAll(childProps));
        verify(mockCollectorContext, times(1)).getEvaluatedProperties();
    }

    /**
     * 8. Validator throws RuntimeException – post‑walk still executed, exception propagates **
     */
    @Test
    void walk_ShouldPropagateException_FromValidatorAfterPostWalk() {
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        RuntimeException ex = new RuntimeException("validator failure");
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenThrow(ex);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
        assertSame(ex, thrown);
        // post‑walk listener must still have been called
        verify(mockWalkListenerRunner, times(1)).runPostWalkListeners(eq(mockExecutionContext), anyString(), eq(mockNode), eq(mockRootNode), eq(mockInstanceLocation), any(), any(), eq(schemaUnderTest.schemaNode), eq(schemaUnderTest.parentSchema), eq(schemaUnderTest.validationContext), eq(schemaUnderTest.validationContext.getJsonSchemaFactory()), anySet());
    }

    /**
     * 9. Null keywordWalkListenerRunner – NPE before any validator work **
     */
    @Test
    void walk_ShouldThrowNPE_WhenKeywordWalkListenerRunnerIsNull() {
        schemaUnderTest.keywordWalkListenerRunner = null;
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 10. Multiple validators – first empty, second with errors **
     */
    @Test
    void walk_ShouldAggregateErrorsFromSecondValidator_AndMergeFirstScope() {
        // first validator returns empty set
        JsonValidator v1 = mock(JsonValidator.class);
        when(v1.getEvaluationPath()).thenReturn(new JsonNodePath(PathType.JSON_POINTER));
        when(v1.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        // second validator returns an error
        JsonValidator v2 = mock(JsonValidator.class);
        when(v2.getEvaluationPath()).thenReturn(new JsonNodePath(PathType.JSON_POINTER));
        ValidationMessage vm = ValidationMessage.of("second", "second error");
        Set<ValidationMessage> secondErrors = Collections.singleton(vm);
        when(v2.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(secondErrors);
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Arrays.asList(v1, v2)).when(spy).getValidators();
        schemaUnderTest = spy;
        // scopes for first validator
        CollectorContext.Scope parent1 = mock(CollectorContext.Scope.class);
        CollectorContext.Scope child1 = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parent1);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(child1);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        // only second validator's message should be present
        assertEquals(1, result.size());
        assertTrue(result.contains(vm));
        // first validator's empty result triggers scope merge
        verify(parent1, times(1)).mergeWith(child1);
        // second validator's non‑empty result should NOT merge scopes
        // already verified above; second merge not called
        verify(parent1, never()).mergeWith(any());
    }

    /**
     * 11. Empty validators list – no listeners invoked **
     */
    @Test
    void walk_ShouldReturnEmptySet_WhenNoValidators() {
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        schemaUnderTest = spy;
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        // pre‑ and post‑walk listeners should never be called because loop body never runs
        verify(mockWalkListenerRunner, never()).runPreWalkListeners(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(mockWalkListenerRunner, never()).runPostWalkListeners(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    /**
     * 12. Null executionContext – immediate NPE **
     */
    @Test
    void walk_ShouldThrowNPE_WhenExecutionContextIsNull() {
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(null, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 13. ExecutionContext with null CollectorContext – NPE on enterDynamicScope **
     */
    @Test
    void walk_ShouldThrowNPE_WhenCollectorContextIsNull() {
        when(mockExecutionContext.getCollectorContext()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 14. getValidators returns null – NPE during enhanced for‑loop **
     */
    @Test
    void walk_ShouldThrowNPE_WhenValidatorsListIsNull() {
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(null).when(spy).getValidators();
        schemaUnderTest = spy;
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 15. Validators list contains a null element – NPE on getEvaluationPath **
     */
    @Test
    void walk_ShouldThrowNPE_WhenValidatorInListIsNull() {
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Arrays.asList((JsonValidator) null)).when(spy).getValidators();
        schemaUnderTest = spy;
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 16. Validator returns null evaluation path – NPE when getName(-1) is called **
     */
    @Test
    void walk_ShouldThrowNPE_WhenValidatorEvaluationPathIsNull() {
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.getEvaluationPath()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 17. Empty evaluation path – IndexOutOfBoundsException on getName(-1) **
     */
    @Test
    void walk_ShouldThrowIndexOutOfBounds_WhenEvaluationPathIsEmpty() {
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        // no segments
        JsonNodePath emptyPath = new JsonNodePath(PathType.JSON_POINTER);
        when(validator.getEvaluationPath()).thenReturn(emptyPath);
        assertThrows(IndexOutOfBoundsException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 18. schemaNode is null – NPE inside post‑walk listener **
     */
    @Test
    void walk_ShouldThrowNPE_WhenSchemaNodeIsNullDuringPostWalk() {
        // set schemaNode to null
        schemaUnderTest.schemaNode = null;
        // pre‑walk returns true, validator returns empty set (so no errors)
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        // post‑walk listener will be invoked with null schemaNode and should NPE
        doThrow(new NullPointerException("schemaNode null")).when(mockWalkListenerRunner).runPostWalkListeners(any(), anyString(), any(), any(), any(), any(), any(), eq(null), any(), any(), any(), anySet());
        assertThrows(NullPointerException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
    }

    /**
     * 19. Post‑walk listener throws RuntimeException – exception propagates **
     */
    @Test
    void walk_ShouldPropagateException_FromPostWalkListener() {
        // make pre‑walk succeed and validator return empty set
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        RuntimeException ex = new RuntimeException("post‑walk failure");
        doThrow(ex).when(mockWalkListenerRunner).runPostWalkListeners(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anySet());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
        assertSame(ex, thrown);
    }

    /**
     * 20. Pre‑walk listener throws RuntimeException – post‑walk still executed **
     */
    @Test
    void walk_ShouldInvokePostWalkAndPropagateException_WhenPreWalkListenerThrows() {
        RuntimeException ex = new RuntimeException("pre‑walk failure");
        when(mockWalkListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(ex);
        // post‑walk should still be called (the finally block runs)
        doNothing().when(mockWalkListenerRunner).runPostWalkListeners(any(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anySet());
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true));
        assertSame(ex, thrown);
        verify(mockWalkListenerRunner, times(1)).runPostWalkListeners(eq(mockExecutionContext), anyString(), eq(mockNode), eq(mockRootNode), eq(mockInstanceLocation), any(), any(), eq(schemaUnderTest.schemaNode), eq(schemaUnderTest.parentSchema), eq(schemaUnderTest.validationContext), eq(schemaUnderTest.validationContext.getJsonSchemaFactory()), anySet());
    }

    /**
     * 21. Pre‑walk listener proceeds but validator returns null – ensure scope merge **
     */
    @Test
    void walk_PreWalkProceed_ValidatorReturnsNull_MergesScopes() {
        // pre‑walk already returns true by default
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(null);
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, false);
        assertTrue(result.isEmpty());
        verify(parentScope, times(1)).mergeWith(childScope);
    }

    /**
     * 22. Pre‑walk listener proceeds, validator returns empty set – merge scopes **
     */
    @Test
    void walk_PreWalkProceed_ValidatorReturnsEmpty_MergesScopes() {
        JsonValidator validator = schemaUnderTest.getValidators().get(0);
        when(validator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, false);
        assertTrue(result.isEmpty());
        verify(parentScope, times(1)).mergeWith(childScope);
    }

    /**
     * 23. Items validator returns empty set – evaluated items NOT added **
     */
    @Test
    void walk_ItemsValidatorEmptyResult_DoesNotAddEvaluatedItems() {
        ItemsValidator itemsValidator = mock(ItemsValidator.class);
        when(itemsValidator.getEvaluationPath()).thenReturn(new JsonNodePath(PathType.JSON_POINTER));
        when(itemsValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Collections.singletonList(itemsValidator)).when(spy).getValidators();
        schemaUnderTest = spy;
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        // child scope returns some items – they should NOT be copied because result is empty
        Set<String> childItems = new HashSet<>(Arrays.asList("x"));
        when(childScope.getEvaluatedItems()).thenReturn(childItems);
        Set<String> collectorItems = new HashSet<>();
        when(mockCollectorContext.getEvaluatedItems()).thenReturn(collectorItems);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, true);
        assertTrue(result.isEmpty());
        assertTrue(collectorItems.isEmpty(), "Evaluated items should remain unchanged when validator returns empty");
        verify(parentScope, times(1)).mergeWith(childScope);
    }

    /**
     * 24. Properties validator returns empty set – evaluated properties NOT added **
     */
    @Test
    void walk_PropertiesValidatorEmptyResult_DoesNotAddEvaluatedProperties() {
        PropertiesValidator propValidator = mock(PropertiesValidator.class);
        when(propValidator.getEvaluationPath()).thenReturn(new JsonNodePath(PathType.JSON_POINTER));
        when(propValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        JsonSchema spy = spy(schemaUnderTest);
        doReturn(Collections.singletonList(propValidator)).when(spy).getValidators();
        schemaUnderTest = spy;
        CollectorContext.Scope parentScope = mock(CollectorContext.Scope.class);
        CollectorContext.Scope childScope = mock(CollectorContext.Scope.class);
        when(mockCollectorContext.enterDynamicScope(schemaUnderTest)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        Set<String> childProps = new HashSet<>(Arrays.asList("y"));
        when(childScope.getEvaluatedProperties()).thenReturn(childProps);
        Set<String> collectorProps = new HashSet<>();
        when(mockCollectorContext.getEvaluatedProperties()).thenReturn(collectorProps);
        Set<ValidationMessage> result = schemaUnderTest.walk(mockExecutionContext, mockNode, mockRootNode, mockInstanceLocation, false);
        assertTrue(result.isEmpty());
        assertTrue(collectorProps.isEmpty(), "Evaluated properties should remain unchanged when validator returns empty");
        verify(parentScope, times(1)).mergeWith(childScope);
    }
}
