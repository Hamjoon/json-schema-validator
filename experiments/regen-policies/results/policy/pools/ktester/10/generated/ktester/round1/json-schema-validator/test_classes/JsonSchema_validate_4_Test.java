package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    /**
     * Mocks required to build a JsonSchema instance.
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    // the schema definition node
    @Mock
    JsonNode schemaNode;

    // the JSON document to validate
    @Mock
    JsonNode rootNode;

    @Mock
    ExecutionContextCustomizer executionCustomizer;

    /**
     * The JsonSchema instance under test (wrapped in a spy).
     */
    JsonSchema jsonSchemaSpy;

    /**
     * Helper holder for side‑effect verification.
     */
    static class SideEffectHolder {

        static boolean invoked = false;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real JsonSchema via the package‑private factory method.
        JsonSchema realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // Spy the instance so that we can stub the overloaded validate method.
        jsonSchemaSpy = spy(realSchema);
        // Build a real JsonSchema via the package‑private factory method.
        JsonSchema realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // Spy the instance so that we can stub the three‑argument overload.
        jsonSchemaSpy = spy(realSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference.
        clearInvocations(validationContext, schemaLocation, evaluationPath, schemaNode, rootNode, executionCustomizer);
        // Reset static side‑effect flag and Mockito invocations.
        SideEffectHolder.invoked = false;
        clearInvocations(validationContext, schemaLocation, evaluationPath, schemaNode, rootNode);
    }

    /**
     * Verifies that {@code JsonSchema.validate(JsonNode, ExecutionContextCustomizer)}
     * delegates to {@code validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}
     * using {@link OutputFormat#DEFAULT} and returns the resulting set of
     * {@link ValidationMessage}s.
     */
    @Test
    void shouldDelegateToThreeArgumentValidateAndReturnResult() {
        // Arrange: expected validation result
        Set<ValidationMessage> expectedMessages = new HashSet<>();
        // a dummy message
        expectedMessages.add(mock(ValidationMessage.class));
        // Stub the three‑argument overload.
        when(jsonSchemaSpy.validate(eq(rootNode), eq(OutputFormat.DEFAULT), eq(executionCustomizer))).thenReturn(expectedMessages);
        // Act: call the two‑argument overload.
        Set<ValidationMessage> actualMessages = jsonSchemaSpy.validate(rootNode, executionCustomizer);
        // Assert: the result is exactly what the stub returned.
        assertSame(expectedMessages, actualMessages, "The two‑argument validate method should return the set produced by the three‑argument overload.");
        // Verify that the three‑argument method was invoked exactly once.
        verify(jsonSchemaSpy, times(1)).validate(eq(rootNode), eq(OutputFormat.DEFAULT), eq(executionCustomizer));
    }

    /**
     * The method should reject a {@code null} root node. The contract of the
     * underlying three‑argument overload is to throw a {@link NullPointerException}
     * (or {@link IllegalArgumentException}); we stub that behaviour and verify that
     * it propagates unchanged.
     */
    @Test
    void shouldThrowWhenRootNodeIsNull() {
        // Arrange – stub the three‑argument method to throw NPE for a null root node.
        when(jsonSchemaSpy.validate(isNull(), eq(OutputFormat.DEFAULT), any())).thenThrow(new NullPointerException("rootNode is null"));
        // Act & Assert.
        NullPointerException ex = assertThrows(NullPointerException.class, () -> jsonSchemaSpy.validate(null, null), "Passing a null rootNode must result in NullPointerException");
        assertEquals("rootNode is null", ex.getMessage());
    }

    /**
     * If the supplied {@link ExecutionContextCustomizer} throws an exception,
     * the {@code validate} method must not swallow it – the exception should be
     * propagated to the caller.
     *
     * <p>We use a real (non‑mocked) {@link JsonSchema} instance built from a minimal
     * schema so that the actual three‑argument method is executed. The customizer
     * simply throws a {@link RuntimeException}.
     */
    @Test
    void shouldPropagateExceptionFromCustomizer() throws Exception {
        // Build a minimal, valid schema: {"type":"object"}
        ObjectMapper mapper = new ObjectMapper();
        JsonNode minimalSchema = mapper.readTree("{\"type\":\"object\"}");
        JsonSchema realSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, minimalSchema, null, false);
        // Instance that satisfies the schema.
        JsonNode instance = mapper.readTree("{\"any\":\"value\"}");
        // Customizer that throws.
        ExecutionContextCustomizer throwingCustomizer = execution -> {
            throw new RuntimeException("customizer failure");
        };
        // Act & Assert.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> realSchema.validate(instance, throwingCustomizer), "Exception thrown by the customizer must be propagated");
        assertEquals("customizer failure", ex.getMessage());
    }

    /**
     * Verifies that the provided {@link ExecutionContextCustomizer} is actually
     * invoked. The customizer flips a static flag; after validation the flag must
     * be {@code true}. The validation result itself is stubbed to be an empty set.
     */
    @Test
    void shouldExecuteCustomizerSideEffect() {
        // Arrange – stub three‑argument overload to return an empty set.
        when(jsonSchemaSpy.validate(eq(rootNode), eq(OutputFormat.DEFAULT), any())).thenReturn(Collections.emptySet());
        // Customizer that sets the side‑effect flag.
        ExecutionContextCustomizer sideEffectCustomizer = execution -> SideEffectHolder.invoked = true;
        // Act.
        Set<ValidationMessage> result = jsonSchemaSpy.validate(rootNode, sideEffectCustomizer);
        // Assert – result is the stubbed empty set and side effect occurred.
        assertTrue(result.isEmpty(), "Validation result should be the stubbed empty set");
        assertTrue(SideEffectHolder.invoked, "Customizer must have been executed");
    }
}
