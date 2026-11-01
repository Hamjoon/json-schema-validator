```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Basic unit tests for {@link JsonSchema#validate(JsonNode, OutputFormat, ExecutionContextCustomizer)}.
 *
 * <p>The tests use a very small schema (a simple string type) so that the validation
 * logic exercised is deterministic and does not depend on external resources.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_9_Test {

    private static JsonSchemaFactory factory;
    private JsonSchema schema;
    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for Draft‑2020‑12 – the version used in the existing test suite.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        mapper = new ObjectMapper();

        // Very small schema: the root value must be a string.
        String schemaJson = "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "\"type\": \"string\" }";

        // Build the JsonSchema instance once per test method.
        schema = factory.getSchema(schemaJson);
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable state to clean up for the current tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to release at the class level.
    }

    /**
     * Validate a correct JSON string using the three‑argument overload.
     * The {@link OutputFormat#DEFAULT} implementation returns a {@link Set} of
     * {@link ValidationMessage}s, which should be empty for a valid instance.
     */
    @Test
    void validate_withValidString_andNoCustomizer_returnsEmptySet() throws Exception {
        // given
        JsonNode validNode = mapper.readTree("\"hello world\"");

        // when
        Set<ValidationMessage> result = schema.validate(validNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);

        // then
        assertTrue(result.isEmpty(), "A valid string should produce no validation messages");
    }

    /**
     * Validate an invalid JSON value (number instead of string) and ensure that
     * the returned {@link Set} contains exactly one message describing the type error.
     */
    @Test
    void validate_withInvalidType_returnsOneMessage() throws Exception {
        // given
        JsonNode invalidNode = mapper.readTree("123");

        // when
        Set<ValidationMessage> result = schema.validate(invalidNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);

        // then
        assertEquals(1, result.size(), "A type mismatch should produce exactly one validation message");
        ValidationMessage message = result.iterator().next();
        assertEquals("type", message.getType(), "The message type should be 'type'");
    }

    /**
     * Validate using a no‑op {@link ExecutionContextCustomizer}. The customizer
     * does not change the execution context, so the behaviour must be identical
     * to the call with a {@code null} customizer.
     */
    @Test
    void validate_withNoOpCustomizer_behavesLikeNullCustomizer() throws Exception {
        // given
        JsonNode validNode = mapper.readTree("\"test\"");

        ExecutionContextCustomizer noOp = executionContext -> {
            // intentionally left blank – does not modify the context
        };

        // when
        Set<ValidationMessage> resultWithNull = schema.validate(validNode, OutputFormat.DEFAULT, (ExecutionContextCustomizer) null);
        Set<ValidationMessage> resultWithCustomizer = schema.validate(validNode, OutputFormat.DEFAULT, noOp);

        // then
        assertEquals(resultWithNull, resultWithCustomizer,
                "Validation result must be identical whether a no‑op customizer or null is supplied");
    }

    /**
     * Demonstrates that a custom {@link ExecutionContextCustomizer} can be used
     * to inject a dummy attribute into the {@link ExecutionContext}. The test
     * does not assert on the attribute (as the internal API is not public) but
     * verifies that the customizer is executed without throwing an exception.
     */
    @Test
    void validate_withCustomizerThatModifiesContext_executesSuccessfully() throws Exception {
        // given
        JsonNode validNode = mapper.readTree("\"foo\"");

        ExecutionContextCustomizer customizer = executionContext -> {
            // Example modification – store a flag in the context's attributes map.
            executionContext.getAttributes().put("test-flag", Boolean.TRUE);
        };

        // when / then – the call must complete without propagating any exception.
        assertDoesNotThrow(() -> schema.validate(validNode, OutputFormat.DEFAULT, customizer));
    }
}
```