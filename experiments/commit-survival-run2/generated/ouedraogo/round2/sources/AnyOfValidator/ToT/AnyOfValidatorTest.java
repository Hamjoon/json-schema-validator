/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The tests cover:
 *   • normal validation where at least one subschema is valid,
 *   • validation where all subschemas are invalid,
 *   • type‑mismatch handling,
 *   • OpenAPI‑3 style discriminator behaviour (match‑found with & without errors),
 *   • the {@code walk} method with both {@code shouldValidateSchema} values,
 *   • the {@code preloadJsonSchema} delegation.
 *
 * Mockito is used to stub all collaborators:
 *   – {@link com.networknt.schema.ValidationContext},
 *   – {@link com.networknt.schema.ExecutionContext},
 *   – {@link com.networknt.schema.CollectorContext},
 *   – {@link com.networknt.schema.JsonSchema},
 *   – {@link com.networknt.schema.TypeValidator},
 *   – {@link com.networknt.schema.ValidationContext.Config},
 *   – {@link com.networknt.schema.ValidatorState},
 *   – {@link com.networknt.schema.CollectorContext.Scope}.
 *
 * The tests use real {@link com.fasterxml.jackson.databind.JsonNode} instances
 * (created via {@link com.fasterxml.jackson.databind.ObjectMapper}) for the
 * node being validated, which keeps the test logic simple and realistic.
 */
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

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    private static final String AT = "$.test";

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private JsonSchema parentSchema; // not used directly

    private ObjectMapper mapper = new ObjectMapper();

    /** Helper to build a minimal ArrayNode that will be used as the `anyOf` schema definition. */
    private JsonNode buildAnyOfSchemaNode(int count) {
        JsonNode array = mapper.createArrayNode();
        for (int i = 0; i < count; i++) {
            ((com.fasterxml.jackson.databind.node.ArrayNode) array).add(mapper.createObjectNode());
        }
        return array;
    }

    /** Common stubbing performed before every test. */
    @Before
    public void commonStubbing() {
        // ValidationContext always returns the same config mock
        when(validationContext.getConfig()).thenReturn(config);
        // ExecutionContext returns the collector context mock
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // CollectorContext stores a ValidatorState under the known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // By default walks are disabled (the real validator toggles this)
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // Scopes – we don't need any real merging, just return dummy objects
        when(collectorContext.enterDynamicScope()).thenReturn(mock(Scope.class));
        when(collectorContext.exitDynamicScope()).thenReturn(mock(Scope.class));
    }

    /**
     * Creates an {@link AnyOfValidator} instance with the supplied list of
     * {@link JsonSchema} stubs.  The {@code validationContext} is pre‑programmed
     * to return those stubs when {@code newSchema} is invoked.
     */
    private AnyOfValidator createValidator(List<JsonSchema> mockSchemas) {
        // stub validationContext.newSchema to return the supplied mocks in order
        Answer<JsonSchema> answer = new Answer<JsonSchema>() {
            private int idx = 0;
            @Override public JsonSchema answer(InvocationOnMock invocation) {
                return mockSchemas.get(idx++);
            }
        };
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenAnswer(answer);

        // Build a simple anyOf array node with the same number of entries
        JsonNode schemaNode = buildAnyOfSchemaNode(mockSchemas.size());

        // Finally instantiate the validator under test
        return new AnyOfValidator("anyOf", schemaNode, parentSchema, validationContext);
    }

    /** ---------------------------  VALIDATE() TESTS --------------------------- */

    @Test
    public void validate_oneSubSchemaValid_returnsEmptySet() {
        // ----- arrange ---------------------------------------------------------
        // First schema returns an error, second schema succeeds
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        when(schema1.hasTypeValidator()).thenReturn(false);
        when(schema2.hasTypeValidator()).thenReturn(false);

        Set<ValidationMessage> errors1 = Collections.singleton(
                ValidationMessage.builder().type("type1").message("error1").path(AT).build()
        );
        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(errors1);
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        // validatorState initially reports no matched node
        when(validatorState.hasMatchedNode()).thenReturn(false);
        // discriminator disabled for this test
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        AnyOfValidator validator = createValidator(Arrays.asList(schema1, schema2));

        JsonNode nodeToValidate = mapper.readTree("{\"foo\":\"bar\"}");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, nodeToValidate,
                nodeToValidate, AT);

        // ----- assert -----------------------------------------------------------
        assertTrue("Expected no validation errors because one subschema is valid", result.isEmpty());
        // ensure the second schema was indeed consulted
        verify(schema2).validate(executionContext, nodeToValidate, nodeToValidate, AT);
    }

    @Test
    public void validate_allSubSchemasInvalid_returnsAggregatedErrors() {
        // ----- arrange ---------------------------------------------------------
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        when(schema1.hasTypeValidator()).thenReturn(false);
        when(schema2.hasTypeValidator()).thenReturn(false);

        ValidationMessage msg1 = ValidationMessage.builder()
                .type("type1").message("first error").path(AT).build();
        ValidationMessage msg2 = ValidationMessage.builder()
                .type("type2").message("second error").path(AT).build();

        when(schema1.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(msg1));
        when(schema2.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(msg2));

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        AnyOfValidator validator = createValidator(Arrays.asList(schema1, schema2));
        JsonNode node = mapper.readTree("{\"x\":1}");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, AT);

        // ----- assert -----------------------------------------------------------
        assertEquals("Two error messages should be aggregated", 2, result.size());
        assertTrue(result.contains(msg1));
        assertTrue(result.contains(msg2));
    }

    @Test
    public void validate_typeMismatch_skipsSchemaAndAddsTypeError() {
        // ----- arrange ---------------------------------------------------------
        JsonSchema schema = mock(JsonSchema.class);
        TypeValidator typeValidator = mock(TypeValidator.class);

        when(schema.hasTypeValidator()).thenReturn(true);
        when(schema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        // The node is a number, so type mismatch
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);
        // The validator will add a type‑mismatch message via buildValidationMessage;
        // we don't need to stub that – we just verify that the returned set contains
        // a message whose type equals the schema type.
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        AnyOfValidator validator = createValidator(Collections.singletonList(schema));
        JsonNode numberNode = mapper.readTree("123");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, numberNode,
                numberNode, AT);

        // ----- assert -----------------------------------------------------------
        assertEquals("Only the type‑mismatch error should be present", 1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertEquals("The message type must be the mismatched schema type",
                JsonType.STRING.toString(), vm.getType());
    }

    @Test
    public void validate_openApiDiscriminator_matchFoundWithErrors_returnsErrorsPlusRemark() {
        // ----- arrange ---------------------------------------------------------
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // Set up a discriminator context that reports a match
        ValidationContext.DiscriminatorContext discCtx = new ValidationContext.DiscriminatorContext();
        discCtx.setDiscriminatorMatchFound(true);
        // Force the validator to use this context (the constructor creates a new one,
        // but we can replace it via reflection for the purpose of the test)
        AnyOfValidator validator = new AnyOfValidator("anyOf",
                buildAnyOfSchemaNode(1), parentSchema, validationContext);
        // inject the prepared context
        TestUtils.setField(validator, "discriminatorContext", discCtx);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.hasTypeValidator()).thenReturn(false);
        ValidationMessage err = ValidationMessage.builder()
                .type("typeX").message("some error").path(AT).build();
        when(schema.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.singleton(err));

        // replace the internal schemas list (the constructor already added one)
        TestUtils.setField(validator, "schemas", Collections.singletonList(schema));

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        JsonNode node = mapper.readTree("{\"prop\":true}");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, AT);

        // ----- assert -----------------------------------------------------------
        assertEquals("Should contain original error and the discriminator remark", 2, result.size());

        boolean hasRemark = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result must contain the discriminator remark", hasRemark);
    }

    @Test
    public void validate_openApiDiscriminator_matchFoundNoErrors_returnsEmptySet() {
        // ----- arrange ---------------------------------------------------------
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ValidationContext.DiscriminatorContext discCtx = new ValidationContext.DiscriminatorContext();
        discCtx.setDiscriminatorMatchFound(true);
        AnyOfValidator validator = new AnyOfValidator("anyOf",
                buildAnyOfSchemaNode(1), parentSchema, validationContext);
        TestUtils.setField(validator, "discriminatorContext", discCtx);

        JsonSchema schema = mock(JsonSchema.class);
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        TestUtils.setField(validator, "schemas", Collections.singletonList(schema));

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        JsonNode node = mapper.readTree("{\"a\":1}");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.validate(executionContext, node, node, AT);

        // ----- assert -----------------------------------------------------------
        assertTrue("No errors expected when discriminator match succeeds without errors", result.isEmpty());
    }

    /** ---------------------------  WALK() TESTS --------------------------- */

    @Test
    public void walk_shouldValidate_callsValidate() {
        // ----- arrange ---------------------------------------------------------
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.hasTypeValidator()).thenReturn(false);
        when(schema.validate(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT)))
                .thenReturn(Collections.emptySet());

        AnyOfValidator validator = createValidator(Collections.singletonList(schema));

        // Spy the validator to verify that validate() is invoked
        AnyOfValidator spyValidator = Mockito.spy(validator);
        JsonNode node = mapper.readTree("{\"x\":2}");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = spyValidator.walk(executionContext, node, node, AT, true);

        // ----- assert -----------------------------------------------------------
        verify(spyValidator).validate(executionContext, node, node, AT);
        assertTrue(result.isEmpty());
    }

    @Test
    public void walk_shouldNotValidate_callsEachSchemaWalk() {
        // ----- arrange ---------------------------------------------------------
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        // walk should be called with shouldValidateSchema = false
        when(schema1.walk(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT), eq(false)))
                .thenReturn(new LinkedHashSet<>());
        when(schema2.walk(executionContext, any(JsonNode.class), any(JsonNode.class), eq(AT), eq(false)))
                .thenReturn(new LinkedHashSet<>());

        AnyOfValidator validator = createValidator(Arrays.asList(schema1, schema2));
        JsonNode node = mapper.readTree("{\"y\":\"z\"}");

        // ----- act -------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext, node, node, AT, false);

        // ----- assert -----------------------------------------------------------
        verify(schema1).walk(executionContext, node, node, AT, false);
        verify(schema2).walk(executionContext, node, node, AT, false);
        assertTrue("walk with shouldValidateSchema=false must return an empty set", result.isEmpty());
    }

    /** ---------------------------  PRELOADJSONSCHEMA TEST --------------------------- */

    @Test
    public void preloadJsonSchema_delegatesToBaseClassMethod_forAllSubSchemas() {
        // ----- arrange ---------------------------------------------------------
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);

        // Create a concrete subclass that exposes the protected method for verification
        AnyOfValidator validator = new AnyOfValidator("anyOf",
                buildAnyOfSchemaNode(2), parentSchema, validationContext) {
            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                // capture the list passed to the base implementation
                TestUtils.setField(this, "capturedPreloadList", new ArrayList<>(schemas));
            }
        };

        // Inject our two mock schemas (the constructor already added two placeholders)
        TestUtils.setField(validator, "schemas", Arrays.asList(schema1, schema2));

        // ----- act -------------------------------------------------------------
        validator.preloadJsonSchema();

        // ----- assert -----------------------------------------------------------
        @SuppressWarnings("unchecked")
        List<JsonSchema> captured = (List<JsonSchema>) TestUtils.getField(validator, "capturedPreloadList");
        assertNotNull("The protected preloadJsonSchemas should have been called", captured);
        assertEquals("Both schemas must be passed to preloadJsonSchemas", 2, captured.size());
        assertTrue(captured.containsAll(Arrays.asList(schema1, schema2)));
    }

    /** ---------------------------  UTILITIES --------------------------- */

    /**
     * Small reflection helper used to set / get private fields for test purposes.
     */
    private static class TestUtils {
        static void setField(Object target, String fieldName, Object value) {
            try {
                java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static Object getField(Object target, String fieldName) {
            try {
                java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(target);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
