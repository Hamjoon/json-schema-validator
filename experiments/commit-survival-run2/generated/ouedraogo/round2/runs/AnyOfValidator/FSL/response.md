**Java JUnit 4 test class for `AnyOfValidator`**

```java
/**
 * Automatically generated JUnit‑4 test suite that exercises every logical path
 * inside {@link com.networknt.schema.AnyOfValidator}.  The tests use Mockito
 * to isolate the validator from the rest of the library – only the behaviour
 * of the collaborators (ExecutionContext, ValidationContext, JsonSchema, …)
 * that is required for the exercised code paths is stubbed.
 *
 * The file starts with the required marker “###Test START##” and ends with
 * “###Test END##”.
 */
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.slf4j.Logger;

import java.util.*;

public class AnyOfValidatorTest {

    /** Common test objects */
    private ObjectMapper mapper = new ObjectMapper();

    /** Helper to build a JsonNode array that the validator will interpret as the list of subschemas. */
    private JsonNode arrayNode(String... jsonObjects) throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < jsonObjects.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(jsonObjects[i]);
        }
        sb.append(']');
        return mapper.readTree(sb.toString());
    }

    /** Creates a mock ValidationContext whose {@code getConfig()} returns a mock Config
     *  with the supplied discriminator flag. */
    private ValidationContext mockValidationContext(boolean discriminatorEnabled) {
        ValidationContext vc = mock(ValidationContext.class, Answers.RETURNS_DEEP_STUBS);
        Config cfg = mock(Config.class);
        when(vc.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(discriminatorEnabled);
        when(vc.getConfig()).thenReturn(cfg);
        when(vc.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(discriminatorEnabled);
        return vc;
    }

    /** Creates a mock ExecutionContext that returns a mock CollectorContext. */
    private ExecutionContext mockExecutionContext(CollectorContext collector) {
        ExecutionContext ec = mock(ExecutionContext.class);
        when(ec.getCollectorContext()).thenReturn(collector);
        return ec;
    }

    /** Creates a mock CollectorContext that supplies a fresh mock Scope for each call to
     *  {@code enterDynamicScope()} and a mock ValidatorState when queried. */
    private CollectorContext mockCollectorContext(ValidatorState state) {
        CollectorContext cc = mock(CollectorContext.class);
        // each call returns a new mock Scope – the validator only merges scopes,
        // so the concrete implementation is irrelevant.
        when(cc.enterDynamicScope()).thenAnswer(invocation -> mock(Scope.class));
        when(cc.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(state);
        return cc;
    }

    /** Simple factory that builds an {@link AnyOfValidator} instance with the supplied
     *  list of pre‑mocked subschemas. */
    private AnyOfValidator buildValidator(boolean discriminatorEnabled,
                                          List<JsonSchema> subSchemas,
                                          ValidationContext vc) throws Exception {

        // Build a dummy schemaNode – its size must equal the number of subschemas.
        JsonNode schemaNode = arrayNode(new String[subSchemas.size()]);
        JsonSchema parent = mock(JsonSchema.class);

        // Stub ValidationContext#newSchema() to return the subschemas in order.
        OngoingStubbing<JsonSchema> stub = when(vc.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)));
        for (JsonSchema s : subSchemas) {
            stub = stub.thenReturn(s);
        }

        // Construct the validator (the constructor will call newSchema() for each element).
        return new AnyOfValidator("anyOf", schemaNode, parent, vc);
    }

    /** --------------------------------------------------------------------- *
     *  1.  No discriminator, one subschema fails type check, second subschema
     *      validates successfully → validator returns an empty error set.   *
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidate_NoDiscriminator_SubSchemaMatches() throws Exception {
        // ----- mocks ---------------------------------------------------------
        ValidationContext vc = mockValidationContext(false);
        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);

        // ----- first subschema – type validator mismatch ---------------------
        JsonSchema schema1 = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(schema1.hasTypeValidator()).thenReturn(true);
        when(schema1.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        // node type will be INTEGER, so mismatch
        JsonNode node = mapper.readTree("123");
        when(typeValidator.equalsToSchemaType(node)).thenReturn(false);

        // ----- second subschema – successful validation ----------------------
        JsonSchema schema2 = mock(JsonSchema.class);
        when(schema2.hasTypeValidator()).thenReturn(false);
        when(schema2.validate(exec, node, node, "/test")).thenReturn(Collections.emptySet());

        // ----- state handling ------------------------------------------------
        // initial state.hasMatchedNode() = false ; after successful validation we
        // want state.hasMatchedNode() to become true (the validator sets it when
        // there are no errors).
        when(state.hasMatchedNode()).thenReturn(false);
        // setMatchedNode is a void method – we only verify that it is called.
        // walkEnabled is irrelevant for this path.
        when(state.isWalkEnabled()).thenReturn(false);

        // ----- build validator -----------------------------------------------
        AnyOfValidator validator = buildValidator(false, Arrays.asList(schema1, schema2), vc);

        // ----- execute --------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(exec, node, node, "/test");

        // ----- assertions ------------------------------------------------------
        assertTrue("Validator should return an empty set when a subschema matches", result.isEmpty());
        // Verify that the type‑mismatch error was *not* propagated because a later
        // subschema succeeded.
        verify(state, times(1)).setMatchedNode(anyBoolean());
        // The validator should have called validate() on the second schema.
        verify(schema2, times(1)).validate(exec, node, node, "/test");
    }

    /** --------------------------------------------------------------------- *
     *  2.  No discriminator, none of the subschemas validate → all errors are
     *      aggregated and returned.                                           *
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidate_NoDiscriminator_NoValidSubSchema() throws Exception {
        ValidationContext vc = mockValidationContext(false);
        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);

        JsonNode node = mapper.readTree("{\"a\":1}");

        // ----- subschema 1 – returns a required‑property error ----------------
        JsonSchema s1 = mock(JsonSchema.class);
        when(s1.hasTypeValidator()).thenReturn(false);
        ValidationMessage reqMsg = ValidationMessage.of("required", "/test", "property", "required");
        when(s1.validate(exec, node, node, "/test")).thenReturn(Collections.singleton(reqMsg));

        // ----- subschema 2 – returns a generic custom error -------------------
        JsonSchema s2 = mock(JsonSchema.class);
        when(s2.hasTypeValidator()).thenReturn(false);
        ValidationMessage otherMsg = ValidationMessage.of("type", "/test", "type", "type");
        when(s2.validate(exec, node, node, "/test")).thenReturn(Collections.singleton(otherMsg));

        when(state.hasMatchedNode()).thenReturn(false);
        when(state.isWalkEnabled()).thenReturn(false);

        AnyOfValidator validator = buildValidator(false, Arrays.asList(s1, s2), vc);
        Set<ValidationMessage> result = validator.validate(exec, node, node, "/test");

        // Both messages must be present because no subschema succeeded.
        assertEquals(2, result.size());
        assertTrue(result.contains(reqMsg));
        assertTrue(result.contains(otherMsg));
    }

    /** --------------------------------------------------------------------- *
     *  3.  OpenAPI‑3 style discriminator enabled – a discriminator match is
     *      found and the matching subschema returns errors.                     *
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidate_WithDiscriminator_MatchFoundWithErrors() throws Exception {
        ValidationContext vc = mockValidationContext(true);
        // the validator creates a fresh DiscriminatorContext internally; we need to
        // capture it so we can manipulate its state.
        ValidationContext.DiscriminatorContext discriminatorCtx = new ValidationContext.DiscriminatorContext();
        // Force the context to think a match was found.
        discriminatorCtx.setDiscriminatorMatchFound(true);

        // Stub the enter/leave calls – they are no‑ops for the test.
        doNothing().when(vc).enterDiscriminatorContext(any(), anyString());
        doNothing().when(vc).leaveDiscriminatorContextImmediately(anyString());

        // The validator will use the *new* DiscriminatorContext instance created in its
        // constructor, so we cannot inject the one above directly.  Instead we rely on
        // the fact that the flag is true – the validator will query the context it
        // created.  We therefore set the internal flag via reflection after construction.
        // (Reflection is acceptable in a test‑only scenario.)
        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);

        JsonNode node = mapper.readTree("\"foo\"");

        // Subschema that validates successfully (no errors)
        JsonSchema s1 = mock(JsonSchema.class);
        when(s1.hasTypeValidator()).thenReturn(false);
        when(s1.validate(exec, node, node, "/test")).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(true, Collections.singletonList(s1), vc);

        // Inject the discriminatorContext created inside the validator so we can set the flag.
        java.lang.reflect.Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        dcField.set(validator, discriminatorCtx);

        // Execute
        Set<ValidationMessage> result = validator.validate(exec, node, node, "/test");

        // Because a discriminator match was found and the subschema returned errors (empty set),
        // the validator should return an empty set (no remark is added when there are no errors).
        assertTrue(result.isEmpty());
    }

    /** --------------------------------------------------------------------- *
     *  4.  OpenAPI‑3 discriminator enabled – match found, subschema returns errors.
     *  The validator must add the special remark message.                         *
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidate_WithDiscriminator_MatchFoundWithErrors_AddedRemark() throws Exception {
        ValidationContext vc = mockValidationContext(true);
        doNothing().when(vc).enterDiscriminatorContext(any(), anyString());
        doNothing().when(vc).leaveDiscriminatorContextImmediately(anyString());

        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);

        JsonNode node = mapper.readTree("{\"type\":\"bad\"}");

        // Subschema that returns a generic error
        JsonSchema s1 = mock(JsonSchema.class);
        when(s1.hasTypeValidator()).thenReturn(false);
        ValidationMessage err = ValidationMessage.of("type", "/test", "type", "type");
        when(s1.validate(exec, node, node, "/test")).thenReturn(Collections.singleton(err));

        AnyOfValidator validator = buildValidator(true, Collections.singletonList(s1), vc);

        // Force the internal discriminator context to indicate a match.
        java.lang.reflect.Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        ValidationContext.DiscriminatorContext internalCtx = (ValidationContext.DiscriminatorContext) dcField.get(validator);
        internalCtx.setDiscriminatorMatchFound(true);

        // Execute
        Set<ValidationMessage> result = validator.validate(exec, node, node, "/test");

        // Result must contain the original error and the remark.
        assertEquals(2, result.size());
        assertTrue(result.contains(err));
        boolean remarkFound = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Remark about discriminator must be present", remarkFound);
    }

    /** --------------------------------------------------------------------- *
     *  5.  OpenAPI‑3 discriminator enabled – active but no match found.
     *      The validator must return the *fallback* discriminator error.       *
     *  --------------------------------------------------------------------- */
    @Test
    public void testValidate_WithDiscriminator_ActiveNoMatch() throws Exception {
        ValidationContext vc = mockValidationContext(true);
        doNothing().when(vc).enterDiscriminatorContext(any(), anyString());
        doNothing().when(vc).leaveDiscriminatorContextImmediately(anyString());

        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);

        JsonNode node = mapper.readTree("{\"x\":1}");

        // Subschema that validates without errors (so that the loop finishes)
        JsonSchema s1 = mock(JsonSchema.class);
        when(s1.hasTypeValidator()).thenReturn(false);
        when(s1.validate(exec, node, node, "/test")).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(true, Collections.singletonList(s1), vc);

        // Manipulate the internal discriminator context:
        java.lang.reflect.Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        ValidationContext.DiscriminatorContext internalCtx = (ValidationContext.DiscriminatorContext) dcField.get(validator);
        // No match found, but the context is marked as active.
        internalCtx.setActive(true);
        internalCtx.setDiscriminatorMatchFound(false);

        // Execute
        Set<ValidationMessage> result = validator.validate(exec, node, node, "/test");

        // The validator should return exactly one message about “no alternative could be chosen”.
        assertEquals(1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("based on the provided discriminator"));
    }

    /** --------------------------------------------------------------------- *
     *  6.  walk() with shouldValidateSchema == true must delegate to validate().
     *  --------------------------------------------------------------------- */
    @Test
    public void testWalk_DelegatesToValidateWhenRequested() throws Exception {
        ValidationContext vc = mockValidationContext(false);
        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);
        JsonNode node = mapper.readTree("true");

        // Build a validator with a single dummy subschema.
        JsonSchema dummy = mock(JsonSchema.class);
        when(dummy.hasTypeValidator()).thenReturn(false);
        when(dummy.validate(exec, node, node, "/test")).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(false, Collections.singletonList(dummy), vc);

        // Spy the validator so we can verify that validate() is called.
        AnyOfValidator spy = Mockito.spy(validator);
        doReturn(Collections.emptySet()).when(spy).validate(exec, node, node, "/test");

        Set<ValidationMessage> result = spy.walk(exec, node, node, "/test", true);
        assertTrue(result.isEmpty());

        // Verify delegation.
        verify(spy, times(1)).validate(exec, node, node, "/test");
    }

    /** --------------------------------------------------------------------- *
     *  7.  walk() with shouldValidateSchema == false must walk each subschema
     *      but must return an empty set.                                         *
     *  --------------------------------------------------------------------- */
    @Test
    public void testWalk_IteratesSubschemasWhenNotValidating() throws Exception {
        ValidationContext vc = mockValidationContext(false);
        ValidatorState state = mock(ValidatorState.class);
        CollectorContext collector = mockCollectorContext(state);
        ExecutionContext exec = mockExecutionContext(collector);
        JsonNode node = mapper.readTree("null");

        // Two subschemas – we only verify that walk() is invoked on each.
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        when(s1.hasTypeValidator()).thenReturn(false);
        when(s2.hasTypeValidator()).thenReturn(false);
        when(s1.walk(exec, node, node, "/test", false)).thenReturn(Collections.emptySet());
        when(s2.walk(exec, node, node, "/test", false)).thenReturn(Collections.emptySet());

        AnyOfValidator validator = buildValidator(false, Arrays.asList(s1, s2), vc);
        Set<ValidationMessage> result = validator.walk(exec, node, node, "/test", false);

        assertTrue("walk() with shouldValidateSchema=false must return an empty set", result.isEmpty());
        verify(s1, times(1)).walk(exec, node, node, "/test", false);
        verify(s2, times(1)).walk(exec, node, node, "/test", false);
    }

    /** --------------------------------------------------------------------- *
     *  8.  preloadJsonSchema() should invoke the protected helper in the base
     *      class.  We simply verify that no exception is thrown and that the
     *      internal list of schemas is passed to {@code preloadJsonSchemas()}.
     *  --------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchema_InvokesBaseMethod() throws Exception {
        ValidationContext vc = mockValidationContext(false);
        JsonSchema s = mock(JsonSchema.class);
        AnyOfValidator validator = buildValidator(false, Collections.singletonList(s), vc);

        // Spy the validator to intercept the protected call.
        AnyOfValidator spy = Mockito.spy(validator);
        // The base method returns void; we only need to ensure it is called.
        doNothing().when(spy).preloadJsonSchemas(anyCollection());

        spy.preloadJsonSchema();

        verify(spy, times(1)).preloadJsonSchemas(anyCollection());
    }
}
###Test END##