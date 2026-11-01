
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.SpecVersion.VersionFlag;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_10_Test {

    /**
     * The schema instance under test.
     */
    private JsonSchema schema;

    /**
     * Execution context passed to {@code walk}.
     */
    private ExecutionContext executionContext;

    /**
     * Mocked validation context used inside the schema.
     */
    @Mock
    private ValidationContext mockValidationContext;

    /**
     * Mocked configuration returned from the validation context.
     */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    /**
     * Mocked listener runner handling pre‑ and post‑walk callbacks.
     */
    @Mock
    private KeywordWalkListenerRunner mockListenerRunner;

    /**
     * A single validator that will be returned from {@code getValidators()}.
     */
    @Mock
    private JsonValidator mockValidator;

    /**
     * A dummy validation message returned by the mock validator.
     */
    private ValidationMessage dummyMessage;

    /**
     * Sample JSON nodes used as parameters for the walk call.
     */
    private JsonNode node;

    private JsonNode rootNode;

    private JsonNodePath instancePath;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Create a minimal real JsonSchema instance.
        // -----------------------------------------------------------------
        // The public factory method is package‑private, but our test class lives
        // in the same package, so we can call it directly.
        ValidationContext realVc = new ValidationContext(JsonMetaSchema.getInstance(VersionFlag.V202012), JsonSchemaFactory.getInstance(VersionFlag.V202012), new SchemaValidatorsConfig());
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evalPath = new JsonNodePath(PathType.JSON_POINTER);
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        // The static `from` method builds a fully‑initialised schema.
        schema = JsonSchema.from(realVc, schemaLocation, evalPath, emptySchemaNode, null, false);
        // -----------------------------------------------------------------
        Field vcField = BaseJsonValidator.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(schema, mockValidationContext);
        // -----------------------------------------------------------------
        // 3. Stub the configuration hierarchy.
        // -----------------------------------------------------------------
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getKeywordWalkListenerRunner()).thenReturn(mockListenerRunner);
        // Pre‑walk listeners return true so the walk proceeds.
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
        // Post‑walk listener does nothing (void method).
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
        // -----------------------------------------------------------------
        // 4. Prepare a mock validator.
        // -----------------------------------------------------------------
        // The validator's evaluation path is required only for the listener
        // call; we can return a simple JsonNodePath mock.
        JsonNodePath validatorPath = mock(JsonNodePath.class);
        when(validatorPath.getName(-1)).thenReturn("dummyKeyword");
        when(mockValidator.getEvaluationPath()).thenReturn(validatorPath);
        // The validator returns a set containing a single dummy ValidationMessage.
        dummyMessage = ValidationMessage.of("dummy error");
        Set<ValidationMessage> validatorResult = new HashSet<>(Collections.singleton(dummyMessage));
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorResult);
        // -----------------------------------------------------------------
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        validatorsField.set(schema, List.of(mockValidator));
        // -----------------------------------------------------------------
        // 6. Common objects used in the test method.
        // -----------------------------------------------------------------
        executionContext = new ExecutionContext();
        node = JsonNodeFactory.instance.objectNode().put("a", "value");
        rootNode = node.deepCopy();
        instancePath = new JsonNodePath(PathType.JSON_POINTER);
        // -----------------------------------------------------------------
        ValidationContext realVc = new ValidationContext(JsonMetaSchema.getInstance(VersionFlag.V202012), JsonSchemaFactory.getInstance(VersionFlag.V202012), new SchemaValidatorsConfig());
        // -----------------------------------------------------------------
        // 2. Replace the internal ValidationContext with a mock.
        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evalPath = new JsonNodePath(PathType.JSON_POINTER);
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        schema = JsonSchema.from(realVc, schemaLocation, evalPath, emptySchemaNode, null, false);
        // -----------------------------------------------------------------
        setValidationContext(mockValidationContext);
        // -----------------------------------------------------------------
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getKeywordWalkListenerRunner()).thenReturn(mockListenerRunner);
        // Default: pre‑walk listener returns true.
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
        // -----------------------------------------------------------------
        JsonNodePath validatorPath = mock(JsonNodePath.class);
        when(validatorPath.getName(-1)).thenReturn("dummyKeyword");
        when(mockValidator.getEvaluationPath()).thenReturn(validatorPath);
        // -----------------------------------------------------------------
        setValidators(List.of(mockValidator));
        // -----------------------------------------------------------------
        executionContext = new ExecutionContext();
        // -----------------------------------------------------------------
        node = JsonNodeFactory.instance.objectNode().put("a", "value");
        // 5. Inject the mock validator list into the schema.
        rootNode = node.deepCopy();
        instancePath = new JsonNodePath(PathType.JSON_POINTER);
    }

    /**
     * Helper to replace the private {@code validators} field.
     */
    private void setValidators(List<JsonValidator> list) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        f.set(schema, list);
    }

    /**
     * Helper to replace the private {@code validationContext} field.
     */
    private void setValidationContext(ValidationContext vc) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(schema, vc);
    }

    /**
     * Verifies that {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}
     * correctly delegates to validators, respects the pre‑walk listener result,
     * aggregates validation messages and invokes the post‑walk listener.
     */
    @Test
    void walkShouldDelegateToValidatorAndInvokeListeners() {
        // Act: invoke the method under test.
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, // shouldValidateSchema = true (the flag is not used in the method body)
        true);
        // Assert: the result must contain the dummy message returned by the validator.
        assertNotNull(result, "Resulting set must not be null");
        assertEquals(1, result.size(), "Exactly one validation message should be returned");
        assertTrue(result.contains(dummyMessage), "Result set must contain the dummy message");
        // Verify that the pre‑walk listener was called once with the expected keyword.
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
        // Verify that the validator's walk method was invoked.
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
        // Verify that the post‑walk listener was called once.
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
    }

    /* --------------------------------------------------------------
     * 1. Pre‑walk listener allows validation – different validator results.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "pre‑walk true, validator result = {0}")
    @EnumSource(ValidatorResult.class)
    void walk_preWalkAllowsVariousValidatorResults(ValidatorResult resultType) throws Exception {
        // ---- arrange -------------------------------------------------
        Set<ValidationMessage> validatorSet;
        ValidationMessage dummy = ValidationMessage.of("dummy error");
        switch(resultType) {
            case NON_EMPTY:
                validatorSet = new LinkedHashSet<>(Collections.singleton(dummy));
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                break;
            case EMPTY:
                validatorSet = Collections.emptySet();
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                break;
            case NULL:
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(null);
                break;
            default:
                throw new IllegalArgumentException();
        }
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        // ---- assert -------------------------------------------------
        if (resultType == ValidatorResult.NON_EMPTY) {
            assertEquals(1, result.size(), "Non‑empty validator result should be propagated");
            assertTrue(result.contains(dummy), "Result must contain the dummy message");
        } else {
            assertTrue(result.isEmpty(), "Empty or null validator result must yield empty error set");
        }
        // ---- verify -------------------------------------------------
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), anySet());
    }

    /* --------------------------------------------------------------
     * 2. Pre‑walk listener skips validation.
     * -------------------------------------------------------------- */
    @Test
    void walk_preWalkSkipsValidator() throws Exception {
        // ---- arrange -------------------------------------------------
        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(), any(), eq(schema), any())).thenReturn(// skip
        false);
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        // ---- assert -------------------------------------------------
        assertTrue(result.isEmpty(), "When pre‑walk skips, result must be empty");
        // ---- verify -------------------------------------------------
        verify(mockValidator, never()).walk(any(), any(), any(), any(), anyBoolean());
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(Collections.emptySet()));
    }

    /* --------------------------------------------------------------
     * 3. Multiple validators with mixed outcomes.
     * -------------------------------------------------------------- */
    @Test
    void walk_multipleValidatorsMixedOutcomes() throws Exception {
        // ---- arrange -------------------------------------------------
        // Validator A – returns non‑empty set
        JsonValidator validatorA = mock(JsonValidator.class);
        JsonNodePath pathA = mock(JsonNodePath.class);
        when(pathA.getName(-1)).thenReturn("keywordA");
        when(validatorA.getEvaluationPath()).thenReturn(pathA);
        ValidationMessage msgA = ValidationMessage.of("msgA");
        when(validatorA.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(new LinkedHashSet<>(Collections.singleton(msgA)));
        when(mockListenerRunner.runPreWalkListeners(any(), eq("keywordA"), any(), any(), any(), eq(schema), eq(validatorA))).thenReturn(true);
        // Validator B – pre‑walk returns false (skip)
        JsonValidator validatorB = mock(JsonValidator.class);
        JsonNodePath pathB = mock(JsonNodePath.class);
        when(pathB.getName(-1)).thenReturn("keywordB");
        when(validatorB.getEvaluationPath()).thenReturn(pathB);
        when(mockListenerRunner.runPreWalkListeners(any(), eq("keywordB"), any(), any(), any(), eq(schema), eq(validatorB))).thenReturn(false);
        // Validator C – returns non‑empty set
        JsonValidator validatorC = mock(JsonValidator.class);
        JsonNodePath pathC = mock(JsonNodePath.class);
        when(pathC.getName(-1)).thenReturn("keywordC");
        when(validatorC.getEvaluationPath()).thenReturn(pathC);
        ValidationMessage msgC = ValidationMessage.of("msgC");
        when(validatorC.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(new LinkedHashSet<>(Collections.singleton(msgC)));
        when(mockListenerRunner.runPreWalkListeners(any(), eq("keywordC"), any(), any(), any(), eq(schema), eq(validatorC))).thenReturn(true);
        // Inject the three validators in order A, B, C
        setValidators(List.of(validatorA, validatorB, validatorC));
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        // ---- assert -------------------------------------------------
        assertEquals(2, result.size(), "Only validators A and C should contribute messages");
        assertTrue(result.contains(msgA));
        assertTrue(result.contains(msgC));
        // ---- verify -------------------------------------------------
        // A
        verify(validatorA, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
        // B – never called
        verify(validatorB, never()).walk(any(), any(), any(), any(), anyBoolean());
        // C
        verify(validatorC, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
        // post‑walk called for all three
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("keywordA"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(validatorA), anySet());
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("keywordB"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(validatorB), anySet());
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("keywordC"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(validatorC), anySet());
    }

    /* --------------------------------------------------------------
     * 4. Empty validators list.
     * -------------------------------------------------------------- */
    @Test
    void walk_emptyValidatorsList() throws Exception {
        // ---- arrange -------------------------------------------------
        setValidators(Collections.emptyList());
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        // ---- assert -------------------------------------------------
        assertTrue(result.isEmpty(), "With no validators the result must be empty");
        // ---- verify -------------------------------------------------
        verifyNoInteractions(mockListenerRunner);
        verifyNoInteractions(mockValidator);
    }

    /* --------------------------------------------------------------
     * 5. Validator throws exception – post‑walk still executed.
     * -------------------------------------------------------------- */
    @Test
    void walk_validatorThrowsException() throws Exception {
        // ---- arrange -------------------------------------------------
        RuntimeException boom = new RuntimeException("boom");
        when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenThrow(boom);
        // ---- act & assert --------------------------------------------
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
        assertSame(boom, ex, "Exception from validator must propagate");
        // ---- verify -------------------------------------------------
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
        // post‑walk must be called even though the validator threw
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), anySet());
    }

    /* --------------------------------------------------------------
     * 6. Pre‑walk listener throws exception – validator not invoked.
     * -------------------------------------------------------------- */
    @Test
    void walk_preWalkListenerThrowsException() throws Exception {
        // ---- arrange -------------------------------------------------
        RuntimeException boom = new RuntimeException("listener boom");
        when(mockListenerRunner.runPreWalkListeners(any(), anyString(), any(), any(), any(), eq(schema), any())).thenThrow(boom);
        // ---- act & assert --------------------------------------------
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
        assertSame(boom, ex, "Exception from pre‑walk listener must propagate");
        // ---- verify -------------------------------------------------
        // validator must never be called
        verify(mockValidator, never()).walk(any(), any(), any(), any(), anyBoolean());
        // post‑walk must still be called once
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), anyString(), eq(node), eq(rootNode), eq(instancePath), eq(schema), any(JsonValidator.class), anySet());
    }

    /* --------------------------------------------------------------
     * 7. Duplicate ValidationMessage handling.
     * -------------------------------------------------------------- */
    @Test
    void walk_duplicateValidationMessages() throws Exception {
        // ---- arrange -------------------------------------------------
        ValidationMessage dup = ValidationMessage.of("duplicate");
        Set<ValidationMessage> dupSet = new LinkedHashSet<>(Collections.singleton(dup));
        JsonValidator validatorA = mock(JsonValidator.class);
        JsonNodePath pathA = mock(JsonNodePath.class);
        when(pathA.getName(-1)).thenReturn("kwA");
        when(validatorA.getEvaluationPath()).thenReturn(pathA);
        when(validatorA.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(dupSet);
        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwA"), any(), any(), any(), eq(schema), eq(validatorA))).thenReturn(true);
        JsonValidator validatorB = mock(JsonValidator.class);
        JsonNodePath pathB = mock(JsonNodePath.class);
        when(pathB.getName(-1)).thenReturn("kwB");
        when(validatorB.getEvaluationPath()).thenReturn(pathB);
        when(validatorB.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(dupSet);
        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwB"), any(), any(), any(), eq(schema), eq(validatorB))).thenReturn(true);
        setValidators(List.of(validatorA, validatorB));
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        // ---- assert -------------------------------------------------
        assertEquals(1, result.size(), "LinkedHashSet must deduplicate identical messages");
        assertTrue(result.contains(dup), "Result must contain the duplicate message");
        // ---- verify -------------------------------------------------
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("kwA"), any(), any(), any(), eq(schema), eq(validatorA), anySet());
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("kwB"), any(), any(), any(), eq(schema), eq(validatorB), anySet());
    }

    /* --------------------------------------------------------------
     * 8. Order preservation of error messages.
     * -------------------------------------------------------------- */
    @Test
    void walk_preservesErrorMessageOrder() throws Exception {
        // ---- arrange -------------------------------------------------
        JsonValidator validatorA = mock(JsonValidator.class);
        JsonNodePath pathA = mock(JsonNodePath.class);
        when(pathA.getName(-1)).thenReturn("kwA");
        when(validatorA.getEvaluationPath()).thenReturn(pathA);
        ValidationMessage msgA = ValidationMessage.of("msgA");
        when(validatorA.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(new LinkedHashSet<>(Collections.singleton(msgA)));
        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwA"), any(), any(), any(), eq(schema), eq(validatorA))).thenReturn(true);
        JsonValidator validatorB = mock(JsonValidator.class);
        JsonNodePath pathB = mock(JsonNodePath.class);
        when(pathB.getName(-1)).thenReturn("kwB");
        when(validatorB.getEvaluationPath()).thenReturn(pathB);
        ValidationMessage msgB = ValidationMessage.of("msgB");
        when(validatorB.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(new LinkedHashSet<>(Collections.singleton(msgB)));
        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwB"), any(), any(), any(), eq(schema), eq(validatorB))).thenReturn(true);
        JsonValidator validatorC = mock(JsonValidator.class);
        JsonNodePath pathC = mock(JsonNodePath.class);
        when(pathC.getName(-1)).thenReturn("kwC");
        when(validatorC.getEvaluationPath()).thenReturn(pathC);
        ValidationMessage msgC = ValidationMessage.of("msgC");
        when(validatorC.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(new LinkedHashSet<>(Collections.singleton(msgC)));
        when(mockListenerRunner.runPreWalkListeners(any(), eq("kwC"), any(), any(), any(), eq(schema), eq(validatorC))).thenReturn(true);
        setValidators(List.of(validatorA, validatorB, validatorC));
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, true);
        // ---- assert -------------------------------------------------
        assertEquals(3, result.size(), "All three messages must be present");
        Iterator<ValidationMessage> it = result.iterator();
        assertEquals(msgA, it.next(), "First message should be from validator A");
        assertEquals(msgB, it.next(), "Second message should be from validator B");
        assertEquals(msgC, it.next(), "Third message should be from validator C");
    }

    /* --------------------------------------------------------------
     * 9. shouldValidateSchema flag false but validator still executed.
     * -------------------------------------------------------------- */
    @Test
    void walk_shouldValidateSchemaFlagForwarded() throws Exception {
        // ---- arrange -------------------------------------------------
        ValidationMessage dummy = ValidationMessage.of("dummy");
        when(mockValidator.walk(any(), any(), any(), any(), eq(false))).thenReturn(new LinkedHashSet<>(Collections.singleton(dummy)));
        // ---- act ----------------------------------------------------
        Set<ValidationMessage> result = schema.walk(executionContext, node, rootNode, instancePath, false);
        // ---- assert -------------------------------------------------
        assertEquals(1, result.size());
        assertTrue(result.contains(dummy));
        // ---- verify -------------------------------------------------
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(false));
    }

    /* --------------------------------------------------------------
     * 10. Null ValidationContext – expect NPE.
     * -------------------------------------------------------------- */
    @Test
    void walk_nullValidationContext_throwsNPE() throws Exception {
        // ---- arrange -------------------------------------------------
        // make internal field null
        setValidationContext(null);
        // ---- act & assert --------------------------------------------
        assertThrows(NullPointerException.class, () -> schema.walk(executionContext, node, rootNode, instancePath, true));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:4: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:50: error: cannot find symbol
    private KeywordWalkListenerRunner mockListenerRunner;
            ^
  symbol:   class KeywordWalkListenerRunner
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:197: error: cannot find symbol
    void walk_preWalkAllowsVariousValidatorResults(ValidatorResult resultType) throws Exception {
                                                   ^
  symbol:   class ValidatorResult
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:11: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:18: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:21: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:43: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:49: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:55: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:196: error: cannot find symbol
    @EnumSource(ValidatorResult.class)
                ^
  symbol:   class ValidatorResult
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:79: error: cannot find symbol
        ValidationContext realVc = new ValidationContext(JsonMetaSchema.getInstance(VersionFlag.V202012), JsonSchemaFactory.getInstance(VersionFlag.V202012), new SchemaValidatorsConfig());
                                                                       ^
  symbol:   method getInstance(VersionFlag)
  location: class JsonMetaSchema
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:92: error: cannot find symbol
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:93: error: cannot find symbol
        when(mockConfig.getKeywordWalkListenerRunner()).thenReturn(mockListenerRunner);
        ^
  symbol:   method when(WalkListenerRunner)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                    ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                 ^
  symbol:   method anyString()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                              ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                   ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                                        ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                                                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:95: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                                                                             ^
  symbol:   method any(Class<JsonValidator>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
        ^
  symbol:   method doNothing()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                  ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                               ^
  symbol:   method anyString()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                            ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                 ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                      ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                                               ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                                                           ^
  symbol:   method any(Class<JsonValidator>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:97: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                                                                                     ^
  symbol:   method anySet()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:103: error: cannot find symbol
        JsonNodePath validatorPath = mock(JsonNodePath.class);
                                     ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:104: error: cannot find symbol
        when(validatorPath.getName(-1)).thenReturn("dummyKeyword");
        ^
  symbol:   method when(String)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:105: error: cannot find symbol
        when(mockValidator.getEvaluationPath()).thenReturn(validatorPath);
        ^
  symbol:   method when(JsonNodePath)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:107: error: cannot find symbol
        dummyMessage = ValidationMessage.of("dummy error");
                                        ^
  symbol:   method of(String)
  location: class ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:109: error: cannot find symbol
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorResult);
                                ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:109: error: cannot find symbol
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorResult);
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:109: error: cannot find symbol
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorResult);
                                                                                  ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:109: error: cannot find symbol
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorResult);
                                                                                                       ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:109: error: cannot find symbol
        when(mockValidator.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), anyBoolean())).thenReturn(validatorResult);
                                                                                                                                ^
  symbol:   method anyBoolean()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:122: error: variable realVc is already defined in method setupBeforeEach()
        ValidationContext realVc = new ValidationContext(JsonMetaSchema.getInstance(VersionFlag.V202012), JsonSchemaFactory.getInstance(VersionFlag.V202012), new SchemaValidatorsConfig());
                          ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:122: error: cannot find symbol
        ValidationContext realVc = new ValidationContext(JsonMetaSchema.getInstance(VersionFlag.V202012), JsonSchemaFactory.getInstance(VersionFlag.V202012), new SchemaValidatorsConfig());
                                                                       ^
  symbol:   method getInstance(VersionFlag)
  location: class JsonMetaSchema
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:125: error: variable schemaLocation is already defined in method setupBeforeEach()
        SchemaLocation schemaLocation = new SchemaLocation(null);
                       ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:126: error: variable evalPath is already defined in method setupBeforeEach()
        JsonNodePath evalPath = new JsonNodePath(PathType.JSON_POINTER);
                     ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:127: error: variable emptySchemaNode is already defined in method setupBeforeEach()
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
                   ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:132: error: cannot find symbol
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:133: error: cannot find symbol
        when(mockConfig.getKeywordWalkListenerRunner()).thenReturn(mockListenerRunner);
        ^
  symbol:   method when(WalkListenerRunner)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                    ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                 ^
  symbol:   method anyString()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                              ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                   ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                                        ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                                                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:135: error: cannot find symbol
        when(mockListenerRunner.runPreWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class))).thenReturn(true);
                                                                                                                                                                             ^
  symbol:   method any(Class<JsonValidator>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
        ^
  symbol:   method doNothing()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                  ^
  symbol:   method any(Class<ExecutionContext>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                               ^
  symbol:   method anyString()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                            ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                 ^
  symbol:   method any(Class<JsonNode>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                      ^
  symbol:   method any(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                                               ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                                                           ^
  symbol:   method any(Class<JsonValidator>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:136: error: cannot find symbol
        doNothing().when(mockListenerRunner).runPostWalkListeners(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(schema), any(JsonValidator.class), anySet());
                                                                                                                                                                                                                     ^
  symbol:   method anySet()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:138: error: variable validatorPath is already defined in method setupBeforeEach()
        JsonNodePath validatorPath = mock(JsonNodePath.class);
                     ^
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:138: error: cannot find symbol
        JsonNodePath validatorPath = mock(JsonNodePath.class);
                                     ^
  symbol:   method mock(Class<JsonNodePath>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:139: error: cannot find symbol
        when(validatorPath.getName(-1)).thenReturn("dummyKeyword");
        ^
  symbol:   method when(String)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:140: error: cannot find symbol
        when(mockValidator.getEvaluationPath()).thenReturn(validatorPath);
        ^
  symbol:   method when(JsonNodePath)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                   ^
  symbol:   method times(int)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                 ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                                       ^
  symbol:   method eq(String)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                                                           ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                                                                     ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                                                                                   ^
  symbol:   method eq(JsonNodePath)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                                                                                                     ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:185: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPreWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator));
                                                                                                                                                                 ^
  symbol:   method eq(JsonValidator)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:187: error: cannot find symbol
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
                              ^
  symbol:   method times(int)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:187: error: cannot find symbol
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
                                             ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:187: error: cannot find symbol
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
                                                                   ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:187: error: cannot find symbol
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
                                                                             ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:187: error: cannot find symbol
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
                                                                                           ^
  symbol:   method eq(JsonNodePath)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:187: error: cannot find symbol
        verify(mockValidator, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instancePath), eq(true));
                                                                                                             ^
  symbol:   method eq(boolean)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                   ^
  symbol:   method times(int)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                  ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                        ^
  symbol:   method eq(String)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                                            ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                                                      ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                                                                    ^
  symbol:   method eq(JsonNodePath)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                                                                                      ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                                                                                                  ^
  symbol:   method eq(JsonValidator)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:189: error: cannot find symbol
        verify(mockListenerRunner, times(1)).runPostWalkListeners(eq(executionContext), eq("dummyKeyword"), eq(node), eq(rootNode), eq(instancePath), eq(schema), eq(mockValidator), eq(result));
                                                                                                                                                                                     ^
  symbol:   method eq(Set<ValidationMessage>)
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:200: error: cannot find symbol
        ValidationMessage dummy = ValidationMessage.of("dummy error");
                                                   ^
  symbol:   method of(String)
  location: class ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:202: error: cannot find symbol
            case NON_EMPTY:
                 ^
  symbol:   variable NON_EMPTY
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                                        ^
  symbol:   method any()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                                               ^
  symbol:   method any()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                                                      ^
  symbol:   method any()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                                                             ^
  symbol:   method any()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:204: error: cannot find symbol
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                                                                    ^
  symbol:   method anyBoolean()
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:206: error: cannot find symbol
            case EMPTY:
                 ^
  symbol:   variable EMPTY
  location: class JsonSchema_walk_10_Test
src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java:208: error: cannot find symbol
                when(mockValidator.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(validatorSet);
                                        ^
  symbol:   method any()
  location: class JsonSchema_walk_10_Test
Note: src/test/java/com/networknt/schema/JsonSchema_walk_10_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
100 errors
only showing the first 100 errors, of 413 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, String)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;\n\t\tmethod `public ExecutionContext createExecutionContext()` returns `com.networknt.schema.ExecutionContext`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`, api document: \n     * Creates an execution context.\n     * \n     * @return the execution context\n     ;\n\t\tmethod `private T walkAtNodeInternal(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean, OutputFormat<T>, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;\nclass `com.networknt.schema.MultipleOfValidator`:\n\trelated functions:\n\t\tmethod `protected BigDecimal getDividend(JsonNode)` returns `java.math.BigDecimal`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`, api document: \n     * Gets the dividend to use.\n     * \n     * @param node the node\n     * @return the dividend or null if the type is incorrect\n     ;\nclass `com.networknt.schema.PrefixItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


