package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /**
     * Mocks and test fixtures **
     */
    @Mock
    ValidationContext // used by the constructor
    validationContext;

    @Mock
    SchemaLocation // constructor argument – not used directly
    schemaLocation;

    @Mock
    JsonNodePath // constructor argument – not used directly
    evaluationPath;

    @Mock
    JsonSchema // constructor argument – can be null, mocked for safety
    parentSchema;

    // the “oneOf” array node supplied to the constructor
    JsonNode schemaNode;

    // real instance – lightweight
    ExecutionContext executionContext;

    // node being validated (mocked)
    JsonNode instanceNode;

    // root of the document (mocked)
    JsonNode rootNode;

    // location of the instance (mocked)
    JsonNodePath instanceLocation;

    // system under test
    OneOfValidator validator;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a simple array node that mimics a “oneOf” definition with two sub‑schemas
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        // first child schema (empty – content irrelevant)
        arrayNode.add(mapper.createObjectNode());
        // second child schema
        arrayNode.add(mapper.createObjectNode());
        this.schemaNode = arrayNode;
        // ExecutionContext can be a real object – it has no side effects for this test
        this.executionContext = new ExecutionContext();
        // Mock the JsonNode arguments used by walk()
        this.instanceNode = mock(JsonNode.class);
        this.rootNode = mock(JsonNode.class);
        this.instanceLocation = mock(JsonNodePath.class);
        // Stub ValidationContext.newSchema(...) so that the constructor can fill the internal
        // “schemas” list with mock JsonSchema objects.
        JsonSchema mockSubSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSubSchema);
        // The constructor will call newSchema twice (array size = 2) – the same mock is fine.
        // Build the validator under test.
        this.validator = new OneOfValidator(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
    }

    /**
     * Test when shouldValidateSchema == true – validate() must be invoked and its result returned **
     */
    @Test
    void walk_ShouldValidateSchema_InvokesValidateAndReturnsMessages() {
        // Arrange – spy the validator so we can stub validate()
        OneOfValidator spyValidator = spy(this.validator);
        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expectedMessages = new LinkedHashSet<>();
        expectedMessages.add(mockMessage);
        // Stub the protected validate(...) method (declared in BaseJsonValidator)
        doReturn(expectedMessages).when(spyValidator).validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));
        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, // shouldValidateSchema = true
        true);
        // Assert
        assertSame(expectedMessages, actual, "walk should return the set produced by validate()");
        // Verify that validate was called exactly once with the same arguments
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));
    }

    /**
     * Test when shouldValidateSchema == false – each child schema’s walk() must be invoked **
     */
    @Test
    void walk_ShouldNotValidateSchema_DelegatesToChildSchemas() {
        // Arrange – the validator already contains two mock child schemas (created in @BeforeEach)
        // Retrieve them via reflection (allowed because they are package‑private in the test)
        // but we avoid accessing private fields; instead we rely on the behaviour of the
        // mocked ValidationContext which always returns the same mock JsonSchema instance.
        JsonSchema childSchema = validationContext.newSchema(null, null, null, null);
        // Stub the child schema’s walk() to return an empty set (the return value is ignored)
        when(childSchema.walk(any(ExecutionContext.class), any(JsonNode.class), any(JsonNode.class), any(JsonNodePath.class), eq(false))).thenReturn(Collections.emptySet());
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, instanceLocation, // shouldValidateSchema = false
        false);
        // Assert – result should be empty because we never added messages
        assertTrue(result.isEmpty(), "When shouldValidateSchema is false, walk should return an empty set");
        // Verify that each child schema’s walk() was invoked exactly once
        verify(childSchema, times(2)).walk(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation), eq(false));
    }

    /**
     * Group: walk_shouldValidateSchema_true (validator holds pre‑loaded schemas) **
     */
    @Test
    void walk_ShouldValidateSchema_True_IgnoresChildSchemas() {
        // Spy the validator to ensure validate() is called and child schemas are NOT touched
        OneOfValidator spyValidator = spy(this.validator);
        Set<ValidationMessage> expected = Collections.singleton(mock(ValidationMessage.class));
        doReturn(expected).when(spyValidator).validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, true);
        assertSame(expected, actual, "When shouldValidateSchema is true, walk must return exactly what validate() returns");
        verify(spyValidator, times(1)).validate(any(), any(), any(), any());
        // Ensure no child schema walk() was invoked
        verifyNoMoreInteractions(validationContext);
    }

    /**
     * Group: walk_shouldValidateSchema_false_noSchemas – empty internal schema list **
     */
    @Test
    void walk_ShouldNotValidateSchema_NoSchemas_ReturnsEmptySet() throws Exception {
        // Create a validator whose schemaNode is an empty array (no child schemas)
        ObjectMapper mapper = new ObjectMapper();
        // size 0
        ArrayNode emptyArray = mapper.createArrayNode();
        OneOfValidator emptyValidator = new OneOfValidator(schemaLocation, evaluationPath, emptyArray, parentSchema, validationContext);
        Set<ValidationMessage> result = emptyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, false);
        assertTrue(result.isEmpty(), "With no schemas and shouldValidateSchema=false, walk should return an empty set");
    }

    @Test
    void walk_NullExecutionContext_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("executionContext is null")).when(spy).validate(isNull(), any(), any(), any());
        assertThrows(NullPointerException.class, () -> spy.walk(null, instanceNode, rootNode, instanceLocation, true), "Null executionContext should cause NullPointerException from validate()");
    }

    @Test
    void walk_NullNode_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("node is null")).when(spy).validate(any(), isNull(), any(), any());
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, null, rootNode, instanceLocation, true), "Null node should cause NullPointerException from validate()");
    }

    @Test
    void walk_NullRootNode_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("rootNode is null")).when(spy).validate(any(), any(), isNull(), any());
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, instanceNode, null, instanceLocation, true), "Null rootNode should cause NullPointerException from validate()");
    }

    @Test
    void walk_NullInstanceLocation_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("instanceLocation is null")).when(spy).validate(any(), any(), any(), isNull());
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, instanceNode, rootNode, null, true), "Null instanceLocation should cause NullPointerException from validate()");
    }

    /**
     * Group: walk_validateReturnsNull_shouldValidateTrue – validate() returns null **
     */
    @Test
    void walk_ValidateReturnsNull_ShouldThrowNPE() {
        OneOfValidator spy = spy(this.validator);
        // Simulate a subclass that mistakenly returns null
        doReturn(null).when(spy).validate(any(), any(), any(), any());
        assertThrows(NullPointerException.class, () -> spy.walk(executionContext, instanceNode, rootNode, instanceLocation, true), "validate() returning null should cause NullPointerException when addAll(null) is executed");
    }

    /**
     * Group: walk_nullSchemaInList_shouldValidateFalse – internal list contains null **
     */
    @Test
    void walk_NullSchemaInList_ShouldThrowNPE() throws Exception {
        // Create a validator with a normal schema list, then replace it with a list containing null
        OneOfValidator spyValidator = spy(this.validator);
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        // list with a single null entry
        schemasField.set(spyValidator, Collections.singletonList(null));
        assertThrows(NullPointerException.class, () -> spyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, false), "A null schema in the list should cause NullPointerException when its walk() is invoked");
    }

    /**
     * Group: walk_schemaWalkThrowsRuntimeException_shouldValidateFalse – child throws **
     */
    @Test
    void walk_ChildSchemaThrowsRuntimeException_PropagatesException() throws Exception {
        // Mock a child schema that throws a RuntimeException on walk()
        JsonSchema explodingSchema = mock(JsonSchema.class);
        when(explodingSchema.walk(any(), any(), any(), any(), eq(false))).thenThrow(new RuntimeException("boom"));
        // Inject the exploding schema into the validator's private list
        OneOfValidator spyValidator = spy(this.validator);
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(spyValidator, Collections.singletonList(explodingSchema));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> spyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, false), "RuntimeException from child schema.walk() must propagate");
        assertEquals("boom", thrown.getMessage());
    }
}
