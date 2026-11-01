package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /**
     * The schema under test.
     */
    private JsonSchema jsonSchema;

    /**
     * Real validation context used to build a functional {@link JsonSchema} instance.
     */
    private ValidationContext validationContext;

    /**
     * Mocked collaborators for the {@code validate} call.
     */
    @Mock
    private InputFormat inputFormat;

    @Mock
    private ExecutionContextCustomizer executionCustomizer;

    @Mock
    private OutputFormat<Integer> outputFormat;

    @Mock
    private OutputFormat<Integer> // used in the original example test
    outputFormatInt;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // Build a minimal, valid JsonSchema instance.
        // -----------------------------------------------------------------
        // 1. Create a real ValidationContext (uses real meta‑schema & factory).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV201909();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);
        // 2. Provide a trivial (empty) schema node – this accepts any JSON.
        ObjectMapper mapper = new ObjectMapper();
        // {}
        JsonNode emptySchemaNode = mapper.createObjectNode();
        // 3. Mock the location and evaluation path – they are only used for
        //    bookkeeping inside JsonSchema and are not relevant for validation.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        when(schemaLocation.toString()).thenReturn("mocked-location");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.resolve(anyString())).thenReturn(schemaLocation);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        when(evaluationPath.toString()).thenReturn("mocked-path");
        // 4. Build the JsonSchema instance via the package‑private factory method.
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, emptySchemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // -----------------------------------------------------------------
        // Stub the OutputFormat – we simply return a constant value so that
        // we can assert the result of the validate(...) call.
        // -----------------------------------------------------------------
        when(outputFormat.apply(any(JsonNode.class), anySet())).thenReturn(42);
    }

    /* --------------------------------------------------------------
     * Helper – builds a schema that enforces:
     *   - required property "age"
     *   - "age" must be integer
     *   - no additional properties
     * -------------------------------------------------------------- */
    private JsonSchema buildSchemaWithConstraints() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": ["age"],
              "additionalProperties": false
            }
            """;
        JsonNode schemaNode = mapper.readTree(schemaJson);
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        when(schemaLocation.toString()).thenReturn("constraint-schema");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.resolve(anyString())).thenReturn(schemaLocation);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        when(evaluationPath.toString()).thenReturn("constraint-path");
        return JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    @Test
    void validate_shouldReturnFormattedResult_usingExecutionContextCustomizer() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        // WHEN
        Integer result = jsonSchema.validate(json, inputFormat, outputFormat, executionCustomizer);
        // THEN
        assertEquals(42, result, "The formatted result should be the value supplied by the OutputFormat mock");
        // Verify that the OutputFormat was indeed invoked with a JsonNode and a (possibly empty) set of messages.
        verify(outputFormat, times(1)).apply(any(JsonNode.class), anySet());
    }

    /* --------------------------------------------------------------
     *  1. Normal validation – JSON input, successful walk
     * -------------------------------------------------------------- */
    @Test
    void normalJsonValidation_successfulWalk() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";
        OutputFormat<List<ValidationMessage>> format = new OutputFormat<>() {

            @Override
            public List<ValidationMessage> apply(JsonNode node, Set<ValidationMessage> msgs) {
                return new ArrayList<>(msgs);
            }
        };
        ExecutionContextCustomizer customizer = ctx -> ctx.setConfig(new SchemaValidatorsConfig());
        // WHEN
        List<ValidationMessage> result = jsonSchema.validate(json, InputFormat.JSON, format, customizer);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty(), "No validation errors are expected for a valid JSON document");
    }

    /* --------------------------------------------------------------
     *  2. Normal validation – YAML input, successful walk
     * -------------------------------------------------------------- */
    @Test
    void normalYamlValidation_successfulWalk() {
        // GIVEN
        String yaml = "name: John\nage: 30";
        OutputFormat<Set<ValidationMessage>> format = new OutputFormat<>() {

            @Override
            public Set<ValidationMessage> apply(JsonNode node, Set<ValidationMessage> msgs) {
                return msgs;
            }
        };
        // WHEN
        Set<ValidationMessage> result = jsonSchema.validate(yaml, InputFormat.YAML, format, null);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty(), "YAML document should validate without errors");
    }

    /* --------------------------------------------------------------
     *  3. Invalid JSON – parsing error
     * -------------------------------------------------------------- */
    @Test
    void invalidJson_parsingError() {
        // GIVEN: malformed JSON (missing closing brace)
        String malformed = "{\"name\":\"John\",\"age\":30";
        // dummy
        OutputFormat<Object> format = msgs -> msgs;
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformed, InputFormat.JSON, format, null));
        assertTrue(ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException, "Root cause should be JsonProcessingException");
    }

    /* --------------------------------------------------------------
     *  4. Invalid YAML – parsing error
     * -------------------------------------------------------------- */
    @Test
    void invalidYaml_parsingError() {
        // GIVEN: malformed YAML
        String malformed = "name: John\nage";
        // dummy
        OutputFormat<Object> format = msgs -> msgs;
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformed, InputFormat.YAML, format, null));
        assertTrue(ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException, "Root cause should be JsonProcessingException");
    }

    /* --------------------------------------------------------------
     *  5. Null input string
     * -------------------------------------------------------------- */
    @Test
    void nullInputString_throwsNPE() {
        OutputFormat<Object> format = msgs -> msgs;
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(null, InputFormat.JSON, format, null));
    }

    /* --------------------------------------------------------------
     *  6. Null InputFormat
     * -------------------------------------------------------------- */
    @Test
    void nullInputFormat_throwsNPE() {
        String json = "{\"a\":1}";
        OutputFormat<Object> format = msgs -> msgs;
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(json, null, format, null));
    }

    /* --------------------------------------------------------------
     *  7. Null OutputFormat
     * -------------------------------------------------------------- */
    @Test
    void nullOutputFormat_throwsNPE() {
        String json = "{\"a\":1}";
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(json, InputFormat.JSON, null, null));
    }

    /* --------------------------------------------------------------
     *  8. Null ExecutionContextCustomizer – handled gracefully
     * -------------------------------------------------------------- */
    @Test
    void nullExecutionContextCustomizer_successfulWalk() {
        String json = "{\"a\":1}";
        OutputFormat<List<ValidationMessage>> format = (node, msgs) -> new ArrayList<>(msgs);
        List<ValidationMessage> result = jsonSchema.validate(json, InputFormat.JSON, format, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /* --------------------------------------------------------------
     *  9. ExecutionContextCustomizer throws runtime exception
     * -------------------------------------------------------------- */
    @Test
    void executionContextCustomizerThrows_exceptionPropagates() {
        String json = "{\"a\":1}";
        OutputFormat<Object> format = msgs -> msgs;
        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new IllegalStateException("Customizer failure");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jsonSchema.validate(json, InputFormat.JSON, format, badCustomizer));
        assertEquals("Customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     * 10. Unsupported charset handling (internal branch)
     * -------------------------------------------------------------- */
    @Test
    void urlDecodedInput_successfulWalk() {
        // "%E4%BD%A0%E5%A5%BD" is "你好" URL‑encoded in UTF‑8.
        String encoded = "%E4%BD%A0%E5%A5%BD";
        OutputFormat<Set<ValidationMessage>> format = (node, msgs) -> msgs;
        Set<ValidationMessage> result = jsonSchema.validate(encoded, InputFormat.JSON, format, null);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Decoding should succeed and produce no validation errors");
    }

    /* --------------------------------------------------------------
     * 11. Validation error – missing required property
     * -------------------------------------------------------------- */
    @Test
    void validationError_missingRequiredProperty() throws Exception {
        JsonSchema schema = buildSchemaWithConstraints();
        // age missing
        String json = "{\"name\":\"John\"}";
        OutputFormat<Set<ValidationMessage>> format = (node, msgs) -> msgs;
        Set<ValidationMessage> msgs = schema.validate(json, InputFormat.JSON, format, null);
        assertFalse(msgs.isEmpty(), "Missing required property should produce a validation message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("age")), "Message should mention the missing 'age' property");
    }

    /* --------------------------------------------------------------
     * 12. Validation error – type mismatch
     * -------------------------------------------------------------- */
    @Test
    void validationError_typeMismatch() throws Exception {
        JsonSchema schema = buildSchemaWithConstraints();
        // age as string
        String json = "{\"name\":\"John\",\"age\":\"thirty\"}";
        OutputFormat<List<ValidationMessage>> format = (node, msgs) -> new ArrayList<>(msgs);
        List<ValidationMessage> msgs = schema.validate(json, InputFormat.JSON, format, null);
        assertFalse(msgs.isEmpty(), "Type mismatch should produce a validation message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("age")), "Message should mention the property 'age'");
    }

    /* --------------------------------------------------------------
     * 13. Validation error – additional property not allowed
     * -------------------------------------------------------------- */
    @Test
    void validationError_additionalPropertyNotAllowed() throws Exception {
        JsonSchema schema = buildSchemaWithConstraints();
        String json = "{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}";
        OutputFormat<Set<ValidationMessage>> format = (node, msgs) -> msgs;
        Set<ValidationMessage> msgs = schema.validate(json, InputFormat.JSON, format, null);
        assertFalse(msgs.isEmpty(), "Disallowed additional property should produce a validation message");
        assertTrue(msgs.stream().anyMatch(m -> m.getMessage().contains("additionalProperties")), "Message should mention additionalProperties");
    }

    /* --------------------------------------------------------------
     * 14. Validation succeeds when customizer relaxes additionalProperties
     * -------------------------------------------------------------- */
    @Test
    void validationSucceeds_whenCustomizerAllowsAdditionalProperties() throws Exception {
        JsonSchema schema = buildSchemaWithConstraints();
        String json = "{\"name\":\"John\",\"age\":30,\"extra\":\"value\"}";
        OutputFormat<List<ValidationMessage>> format = (node, msgs) -> new ArrayList<>(msgs);
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setAdditionalProperties(true);
        List<ValidationMessage> msgs = schema.validate(json, InputFormat.JSON, format, customizer);
        assertTrue(msgs.isEmpty(), "Customiser should allow additional properties, resulting in no errors");
    }

    /* --------------------------------------------------------------
     * 15. Empty input string – JSON parsing error
     * -------------------------------------------------------------- */
    @Test
    void emptyInputString_parsingError() {
        String empty = "";
        OutputFormat<Object> format = msgs -> msgs;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.validate(empty, InputFormat.JSON, format, null));
        assertTrue(ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException);
    }

    /* --------------------------------------------------------------
     * 16. Unsupported InputFormat value
     * -------------------------------------------------------------- */
    @Test
    void unsupportedInputFormat_throwsIllegalArgumentException() {
        // Mock an InputFormat that the validator does not recognise.
        InputFormat unknown = mock(InputFormat.class);
        String json = "{\"a\":1}";
        OutputFormat<Object> format = msgs -> msgs;
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate(json, unknown, format, null));
    }

    /* --------------------------------------------------------------
     * 17. Invalid percent‑encoding – IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void invalidPercentEncoding_throwsIllegalArgumentException() {
        // malformed percent‑encoding
        String badEncoded = "%E4%BD%A0%ZZ";
        OutputFormat<Object> format = msgs -> msgs;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> jsonSchema.validate(badEncoded, InputFormat.JSON, format, null));
        assertTrue(ex.getMessage().contains("Invalid URL"));
    }

    /* --------------------------------------------------------------
     * 18. OutputFormat.apply throws runtime exception
     * -------------------------------------------------------------- */
    @Test
    void outputFormatApplyThrows_exceptionPropagates() {
        String json = "{\"a\":1}";
        OutputFormat<Object> badFormat = new OutputFormat<>() {

            @Override
            public Object apply(JsonNode node, Set<ValidationMessage> msgs) {
                throw new IllegalStateException("Format failure");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> jsonSchema.validate(json, InputFormat.JSON, badFormat, null));
        assertEquals("Format failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     * 19. ExecutionContextCustomizer throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void executionContextCustomizerThrowsNPE_exceptionPropagates() {
        String json = "{\"a\":1}";
        OutputFormat<Object> format = msgs -> msgs;
        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new NullPointerException("Customizer NPE");
        };
        NullPointerException ex = assertThrows(NullPointerException.class, () -> jsonSchema.validate(json, InputFormat.JSON, format, badCustomizer));
        assertEquals("Customizer NPE", ex.getMessage());
    }
}
