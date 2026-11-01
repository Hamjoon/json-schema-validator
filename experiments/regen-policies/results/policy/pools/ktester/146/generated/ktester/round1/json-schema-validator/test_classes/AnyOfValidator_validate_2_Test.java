package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The validator under test – obtained from a schema that contains an {@code anyOf}.
     */
    private AnyOfValidator anyOfValidator;

    /**
     * Execution context used for all invocations.
     */
    private ExecutionContext executionContext;

    /**
     * Instance location – a simple JSON path used for the calls.
     */
    private JsonNodePath instanceLocation;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1️⃣ Build a schema that contains an anyOf with two trivial subschemas.
        String schemaJson = """
            {
              "anyOf": [
                { "type": "string" },
                { "type": "integer" }
              ]
            }
            """;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        JsonSchema schema = factory.getSchema(schemaJson);
        // 2️⃣ Extract the AnyOfValidator from the list of validators.
        this.anyOfValidator = (AnyOfValidator) schema.getValidators().stream().filter(v -> v instanceof AnyOfValidator).findFirst().orElseThrow(() -> new IllegalStateException("AnyOfValidator not found"));
        // 3️⃣ Fresh ExecutionContext for each test (will be replaced by mocks in many tests).
        this.executionContext = new ExecutionContext();
        // 4️⃣ Simple instance location (root path).
        this.instanceLocation = new JsonNodePath(JsonNodePath.PathType.JSON);
    }

    /**
     * Replaces the internal {@code schemas} list of the validator with the given list.
     */
    private void setSchemas(List<JsonSchema> schemas) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(anyOfValidator, schemas);
    }

    /**
     * Replaces the internal {@code validationContext} with a mock that returns the
     * supplied {@code config} when {@code getConfig()} is called.
     */
    private void setValidationContextConfig(Object config) throws Exception {
        // ValidationContext is a protected field in BaseJsonValidator.
        Field ctxField = BaseJsonValidator.class.getDeclaredField("validationContext");
        ctxField.setAccessible(true);
        ValidationContext vcMock = mock(ValidationContext.class);
        when(vcMock.getConfig()).thenReturn(config);
        ctxField.set(anyOfValidator, vcMock);
    }

    /**
     * When the supplied node matches one of the subschemas (e.g. a string), the
     * validator must return an empty error set irrespective of the {@code walk}
     * flag.
     */
    @Test
    void validate_StringNode_ReturnsEmptySet() throws Exception {
        // a JSON string
        JsonNode node = MAPPER.readTree("\"hello\"");
        // root is the same for this simple test
        JsonNode root = node;
        Set<ValidationMessage> messages = invokeValidate(node, root, false);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a matching string node");
        // also verify that the same result is obtained when walk = true
        Set<ValidationMessage> messagesWalk = invokeValidate(node, root, true);
        assertTrue(messagesWalk.isEmpty(), "Expected no validation messages for a matching string node when walk=true");
    }

    /**
     * When the supplied node matches none of the subschemas (e.g. a boolean),
     * the validator must return a non‑empty error set containing the errors from
     * each subschema.
     */
    @Test
    void validate_BooleanNode_ReturnsErrorsFromAllSubschemas() throws Exception {
        // a JSON boolean – does not match string or integer
        JsonNode node = MAPPER.readTree("true");
        JsonNode root = node;
        Set<ValidationMessage> messages = invokeValidate(node, root, false);
        assertFalse(messages.isEmpty(), "Expected validation messages for a non‑matching boolean node");
        // The anyOf schema contains two subschemas, therefore we should have at least two messages.
        // The exact number may vary (some implementations combine messages), so we only assert >=2.
        long count = messages.stream().filter(m -> "type".equals(m.getMessageKey())).count();
        assertTrue(count >= 2, "Expected at least two type‑mismatch messages, got: " + count);
    }

    /**
     * Helper that invokes the protected {@code validate(..., boolean)} method via reflection.
     *
     * @param node   node to validate
     * @param root   root node (same as {@code node} for these simple cases)
     * @param walk   value for the {@code walk} flag
     * @return the set of {@link ValidationMessage}s returned by the validator
     */
    @SuppressWarnings("unchecked")
    private Set<ValidationMessage> invokeValidate(JsonNode node, JsonNode root, boolean walk) throws Exception {
        Method m = AnyOfValidator.class.getDeclaredMethod("validate", ExecutionContext.class, JsonNode.class, JsonNode.class, JsonNodePath.class, boolean.class);
        m.setAccessible(true);
        Object result = m.invoke(anyOfValidator, executionContext, node, root, instanceLocation, walk);
        return (Set<ValidationMessage>) result;
    }

    /* --------------------------------------------------------------------- *
     *  1. Discriminator Disabled – Simple Walk = false, all schemas invalid
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorDisabled_SimpleWalkFalse_AllSchemasInvalid() throws Exception {
        // ---- mocks ---------------------------------------------------------
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        // both schemas return a non‑empty error set when validate() is called
        ValidationMessage msg1 = mock(ValidationMessage.class);
        ValidationMessage msg2 = mock(ValidationMessage.class);
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Set.of(msg1));
        when(schema2.validate(any(), any(), any(), any())).thenReturn(Set.of(msg2));
        // no type validator (null) – the validator will go straight to validate()
        when(schema1.getTypeValidator()).thenReturn(null);
        when(schema2.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(schema1, schema2));
        // discriminator disabled – config returns false
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        // ---- input ---------------------------------------------------------
        // a string – does NOT match integer schema
        JsonNode node = MAPPER.readTree("\"foo\"");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        // both error messages must be present
        assertEquals(2, result.size(), "Both schema error messages should be accumulated");
    }

    /* --------------------------------------------------------------------- *
     *  2. Discriminator Disabled – Short‑circuit enabled
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorDisabled_ShortCircuitEnabled() throws Exception {
        // Create a subclass that forces short‑circuit to be true
        AnyOfValidator shortCircuitValidator = new AnyOfValidator(anyOfValidator.validationContext) {

            @Override
            protected boolean canShortCircuit() {
                return true;
            }

            @Override
            protected boolean canShortCircuit(ExecutionContext ctx) {
                return true;
            }
        };
        // replace the field reference used by the test helpers
        this.anyOfValidator = shortCircuitValidator;
        // mock a single schema that validates successfully (empty set)
        JsonSchema okSchema = mock(JsonSchema.class);
        when(okSchema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(okSchema.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(okSchema));
        // discriminator disabled
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        // integer
        JsonNode node = MAPPER.readTree("5");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, true);
        assertTrue(result.isEmpty(), "Short‑circuit should return an empty set after first success");
    }

    /* --------------------------------------------------------------------- *
     *  3. Discriminator Disabled – Valid subschema after several failures
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorDisabled_ValidAfterFailures() throws Exception {
        // three schemas: first two fail, third succeeds
        JsonSchema fail1 = mock(JsonSchema.class);
        JsonSchema fail2 = mock(JsonSchema.class);
        JsonSchema success = mock(JsonSchema.class);
        when(fail1.validate(any(), any(), any(), any())).thenReturn(Set.of(mock(ValidationMessage.class)));
        when(fail2.validate(any(), any(), any(), any())).thenReturn(Set.of(mock(ValidationMessage.class)));
        when(success.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(fail1.getTypeValidator()).thenReturn(null);
        when(fail2.getTypeValidator()).thenReturn(null);
        when(success.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(fail1, fail2, success));
        // discriminator disabled
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        JsonNode node = MAPPER.readTree("{\"type\":\"A\"}");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        assertTrue(result.isEmpty(), "A successful subschema should cause an empty result set");
    }

    /* --------------------------------------------------------------------- *
     *  4. Discriminator Enabled – Match found with errors
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorEnabled_MatchFoundWithErrors() throws Exception {
        // mock execution context with discriminator enabled
        ExecutionContext ctx = mock(ExecutionContext.class);
        this.executionContext = ctx;
        // mock discriminator context
        DiscriminatorContext dctx = mock(DiscriminatorContext.class);
        when(dctx.isDiscriminatorMatchFound()).thenReturn(true);
        when(dctx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dctx);
        when(ctx.isFailFast()).thenReturn(false);
        doNothing().when(ctx).setFailFast(anyBoolean());
        // config says discriminator enabled
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(true);
        setValidationContextConfig(config);
        // a single schema that returns a non‑empty error set
        JsonSchema schema = mock(JsonSchema.class);
        ValidationMessage err = mock(ValidationMessage.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(Set.of(err));
        when(schema.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(schema));
        JsonNode node = MAPPER.readTree("true");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, true);
        // the method should return the original error set (size 1)
        assertEquals(1, result.size(), "When discriminator match has errors, original errors are returned");
    }

    /* --------------------------------------------------------------------- *
     *  5. Discriminator Enabled – Match found without errors
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorEnabled_MatchFoundWithoutErrors() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        this.executionContext = ctx;
        DiscriminatorContext dctx = mock(DiscriminatorContext.class);
        when(dctx.isDiscriminatorMatchFound()).thenReturn(true);
        when(dctx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dctx);
        when(ctx.isFailFast()).thenReturn(false);
        doNothing().when(ctx).setFailFast(anyBoolean());
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(true);
        setValidationContextConfig(config);
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(schema.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(schema));
        JsonNode node = MAPPER.readTree("\"any\"");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        assertTrue(result.isEmpty(), "No errors should be returned when discriminator match validates cleanly");
    }

    /* --------------------------------------------------------------------- *
     *  6. Discriminator Enabled – Ignore flag
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorEnabled_IgnoreFlag() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        this.executionContext = ctx;
        DiscriminatorContext dctx = mock(DiscriminatorContext.class);
        when(dctx.isDiscriminatorMatchFound()).thenReturn(false);
        when(dctx.isDiscriminatorIgnore()).thenReturn(true);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dctx);
        when(ctx.isFailFast()).thenReturn(false);
        doNothing().when(ctx).setFailFast(anyBoolean());
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(true);
        setValidationContextConfig(config);
        // schema that produces an error set
        JsonSchema schema = mock(JsonSchema.class);
        ValidationMessage err = mock(ValidationMessage.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(Set.of(err));
        when(schema.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(schema));
        JsonNode node = MAPPER.readTree("123");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, true);
        // because ignore flag is true we get the same behaviour as “match‑found” branch
        assertEquals(1, result.size(), "When ignore flag is set and errors exist, they are returned");
    }

    /* --------------------------------------------------------------------- *
     *  7. Discriminator Enabled – No match after loop
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorEnabled_NoMatchAfterLoop() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        this.executionContext = ctx;
        DiscriminatorContext dctx = mock(DiscriminatorContext.class);
        when(dctx.isActive()).thenReturn(true);
        when(dctx.isDiscriminatorMatchFound()).thenReturn(false);
        when(dctx.isDiscriminatorIgnore()).thenReturn(false);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dctx);
        when(ctx.isFailFast()).thenReturn(false);
        doNothing().when(ctx).setFailFast(anyBoolean());
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(true);
        setValidationContextConfig(config);
        // two schemas that both fail
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        ValidationMessage e1 = mock(ValidationMessage.class);
        ValidationMessage e2 = mock(ValidationMessage.class);
        when(s1.validate(any(), any(), any(), any())).thenReturn(Set.of(e1));
        when(s2.validate(any(), any(), any(), any())).thenReturn(Set.of(e2));
        when(s1.getTypeValidator()).thenReturn(null);
        when(s2.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(s1, s2));
        JsonNode node = MAPPER.readTree("{\"noDiscriminator\":true}");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        // expect the special discriminator‑no‑alternative message (single entry)
        assertEquals(1, result.size(), "When no discriminator alternative is found, a single message is returned");
        ValidationMessage only = result.iterator().next();
        assertTrue(only.getMessage().contains("based on the provided discriminator"), "Message should contain the expected text");
    }

    /* --------------------------------------------------------------------- *
     *  8. TypeValidator mismatch (non‑UNION)
     * --------------------------------------------------------------------- */
    @Test
    void validate_TypeValidatorMismatch_NonUnion() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        TypeValidator tv = mock(TypeValidator.class);
        // pretend the schema expects STRING, node is BOOLEAN
        when(tv.getSchemaType()).thenReturn(JsonType.STRING);
        when(tv.equalsToSchemaType(any())).thenReturn(false);
        ValidationMessage typeMsg = mock(ValidationMessage.class);
        when(tv.validate(any(), any(), any(), any())).thenReturn(Set.of(typeMsg));
        when(schema.getTypeValidator()).thenReturn(tv);
        // validate() should never be called because of early continue
        when(schema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        setSchemas(List.of(schema));
        // discriminator disabled
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        // BOOLEAN
        JsonNode node = MAPPER.readTree("true");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        assertEquals(1, result.size(), "Only the type‑validator message should be present");
        assertSame(typeMsg, result.iterator().next(), "Returned message must be the one from TypeValidator");
    }

    /* --------------------------------------------------------------------- *
     *  9. TypeValidator UNION (always evaluated)
     * --------------------------------------------------------------------- */
    @Test
    void validate_TypeValidatorUnion_AlwaysEvaluated() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        TypeValidator tv = mock(TypeValidator.class);
        when(tv.getSchemaType()).thenReturn(JsonType.UNION);
        // even if equalsToSchemaType returns false, UNION forces evaluation
        when(tv.equalsToSchemaType(any())).thenReturn(false);
        // underlying schema validation returns empty set (valid)
        when(schema.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(schema.getTypeValidator()).thenReturn(tv);
        setSchemas(List.of(schema));
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        // integer
        JsonNode node = MAPPER.readTree("10");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        assertTrue(result.isEmpty(), "UNION type validator should not block validation");
    }

    /* --------------------------------------------------------------------- *
     * 10. Fail‑Fast flag restoration
     * --------------------------------------------------------------------- */
    @Test
    void validate_FailFastFlagRestoration() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.isFailFast()).thenReturn(true);
        // capture the value set back in the finally block
        doAnswer(invocation -> {
            boolean val = invocation.getArgument(0);
            // we expect it to be set to false during processing
            assertFalse(val, "failFast must be set to false while evaluating subschemas");
            return null;
        }).when(ctx).setFailFast(anyBoolean());
        this.executionContext = ctx;
        // one schema that validates successfully
        JsonSchema ok = mock(JsonSchema.class);
        when(ok.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(ok.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(ok));
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        JsonNode node = MAPPER.readTree("\"any\"");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, false);
        assertTrue(result.isEmpty(), "Result should be empty");
        // after the call the original flag must still be true
        verify(ctx, times(1)).isFailFast();
        verify(ctx, times(1)).setFailFast(false);
    }

    /* --------------------------------------------------------------------- *
     * 11. Discriminator Disabled – Walk = true, all schemas invalid
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorDisabled_WalkTrue_AllSchemasInvalid() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        ValidationMessage m1 = mock(ValidationMessage.class);
        ValidationMessage m2 = mock(ValidationMessage.class);
        when(s1.walk(any(), any(), any(), any(), eq(true))).thenReturn(Set.of(m1));
        when(s2.walk(any(), any(), any(), any(), eq(true))).thenReturn(Set.of(m2));
        when(s1.getTypeValidator()).thenReturn(null);
        when(s2.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(s1, s2));
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        JsonNode node = MAPPER.readTree("\"invalid\"");
        JsonNode root = node;
        Set<ValidationMessage> result = invokeValidate(node, root, true);
        assertEquals(2, result.size(), "Both walk‑generated messages must be accumulated");
    }

    /* --------------------------------------------------------------------- *
     * 12. Null ExecutionContext → NPE
     * --------------------------------------------------------------------- */
    @Test
    void validate_NullExecutionContext_ThrowsNPE() throws Exception {
        // deliberately null
        this.executionContext = null;
        JsonNode node = MAPPER.readTree("\"test\"");
        JsonNode root = node;
        assertThrows(NullPointerException.class, () -> invokeValidate(node, root, false), "Passing null ExecutionContext must cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * 13. Null ValidationContext config → NPE
     * --------------------------------------------------------------------- */
    @Test
    void validate_NullValidationContextConfig_ThrowsNPE() throws Exception {
        // validationContext mock returns null config
        ValidationContext vc = mock(ValidationContext.class);
        when(vc.getConfig()).thenReturn(null);
        Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(anyOfValidator, vc);
        JsonNode node = MAPPER.readTree("123");
        JsonNode root = node;
        assertThrows(NullPointerException.class, () -> invokeValidate(node, root, false), "Null config should cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * 14. Null schemas list → NPE
     * --------------------------------------------------------------------- */
    @Test
    void validate_NullSchemasList_ThrowsNPE() throws Exception {
        // force null list
        setSchemas(null);
        JsonNode node = MAPPER.readTree("\"any\"");
        JsonNode root = node;
        assertThrows(NullPointerException.class, () -> invokeValidate(node, root, false), "Null schemas list should cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * 15. Schemas list contains null element → NPE
     * --------------------------------------------------------------------- */
    @Test
    void validate_SchemasListContainsNullElement_ThrowsNPE() throws Exception {
        JsonSchema good = mock(JsonSchema.class);
        when(good.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(good.getTypeValidator()).thenReturn(null);
        setSchemas(Arrays.asList(good, null, good));
        JsonNode node = MAPPER.readTree("\"any\"");
        JsonNode root = node;
        assertThrows(NullPointerException.class, () -> invokeValidate(node, root, false), "Null element inside schemas list should cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * 16. Discriminator enabled but getCurrentDiscriminatorContext() returns null → NPE
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorEnabled_NoDiscriminatorContext_ThrowsNPE() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(null);
        this.executionContext = ctx;
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(true);
        setValidationContextConfig(config);
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(Set.of(mock(ValidationMessage.class)));
        when(schema.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(schema));
        JsonNode node = MAPPER.readTree("true");
        JsonNode root = node;
        assertThrows(NullPointerException.class, () -> invokeValidate(node, root, false), "Missing DiscriminatorContext should cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * 17. DiscriminatorContext methods throw RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void validate_DiscriminatorContextMethodsThrowRuntimeException() throws Exception {
        ExecutionContext ctx = mock(ExecutionContext.class);
        DiscriminatorContext dctx = mock(DiscriminatorContext.class);
        when(dctx.isDiscriminatorMatchFound()).thenThrow(new RuntimeException("boom"));
        when(ctx.getCurrentDiscriminatorContext()).thenReturn(dctx);
        this.executionContext = ctx;
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(true);
        setValidationContextConfig(config);
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(Set.of(mock(ValidationMessage.class)));
        when(schema.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(schema));
        JsonNode node = MAPPER.readTree("123");
        JsonNode root = node;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeValidate(node, root, false), "RuntimeException from DiscriminatorContext should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     * 18. Subschema.validate throws IllegalStateException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void validate_SchemaValidateThrowsException() throws Exception {
        JsonSchema bad = mock(JsonSchema.class);
        when(bad.validate(any(), any(), any(), any())).thenThrow(new IllegalStateException("bad schema"));
        when(bad.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(bad));
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        JsonNode node = MAPPER.readTree("\"any\"");
        JsonNode root = node;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> invokeValidate(node, root, false), "Exception from schema.validate must propagate");
        assertEquals("bad schema", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     * 19. Subschema.walk throws RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void validate_SchemaWalkThrowsException() throws Exception {
        JsonSchema bad = mock(JsonSchema.class);
        when(bad.walk(any(), any(), any(), any(), eq(true))).thenThrow(new RuntimeException("walk fail"));
        when(bad.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(bad));
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        JsonNode node = MAPPER.readTree("123");
        JsonNode root = node;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeValidate(node, root, true), "Exception from schema.walk must propagate");
        assertEquals("walk fail", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     * 20. canShortCircuit methods throw RuntimeException → exception propagates
     * --------------------------------------------------------------------- */
    @Test
    void validate_CanShortCircuitThrowsException() throws Exception {
        // Subclass that throws from canShortCircuit()
        AnyOfValidator throwingValidator = new AnyOfValidator(anyOfValidator.validationContext) {

            @Override
            protected boolean canShortCircuit() {
                throw new RuntimeException("short‑circuit fail");
            }
        };
        this.anyOfValidator = throwingValidator;
        JsonSchema ok = mock(JsonSchema.class);
        when(ok.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(ok.getTypeValidator()).thenReturn(null);
        setSchemas(List.of(ok));
        Object config = mock(Object.class);
        when(((com.networknt.schema.ValidationContext.Config) config).isDiscriminatorKeywordEnabled()).thenReturn(false);
        setValidationContextConfig(config);
        JsonNode node = MAPPER.readTree("5");
        JsonNode root = node;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeValidate(node, root, false), "Exception from canShortCircuit must propagate");
        assertEquals("short‑circuit fail", ex.getMessage());
    }
}
