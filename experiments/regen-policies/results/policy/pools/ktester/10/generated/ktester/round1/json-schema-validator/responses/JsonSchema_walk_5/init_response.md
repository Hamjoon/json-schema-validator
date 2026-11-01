```java
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

    /*** Mocks and test fixtures ***/
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
    JsonSchema parentSchema;   // can be null as well

    JsonSchema schemaUnderTest;
    ValidationContext validationContext;
    SchemaLocation schemaLocation;
    JsonNodePath evaluationPath;
    ObjectNode schemaNode; // a simple empty schema node

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- Build minimal required objects -----
        // a very small (empty) schema node – sufficient for construction
        schemaNode = JsonMapperFactory.getInstance().createObjectNode();

        // ValidationContext – uses a default meta‑schema and factory; config can be default
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7(); // any supported version
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        validationContext = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig());

        // SchemaLocation – we can use a dummy absolute IRI (null is acceptable for tests)
        schemaLocation = new SchemaLocation(null);

        // Evaluation path – root path (type does not matter for the test)
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // ----- Create the JsonSchema instance -----
        // Use the private constructor via reflection because it is not public.
        // The class is in the same package as the test, so we can invoke it directly.
        schemaUnderTest = new JsonSchema(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);

        // ----- Inject mocks -----
        // The field `keywordWalkListenerRunner` has package‑private visibility,
        // therefore we can set it directly.
        schemaUnderTest.keywordWalkListenerRunner = mockWalkListenerRunner;

        // Stub the ExecutionContext to return a CollectorContext
        when(mockExecutionContext.getCollectorContext()).thenReturn(mockCollectorContext);

        // Stub the WalkListenerRunner to allow the walk to proceed
        when(mockWalkListenerRunner.runPreWalkListeners(
                any(ExecutionContext.class),
                anyString(),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                any(JsonSchema.class),
                any(ValidationContext.class),
                any(JsonSchemaFactory.class)))
                .thenReturn(true);
        // post‑walk listener does not affect the result
        doNothing().when(mockWalkListenerRunner).runPostWalkListeners(
                any(ExecutionContext.class),
                anyString(),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                any(JsonSchema.class),
                any(ValidationContext.class),
                any(JsonSchemaFactory.class),
                anySet());

        // ----- Prepare a mock validator -----
        // The validator must provide an evaluation path and a walk implementation.
        JsonNodePath validatorEvalPath = new JsonNodePath(PathType.JSON_POINTER);
        when(mockValidator.getEvaluationPath()).thenReturn(validatorEvalPath);
        // For simplicity we let the validator return a single ValidationMessage.
        ValidationMessage sampleMessage = ValidationMessage.of("sample", "sample message");
        Set<ValidationMessage> validatorErrors = new LinkedHashSet<>(Collections.singleton(sampleMessage));
        when(mockValidator.walk(
                any(ExecutionContext.class),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                anyBoolean()))
                .thenReturn(validatorErrors);

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

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /*** Actual test cases ***/

    @Test
    void walk_ShouldReturnValidatorErrors_WhenPreWalkListenerAllowsExecution() {
        // GIVEN – all mocks are prepared in @BeforeEach

        // WHEN
        Set<ValidationMessage> result = schemaUnderTest.walk(
                mockExecutionContext,
                mockNode,
                mockRootNode,
                mockInstanceLocation,
                true); // shouldValidateSchema = true

        // THEN
        assertNotNull(result, "Resulting error set must not be null");
        assertEquals(1, result.size(), "Exactly one validation message should be returned");
        ValidationMessage message = result.iterator().next();
        assertEquals("sample", message.getCode());
        assertEquals("sample message", message.getMessage());

        // Verify that the pre‑walk listener was consulted
        verify(mockWalkListenerRunner, times(1)).runPreWalkListeners(
                eq(mockExecutionContext),
                anyString(),
                eq(mockNode),
                eq(mockRootNode),
                eq(mockInstanceLocation),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                eq(schemaUnderTest.schemaNode),
                eq(schemaUnderTest.parentSchema),
                eq(schemaUnderTest.validationContext),
                eq(schemaUnderTest.validationContext.getJsonSchemaFactory()));

        // Verify that the post‑walk listener was called exactly once
        verify(mockWalkListenerRunner, times(1)).runPostWalkListeners(
                eq(mockExecutionContext),
                anyString(),
                eq(mockNode),
                eq(mockRootNode),
                eq(mockInstanceLocation),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                eq(schemaUnderTest.schemaNode),
                eq(schemaUnderTest.parentSchema),
                eq(schemaUnderTest.validationContext),
                eq(schemaUnderTest.validationContext.getJsonSchemaFactory()),
                eq(result));
    }

    @Test
    void walk_ShouldSkipValidator_WhenPreWalkListenerReturnsFalse() {
        // GIVEN – make the pre‑walk listener deny execution
        when(mockWalkListenerRunner.runPreWalkListeners(
                any(ExecutionContext.class),
                anyString(),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                any(JsonSchema.class),
                any(ValidationContext.class),
                any(JsonSchemaFactory.class)))
                .thenReturn(false);

        // WHEN
        Set<ValidationMessage> result = schemaUnderTest.walk(
                mockExecutionContext,
                mockNode,
                mockRootNode,
                mockInstanceLocation,
                true);

        // THEN – no validator should have been invoked and the result set is empty
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when pre‑walk listener skips execution");
        verify(mockValidator, never()).walk(any(), any(), any(), any(), anyBoolean());
        // Post‑walk listener is still expected to be called
        verify(mockWalkListenerRunner, times(1)).runPostWalkListeners(
                eq(mockExecutionContext),
                anyString(),
                eq(mockNode),
                eq(mockRootNode),
                eq(mockInstanceLocation),
                any(JsonNodePath.class),
                any(JsonNodePath.class),
                eq(schemaUnderTest.schemaNode),
                eq(schemaUnderTest.parentSchema),
                eq(schemaUnderTest.validationContext),
                eq(schemaUnderTest.validationContext.getJsonSchemaFactory()),
                eq(result));
    }
}
```