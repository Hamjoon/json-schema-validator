```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(JsonNode, OutputFormat, boolean)}.
 *
 * The tests focus on the delegation behaviour of the {@code walk} method:
 * <ul>
 *   <li>It creates an {@link ExecutionContext} internally.</li>
 *   <li>It forwards the {@link ValidationResult} to the supplied {@link OutputFormat}
 *       and returns the formatted value.</li>
 * </ul>
 *
 * The {@link JsonSchema} instance is created via {@link JsonSchemaFactory} to avoid
 * dealing with the complex constructor arguments directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_4_Test {

    /** Shared Jackson mapper used to build {@link JsonNode} instances. */
    private static ObjectMapper mapper;

    /** The schema under test – an empty schema that always validates successfully. */
    private JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // An empty schema (“{}”) accepts any JSON value – perfect for a lightweight test.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        jsonSchema = factory.getSchema("{}");
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test class.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release at the class level.
    }

    /**
     * Verifies that {@code walk(JsonNode, OutputFormat, boolean)} delegates to the
     * provided {@link OutputFormat} and returns its result.
     *
     * @throws IOException if the test JSON cannot be parsed (should never happen)
     */
    @Test
    void walkShouldDelegateToOutputFormat() throws IOException {
        // GIVEN – a simple JSON node and a mocked OutputFormat
        JsonNode node = mapper.readTree("{\"key\":\"value\"}");
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(any())).thenReturn("formatted-result");

        // WHEN – invoking the method under test
        String result = jsonSchema.walk(node, format, false);

        // THEN – the result comes from the OutputFormat and the format method was called once
        assertEquals("formatted-result", result);
        verify(format, times(1)).format(any());
    }

    /**
     * When validation is requested (the {@code validate} flag is {@code true}),
     * the {@link OutputFormat} should receive a {@link ValidationResult} that
     * contains any validation messages. This test uses a schema that expects a
     * string but supplies a numeric value, causing a validation error.
     *
     * @throws IOException if the test JSON cannot be parsed
     */
    @Test
    void walkWithValidationShouldPassValidationResultToOutputFormat() throws IOException {
        // GIVEN – a schema that requires a string value
        String schemaJson = "{ \"type\": \"string\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema stringSchema = factory.getSchema(schemaJson);

        // Node that violates the schema (number instead of string)
        JsonNode invalidNode = mapper.readTree("123");

        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.format(any())).thenReturn("invalid");

        // WHEN – walk with validation enabled
        String result = stringSchema.walk(invalidNode, format, true);

        // THEN – the OutputFormat is called and its return value is propagated
        assertEquals("invalid", result);
        verify(format, times(1)).format(any());
    }
}
```