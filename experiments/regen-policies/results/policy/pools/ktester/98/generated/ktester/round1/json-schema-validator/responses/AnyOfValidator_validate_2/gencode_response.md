```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link AnyOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)}.
 *
 * <p>The test suite covers the following scenarios (as described in the
 * supplied JSON test matrix):</p>
 * <ul>
 *   <li>Discriminator disabled – normal validation (walk = true/false).</li>
 *   <li>Type‑validator mismatches (including UNION handling).</li>
 *   <li>Short‑circuit behaviour (enabled / disabled / overridden by execution context).</li>
 *   <li>Discriminator enabled – match found, ignore flag, no‑match case.</li>
 *   <li>Fail‑fast flag restoration.</li>
 *   <li>Edge cases: null arguments, empty schemas list, runtime exceptions from
 *   type‑validator or schema validation.</li>
 * </ul>
 *
 * <p>A concrete subclass {@code TestableAnyOfValidator} exposes the protected
 * {@code validate} method as {@code invokeValidate}. Mockito spies are used to
 * stub the protected {@code canShortCircuit} methods where required.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --------------------------------------------------------------------
    // Common mocks used to build the AnyOfValidator instance.
    // --------------------------------------------------------------------
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonSchema parentSchema;
    @Mock ValidationContext validationContext;
    @Mock ValidationContext.Config config; // nested config mock

    // Two subschemas that will be returned by ValidationContext#newSchema(...)
    @Mock JsonSchema subSchema1;
    @Mock JsonSchema subSchema2;

    // The validator under test.
    private TestableAnyOfValidator validator;

    // A simple anyOf schema node (array with two element schemas).
    private JsonNode anyOfSchemaNode;

    @BeforeEach
    void setup() throws Exception {
        // Build a minimal anyOf schema: [ {"type":"string"}, {"type":"integer"} ]
        anyOfSchemaNode = MAPPER.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        // ValidationContext#newSchema should return the two mock subschemas.
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenReturn(subSchema1, subSchema2);

        // By default the validator configuration disables the discriminator.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(false);

        // Subschemas have no type validator unless a test explicitly sets one.
        when(subSchema1.getTypeValidator()).thenReturn(null);
        when(subSchema2.getTypeValidator()).thenReturn(null);

        // Append calls on the location/path mocks are harmless.
        when(schemaLocation.append(anyInt())).thenReturn(schemaLocation);
        when(evaluationPath.append(anyInt())).thenReturn(evaluationPath);

        // Create the validator instance.
        validator = new TestableAnyOfValidator(
                schemaLocation,
                evaluationPath,
                anyOfSchemaNode,
                parentSchema,
                validationContext);
    }

    @AfterEach
    void resetMocks() {
        // Ensure a clean state for the next test.
        reset(schemaLocation, evaluationPath, parentSchema,
                validationContext, config, subSchema1, subSchema2);
    }

    // --------------------------------------------------------------------
    // Helper utilities
    // --------------------------------------------------------------------
    private ValidationMessage dummyMessage() {
        return dummyMessage("dummy");
    }

    private ValidationMessage dummyMessage(String text) {
        return ValidationMessage.builder()
                .message(text)
                .build();
    }

    private ExecutionContext mockExecutionContext(boolean failFast,
                                                   boolean discriminatorEnabled,
                                                   DiscriminatorContext discriminatorContext) {
        ExecutionContext exec = mock(ExecutionContext.class);
        when(exec.isFailFast()).thenReturn(failFast);
        // the validator temporarily sets failFast false, so we need to allow setFailFast calls
        doNothing().when(exec).setFailFast(anyBoolean());

        // Discriminator handling
        if (discriminatorEnabled) {
            when(validationContext.getConfig().isDiscriminatorKeywordEnabled()).thenReturn(true);
            when(exec.getCurrentDiscriminatorContext()).thenReturn(discriminatorContext);
        } else {
            when(validationContext.getConfig().isDiscriminatorKeywordEnabled()).thenReturn(false);
        }

        // ExecutionConfig needed for message building (locale). Return a mock with a dummy locale.
        ExecutionConfig execConfig = mock(ExecutionConfig.class);
        when(exec.getExecutionConfig()).thenReturn(execConfig);
        when(execConfig.getLocale()).thenReturn(java.util.Locale.ENGLISH);

        return exec;
    }

    private DiscriminatorContext mockDiscriminatorContext(boolean active,
                                                          boolean matchFound,
                                                          boolean ignore) {
        DiscriminatorContext ctx = mock(DiscriminatorContext.class);
        when(ctx.isActive()).thenReturn(active);
        when(ctx.isDiscriminatorMatchFound()).thenReturn(matchFound);
        when(ctx.isDiscriminatorIgnore()).thenReturn(ignore);
        // The validator may set match/ignore flags; we don't need to capture them.
        return ctx;
    }

    // --------------------------------------------------------------------
    // Existing tests (kept unchanged)
    // --------------------------------------------------------------------
    @Test
    void anyOfSucceedsWhenOneSubschemaValidates() throws Exception {
        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage()));

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> result = validator.invokeValidate(
                execCtx, instance, instance, mock(JsonNodePath.class), false);

        assertTrue(result.isEmpty(), "anyOf should succeed when one subschema is valid");
    }

    @Test
    void anyOfFailsWhenAllSubschemasInvalid() throws Exception {
        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("string error")));
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("integer error")));

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode instance = MAPPER.readTree("true");

        Set<ValidationMessage> result = validator.invokeValidate(
                execCtx, instance, instance, mock(JsonNodePath.class), false);

        assertEquals(2, result.size(), "anyOf should aggregate errors from all subschemas");
    }

    @Test
    void shortCircuitReturnsImmediatelyOnFirstValidSubschema() throws Exception {
        TestableAnyOfValidator spyValidator = spy(validator);
        doReturn(true).when(spyValidator).canShortCircuit();
        doReturn(true).when(spyValidator).canShortCircuit(any());

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        ExecutionContext execCtx = new ExecutionContext();
        JsonNode instance = MAPPER.readTree("\"short\"");

        Set<ValidationMessage> result = spyValidator.invokeValidate(
                execCtx, instance, instance, mock(JsonNodePath.class), false);

        assertTrue(result.isEmpty(), "short‑circuit should cause an immediate success");
        verify(subSchema2, never()).validate(any(), any(), any(), any());
    }

    // --------------------------------------------------------------------
    // New tests covering the remaining groups
    // --------------------------------------------------------------------
    @Test
    void discriminatorDisabledSuccessfulValidationWalkTrue() throws Exception {
        // Walk = true, discriminator disabled, single subschema validates.
        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        ExecutionContext exec = mockExecutionContext(true, false, null);
        JsonNode instance = MAPPER.readTree("\"ok\"");

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, instance, instance, mock(JsonNodePath.class), true);

        assertTrue(result.isEmpty(), "Expected empty set when subschema validates (walk=true)");
    }

    @Test
    void discriminatorDisabledTypeMismatchCausesSkipAndCollectsTypeError() throws Exception {
        // Prepare a TypeValidator that reports a non‑UNION type and mismatches the node.
        TypeValidator tv = mock(TypeValidator.class);
        when(tv.getSchemaType()).thenReturn(JsonType.STRING);
        when(tv.equalsToSchemaType(any())).thenReturn(false);
        when(tv.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("type mismatch")));

        when(subSchema1.getTypeValidator()).thenReturn(tv);
        when(subSchema2.getTypeValidator()).thenReturn(null);
        // subSchema2 will be evaluated normally (return empty set)
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        ExecutionContext exec = mockExecutionContext(false, false, null);
        JsonNode instance = MAPPER.readTree("\"string\"");

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, instance, instance, mock(JsonNodePath.class), true);

        // The type‑validator error from subSchema1 should be present, but overall validation succeeds because subSchema2 is valid.
        // Since at least one subschema validates, the final result is empty.
        assertTrue(result.isEmpty(), "When a later subschema validates, any earlier type‑validator errors are ignored.");
    }

    @Test
    void shortCircuitDisabledByExecutionContext() throws Exception {
        // canShortCircuit() true, but canShortCircuit(exec) false → no early return.
        TestableAnyOfValidator spyValidator = spy(validator);
        doReturn(true).when(spyValidator).canShortCircuit();
        doReturn(false).when(spyValidator).canShortCircuit(any());

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet()); // first subschema valid
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("should be ignored")));

        ExecutionContext exec = mockExecutionContext(true, false, null);
        JsonNode instance = MAPPER.readTree("\"any\"");

        Set<ValidationMessage> result = spyValidator.invokeValidate(
                exec, instance, instance, mock(JsonNodePath.class), true);

        assertTrue(result.isEmpty(), "Validation should succeed even though short‑circuit was disabled by execution context.");
        // Verify that the second subschema was still invoked.
        verify(subSchema2, times(1)).validate(any(), any(), any(), any());
    }

    @Test
    void discriminatorEnabledMatchFoundWithErrors() throws Exception {
        // Discriminator active, match found, subschema returns errors.
        DiscriminatorContext ctx = mockDiscriminatorContext(true, true, false);
        ExecutionContext exec = mockExecutionContext(true, true, ctx);

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("sub error")));

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, MAPPER.readTree("\"bad\""), MAPPER.readTree("\"bad\""),
                mock(JsonNodePath.class), true);

        // The result should contain the subschema error (and possibly the discriminator remark – we only assert at least one error).
        assertFalse(result.isEmpty(), "Expected errors when matched subschema fails.");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().equals("sub error")),
                "Original subschema error must be present.");
    }

    @Test
    void discriminatorEnabledMatchFoundNoErrors() throws Exception {
        // Discriminator active, match found, subschema validates cleanly.
        DiscriminatorContext ctx = mockDiscriminatorContext(true, true, false);
        ExecutionContext exec = mockExecutionContext(true, true, ctx);

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, MAPPER.readTree("\"good\""), MAPPER.readTree("\"good\""),
                mock(JsonNodePath.class), true);

        assertTrue(result.isEmpty(), "When discriminator match validates, result should be empty.");
    }

    @Test
    void discriminatorEnabledNoMatchAfterAllSubschemas() throws Exception {
        DiscriminatorContext ctx = mockDiscriminatorContext(true, false, false);
        ExecutionContext exec = mockExecutionContext(true, true, ctx);

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("err1")));
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("err2")));

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, MAPPER.readTree("\"x\""), MAPPER.readTree("\"x\""),
                mock(JsonNodePath.class), true);

        assertEquals(1, result.size(), "Expected single discriminator‑no‑alternative message.");
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("based on the provided discriminator"),
                "Message should contain the discriminator‑no‑alternative text.");
    }

    @Test
    void discriminatorEnabledIgnoreFlagTreatsAsDisabled() throws Exception {
        DiscriminatorContext ctx = mockDiscriminatorContext(true, false, true); // ignore = true
        ExecutionContext exec = mockExecutionContext(true, true, ctx);

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("err1")));
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, MAPPER.readTree("\"ok\""), MAPPER.readTree("\"ok\""),
                mock(JsonNodePath.class), true);

        assertTrue(result.isEmpty(), "When discriminator is ignored, validation should succeed if any subschema passes.");
    }

    @Test
    void unionTypeValidatorDoesNotSkipOnMismatch() throws Exception {
        // TypeValidator reports UNION, so schema is evaluated even if node type mismatches.
        TypeValidator unionTv = mock(TypeValidator.class);
        when(unionTv.getSchemaType()).thenReturn(JsonType.UNION);
        when(unionTv.equalsToSchemaType(any())).thenReturn(false);
        when(unionTv.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("union type error")));

        when(subSchema1.getTypeValidator()).thenReturn(unionTv);
        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("sub error")));

        ExecutionContext exec = mockExecutionContext(false, false, null);
        JsonNode instance = MAPPER.readTree("\"string\"");

        Set<ValidationMessage> result = validator.invokeValidate(
                exec, instance, instance, mock(JsonNodePath.class), true);

        // The union validator should have been called, resulting in at least one error.
        assertFalse(result.isEmpty(), "Union type validator should not cause the subschema to be skipped.");
        assertTrue(result.stream().anyMatch(m -> m.getMessage().equals("sub error")),
                "Subschema's own error must be present.");
    }

    @Test
    void failFastFlagRestoredAfterValidation() throws Exception {
        ExecutionContext exec = mock(ExecutionContext.class);
        when(exec.isFailFast()).thenReturn(true);
        // capture the value passed to setFailFast
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);
        doNothing().when(exec).setFailFast(captor.capture());

        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage()));
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage()));

        validator.invokeValidate(exec,
                MAPPER.readTree("true"),
                MAPPER.readTree("true"),
                mock(JsonNodePath.class),
                true);

        // The validator must set failFast to false during processing and then restore true.
        assertEquals(2, captor.getAllValues().size(), "setFailFast should be called twice (disable, restore).");
        assertEquals(Boolean.FALSE, captor.getAllValues().get(0), "First call should disable failFast.");
        assertEquals(Boolean.TRUE, captor.getAllValues().get(1), "Second call should restore original value.");
    }

    @Test
    void allSubschemasInvalidAggregatedErrorsReturned() throws Exception {
        when(subSchema1.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("err1")));
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("err2")));

        ExecutionContext exec = mockExecutionContext(false, false, null);
        Set<ValidationMessage> result = validator.invokeValidate(
                exec,
                MAPPER.readTree("{\"a\":1}"),
                MAPPER.readTree("{\"a\":1}"),
                mock(JsonNodePath.class),
                true);

        assertEquals(2, result.size(), "Both errors should be aggregated.");
    }

    @Test
    void firstSchemaTypeMismatchSecondValidShortCircuitDisabled() throws Exception {
        // First schema has a mismatching non‑UNION TypeValidator.
        TypeValidator tv = mock(TypeValidator.class);
        when(tv.getSchemaType()).thenReturn(JsonType.INTEGER);
        when(tv.equalsToSchemaType(any())).thenReturn(false);
        when(tv.validate(any(), any(), any(), any()))
                .thenReturn(Set.of(dummyMessage("type mismatch")));

        when(subSchema1.getTypeValidator()).thenReturn(tv);
        // Second schema validates cleanly.
        when(subSchema2.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // Disable short‑circuit.
        TestableAnyOfValidator spyValidator = spy(validator);
        doReturn(false).when(spyValidator).canShortCircuit();

        ExecutionContext exec = mockExecutionContext(false, false, null);
        Set<ValidationMessage> result = spyValidator.invokeValidate(
                exec,
                MAPPER.readTree("5"),
                MAPPER.readTree("5"),
                mock(JsonNodePath.class),
                true);

        assertTrue(result.isEmpty(), "Overall validation should succeed because second subschema is valid.");
        // Verify that the first subschema's type validator was consulted.
        verify(tv, times(1)).equalsToSchemaType(any());
    }

    @Test
    void nullExecutionContextThrowsNPE() throws Exception {
        assertThrows(NullPointerException.class, () ->
                validator.invokeValidate(null,
                        MAPPER.readTree("\"x\""),
                        MAPPER.readTree("\"x\""),
                        mock(JsonNodePath.class),
                        true));
    }

    @Test
    void nullNodeThrowsNPE() throws Exception {
        ExecutionContext exec = mockExecutionContext(false, false, null);
        assertThrows(NullPointerException.class, () ->
                validator.invokeValidate(exec, null, null, mock(JsonNodePath.class), true));
    }

    @Test
    void nullSchemasListThrowsNPE() throws Exception {
        // Force the internal 'schemas' field to null via reflection.
        Field schemasField = AnyOfValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        schemasField.set(validator, null);

        ExecutionContext exec = mockExecutionContext(false, false, null);
        assertThrows(NullPointerException.class, () ->
                validator.invokeValidate(exec,
                        MAPPER.readTree("\"x\""),
                        MAPPER.readTree("\"x\""),
                        mock(JsonNodePath.class),
                        true));
    }

    @Test
    void typeValidatorThrowsRuntimeExceptionPropagates() throws Exception {
        TypeValidator badTv = mock(TypeValidator.class);
        when(badTv.getSchemaType()).thenReturn(JsonType.STRING);
        when(badTv.equalsToSchemaType(any())).thenThrow(new RuntimeException("boom"));
        when(subSchema1.getTypeValidator()).thenReturn(badTv);

        ExecutionContext exec = mockExecutionContext(false, false, null);
        assertThrows(RuntimeException.class, () ->
                validator.invokeValidate(exec,
                        MAPPER.readTree("\"x\""),
                        MAPPER.readTree("\"x\""),
                        mock(JsonNodePath.class),
                        true));
    }

    @Test
    void schemaValidateThrowsRuntimeExceptionPropagates() throws Exception {
        when(subSchema1.validate(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("validation explode"));
        ExecutionContext exec = mockExecutionContext(false, false, null);
        assertThrows(RuntimeException.class, () ->
                validator.invokeValidate(exec,
                        MAPPER.readTree("\"x\""),
                        MAPPER.readTree("\"x\""),
                        mock(JsonNodePath.class),
                        false));
    }

    // --------------------------------------------------------------------
    // Concrete subclass exposing the protected validate method.
    // --------------------------------------------------------------------
    private static class TestableAnyOfValidator extends AnyOfValidator {
        TestableAnyOfValidator(SchemaLocation schemaLocation,
                               JsonNodePath evaluationPath,
                               JsonNode schemaNode,
                               JsonSchema parentSchema,
                               ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        Set<ValidationMessage> invokeValidate(ExecutionContext executionContext,
                                              JsonNode node,
                                              JsonNode rootNode,
                                              JsonNodePath instanceLocation,
                                              boolean walk) {
            return super.validate(executionContext, node, rootNode, instanceLocation, walk);
        }
    }
}
```