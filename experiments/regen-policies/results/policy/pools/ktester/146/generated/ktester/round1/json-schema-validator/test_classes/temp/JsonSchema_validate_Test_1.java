package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    /**
     * The schema used for all tests – an object with a required string property “name”.
     */
    private static JsonSchema schema;

    /**
     * Jackson mapper for creating JsonNode instances from JSON strings.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * ExecutionContext created fresh for each test.
     */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        // Build a JsonSchemaFactory for the 2020‑12 draft.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        // Simple schema: {"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        // Create the JsonSchema instance once – it is thread‑safe.
        schema = factory.getSchema(schemaJson);
    }

    @BeforeEach
    void setupBeforeEach() {
        // A plain ExecutionContext is sufficient for the validation run.
        executionContext = new ExecutionContext();
    }

    /**
     * Creates a {@link JsonSchema} from the supplied JSON string.
     */
    private JsonSchema createSchema(String json) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(json);
    }

    /**
     * Extracts the (protected) {@code schemaLocation} field via reflection – used only for verification.
     */
    private Object getSchemaLocation(JsonSchema js) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("schemaLocation");
        f.setAccessible(true);
        return f.get(js);
    }

    /**
     * Extracts the (protected) {@code evaluationPath} field via reflection – used only for verification.
     */
    private JsonNodePath getEvaluationPath(JsonSchema js) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("evaluationPath");
        f.setAccessible(true);
        return (JsonNodePath) f.get(js);
    }

    /**
     * Verify that the {@code validate(ExecutionContext,JsonNode,JsonNode,JsonNodePath)} method
     * returns a non‑empty error set when the instance does not satisfy the schema.
     */
    @Test
    void validateDetectsTypeMismatch() throws Exception {
        // Input JSON: {"name":1} – the “name” property is a number, but the schema expects a string.
        JsonNode input = MAPPER.readTree("{\"name\":1}");
        // The root node and the instance node are the same for this simple test.
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        // Call the focal method.
        Set<ValidationMessage> errors = schema.validate(executionContext, input, input, rootPath);
        // The type validator should have produced exactly one ValidationMessage.
        assertEquals(1, errors.size(), "Expected a single validation error for type mismatch");
        ValidationMessage msg = errors.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("string"), "Error message should mention the expected string type");
    }

    /**
     * Verify that a valid instance produces an empty error set.
     */
    @Test
    void validateSucceedsWhenInstanceMatchesSchema() throws Exception {
        // Input JSON that satisfies the schema: {"name":"John"}
        JsonNode input = MAPPER.readTree("{\"name\":\"John\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = schema.validate(executionContext, input, input, rootPath);
        assertTrue(errors.isEmpty(), "No validation errors should be reported for a valid instance");
    }

    @Test
    void discriminatorDisabled_skipsAllDiscriminatorLogic() throws Exception {
        // ----- mocks -------------------------------------------------------
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        // a discriminator context exists but must NOT be touched
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // ----- spy the schema to avoid real validators --------------------
        JsonSchema spySchema = Mockito.spy(schema);
        // make validator list empty so that only discriminator logic could run
        doReturn(Collections.emptyList()).when(spySchema).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        // ----- call method ------------------------------------------------
        Set<ValidationMessage> errors = spySchema.validate(execCtx, node, node, rootPath);
        // ----- assertions -------------------------------------------------
        assertTrue(errors.isEmpty(), "No validators → empty error set");
        // discriminator related calls must never happen
        verify(execCtx, never()).getCurrentDiscriminatorContext();
        verify(discCtx, never()).registerDiscriminator(any(), any());
        verify(spySchema, never()).checkDiscriminatorMatch(any(), any(), any(), any());
    }

    @Test
    void discriminatorEnabled_schemaHasNoDiscriminator() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // schema without discriminator
        JsonSchema spySchema = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spySchema).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spySchema.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());
        // No registration nor matching should happen
        // first block guarded by config, but discriminatorNode null => no call
        verify(execCtx, never()).getCurrentDiscriminatorContext();
        verify(discCtx, never()).registerDiscriminator(any(), any());
        verify(spySchema, never()).checkDiscriminatorMatch(any(), any(), any(), any());
    }

    @Test
    void discriminatorPresent_contextIsNull_noRegistrationOrMatch() throws Exception {
        // schema containing a discriminator object
        String schemaWithDisc = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "discriminator": { "propertyName": "type" },
              "properties": { "type": { "type": "string" } }
            }
            """;
        JsonSchema js = createSchema(schemaWithDisc);
        JsonSchema spy = Mockito.spy(js);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        // current discriminator context is null
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(null);
        JsonNode node = MAPPER.readTree("{\"type\":\"car\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());
        // verify that no registration or match is attempted
        verify(execCtx, never()).getCurrentDiscriminatorContext();
        verify(spy, never()).checkDiscriminatorMatch(any(), any(), any(), any());
    }

    @Test
    void discriminatorPresent_contextPresent_discriminatorFromContextNull() throws Exception {
        String schemaWithDisc = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "discriminator": { "propertyName": "type" },
              "properties": { "type": { "type": "string" } }
            }
            """;
        JsonSchema js = createSchema(schemaWithDisc);
        JsonSchema spy = Mockito.spy(js);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        // mock config → enabled
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        // mock discriminator context
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        // null → registration required
        when(discCtx.getDiscriminatorForPath(any())).thenReturn(null);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // make checkDiscriminatorMatch a no‑op so that the test focuses on registration
        doNothing().when(spy).checkDiscriminatorMatch(any(), any(), any(), any());
        JsonNode node = MAPPER.readTree("{\"type\":\"car\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());
        // registration must happen once
        verify(discCtx, times(1)).registerDiscriminator(any(), any());
        // checkDiscriminatorMatch must be invoked with the property value "car"
        verify(spy, times(1)).checkDiscriminatorMatch(eq(discCtx), any(), eq("car"), eq(spy));
    }

    @Test
    void discriminatorPresent_contextPresent_discriminatorFromContextNonNull() throws Exception {
        String schemaWithDisc = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "discriminator": { "propertyName": "type" },
              "properties": { "type": { "type": "string" } }
            }
            """;
        JsonSchema js = createSchema(schemaWithDisc);
        JsonSchema spy = Mockito.spy(js);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        // return a different discriminator (simulating one already registered)
        ObjectNode alreadyRegistered = MAPPER.createObjectNode().put("propertyName", "type");
        when(discCtx.getDiscriminatorForPath(any())).thenReturn(alreadyRegistered);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // make checkDiscriminatorMatch a no‑op
        doNothing().when(spy).checkDiscriminatorMatch(any(), any(), any(), any());
        JsonNode node = MAPPER.readTree("{\"type\":\"bike\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());
        // No registration because a discriminator already exists for this location
        verify(discCtx, never()).registerDiscriminator(any(), any());
        // checkDiscriminatorMatch must be called with the value from the instance
        verify(spy, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(alreadyRegistered), eq("bike"), eq(spy));
    }

    @Test
    void validatorReturnsErrors_aggregatesAndMarksFailure() throws Exception {
        // mock config → discriminator disabled (to focus on validator branch)
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        Results results = mock(Results.class);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getResults()).thenReturn(results);
        // mock a validator that returns a non‑empty set
        JsonValidator validator = mock(JsonValidator.class);
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$").message("forced error").build();
        when(validator.validate(any(), any(), any(), any())).thenReturn(Set.of(vm));
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertEquals(1, errors.size());
        assertTrue(errors.contains(vm));
        // verify that the failure is recorded
        verify(results, times(1)).setResult(eq(rootPath), any(), any(), eq(false));
    }

    @Test
    void allValidatorsReturnNullOrEmpty_noFailureRecorded() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        Results results = mock(Results.class);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getResults()).thenReturn(results);
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        when(v1.validate(any(), any(), any(), any())).thenReturn(null);
        when(v2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(v1, v2)).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());
        // setResult must NOT be called because there were no errors
        verify(results, never()).setResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void multipleValidatorsMixedErrors_aggregatesOnlyNonEmpty() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        Results results = mock(Results.class);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getResults()).thenReturn(results);
        // v1 returns errors, v2 returns null, v3 returns empty
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        JsonValidator v3 = mock(JsonValidator.class);
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$").message("error from v1").build();
        when(v1.validate(any(), any(), any(), any())).thenReturn(Set.of(vm));
        when(v2.validate(any(), any(), any(), any())).thenReturn(null);
        when(v3.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(v1, v2, v3)).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertEquals(1, errors.size());
        assertTrue(errors.contains(vm));
        verify(results, times(1)).setResult(eq(rootPath), any(), any(), eq(false));
    }

    @Test
    void discriminatorPresent_missingPropertyInInstance_callsMatchWithNull() throws Exception {
        String schemaWithDisc = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "discriminator": { "propertyName": "type" },
              "properties": { "type": { "type": "string" } }
            }
            """;
        JsonSchema js = createSchema(schemaWithDisc);
        JsonSchema spy = Mockito.spy(js);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        // forces registration
        when(discCtx.getDiscriminatorForPath(any())).thenReturn(null);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        doNothing().when(spy).checkDiscriminatorMatch(any(), any(), any(), any());
        // instance without "type" property
        JsonNode node = MAPPER.readTree("{\"other\":\"value\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());
        // verify registration and that match is called with null property value
        verify(discCtx, times(1)).registerDiscriminator(any(), any());
        verify(spy, times(1)).checkDiscriminatorMatch(eq(discCtx), any(), isNull(), eq(spy));
    }

    @Test
    void jsonNodeNull_throwsNullPointerException() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        JsonSchema spy = Mockito.spy(schema);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> spy.validate(execCtx, null, null, rootPath), "When jsonNode is null the method should propagate NPE");
    }

    @Test
    void validatorThrowsRuntimeException_propagates() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        JsonValidator badValidator = mock(JsonValidator.class);
        when(badValidator.validate(any(), any(), any(), any())).thenThrow(new RuntimeException("boom"));
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(badValidator)).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> spy.validate(execCtx, node, node, rootPath));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void discriminatorNodeNotObjectNode_throwsClassCastException() throws Exception {
        // discriminator defined as a string (invalid per spec)
        String badSchema = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "discriminator": "not-an-object"
            }
            """;
        JsonSchema js = createSchema(badSchema);
        JsonSchema spy = Mockito.spy(js);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        JsonNode node = MAPPER.readTree("{\"type\":\"car\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(ClassCastException.class, () -> spy.validate(execCtx, node, node, rootPath));
    }

    @Test
    void discriminatorWithoutPropertyName_throwsNullPointerException() throws Exception {
        String schemaMissingPropName = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "discriminator": { "mapping": {} }
            }
            """;
        JsonSchema js = createSchema(schemaMissingPropName);
        JsonSchema spy = Mockito.spy(js);
        doReturn(Collections.emptyList()).when(spy).getValidators();
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(true);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.getDiscriminatorForPath(any())).thenReturn(null);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        JsonNode node = MAPPER.readTree("{\"type\":\"car\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> spy.validate(execCtx, node, node, rootPath));
    }

    @Test
    void executionContextNullValidationContext_throwsNullPointerException() throws Exception {
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(null);
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> schema.validate(execCtx, node, node, rootPath));
    }

    @Test
    void executionContextNullConfig_throwsNullPointerException() throws Exception {
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(null);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> schema.validate(execCtx, node, node, rootPath));
    }

    @Test
    void executionContextNullResults_throwsNullPointerExceptionOnSetResult() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        // Results missing
        when(execCtx.getResults()).thenReturn(null);
        JsonValidator validator = mock(JsonValidator.class);
        ValidationMessage vm = ValidationMessage.builder().type("type").path("$").message("error").build();
        when(validator.validate(any(), any(), any(), any())).thenReturn(Set.of(vm));
        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> spy.validate(execCtx, node, node, rootPath));
    }

    @Test
    void getValidatorsReturnsNull_throwsNullPointerException() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getResults()).thenReturn(mock(Results.class));
        JsonSchema spy = Mockito.spy(schema);
        // force null
        doReturn(null).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> spy.validate(execCtx, node, node, rootPath));
    }

    @Test
    void validatorsContainNull_throwsNullPointerException() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getResults()).thenReturn(mock(Results.class));
        JsonSchema spy = Mockito.spy(schema);
        // list with a null entry
        doReturn(List.of((JsonValidator) null)).when(spy).getValidators();
        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        assertThrows(NullPointerException.class, () -> spy.validate(execCtx, node, node, rootPath));
    }
}
