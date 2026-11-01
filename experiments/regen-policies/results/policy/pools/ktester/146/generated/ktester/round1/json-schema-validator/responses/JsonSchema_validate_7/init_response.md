```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, OutputFormat)}.
 *
 * The tests verify that the {@code validate} method forwards the validation
 * result to the supplied {@link OutputFormat} and returns the value produced
 * by the format. Two scenarios are covered:
 *
 * <ul>
 *   <li>Valid JSON node – the format receives an empty {@link Set} of
 *       {@link ValidationMessage}s.</li>
 *   <li>Invalid JSON node – the format receives a non‑empty {@link Set}
 *       containing the expected validation error.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_7_Test {

    /** ObjectMapper used to build JsonNode instances. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Schema used for the tests – validates that the instance is a JSON string. */
    private JsonSchema schema;

    /** ExecutionContext supplied to the validate method. */
    private ExecutionContext executionContext;

    /** Mocked OutputFormat – captures the Set<ValidationMessage> passed to it. */
    @Mock
    OutputFormat<List<ValidationMessage>> outputFormat;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a very small schema that only allows string values.
        String schemaData = "{\"type\":\"string\"}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schema = factory.getSchema(schemaData);
        this.executionContext = new ExecutionContext();

        // Configure the mock OutputFormat to return a mutable List containing the
        // validation messages it receives. This makes the return value easy to assert.
        when(outputFormat.apply(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<ValidationMessage> msgs = (Set<ValidationMessage>) invocation.getArgument(0);
            return new ArrayList<>(msgs);
        });
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        reset(outputFormat);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * When the JSON node conforms to the schema, the OutputFormat should receive
     * an empty set of validation messages and the returned list should be empty.
     */
    @Test
    void validateValidNode_returnsEmptyList() throws Exception {
        JsonNode validNode = MAPPER.readTree("\"valid string\""); // a JSON string → valid

        List<ValidationMessage> result = schema.validate(executionContext, validNode, outputFormat);

        // Verify that the format was invoked exactly once with an empty set.
        ArgumentCaptor<Set<ValidationMessage>> captor = ArgumentCaptor.forClass(Set.class);
        verify(outputFormat, times(1)).apply(captor.capture());
        assertTrue(captor.getValue().isEmpty(), "Expected no validation messages for a valid node");

        // The method should return the list produced by the mock (empty in this case).
        assertNotNull(result, "Result list must not be null");
        assertTrue(result.isEmpty(), "Result list should be empty for a valid node");
    }

    /**
     * When the JSON node does **not** conform to the schema, the OutputFormat should
     * receive a set containing at least one {@link ValidationMessage}. The returned
     * list should contain the same messages.
     */
    @Test
    void validateInvalidNode_returnsMessages() throws Exception {
        JsonNode invalidNode = MAPPER.readTree("123"); // a JSON number → invalid for string schema

        List<ValidationMessage> result = schema.validate(executionContext, invalidNode, outputFormat);

        // Capture the argument passed to the format.
        ArgumentCaptor<Set<ValidationMessage>> captor = ArgumentCaptor.forClass(Set.class);
        verify(outputFormat, times(1)).apply(captor.capture());

        Set<ValidationMessage> capturedMessages = captor.getValue();
        assertFalse(capturedMessages.isEmpty(), "Expected validation messages for an invalid node");
        assertEquals(1, capturedMessages.size(),
                "For this simple schema we expect exactly one validation error");

        // The returned list should contain the same messages.
        assertNotNull(result, "Result list must not be null");
        assertEquals(capturedMessages.size(), result.size(),
                "Returned list size should match the number of validation messages");
        assertEquals(capturedMessages.iterator().next().getMessage(),
                result.get(0).getMessage(),
                "Returned message should match the captured validation message");
    }
}
```