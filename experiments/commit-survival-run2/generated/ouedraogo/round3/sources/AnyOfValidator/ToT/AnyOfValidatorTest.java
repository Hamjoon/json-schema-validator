/**
 * JUnit 4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover the three public methods:
 *   • {@link AnyOfValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}
 *   • {@link AnyOfValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}
 *   • {@link AnyOfValidator#preloadJsonSchema()}
 *
 * The tests use Mockito to mock the heavy‑weight collaborators
 * ( {@link ValidationContext}, {@link ExecutionContext},
 * {@link CollectorContext}, {@link JsonSchema}, etc.).
 *
 * The real {@code AnyOfValidator} logic is exercised while the
 * surrounding infrastructure is kept lightweight.
 */
package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.CollectorContext.Scope;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for {@link AnyOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // -----------------------------------------------------------------------
    // Mocks used across many tests
    // -----------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private Scope dummyScope; // used for dynamic scopes

    private AnyOfValidator anyOfValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Common setup executed before each test.
     *
     * <p>
     *   • The {@code ValidationContext} returns a config that disables the
     *     OpenAPI‑3 discriminator feature (the majority of tests do not need it).
     *   • The {@code ExecutionContext} returns the mocked {@code CollectorContext}.
     *   • The {@code CollectorContext} returns a dummy {@code Scope} for every
     *     {@code enterDynamicScope()} / {@code exitDynamicScope()} call.
     *   • The {@code CollectorContext} holds a mock {@code ValidatorState}
     *     under the well‑known key.
     * </p>
     */
    @Before
    public void setUp() throws Exception {
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Build a validator with an empty anyOf array – we will inject our own
        // mocked schemas via reflection later.
        JsonNode emptyArray = mapper.createArrayNode(); // size == 0
        anyOfValidator = new AnyOfValidator("anyOfPath", emptyArray, null, validationContext);
        // Force the discriminatorContext to null (discriminator disabled)
        java.lang.reflect.Field discField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        discField.setAccessible(true);
        discField.set(anyOfValidator, null);
    }

    /**
     * Helper that injects a list of {@link JsonSchema} mocks into the
     * {@code AnyOfValidator} instance via reflection.
     */
    private void injectSchemas(JsonSchema... schemas) throws Exception {
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(schemas));
        f.set(anyOfValidator, list);
    }

    /** --------------------------------------------------------------------
     *  1. validate() – happy path: at least one subschema matches → no errors
     *  -------------------------------------------------------------------- */
    @Test
    public void testValidateSuccessWhenOneSchemaValid() throws Exception {
        // Mock two subschemas – first fails, second succeeds
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        // No type validator for simplicity
        when(schema1.hasTypeValidator()).thenReturn(false);
        when(schema2.hasTypeValidator()).thenReturn(false);

        // schema1 returns a non‑empty error set
        ValidationMessage err1 = mock(ValidationMessage.class);
        when(err1.getType()).thenReturn("type");
        Set<ValidationMessage> errors1 = new LinkedHashSet<>(Collections.singleton(err1));
        when(schema1.validate(any(), any(), any(), any())).thenReturn(errors1);

        // schema2 returns an empty set → valid
        when(schema2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        injectSchemas(schema1, schema2);

        JsonNode node = mapper.readTree("\"a string\"");
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, node, "$");

        // Expect no errors because schema2 succeeded
        assertTrue("Result should be empty when a subschema validates", result.isEmpty());

        // Verify that both subschemas were processed
        verify(schema1).validate(any(), any(), any(), any());
        verify(schema2).validate(any(), any(), any(), any());
    }

    /** --------------------------------------------------------------------
     *  2. validate() – all subschemas fail → errors are returned
     *  -------------------------------------------------------------------- */
    @Test
    public void testValidateFailsWhenNoSchemaValid() throws Exception {
        JsonSchema schemaA = mock(JsonSchema.class);
        JsonSchema schemaB = mock(JsonSchema.class);
        when(schemaA.hasTypeValidator()).thenReturn(false);
        when(schemaB.hasTypeValidator()).thenReturn(false);

        ValidationMessage errA = mock(ValidationMessage.class);
        when(errA.getType()).thenReturn("typeA");
        ValidationMessage errB = mock(ValidationMessage.class);
        when(errB.getType()).thenReturn("typeB");

        when(schemaA.validate(any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Collections.singleton(errA)));
        when(schemaB.validate(any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Collections.singleton(errB)));

        injectSchemas(schemaA, schemaB);

        JsonNode node = mapper.readTree("123"); // any value – both schemas will fail
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, node, "$");

        // Both errors should be present (order is not guaranteed)
        assertEquals(2, result.size());
        assertTrue(result.contains(errA));
        assertTrue(result.contains(errB));
    }

    /** --------------------------------------------------------------------
     *  3. validate() – required‑property errors are ignored when another
     *      subschema is valid.
     *  -------------------------------------------------------------------- */
    @Test
    public void testValidateIgnoresRequiredErrorsWhenAtLeastOneValid() throws Exception {
        JsonSchema goodSchema = mock(JsonSchema.class);
        JsonSchema badSchema = mock(JsonSchema.class);

        when(goodSchema.hasTypeValidator()).thenReturn(false);
        when(badSchema.hasTypeValidator()).thenReturn(false);

        // goodSchema → success
        when(goodSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // badSchema → returns a REQUIRED‑type error
        ValidationMessage requiredErr = mock(ValidationMessage.class);
        when(requiredErr.getType()).thenReturn(ValidatorTypeCode.REQUIRED.getValue());
        Set<ValidationMessage> requiredSet = new LinkedHashSet<>(Collections.singleton(requiredErr));
        when(badSchema.validate(any(), any(), any(), any())).thenReturn(requiredSet);

        injectSchemas(goodSchema, badSchema);

        JsonNode node = mapper.readTree("{\"foo\":\"bar\"}");
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, node, "$");

        // Because goodSchema succeeded, the REQUIRED error must be dropped
        assertTrue("All errors should be cleared when a subschema validates", result.isEmpty());

        verify(goodSchema).validate(any(), any(), any(), any());
        verify(badSchema).validate(any(), any(), any(), any());
    }

    /** --------------------------------------------------------------------
     *  4. walk() – when the flag is true it must delegate to validate()
     *  -------------------------------------------------------------------- */
    @Test
    public void testWalkDelegatesToValidateWhenFlagTrue() throws Exception {
        // Spy on the validator to intercept the validate() call
        AnyOfValidator spyValidator = Mockito.spy(anyOfValidator);

        // Minimal schema list – single mock schema that always succeeds
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());
        injectSchemas(mockSchema);

        JsonNode node = mapper.readTree("\"any\"");
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, node, "$", true);

        // The result must be exactly what validate() returns (empty set)
        assertTrue(result.isEmpty());

        // Verify that validate() was indeed invoked
        verify(spyValidator, times(1)).validate(any(), any(), any(), any());
    }

    /** --------------------------------------------------------------------
     *  5. walk() – when the flag is false it must walk each subschema without
     *      collecting errors.
     *  -------------------------------------------------------------------- */
    @Test
    public void testWalkSkipsValidationWhenFlagFalse() throws Exception {
        JsonSchema schemaX = mock(JsonSchema.class);
        JsonSchema schemaY = mock(JsonSchema.class);
        when(schemaX.hasTypeValidator()).thenReturn(false);
        when(schemaY.hasTypeValidator()).thenReturn(false);

        // walk() on subschemas should be called with shouldValidate = false
        when(schemaX.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(schemaY.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());

        injectSchemas(schemaX, schemaY);

        JsonNode node = mapper.readTree("123");
        Set<ValidationMessage> result = anyOfValidator.walk(executionContext, node, node, "$", false);

        // walk() should return an empty set (the AnyOfValidator does not collect)
        assertTrue(result.isEmpty());

        verify(schemaX).walk(any(), any(), any(), any(), eq(false));
        verify(schemaY).walk(any(), any(), any(), any(), eq(false));
    }

    /** --------------------------------------------------------------------
     *  6. preloadJsonSchema() – must invoke preload on each contained schema.
     *  -------------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaCallsPreloadOnAllSchemas() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        injectSchemas(s1, s2, s3);

        // Call the method under test
        anyOfValidator.preloadJsonSchema();

        // Verify that each schema received a preload call
        verify(s1, times(1)).preloadJsonSchema();
        verify(s2, times(1)).preloadJsonSchema();
        verify(s3, times(1)).preloadJsonSchema();
    }
}
