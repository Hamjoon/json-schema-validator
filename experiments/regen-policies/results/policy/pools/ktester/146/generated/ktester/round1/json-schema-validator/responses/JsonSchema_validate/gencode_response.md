```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Behaviour when the discriminator feature is disabled.</li>
 *   <li>All branches of the discriminator handling logic (registration, matching, missing data, error cases).</li>
 *   <li>Aggregation of validator results and interaction with {@link Results#setResult(...)}.</li>
 *   <li>Propagation of unchecked exceptions and NPEs that the method does not guard against.</li>
 * </ul>
 *
 * <p>Mocks are used for {@link ExecutionContext}, {@link ValidationContext},
 * {@link Config}, {@link DiscriminatorContext}, {@link Results} and {@link JsonValidator}.
 *
 * <p>When a schema containing a {@code discriminator} keyword is required,
 * a fresh {@link JsonSchema} instance is created inside the test method
 * (the static {@code schema} field is used for the simple “name” schema only).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    /** Simple schema used by the original tests – an object with a required string property “name”. */
    private static JsonSchema schema;

    /** Jackson mapper for creating {@link JsonNode}s from JSON strings. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Fresh {@link ExecutionContext} (or its mock) for each test. */
    private ExecutionContext executionContext;

    @BeforeAll
    static void setupBeforeAll() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
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
        schema = factory.getSchema(schemaJson);
    }

    @BeforeEach
    void setupBeforeEach() {
        executionContext = new ExecutionContext(); // default real instance; may be replaced by a mock in a test
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (type mismatch / success) – kept unchanged.   */
    /* --------------------------------------------------------------------- */

    @Test
    void validateDetectsTypeMismatch() throws Exception {
        JsonNode input = MAPPER.readTree("{\"name\":1}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        var errors = schema.validate(executionContext, input, input, rootPath);
        assertEquals(1, errors.size(), "Expected a single validation error for type mismatch");
        ValidationMessage msg = errors.iterator().next();
        assertTrue(msg.getMessage().toLowerCase().contains("string"),
                "Error message should mention the expected string type");
    }

    @Test
    void validateSucceedsWhenInstanceMatchesSchema() throws Exception {
        JsonNode input = MAPPER.readTree("{\"name\":\"John\"}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
        var errors = schema.validate(executionContext, input, input, rootPath);
        assertTrue(errors.isEmpty(), "No validation errors should be reported for a valid instance");
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                   */
    /* --------------------------------------------------------------------- */

    /** Creates a {@link JsonSchema} from the supplied JSON string. */
    private JsonSchema createSchema(String json) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        return factory.getSchema(json);
    }

    /** Extracts the (protected) {@code schemaLocation} field via reflection – used only for verification. */
    private Object getSchemaLocation(JsonSchema js) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("schemaLocation");
        f.setAccessible(true);
        return f.get(js);
    }

    /** Extracts the (protected) {@code evaluationPath} field via reflection – used only for verification. */
    private JsonNodePath getEvaluationPath(JsonSchema js) throws Exception {
        Field f = BaseJsonValidator.class.getDeclaredField("evaluationPath");
        f.setAccessible(true);
        return (JsonNodePath) f.get(js);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Discriminator disabled – the method must skip all discriminator logic */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* 2. Discriminator enabled – schema has no discriminator node            */
    /* --------------------------------------------------------------------- */

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

        JsonSchema spySchema = Mockito.spy(schema); // schema without discriminator
        doReturn(Collections.emptyList()).when(spySchema).getValidators();

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        Set<ValidationMessage> errors = spySchema.validate(execCtx, node, node, rootPath);
        assertTrue(errors.isEmpty());

        // No registration nor matching should happen
        verify(execCtx, never()).getCurrentDiscriminatorContext(); // first block guarded by config, but discriminatorNode null => no call
        verify(discCtx, never()).registerDiscriminator(any(), any());
        verify(spySchema, never()).checkDiscriminatorMatch(any(), any(), any(), any());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Discriminator present, current context is null                       */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* 4. Discriminator present, context present, discriminatorFromContext null */
    /* --------------------------------------------------------------------- */

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
        when(discCtx.getDiscriminatorForPath(any())).thenReturn(null); // null → registration required

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

    /* --------------------------------------------------------------------- */
    /* 5. Discriminator present, context present, discriminatorFromContext non‑null */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* 6. Validator returns non‑empty error set – setResult must be invoked */
    /* --------------------------------------------------------------------- */

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
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$")
                .message("forced error")
                .build();
        when(validator.validate(any(), any(), any(), any())).thenReturn(Set.of(vm));

        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        Set<ValidationMessage> errors = spy.validate(execCtx, node, node, rootPath);
        assertEquals(1, errors.size());
        assertTrue(errors.contains(vm));

        // verify that the failure is recorded
        verify(results, times(1)).setResult(eq(rootPath),
                any(), any(), eq(false));
    }

    /* --------------------------------------------------------------------- */
    /* 7. All validators return null/empty – no failure recorded */
    /* --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- */
    /* 8. Multiple validators – mixed error sets */
    /* --------------------------------------------------------------------- */

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
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$")
                .message("error from v1")
                .build();
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

    /* --------------------------------------------------------------------- */
    /* 9. Discriminator present – instance missing the discriminator property */
    /* --------------------------------------------------------------------- */

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
        when(discCtx.getDiscriminatorForPath(any())).thenReturn(null); // forces registration
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

    /* --------------------------------------------------------------------- */
    /* 10. jsonNode is null – NPE expected */
    /* --------------------------------------------------------------------- */

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
        assertThrows(NullPointerException.class,
                () -> spy.validate(execCtx, null, null, rootPath),
                "When jsonNode is null the method should propagate NPE");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Validator throws RuntimeException – should propagate */
    /* --------------------------------------------------------------------- */

    @Test
    void validatorThrowsRuntimeException_propagates() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);

        JsonValidator badValidator = mock(JsonValidator.class);
        when(badValidator.validate(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(badValidator)).when(spy).getValidators();

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spy.validate(execCtx, node, node, rootPath));
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 12. Discriminator node not an ObjectNode – ClassCastException */
    /* --------------------------------------------------------------------- */

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

        assertThrows(ClassCastException.class,
                () -> spy.validate(execCtx, node, node, rootPath));
    }

    /* --------------------------------------------------------------------- */
    /* 13. Discriminator without propertyName – NullPointerException */
    /* --------------------------------------------------------------------- */

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

        assertThrows(NullPointerException.class,
                () -> spy.validate(execCtx, node, node, rootPath));
    }

    /* --------------------------------------------------------------------- */
    /* 14. ExecutionContext with null ValidationContext – NPE */
    /* --------------------------------------------------------------------- */

    @Test
    void executionContextNullValidationContext_throwsNullPointerException() throws Exception {
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(null);

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        assertThrows(NullPointerException.class,
                () -> schema.validate(execCtx, node, node, rootPath));
    }

    /* --------------------------------------------------------------------- */
    /* 15. ExecutionContext with null Config – NPE */
    /* --------------------------------------------------------------------- */

    @Test
    void executionContextNullConfig_throwsNullPointerException() throws Exception {
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(null);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        assertThrows(NullPointerException.class,
                () -> schema.validate(execCtx, node, node, rootPath));
    }

    /* --------------------------------------------------------------------- */
    /* 16. ExecutionContext with null Results – NPE when errors exist */
    /* --------------------------------------------------------------------- */

    @Test
    void executionContextNullResults_throwsNullPointerExceptionOnSetResult() throws Exception {
        Config config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);
        ValidationContext vctx = mock(ValidationContext.class);
        when(vctx.getConfig()).thenReturn(config);
        ExecutionContext execCtx = mock(ExecutionContext.class);
        when(execCtx.getValidationContext()).thenReturn(vctx);
        when(execCtx.getResults()).thenReturn(null); // Results missing

        JsonValidator validator = mock(JsonValidator.class);
        ValidationMessage vm = ValidationMessage.builder()
                .type("type")
                .path("$")
                .message("error")
                .build();
        when(validator.validate(any(), any(), any(), any())).thenReturn(Set.of(vm));

        JsonSchema spy = Mockito.spy(schema);
        doReturn(List.of(validator)).when(spy).getValidators();

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        assertThrows(NullPointerException.class,
                () -> spy.validate(execCtx, node, node, rootPath));
    }

    /* --------------------------------------------------------------------- */
    /* 17. JsonSchema.getValidators returns null – NPE */
    /* --------------------------------------------------------------------- */

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
        doReturn(null).when(spy).getValidators(); // force null

        JsonNode node = MAPPER.readTree("{}");
        JsonNodePath rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);

        assertThrows(NullPointerException.class,
                () -> spy.validate(execCtx, node, node, rootPath));
    }

    /* --------------------------------------------------------------------- */
    /* 18. JsonSchema.getValidators contains a null validator – NPE */
    /* --------------------------------------------------------------------- */

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

        assertThrows(NullPointerException.class,
                () -> spy.validate(execCtx, node, node, rootPath));
    }
}
```