###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Public method signatures</li>
 *   <li>Basic functional behaviour</li>
 *   <li>Edge‑case handling (empty schema list, multiple matching schemas, fail‑fast mode)</li>
 *   <li>Exception scenarios (JsonSchemaException when failFast is true)</li>
 *   <li>Interaction with dependent collaborators (ExecutionContext, CollectorContext, JsonSchema, ValidationContext)</li>
 * </ul>
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -------------------------------------------------------------------------
    // Helper / stub classes used in the tests
    // -------------------------------------------------------------------------

    /** Minimal stub for {@link Scope} – mergeWith is a no‑op. */
    private static class DummyScope implements Scope {
        @Override public void mergeWith(Scope other) {/* no‑op */}
    }

    /** Minimal stub for {@link CollectorContext}. */
    private static class DummyCollectorContext implements CollectorContext {
        private final Map<String, Object> map = new HashMap<>();
        private final Scope dummyScope = new DummyScope();

        @Override public Scope enterDynamicScope() { return dummyScope; }
        @Override public Scope exitDynamicScope() { return dummyScope; }
        @Override public Object get(String key) { return map.get(key); }
        @Override public void set(String key, Object value) { map.put(key, value); }
        @Override public Set<String> getEvaluatedItems() { return new HashSet<>(); }
        @Override public Set<String> getEvaluatedProperties() { return new HashSet<>(); }
        @Override public void clearEvaluatedItems() {}
        @Override public void clearEvaluatedProperties() {}
    }

    /** Minimal stub for {@link ExecutionContext}. */
    private static class DummyExecutionContext implements ExecutionContext {
        private final CollectorContext collectorContext = new DummyCollectorContext();
        @Override public CollectorContext getCollectorContext() { return collectorContext; }
        @Override public ExecutionContext createChildContext() { return this; }
    }

    // -------------------------------------------------------------------------
    // Mocks shared by several tests
    // -------------------------------------------------------------------------

    @Mock private ValidationContext validationContext;
    @Mock private JsonSchema schemaA;
    @Mock private JsonSchema schemaB;
    @Mock private JsonSchema schemaC;
    @Mock private ValidatorState validatorState;   // internal state stored in CollectorContext

    private DummyExecutionContext executionContext;
    private JsonNode emptyArrayNode;   // "[]"
    private JsonNode twoSchemaArrayNode; // "[{},{}]"

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        executionContext = new DummyExecutionContext();

        // Prepare JsonNode representations for constructor tests
        ObjectMapper mapper = new ObjectMapper();
        emptyArrayNode = mapper.readTree("[]");
        twoSchemaArrayNode = mapper.readTree("[{},{}]");

        // ValidationContext#newSchema must return the mocked schemas
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(schemaA)   // first call
                .thenReturn(schemaB)   // second call
                .thenReturn(schemaC);  // third call (if needed)

        // Insert a dummy ValidatorState into the CollectorContext used by the validator
        ((DummyCollectorContext) executionContext.getCollectorContext())
                .set(ValidatorState.VALIDATOR_STATE_KEY, validatorState);
        // Default behaviour of the state
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);
    }

    // -------------------------------------------------------------------------
    // 1. Public method signatures (extracted for documentation)
    // -------------------------------------------------------------------------
    //    public OneOfValidator(String schemaPath, JsonNode schemaNode,
    //                         JsonSchema parentSchema, ValidationContext validationContext)
    //    public Set<ValidationMessage> validate(ExecutionContext executionContext,
    //                                          JsonNode node, JsonNode rootNode, String at)
    //    public Set<ValidationMessage> walk(ExecutionContext executionContext,
    //                                       JsonNode node, JsonNode rootNode,
    //                                       String at, boolean shouldValidateSchema)
    //    public void preloadJsonSchema()

    // -------------------------------------------------------------------------
    // 2. Basic functional tests
    // -------------------------------------------------------------------------

    @Test
    public void constructor_shouldCreateSchemasFromArrayNode() {
        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);
        // verify that ValidationContext#newSchema was called twice (once per array element)
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(null));
        // internal list size is not directly accessible – we rely on the interaction above.
    }

    @Test
    public void validate_whenExactlyOneSchemaMatches_returnsNoErrors() {
        // schemaA returns errors, schemaB returns empty set (i.e. matches)
        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(new ValidationMessage.Builder().message("errorA").path("$").build()));
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        assertTrue("No validation errors expected when exactly one schema matches", result.isEmpty());

        // Verify that state flags were updated (matchedNode true) – we only can verify the call
        verify(validatorState).setComplexValidator(true);
        verify(validatorState).setMatchedNode(true);
    }

    @Test
    public void validate_whenNoSchemaMatches_returnsOneOfError() {
        // Both schemas produce errors
        ValidationMessage msgA = new ValidationMessage.Builder().message("A").path("$").build();
        ValidationMessage msgB = new ValidationMessage.Builder().message("B").path("$").build();

        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(msgA));
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(msgB));

        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        assertEquals("Exactly two validation messages expected (OneOf error + child errors)",
                3, result.size());

        // The OneOf error message contains the number of valid schemas ("0")
        boolean oneOfFound = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("OneOf error message with count 0 should be present", oneOfFound);
    }

    @Test
    public void validate_whenMultipleSchemasMatch_returnsOneOfError() {
        // Both schemas validate successfully (empty error sets)
        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);

        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        assertEquals("Exactly one OneOf error expected when more than one schema matches", 1, result.size());

        ValidationMessage message = result.iterator().next();
        assertTrue("OneOf error should report count 2", message.getMessage().contains("2"));
    }

    @Test(expected = JsonSchemaException.class)
    public void validate_whenFailFast_andInvalid_shouldThrowException() throws Exception {
        // Force failFast = true via reflection (BaseJsonValidator has protected boolean failFast)
        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);
        java.lang.reflect.Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        // No schema matches → OneOf error should trigger exception
        when(schemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(new ValidationMessage.Builder().message("A").path("$").build()));
        when(schemaB.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(new ValidationMessage.Builder().message("B").path("$").build()));

        validator.validate(executionContext, mock(JsonNode.class), mock(JsonNode.class), "$");
    }

    @Test
    public void validate_withEmptySchemaArray_returnsOneOfError() {
        OneOfValidator validator = new OneOfValidator("#", emptyArrayNode, null, validationContext);
        Set<ValidationMessage> result = validator.validate(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$");

        assertEquals("One error (the OneOf error) expected for empty schema list", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue("Message should indicate 0 valid schemas", msg.getMessage().contains("0"));
    }

    // -------------------------------------------------------------------------
    // 3. walk() method tests
    // -------------------------------------------------------------------------

    @Test
    public void walk_whenShouldValidateSchemaTrue_delegatesToValidate() {
        // Set up a validator that will return a single error from validate()
        ValidationMessage vm = new ValidationMessage.Builder().message("X").path("$").build();
        OneOfValidator validator = new OneOfValidator("#", emptyArrayNode, null, validationContext) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node,
                                                  JsonNode rootNode, String at) {
                return Collections.singleton(vm);
            }
        };

        Set<ValidationMessage> result = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", true);

        assertEquals(1, result.size());
        assertEquals(vm, result.iterator().next());
    }

    @Test
    public void walk_whenShouldValidateSchemaFalse_callsEachSchemaWalk() {
        // Configure schemas to have walk() called
        doNothing().when(schemaA).walk(any(), any(), any(), anyString(), eq(false));
        doNothing().when(schemaB).walk(any(), any(), any(), anyString(), eq(false));

        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);
        Set<ValidationMessage> result = validator.walk(executionContext,
                mock(JsonNode.class), mock(JsonNode.class), "$", false);

        // walk() should return an empty set when validation is disabled
        assertTrue(result.isEmpty());

        // Verify that each schema's walk method was invoked exactly once
        verify(schemaA, times(1)).walk(any(), any(), any(), anyString(), eq(false));
        verify(schemaB, times(1)).walk(any(), any(), any(), anyString(), eq(false));
    }

    // -------------------------------------------------------------------------
    // 4. preloadJsonSchema() test
    // -------------------------------------------------------------------------

    @Test
    public void preloadJsonSchema_shouldInitializeAllSchemas() {
        OneOfValidator validator = new OneOfValidator("#", twoSchemaArrayNode, null, validationContext);
        validator.preloadJsonSchema();

        verify(schemaA, times(1)).initializeValidators();
        verify(schemaB, times(1)).initializeValidators();
    }
}
```
###Test END##