```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * <p>The tests cover:
 *
 * <ul>
 *   <li>Delegation to {@code validate(..., true)} when {@code shouldValidateSchema} is {@code true}.</li>
 *   <li>Iteration over child schemas when {@code shouldValidateSchema} is {@code false}.</li>
 *   <li>Edge‑cases such as {@code null} arguments, empty or {@code null} schema list,
 *       {@code null} element inside the list, and exceptions thrown by a child schema.</li>
 * </ul>
 *
 * <p>All collaborators are mocked; private fields are accessed only via reflection where
 * required (e.g., to inject a {@code null} {@code schemas} list). No production code is
 * modified.
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_walk_Test {

    /** Common collaborators used by every test. */
    @Mock ExecutionContext executionContext;
    @Mock JsonNode node;
    @Mock JsonNode rootNode;
    @Mock JsonNodePath instanceLocation;
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonSchema parentSchema; // can be null but a mock keeps the constructor happy

    /** Two subschema mocks that the {@link AnyOfValidator} will hold. */
    @Mock JsonSchema subSchema1;
    @Mock JsonSchema subSchema2;

    /** The validator under test – created in {@link #setupBeforeEach()}. */
    AnyOfValidator validator;

    /** Helper to create an array node containing two empty object schemas. */
    private static ArrayNode anyOfSchemaNode() {
        return JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode());
    }

    /** Helper to create an empty array node (no subschemas). */
    private static ArrayNode emptySchemaNode() {
        return JsonNodeFactory.instance.arrayNode(); // [] – no elements
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext so that it returns our two mocked subschemas.
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema1)   // first call
                .thenReturn(subSchema2);  // second call

        // Build the AnyOfValidator instance with a non‑empty schema node.
        validator = new AnyOfValidator(
                schemaLocation,
                new JsonNodePath(JsonNodePath.PathType.JSON_POINTER),
                anyOfSchemaNode(),
                parentSchema,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset all mocks to avoid cross‑test interference.
        reset(executionContext, node, rootNode, instanceLocation,
                validationContext, schemaLocation, parentSchema,
                subSchema1, subSchema2);
    }

    /* --------------------------------------------------------------------- */
    /*  Normal behaviour                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void walkWhenShouldValidateSchemaTrue_ShouldDelegateToValidate() {
        // Spy the validator so we can stub the protected validate method.
        AnyOfValidator spyValidator = Mockito.spy(validator);

        // Dummy ValidationMessage set returned by the stub.
        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = new LinkedHashSet<>(Collections.singleton(dummyMessage));

        // Stub the overload that receives the extra boolean flag.
        doReturn(expected).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, node, rootNode, instanceLocation, true);

        // Assert
        assertSame(expected, actual, "walk(true) should return the set produced by validate(..., true)");
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No child‑schema walk should occur.
        verifyNoInteractions(subSchema1, subSchema2);
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_WithTwoSubSchemas_ShouldWalkAllAndReturnEmptySet() {
        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);

        // Assert – result must be an empty LinkedHashSet.
        assertTrue(result instanceof LinkedHashSet, "Result should be a LinkedHashSet");
        assertTrue(result.isEmpty(), "Result should be empty when shouldValidateSchema is false");

        // Verify each subschema's walk was invoked exactly once with shouldValidateSchema = false.
        verify(subSchema1, times(1))
                .walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(subSchema2, times(1))
                .walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_WithEmptySchemas_ShouldReturnEmptySetAndNoWalks() {
        // Build a validator that holds an empty schema list.
        AnyOfValidator emptyValidator = new AnyOfValidator(
                schemaLocation,
                new JsonNodePath(JsonNodePath.PathType.JSON_POINTER),
                emptySchemaNode(),
                parentSchema,
                validationContext);

        // Act
        Set<ValidationMessage> result = emptyValidator.walk(executionContext, node, rootNode, instanceLocation, false);

        // Assert
        assertTrue(result instanceof LinkedHashSet, "Result should be a LinkedHashSet");
        assertTrue(result.isEmpty(), "Result should be empty when there are no subschemas");
        // No interaction with the previously mocked subschemas because they were never added.
        verifyNoInteractions(subSchema1, subSchema2);
    }

    /* --------------------------------------------------------------------- */
    /*  Null‑argument edge cases                                                */
    /* --------------------------------------------------------------------- */

    @Test
    void walkWhenShouldValidateSchemaTrue_NullExecutionContext_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        // Force the protected validate method to throw NPE when the executionContext is null.
        doThrow(new NullPointerException("executionContext is null"))
                .when(spy).validate(isNull(), any(), any(), any(), eq(true));

        assertThrows(NullPointerException.class,
                () -> spy.walk(null, node, rootNode, instanceLocation, true),
                "walk(true) with null executionContext should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullNode_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        doThrow(new NullPointerException("node is null"))
                .when(spy).validate(eq(executionContext), isNull(), any(), any(), eq(true));

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, null, rootNode, instanceLocation, true),
                "walk(true) with null node should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullRootNode_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        doThrow(new NullPointerException("rootNode is null"))
                .when(spy).validate(eq(executionContext), any(), isNull(), any(), eq(true));

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, node, null, instanceLocation, true),
                "walk(true) with null rootNode should propagate NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_NullInstanceLocation_ShouldThrowNPE() {
        AnyOfValidator spy = Mockito.spy(validator);
        doThrow(new NullPointerException("instanceLocation is null"))
                .when(spy).validate(eq(executionContext), any(), any(), isNull(), eq(true));

        assertThrows(NullPointerException.class,
                () -> spy.walk(executionContext, node, rootNode, null, true),
                "walk(true) with null instanceLocation should propagate NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    /*  Internal field edge cases                                              */
    /* --------------------------------------------------------------------- */

    @Test
    void walkWhenShouldValidateSchemaFalse_NullSchemasField_ShouldThrowNPE() throws Exception {
        // Use reflection to set the private final field `schemas` to null.
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, null);

        assertThrows(NullPointerException.class,
                () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                "walk(false) with internal schemas == null should throw NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_NullElementInSchemas_ShouldThrowNPE() throws Exception {
        // Replace the schemas list with a list that contains a single null entry.
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, List.of((JsonSchema) null));

        assertThrows(NullPointerException.class,
                () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                "walk(false) with a null element in schemas list should throw NullPointerException");
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_ChildSchemaThrowsRuntimeException_ShouldPropagate() {
        // Make the first subschema throw a RuntimeException when its walk method is called.
        doThrow(new RuntimeException("child schema failure"))
                .when(subSchema1).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> validator.walk(executionContext, node, rootNode, instanceLocation, false),
                "Exception from child schema.walk should propagate");

        assertEquals("child schema failure", thrown.getMessage());
        // Verify that the second subschema was never invoked because the first already threw.
        verify(subSchema2, never())
                .walk(any(), any(), any(), any(), any());
    }
}
```