```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;

    /**
     * Build a very small schema (an empty object schema) that will always validate
     * successfully.  The schema is created via {@link JsonSchemaFactory} because the
     * {@code JsonSchema} constructor is private.
     */
    @BeforeEach
    void setupBeforeEach() {
        // a schema that accepts any JSON value – the simplest possible valid schema
        String schemaJson = "{ \"$schema\" : \"https://json-schema.org/draft/2019-09/schema\" }";

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(s -> s.schemas(Collections.emptyMap())));

        // use the default configuration – nothing special is required for the tests
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should have been created");
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    /**
     * Verify that {@code walk(...)} returns the value produced by the supplied
     * {@link OutputFormat}.  The test uses a Mockito mock for {@code OutputFormat}
     * that simply returns a constant string.  The underlying {@link ValidationResult}
     * should contain no validation messages because the schema is permissive.
     */
    @Test
    void walkReturnsOutputFromOutputFormat() {
        // given
        JsonNode node = MAPPER.createObjectNode(); // an empty JSON object
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.apply(any(ValidationResult.class))).thenReturn("OK");

        // when
        String result = schema.walk(schema.createExecutionContext(),
                                    node,
                                    format,
                                    true,                     // ask the walk to validate
                                    (ExecutionContextCustomizer) null);

        // then
        assertEquals("OK", result, "walk should return the value produced by OutputFormat");
        // verify that the format was invoked exactly once with a ValidationResult that has no errors
        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(format, times(1)).apply(captor.capture());
        ValidationResult vr = captor.getValue();
        assertTrue(vr.getMessages().isEmpty(), "ValidationResult should contain no messages for a permissive schema");
    }

    /**
     * Verify that a non‑null {@link ExecutionContextCustomizer} is invoked before the walk
     * proceeds.  The customizer is mocked and we assert that its {@code customize} method is
     * called exactly once.
     */
    @Test
    void walkInvokesExecutionContextCustomizer() {
        // given
        JsonNode node = MAPPER.createObjectNode();
        @SuppressWarnings("unchecked")
        OutputFormat<Set<ValidationMessage>> format = mock(OutputFormat.class);
        when(format.apply(any(ValidationResult.class))).thenReturn(Collections.emptySet());

        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        // the customizer does not need to do anything; we just want to verify the call

        // when
        Set<ValidationMessage> result = schema.walk(schema.createExecutionContext(),
                                                    node,
                                                    format,
                                                    false,               // do not validate – we only care about the customizer call
                                                    customizer);

        // then
        assertNotNull(result, "Result must not be null");
        verify(customizer, times(1)).customize(any(ExecutionContext.class));
        // also verify that the output format was used
        verify(format, times(1)).apply(any(ValidationResult.class));
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these unit tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }
}
```