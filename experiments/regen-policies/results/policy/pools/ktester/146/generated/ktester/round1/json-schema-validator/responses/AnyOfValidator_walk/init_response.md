```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_walk_Test {

    /** Objects that are shared between the tests. */
    private static ObjectMapper mapper;

    /** Instance under test (spied) and its collaborators. */
    private AnyOfValidator anyOfValidator;
    private AnyOfValidator anyOfValidatorSpy;

    private ValidationContext validationContext;
    private JsonSchema schemaMock1;
    private JsonSchema schemaMock2;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        /* ---------- create the JSON array that represents the anyOf keyword ---------- */
        JsonNode anyOfArrayNode;
        try {
            anyOfArrayNode = mapper.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        /* ---------- mock the constructor arguments ---------- */
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null; // not needed for the behaviour we test

        validationContext = mock(ValidationContext.class);
        schemaMock1 = mock(JsonSchema.class);
        schemaMock2 = mock(JsonSchema.class);

        /* newSchema is called once per element of the anyOf array */
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(schemaMock1, schemaMock2);

        /* ---------- create the validator and spy on it ---------- */
        anyOfValidator = new AnyOfValidator(schemaLocation,
                evaluationPath,
                anyOfArrayNode,
                parentSchema,
                validationContext);

        anyOfValidatorSpy = spy(anyOfValidator);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to release
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    @Test
    void walkShouldValidateWhenFlagTrueAndNodeNotNull() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode rootNode = mock(JsonNode.class);
        JsonNodePath instanceLocation = mock(JsonNodePath.class);

        ValidationMessage validationMessage = mock(ValidationMessage.class);
        Set<ValidationMessage> expected = Collections.singleton(validationMessage);

        // Stub the protected validate method (via the spy) to return a known set
        doReturn(expected).when(anyOfValidatorSpy)
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));

        // Act
        Set<ValidationMessage> actual = anyOfValidatorSpy.walk(executionContext,
                node,
                rootNode,
                instanceLocation,
                true);

        // Assert
        assertSame(expected, actual, "walk should return the result of validate when shouldValidateSchema is true");
        verify(anyOfValidatorSpy, times(1))
                .validate(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(true));
        // No schema walk should be triggered
        verifyNoInteractions(schemaMock1, schemaMock2);
    }

    @Test
    void walkShouldDelegateToSchemasWhenFlagFalse() {
        // Arrange
        ExecutionContext executionContext = mock(ExecutionContext.class);
        JsonNode node = mock(JsonNode.class);
        JsonNode rootNode = mock(JsonNode.class);
        JsonNodePath instanceLocation = mock(JsonNodePath.class);

        // The schemas' walk method returns an empty set – the validator itself also returns empty.
        when(schemaMock1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaMock2.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());

        // Act
        Set<ValidationMessage> result = anyOfValidatorSpy.walk(executionContext,
                node,
                rootNode,
                instanceLocation,
                false);

        // Assert
        assertTrue(result.isEmpty(), "walk should return an empty set when shouldValidateSchema is false");
        // Verify that each schema's walk method was invoked with shouldValidateSchema = false
        verify(schemaMock1, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        verify(schemaMock2, times(1)).walk(eq(executionContext), eq(node), eq(rootNode), eq(instanceLocation), eq(false));
        // The validator's own validate method must NOT be called
        verify(anyOfValidatorSpy, never())
                .validate(any(), any(), any(), any(), anyBoolean());
    }
}
```