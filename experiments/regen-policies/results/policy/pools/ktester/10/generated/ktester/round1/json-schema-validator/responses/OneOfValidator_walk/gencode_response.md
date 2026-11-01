```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OneOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The test class follows the framework that was provided.  Each JSON test case group
 * from the specification is represented by one or more JUnit 5 test methods.
 *
 * The tests use Mockito to mock collaborators and, when necessary, reflection to
 * manipulate the private {@code schemas} list inside {@link OneOfValidator}.
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_walk_Test {

    /*** Mocks and test fixtures ***/
    @Mock
    ValidationContext validationContext;          // used by the constructor

    @Mock
    SchemaLocation schemaLocation;               // constructor argument – not used directly

    @Mock
    JsonNodePath evaluationPath;                 // constructor argument – not used directly

    @Mock
    JsonSchema parentSchema;                     // constructor argument – can be null, mocked for safety

    JsonNode schemaNode;                         // the “oneOf” array node supplied to the constructor
    ExecutionContext executionContext;           // real instance – lightweight
    JsonNode instanceNode;                       // node being validated (mocked)
    JsonNode rootNode;                           // root of the document (mocked)
    JsonNodePath instanceLocation;               // location of the instance (mocked)

    OneOfValidator validator;                    // system under test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a simple array node that mimics a “oneOf” definition with two sub‑schemas
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        arrayNode.add(mapper.createObjectNode()); // first child schema (empty – content irrelevant)
        arrayNode.add(mapper.createObjectNode()); // second child schema
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
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         any(JsonNode.class),
                                         any(JsonSchema.class)))
                .thenReturn(mockSubSchema);

        // Build the validator under test.
        this.validator = new OneOfValidator(schemaLocation,
                                            evaluationPath,
                                            schemaNode,
                                            parentSchema,
                                            validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------
     *  Existing tests (kept unchanged)
     * -------------------------------------------------------------- */

    /*** Test when shouldValidateSchema == true – validate() must be invoked and its result returned ***/
    @Test
    void walk_ShouldValidateSchema_InvokesValidateAndReturnsMessages() {
        // Arrange – spy the validator so we can stub validate()
        OneOfValidator spyValidator = spy(this.validator);

        ValidationMessage mockMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expectedMessages = new LinkedHashSet<>();
        expectedMessages.add(mockMessage);

        // Stub the protected validate(...) method (declared in BaseJsonValidator)
        doReturn(expectedMessages).when(spyValidator)
                .validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));

        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext,
                                                          instanceNode,
                                                          rootNode,
                                                          instanceLocation,
                                                          true); // shouldValidateSchema = true

        // Assert
        assertSame(expectedMessages, actual, "walk should return the set produced by validate()");
        // Verify that validate was called exactly once with the same arguments
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));
    }

    /*** Test when shouldValidateSchema == false – each child schema’s walk() must be invoked ***/
    @Test
    void walk_ShouldNotValidateSchema_DelegatesToChildSchemas() {
        // Arrange – the validator already contains two mock child schemas (created in @BeforeEach)
        // Retrieve them via the mocked ValidationContext (the same mock is returned twice)
        JsonSchema childSchema = validationContext.newSchema(null, null, null, null);
        // Stub the child schema’s walk() to return an empty set (the return value is ignored)
        when(childSchema.walk(any(ExecutionContext.class),
                              any(JsonNode.class),
                              any(JsonNode.class),
                              any(JsonNodePath.class),
                              eq(false)))
                .thenReturn(Collections.emptySet());

        // Act
        Set<ValidationMessage> result = validator.walk(executionContext,
                                                       instanceNode,
                                                       rootNode,
                                                       instanceLocation,
                                                       false); // shouldValidateSchema = false

        // Assert – result should be empty because we never added messages
        assertTrue(result.isEmpty(), "When shouldValidateSchema is false, walk should return an empty set");

        // Verify that each child schema’s walk() was invoked exactly once
        verify(childSchema, times(2))
                .walk(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation), eq(false));
    }

    /* --------------------------------------------------------------
     *  Additional tests derived from the JSON specification
     * -------------------------------------------------------------- */

    /*** Group: walk_shouldValidateSchema_true (validator holds pre‑loaded schemas) ***/
    @Test
    void walk_ShouldValidateSchema_True_IgnoresChildSchemas() {
        // Spy the validator to ensure validate() is called and child schemas are NOT touched
        OneOfValidator spyValidator = spy(this.validator);

        Set<ValidationMessage> expected = Collections.singleton(mock(ValidationMessage.class));
        doReturn(expected).when(spyValidator)
                .validate(eq(executionContext), eq(instanceNode), eq(rootNode), eq(instanceLocation));

        Set<ValidationMessage> actual = spyValidator.walk(executionContext,
                                                          instanceNode,
                                                          rootNode,
                                                          instanceLocation,
                                                          true);

        assertSame(expected, actual, "When shouldValidateSchema is true, walk must return exactly what validate() returns");
        verify(spyValidator, times(1)).validate(any(), any(), any(), any());

        // Ensure no child schema walk() was invoked
        verifyNoMoreInteractions(validationContext);
    }

    /*** Group: walk_shouldValidateSchema_false_noSchemas – empty internal schema list ***/
    @Test
    void walk_ShouldNotValidateSchema_NoSchemas_ReturnsEmptySet() throws Exception {
        // Create a validator whose schemaNode is an empty array (no child schemas)
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode(); // size 0
        OneOfValidator emptyValidator = new OneOfValidator(schemaLocation,
                                                            evaluationPath,
                                                            emptyArray,
                                                            parentSchema,
                                                            validationContext);

        Set<ValidationMessage> result = emptyValidator.walk(executionContext,
                                                            instanceNode,
                                                            rootNode,
                                                            instanceLocation,
                                                            false);

        assertTrue(result.isEmpty(), "With no schemas and shouldValidateSchema=false, walk should return an empty set");
    }

    /* --------------------------------------------------------------
     *  Null‑argument handling when shouldValidateSchema == true
     * -------------------------------------------------------------- */

    @Test
    void walk_NullExecutionContext_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("executionContext is null"))
                .when(spy).validate(isNull(), any(), any(), any());

        assertThrows(NullPointerException.class,
                () -> spy.walk(null, instanceNode, rootNode, instanceLocation, true),
                "Null executionContext should cause NullPointerException from validate()");
    }

    @Test
    void walk_NullNode_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("node is null"))
                .when(spy).validate(any(), isNull(), any(), any());

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, null, rootNode, instanceLocation, true),
                "Null node should cause NullPointerException from validate()");
    }

    @Test
    void walk_NullRootNode_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("rootNode is null"))
                .when(spy).validate(any(), any(), isNull(), any());

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, instanceNode, null, instanceLocation, true),
                "Null rootNode should cause NullPointerException from validate()");
    }

    @Test
    void walk_NullInstanceLocation_ShouldValidateTrue_ThrowsNPE() {
        OneOfValidator spy = spy(this.validator);
        doThrow(new NullPointerException("instanceLocation is null"))
                .when(spy).validate(any(), any(), any(), isNull());

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, instanceNode, rootNode, null, true),
                "Null instanceLocation should cause NullPointerException from validate()");
    }

    /*** Group: walk_validateReturnsNull_shouldValidateTrue – validate() returns null ***/
    @Test
    void walk_ValidateReturnsNull_ShouldThrowNPE() {
        OneOfValidator spy = spy(this.validator);
        // Simulate a subclass that mistakenly returns null
        doReturn(null).when(spy).validate(any(), any(), any(), any());

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, instanceNode, rootNode, instanceLocation, true),
                "validate() returning null should cause NullPointerException when addAll(null) is executed");
    }

    /* --------------------------------------------------------------
     *  Behaviour when shouldValidateSchema == false
     * -------------------------------------------------------------- */

    /*** Group: walk_nullSchemaInList_shouldValidateFalse – internal list contains null ***/
    @Test
    void walk_NullSchemaInList_ShouldThrowNPE() throws Exception {
        // Create a validator with a normal schema list, then replace it with a list containing null
        OneOfValidator spyValidator = spy(this.validator);
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(spyValidator, Collections.singletonList(null)); // list with a single null entry

        assertThrows(NullPointerException.class,
                () -> spyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, false),
                "A null schema in the list should cause NullPointerException when its walk() is invoked");
    }

    /*** Group: walk_schemaWalkThrowsRuntimeException_shouldValidateFalse – child throws ***/
    @Test
    void walk_ChildSchemaThrowsRuntimeException_PropagatesException() throws Exception {
        // Mock a child schema that throws a RuntimeException on walk()
        JsonSchema explodingSchema = mock(JsonSchema.class);
        when(explodingSchema.walk(any(), any(), any(), any(), eq(false)))
                .thenThrow(new RuntimeException("boom"));

        // Inject the exploding schema into the validator's private list
        OneOfValidator spyValidator = spy(this.validator);
        Field schemasField = OneOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(spyValidator, Collections.singletonList(explodingSchema));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> spyValidator.walk(executionContext, instanceNode, rootNode, instanceLocation, false),
                "RuntimeException from child schema.walk() must propagate");
        assertEquals("boom", thrown.getMessage());
    }
}
```