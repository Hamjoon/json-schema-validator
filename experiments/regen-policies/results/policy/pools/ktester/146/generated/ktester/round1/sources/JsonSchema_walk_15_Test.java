package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.UnsupportedEncodingException;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_15_Test {

    /**
     * The schema under test – a very simple “object” schema.
     */
    private JsonSchema jsonSchema;

    /**
     * Execution context used for the walk call.
     */
    private ExecutionContext executionContext;

    /**
     * Mocked output format that will receive the {@link ValidationResult}.
     */
    @Mock
    private OutputFormat<String> outputFormat;

    /**
     * Mocked customizer that may modify the execution context.
     */
    @Mock
    private ExecutionContextCustomizer executionCustomizer;

    /**
     * Mocked customizer – used only in the tests that need verification of invocation.
     */
    @Mock
    private ExecutionContextCustomizer mockCustomizer;

    @BeforeEach
    void setupBeforeEach() {
        // 1️⃣ Build a minimal schema instance.
        //    The factory hides the private constructors of JsonSchema.
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(s -> s.schemas(Collections.emptyMap())));
        jsonSchema = factory.getSchema(schemaJson);
        // 2️⃣ Create a fresh execution context for each test.
        executionContext = new ExecutionContext();
        // 3️⃣ Reset the mocks – MockitoExtension already does this, but we keep it explicit.
        reset(outputFormat, executionCustomizer);
        jsonSchema = createSimpleObjectSchema();
        executionContext = new ExecutionContext();
        reset(mockCustomizer);
    }

    /**
     * Helper to create the schema under test.
     */
    private static JsonSchema createSimpleObjectSchema() {
        String schemaJson = "{ \"type\": \"object\" }";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(s -> s.schemas(Collections.emptyMap())));
        return factory.getSchema(schemaJson);
    }

    /**
     * Verifies that {@code walk} returns the value produced by the supplied {@link OutputFormat}
     * and that the {@link ExecutionContextCustomizer} is invoked exactly once.
     */
    @Test
    void walkShouldDelegateToOutputFormatAndInvokeCustomizer() {
        // GIVEN
        String jsonInput = "{\"name\":\"John\"}";
        // The output format is expected to receive a ValidationResult and return a string.
        when(outputFormat.apply(any())).thenReturn("formatted-result");
        // WHEN
        String result = jsonSchema.walk(executionContext, jsonInput, InputFormat.JSON, outputFormat, /*validate=*/
        true, executionCustomizer);
        // THEN
        assertEquals("formatted-result", result, "The walk method should return the value from OutputFormat");
        // Verify that the customizer was called exactly once with the same execution context.
        verify(executionCustomizer, times(1)).customize(executionContext);
        // Verify that the output format received a ValidationResult (the exact instance is not important for this test).
        verify(outputFormat, times(1)).apply(any());
    }

    /**
     * The same scenario as above but with validation disabled.
     * This ensures the boolean flag does not affect the delegation logic.
     */
    @Test
    void walkWithoutValidationShouldStillDelegate() {
        // GIVEN
        String jsonInput = "{\"age\":30}";
        when(outputFormat.apply(any())).thenReturn("no‑validation-result");
        // WHEN
        String result = jsonSchema.walk(executionContext, jsonInput, InputFormat.JSON, outputFormat, /*validate=*/
        false, executionCustomizer);
        // THEN
        assertEquals("no-validation-result", result);
        verify(executionCustomizer, times(1)).customize(executionContext);
        verify(outputFormat, times(1)).apply(any());
    }

    /* -------------------------------------------------------------
     *  1. walk_validJson_validateTrue
     * ------------------------------------------------------------- */
    @Test
    void walk_validJson_validateTrue_returnsEmptyMessageSet() {
        // GIVEN
        String json = "{ \"name\": \"John\", \"age\": 30 }";
        // identity
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN
        Set<ValidationMessage> result = jsonSchema.walk(executionContext, json, InputFormat.JSON, outputFormat, true, null);
        // THEN
        assertNotNull(result, "Result set must not be null");
        assertTrue(result.isEmpty(), "A valid instance should produce no validation messages");
    }

    /* -------------------------------------------------------------
     *  2. walk_invalidJson_validateTrue
     * ------------------------------------------------------------- */
    @Test
    void walk_invalidJson_validateTrue_returnsMessagesForTypeMismatches() {
        // GIVEN
        String json = "{ \"name\": 123, \"age\": \"thirty\" }";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN
        Set<ValidationMessage> result = jsonSchema.walk(executionContext, json, InputFormat.JSON, outputFormat, true, null);
        // THEN
        assertNotNull(result);
        // The schema only defines type "object", so type mismatches are reported on the properties.
        assertTrue(result.size() >= 2, "Expected at least two validation messages for the two type errors");
        // Simple sanity check that messages refer to the offending properties
        boolean nameError = result.stream().anyMatch(m -> m.getPath().contains("name"));
        boolean ageError = result.stream().anyMatch(m -> m.getPath().contains("age"));
        assertTrue(nameError && ageError, "Messages should mention both 'name' and 'age' properties");
    }

    /* -------------------------------------------------------------
     *  3. walk_validJson_validateFalse
     * ------------------------------------------------------------- */
    @Test
    void walk_validJson_validateFalse_returnsEmptyMessageSetEvenIfInvalidStructure() {
        // GIVEN
        String json = "{ \"name\": \"Alice\", \"age\": 25 }";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN validation is disabled the walk still returns a set (empty because no validation occurs)
        Set<ValidationMessage> result = jsonSchema.walk(executionContext, json, InputFormat.JSON, outputFormat, false, null);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty(), "When validation is disabled the result set must be empty");
    }

    /* -------------------------------------------------------------
     *  4. walk_malformedJson_throwsException
     * ------------------------------------------------------------- */
    @Test
    void walk_malformedJson_throwsException() {
        // GIVEN
        // missing closing brace
        String malformed = "{ \"name\": \"Bob\", \"age\": 40 ";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN / THEN
        Exception ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(executionContext, malformed, InputFormat.JSON, outputFormat, true, null));
        // The library wraps parsing errors in a RuntimeException (often ValidationException)
        assertTrue(ex.getMessage().toLowerCase().contains("json") || ex.getMessage().toLowerCase().contains("parse"), "Exception message should indicate a JSON parsing problem");
    }

    /* -------------------------------------------------------------
     *  5. walk_withExecutionCustomizer_modifiesContext
     * ------------------------------------------------------------- */
    @Test
    void walk_withExecutionCustomizer_modifiesContext() {
        // GIVEN
        String json = "{ \"name\": \"Carol\", \"age\": 22 }";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        ExecutionContextCustomizer customizer = ctx -> ctx.setConfig(new SchemaValidatorsConfig().setFailFast(true));
        // WHEN
        Set<ValidationMessage> result = jsonSchema.walk(executionContext, json, InputFormat.JSON, outputFormat, true, customizer);
        // THEN
        assertTrue(result.isEmpty(), "Valid input should yield an empty message set");
        // The customizer should have set failFast = true
        assertTrue(executionContext.getConfig().isFailFast(), "ExecutionContext should be configured with failFast=true by the customizer");
    }

    /* -------------------------------------------------------------
     *  6. walk_withCustomOutputFormat_returnsCustomObject
     * ------------------------------------------------------------- */
    @Test
    void walk_withCustomOutputFormat_returnsCustomObject() {
        // GIVEN
        String json = "{ \"name\": \"Dave\", \"age\": 45 }";
        OutputFormat<String> customFormat = msgs -> msgs.isEmpty() ? "OK" : "FAIL";
        // WHEN
        String result = jsonSchema.walk(executionContext, json, InputFormat.JSON, customFormat, true, null);
        // THEN
        assertEquals("OK", result, "Custom OutputFormat should return \"OK\" for a valid instance");
    }

    /* -------------------------------------------------------------
     *  7. walk_nullInput_returnsEmptySet
     * ------------------------------------------------------------- */
    @Test
    void walk_nullInput_returnsMessageIndicatingMissingRoot() {
        // GIVEN
        String nullInput = null;
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN
        Set<ValidationMessage> result = jsonSchema.walk(executionContext, nullInput, InputFormat.JSON, outputFormat, true, null);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size(), "Null input should generate exactly one validation message");
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("null") || msg.getMessage().toLowerCase().contains("missing"), "Message should indicate that the root node is missing or null");
    }

    /* -------------------------------------------------------------
     *  8. walk_yamlInputFormat_validatesCorrectly
     * ------------------------------------------------------------- */
    @Test
    void walk_yamlInputFormat_validatesCorrectly() {
        // GIVEN
        String yaml = "name: \"Eve\"\nage: 28";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN
        Set<ValidationMessage> result = jsonSchema.walk(executionContext, yaml, InputFormat.YAML, outputFormat, true, null);
        // THEN
        assertTrue(result.isEmpty(), "Valid YAML input should produce no validation messages");
    }

    /* -------------------------------------------------------------
     *  9. walk_nullExecutionContext_throwsNPE
     * ------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext_throwsNPE() {
        // GIVEN
        String json = "{ \"name\": \"John\", \"age\": 30 }";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, json, InputFormat.JSON, outputFormat, true, null));
    }

    /* -------------------------------------------------------------
     * 10. walk_nullInputFormat_throwsIllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void walk_nullInputFormat_throwsIllegalArgumentException() {
        // GIVEN
        String json = "{ \"name\": \"John\", \"age\": 30 }";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.walk(executionContext, json, null, outputFormat, true, null));
    }

    /* -------------------------------------------------------------
     * 11. walk_nullOutputFormat_throwsNullPointerException
     * ------------------------------------------------------------- */
    @Test
    void walk_nullOutputFormat_throwsNullPointerException() {
        // GIVEN
        String json = "{ \"name\": \"John\", \"age\": 30 }";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(executionContext, json, InputFormat.JSON, null, true, null));
    }

    /* -------------------------------------------------------------
     * 12. walk_executionCustomizerThrowsException
     * ------------------------------------------------------------- */
    @Test
    void walk_executionCustomizerThrowsException_propagates() {
        // GIVEN
        String json = "{ \"name\": \"John\", \"age\": 30 }";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(executionContext, json, InputFormat.JSON, outputFormat, true, badCustomizer));
        assertEquals("customizer failure", ex.getMessage(), "Exception from customizer should be propagated unchanged");
    }

    /* -------------------------------------------------------------
     * 13. walk_outputFormatThrowsException
     * ------------------------------------------------------------- */
    @Test
    void walk_outputFormatThrowsException_propagates() {
        // GIVEN
        String json = "{ \"name\": \"John\", \"age\": 30 }";
        OutputFormat<String> failingFormat = msgs -> {
            throw new RuntimeException("output failure");
        };
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.walk(executionContext, json, InputFormat.JSON, failingFormat, true, null));
        assertEquals("output failure", ex.getMessage(), "Exception from OutputFormat should be propagated unchanged");
    }

    /* -------------------------------------------------------------
     * 14. walk_unsupportedInputFormat_throwsIllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void walk_unsupportedInputFormat_throwsIllegalArgumentException() {
        // GIVEN
        String xml = "<root><name>John</name></root>";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> jsonSchema.walk(executionContext, xml, InputFormat.XML, outputFormat, true, null));
        assertTrue(ex.getMessage().toLowerCase().contains("xml"), "Exception message should mention unsupported InputFormat XML");
    }

    /* -------------------------------------------------------------
     * 15. walk_urlEncodedInputUnsupportedEncoding_throwsUnsupportedEncodingException
     * ------------------------------------------------------------- */
    @Test
    void walk_urlEncodedInputUnsupportedEncoding_throwsUnsupportedEncodingException() {
        // GIVEN
        // Invalid percent‑encoding sequence
        String badEncoded = "%E0%A4%A";
        OutputFormat<Set<ValidationMessage>> outputFormat = msgs -> msgs;
        // WHEN / THEN
        UnsupportedEncodingException ex = assertThrows(UnsupportedEncodingException.class, () -> jsonSchema.walk(executionContext, badEncoded, InputFormat.URL_ENCODED, outputFormat, true, null));
        assertTrue(ex.getMessage().toLowerCase().contains("percent"), "Exception message should indicate a malformed percent‑encoding");
    }
}
