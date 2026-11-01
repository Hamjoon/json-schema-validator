```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Additional unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * <p>The tests cover the scenarios described in the supplied JSON test‑case list:
 *
 * <ul>
 *   <li>successful validation of a correct string node</li>
 *   <li>validation failure when the node type does not match the schema</li>
 *   <li>skipping validation when the {@code validate} flag is {@code false}</li>
 *   <li>use of a non‑null {@link ExecutionContextCustomizer}</li>
 *   <li>handling of {@code null} {@link JsonNode} and {@code null} {@link ExecutionContext}</li>
 * </ul>
 *
 * <p>All tests rely on a mocked {@link OutputFormat}. The mock is configured to return the
 * {@link ValidationResult#getMessages()} set it receives, which lets us inspect the
 * validation messages produced by {@code JsonSchema#walk(...)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    @Mock
    private OutputFormat<Set<ValidationMessage>> outputFormatMock; // mocked OutputFormat used in all tests

    @BeforeAll
    static void setupBeforeAll() {
        // Use Draft‑07 – the simplest version that is always available.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test so that previous interactions do not interfere.
        reset(outputFormatMock);
        // By default the mock returns the set of messages contained in the ValidationResult.
        when(outputFormatMock.apply(any())).thenAnswer(invocation -> {
            ValidationResult vr = invocation.getArgument(0);
            return vr.getMessages();
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validStringNode_validatesSuccessfully
     * -------------------------------------------------------------- */
    @Test
    void walk_validStringNode_validatesSuccessfully() throws Exception {
        // GIVEN – a schema that only accepts a JSON string
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"hello\"");
        ExecutionContext executionContext = new ExecutionContext();

        // WHEN
        Set<ValidationMessage> result = schema.walk(
                executionContext,
                node,
                outputFormatMock,
                true,                                   // validation enabled
                (ExecutionContextCustomizer) null);    // no customizer

        // THEN – validation succeeded, therefore the result set must be empty
        assertTrue(result.isEmpty(),
                "When the node matches the schema, walk should return an empty set of ValidationMessage");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_invalidStringNode_validationFails
     * -------------------------------------------------------------- */
    @Test
    void walk_invalidStringNode_validationFails() throws Exception {
        // GIVEN – a schema that expects a string, but the node is a number
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123");
        ExecutionContext executionContext = new ExecutionContext();

        // WHEN
        Set<ValidationMessage> result = schema.walk(
                executionContext,
                node,
                outputFormatMock,
                true,                                   // validation enabled
                (ExecutionContextCustomizer) null);

        // THEN – we expect exactly one message describing the type mismatch
        assertEquals(1, result.size(),
                "A type‑mismatch should produce a single ValidationMessage");
        ValidationMessage message = result.iterator().next();
        assertTrue(message.getMessage().contains("expected type: String"),
                "The message should mention the expected type String");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_validateFlagFalse_skipsValidation
     * -------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse_skipsValidation() throws Exception {
        // GIVEN – same schema as previous test but validation is disabled
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("123"); // mismatching type
        ExecutionContext executionContext = new ExecutionContext();

        // WHEN
        Set<ValidationMessage> result = schema.walk(
                executionContext,
                node,
                outputFormatMock,
                false,                                  // validation disabled
                (ExecutionContextCustomizer) null);

        // THEN – because validation is skipped, the result must be empty
        assertTrue(result.isEmpty(),
                "When validation flag is false, walk should skip validation and return an empty set");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_withExecutionCustomizerAddsCustomMessage
     * -------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizerAddsCustomMessage() throws Exception {
        // GIVEN – a simple schema that accepts any string
        JsonSchema schema = factory.getSchema("{\"type\":\"string\"}");
        JsonNode node = mapper.readTree("\"test\"");
        ExecutionContext executionContext = new ExecutionContext();

        // A customizer that we can verify was invoked.
        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);

        // The mock OutputFormat will ignore the ValidationResult and return a set
        // containing a synthetic custom message, emulating a customizer‑added message.
        Set<ValidationMessage> customSet = new HashSet<>();
        ValidationMessage customMessage = ValidationMessage.of("custom: added by ExecutionContextCustomizer");
        customSet.add(customMessage);
        when(outputFormatMock.apply(any())).thenReturn(customSet);

        // WHEN
        Set<ValidationMessage> result = schema.walk(
                executionContext,
                node,
                outputFormatMock,
                true,
                customizer);

        // THEN – the customizer must have been called and the result must contain the synthetic message
        verify(customizer, times(1)).customize(any());
        assertEquals(1, result.size(),
                "Result set should contain exactly the custom message injected by the test setup");
        assertTrue(result.contains(customMessage),
                "Result set must contain the expected custom ValidationMessage");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullNode_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullNode_throwsNullPointerException() {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        ExecutionContext executionContext = new ExecutionContext();

        // WHEN / THEN – a NullPointerException is expected because the node argument is null
        assertThrows(NullPointerException.class, () ->
                schema.walk(executionContext, null, outputFormatMock, true, (ExecutionContextCustomizer) null));
    }

    /* --------------------------------------------------------------
     *  Test group: walk_nullExecutionContext_throwsNullPointerException
     * -------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNullPointerException() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"valid\":\"yes\"}");

        // WHEN / THEN – a NullPointerException is expected because the executionContext argument is null
        assertThrows(NullPointerException.class, () ->
                schema.walk(null, node, outputFormatMock, true, (ExecutionContextCustomizer) null));
    }
}
```