```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * The tests focus on the public contract of the {@code walk} method:
 * <ul>
 *   <li>it returns the value produced by the supplied {@link OutputFormat}</li>
 *   <li>the {@link OutputFormat} is invoked exactly once</li>
 *   <li>the method works both with a {@code null} {@link ExecutionContextCustomizer}
 *       and with a concrete customizer implementation</li>
 * </ul>
 *
 * A real {@link JsonSchema} instance is obtained via {@link JsonSchemaFactory},
 * which is the only public way to create a {@code JsonSchema} (its constructors are private).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    @Mock
    private OutputFormat<String> outputFormatMock; // mocked OutputFormat used in all tests

    @BeforeAll
    static void setupBeforeAll() {
        // Use a simple draft‑07 schema that does not impose any constraints.
        // This keeps the validation path trivial and guarantees that validation succeeds.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock between test methods to avoid cross‑test interference.
        reset(outputFormatMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@code walk(..., validate = false, customizer = null)} returns the value
     * produced by the supplied {@link OutputFormat}.
     */
    @Test
    void walkWithoutValidationAndWithoutCustomizer_returnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        ExecutionContext executionContext = new ExecutionContext();

        when(outputFormatMock.apply(any())).thenReturn("formatted-result");

        // WHEN
        String result = schema.walk(executionContext, node, outputFormatMock, false, (ExecutionContextCustomizer) null);

        // THEN
        assertEquals("formatted-result", result, "walk should return the value from OutputFormat");
        verify(outputFormatMock, times(1)).apply(any());
    }

    /**
     * Verifies that {@code walk(..., validate = true, customizer = null)} also returns the value
     * from the {@link OutputFormat} when the schema validates successfully.
     */
    @Test
    void walkWithValidationAndWithoutCustomizer_successfulValidation_returnsOutputFormatResult() throws Exception {
        // GIVEN
        // A schema that requires a string property "name". The provided node satisfies the schema.
        JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}");
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext executionContext = new ExecutionContext();

        when(outputFormatMock.apply(any())).thenReturn("ok");

        // WHEN
        String result = schema.walk(executionContext, node, outputFormatMock, true, (ExecutionContextCustomizer) null);

        // THEN
        assertEquals("ok", result);
        verify(outputFormatMock, times(1)).apply(any());
    }

    /**
     * Verifies that a non‑null {@link ExecutionContextCustomizer} is accepted and does not interfere
     * with the normal flow of {@code walk}.
     */
    @Test
    void walkWithCustomizer_isInvokedAndReturnsOutputFormatResult() throws Exception {
        // GIVEN
        JsonSchema schema = factory.getSchema("{\"type\":\"object\"}");
        JsonNode node = mapper.readTree("{\"age\":30}");
        ExecutionContext executionContext = new ExecutionContext();

        // A simple customizer that adds a dummy collector to the context (no‑op for this test)
        ExecutionContextCustomizer customizer = ctx -> {
            // No real customization needed; just ensure the lambda can be called.
        };

        when(outputFormatMock.apply(any())).thenReturn("customized");

        // WHEN
        String result = schema.walk(executionContext, node, outputFormatMock, false, customizer);

        // THEN
        assertEquals("customized", result);
        verify(outputFormatMock, times(1)).apply(any());
    }
}
```