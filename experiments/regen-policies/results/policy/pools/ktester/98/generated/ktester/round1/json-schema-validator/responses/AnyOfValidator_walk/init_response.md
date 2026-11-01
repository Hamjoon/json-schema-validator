```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * <p>The tests cover the two distinct execution paths:
 *
 * <ul>
 *   <li>When {@code shouldValidateSchema} is {@code true}, the method must delegate to
 *       {@code validate(..., true)} and return whatever that method yields.</li>
 *   <li>When {@code shouldValidateSchema} is {@code false}, the method must invoke {@code walk}
 *       on each subschema with {@code shouldValidateSchema = false} and finally return an empty
 *       {@link LinkedHashSet}.</li>
 * </ul>
 *
 * <p>All collaborators are mocked – no private fields or methods are accessed directly.
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
    @Mock JsonSchema parentSchema;          // can be {@code null} but a mock keeps the constructor happy

    /** Two subschema mocks that the {@link AnyOfValidator} will hold. */
    @Mock JsonSchema subSchema1;
    @Mock JsonSchema subSchema2;

    /** The validator under test – created in {@link #setupBeforeEach()}. */
    AnyOfValidator validator;

    /** A minimal JSON array node required by the {@code AnyOfValidator} constructor. */
    private static ArrayNode anyOfSchemaNode() {
        // [{}, {}] – two empty object schemas
        return JsonNodeFactory.instance.arrayNode()
                .add(JsonNodeFactory.instance.objectNode())
                .add(JsonNodeFactory.instance.objectNode());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext so that it returns our two mocked subschemas.
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema1)   // first call
                .thenReturn(subSchema2);  // second call

        // Build the AnyOfValidator instance with the mocked collaborators.
        validator = new AnyOfValidator(
                schemaLocation,
                new JsonNodePath(JsonNodePath.PathType.JSON_POINTER), // evaluationPath – can be a simple instance
                anyOfSchemaNode(),
                parentSchema,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(executionContext, node, rootNode, instanceLocation,
                validationContext, schemaLocation, parentSchema,
                subSchema1, subSchema2);
    }

    @Test
    void walkWhenShouldValidateSchemaTrue_ShouldDelegateToValidate() {
        // Arrange – we spy the validator so that we can stub the protected validate method.
        AnyOfValidator spyValidator = Mockito.spy(validator);

        // Prepare a dummy ValidationMessage set that the stub will return.
        ValidationMessage dummyMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = new LinkedHashSet<>(Collections.singleton(dummyMessage));

        // Stub the protected validate method (the overload with the extra boolean flag).
        doReturn(expected).when(spyValidator)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        // Act
        Set<ValidationMessage> actual = spyValidator.walk(executionContext, node, rootNode, instanceLocation, true);

        // Assert – the result must be exactly the stubbed set.
        assertSame(expected, actual, "walk(true) should return the set produced by validate(..., true)");
        // Verify that validate was called once with the correct arguments.
        verify(spyValidator, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No subschema walk should have happened.
        verifyNoInteractions(subSchema1, subSchema2);
    }

    @Test
    void walkWhenShouldValidateSchemaFalse_ShouldWalkAllSubSchemasAndReturnEmptySet() {
        // Arrange – nothing special; the subschemas are already mocked.

        // Act
        Set<ValidationMessage> result = validator.walk(executionContext, node, rootNode, instanceLocation, false);

        // Assert – the method must return an empty LinkedHashSet.
        assertTrue(result instanceof LinkedHashSet, "Result should be a LinkedHashSet");
        assertTrue(result.isEmpty(), "Result should be empty when shouldValidateSchema is false");

        // Verify that each subschema's walk method was invoked exactly once with shouldValidateSchema = false.
        verify(subSchema1, times(1))
                .walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(subSchema2, times(1))
                .walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
    }
}
```