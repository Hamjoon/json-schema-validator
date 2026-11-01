```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * The test creates a real {@link JsonSchema} instance but replaces its internal
 * {@link ValidationContext}, {@link KeywordWalkListenerRunner} and the list of
 * {@link JsonValidator}s with mocks. This allows us to verify that the walk
 * implementation correctly:
 *
 * <ul>
 *   <li>invokes the pre‑walk listener and respects its return value,</li>
 *   <li>delegates to each {@link JsonValidator#walk(...)} method,</li>
 *   <li>collects the returned {@link ValidationMessage}s, and</li>
 *   <li>invokes the post‑walk listener.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_10_Test {

    /** The schema instance under test. */
    private JsonSchema schema;

    /** Execution context passed to {@code walk}. */
    private ExecutionContext executionContext;

    /** Mocked validation context used inside the schema. */
    @Mock
    private ValidationContext mockValidationContext;

    /** Mocked configuration returned from the validation context. */
    @Mock
    private SchemaValidatorsConfig mockConfig;

    /** Mocked listener runner handling pre‑ and post‑walk callbacks. */
    @Mock
    private KeywordWalkListenerRunner mockListenerRunner;

    /** A single validator that will be returned from {@code getValidators()}. */
    @Mock
    private JsonValidator mockValidator;

    /** A dummy validation message returned by the mock validator. */
    private ValidationMessage dummyMessage;

    /** Sample JSON nodes used as parameters for the walk call. */
    private JsonNode node;
    private JsonNode rootNode;
    private JsonNodePath instancePath;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Create a minimal real JsonSchema instance.
        // -----------------------------------------------------------------
        // The public factory method is package‑private, but our test class lives
        // in the same package, so we can call it directly.
        ValidationContext realVc = new ValidationContext(
                JsonMetaSchema.getInstance(VersionFlag.V202012),
                JsonSchemaFactory.getInstance(VersionFlag.V202012),
                new SchemaValidatorsConfig());

        SchemaLocation schemaLocation = new SchemaLocation(null);
        JsonNodePath evalPath = new JsonNodePath(PathType.JSON_POINTER);
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();

        // The static `from` method builds a fully‑initialised schema.
        schema = JsonSchema.from(realVc, schemaLocation, evalPath, emptySchemaNode, null, false);

        // -----------------------------------------------------------------
        // 2. Replace the internal ValidationContext with a mock.
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
        when(mockListenerRunner.runPreWalkListeners(
                any(ExecutionContext.class),
                anyString(),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                eq(schema),
                any(JsonValidator.class)))
                .thenReturn(true);
        // Post‑walk listener does nothing (void method).
        doNothing().when(mockListenerRunner).runPostWalkListeners(
                any(ExecutionContext.class),
                anyString(),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                eq(schema),
                any(JsonValidator.class),
                anySet());

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
        when(mockValidator.walk(
                any(ExecutionContext.class),
                any(JsonNode.class),
                any(JsonNode.class),
                any(JsonNodePath.class),
                anyBoolean()))
                .thenReturn(validatorResult);

        // -----------------------------------------------------------------
        // 5. Inject the mock validator list into the schema.
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
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verifies that {@link JsonSchema#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}
     * correctly delegates to validators, respects the pre‑walk listener result,
     * aggregates validation messages and invokes the post‑walk listener.
     */
    @Test
    void walkShouldDelegateToValidatorAndInvokeListeners() {
        // Act: invoke the method under test.
        Set<ValidationMessage> result = schema.walk(
                executionContext,
                node,
                rootNode,
                instancePath,
                true); // shouldValidateSchema = true (the flag is not used in the method body)

        // Assert: the result must contain the dummy message returned by the validator.
        assertNotNull(result, "Resulting set must not be null");
        assertEquals(1, result.size(), "Exactly one validation message should be returned");
        assertTrue(result.contains(dummyMessage), "Result set must contain the dummy message");

        // Verify that the pre‑walk listener was called once with the expected keyword.
        verify(mockListenerRunner, times(1)).runPreWalkListeners(
                eq(executionContext),
                eq("dummyKeyword"),
                eq(node),
                eq(rootNode),
                eq(instancePath),
                eq(schema),
                eq(mockValidator));

        // Verify that the validator's walk method was invoked.
        verify(mockValidator, times(1)).walk(
                eq(executionContext),
                eq(node),
                eq(rootNode),
                eq(instancePath),
                eq(true));

        // Verify that the post‑walk listener was called once.
        verify(mockListenerRunner, times(1)).runPostWalkListeners(
                eq(executionContext),
                eq("dummyKeyword"),
                eq(node),
                eq(rootNode),
                eq(instancePath),
                eq(schema),
                eq(mockValidator),
                eq(result));
    }
}
```