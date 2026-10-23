###Test START##
```java
/***************************************************************************************************
 *  AnyOfValidatorTest
 *
 *  This JUnit‑4 test class provides comprehensive coverage of the public behaviour of
 *  {@link com.networknt.schema.AnyOfValidator}.  The tests focus on the three public methods:
 *
 *  <ul>
 *      <li>{@code validate(...)} – the core validation algorithm.</li>
 *      <li>{@code walk(...)} – the delegation to {@code validate(...)} when validation is required,
 *          otherwise a no‑op walk of the child schemas.</li>
 *      <li>{@code preloadJsonSchema()} – the helper that pre‑loads all child schemas.</li>
 *  </ul>
 *
 *  Because {@code AnyOfValidator} collaborates heavily with the validation framework (ExecutionContext,
 *  CollectorContext, ValidationContext, JsonSchema, etc.) the test suite uses Mockito to mock all
 *  external collaborators.  A small concrete subclass, {@code TestableAnyOfValidator}, overrides the
 *  protected {@code buildValidationMessage} method so that we can create deterministic
 *  {@link com.networknt.schema.ValidationMessage} instances without relying on the internal
 *  implementation of {@code BaseJsonValidator}.
 *
 *  The tests cover the following logical branches:
 *
 *  <ul>
 *      <li>Type‑validator mismatch – an error is added and the schema is skipped.</li>
 *      <li>Successful validation without a required‑property error – the validator returns an
 *          empty error set immediately.</li>
 *      <li>Successful validation where the {@code matchedNode} flag is false – the schema is
 *          ignored for the purpose of counting valid subschemas.</li>
 *      <li>Mixed success/failure where only “required” errors are present – those errors are
 *          stripped when at least one subschema validates successfully.</li>
 *      <li>OpenAPI‑3 discriminator enabled scenarios (match found with/without errors,
 *          no match found).</li>
 *      <li>The {@code walk(...)} delegation logic.</li>
 *      <li>The {@code preloadJsonSchema()} helper.</li>
 *  </ul>
 *
 *  The test class is deliberately self‑contained and does not depend on any external JSON files.
 *  All JSON nodes are created with Jackson's {@code JsonNodeFactory}.
 *
 ***************************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static com.networknt.schema.ValidatorTypeCode.ANY_OF;
import static com.networknt.schema.ValidatorTypeCode.REQUIRED;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for {@link AnyOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /* --------------------------------------------------------------------- *
     *  Test fixtures – common objects used by many test methods
     * --------------------------------------------------------------------- */

    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private JsonSchema parentSchema;

    private JsonNodeFactory factory = JsonNodeFactory.instance;
    private JsonNode dummyNode;               // node passed to the validator
    private JsonNode dummyRoot;               // root node (not used by the validator)
    private String at = "$.test";             // JSON‑Pointer location used in messages

    @Before
    public void setUp() {
        // By default the configuration disables OpenAPI‑3 discriminators.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // ExecutionContext returns the mocked CollectorContext.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // The CollectorContext stores the validator state under the known key.
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // The state initially reports that no node has been matched.
        when(validatorState.hasMatchedNode()).thenReturn(false);
        // Walk is disabled for the majority of tests (the default behaviour of the library).
        when(validatorState.isWalkEnabled()).thenReturn(false);
        // Scopes are simple mocks – they do nothing when merged.
        when(collectorContext.enterDynamicScope()).thenReturn(mock(Scope.class));
        when(collectorContext.exitDynamicScope()).thenReturn(mock(Scope.class));

        dummyNode = factory.objectNode(); // empty object node
        dummyRoot = factory.objectNode();
    }

    /* --------------------------------------------------------------------- *
     *  Helper – a concrete subclass that overrides the protected message builder.
     * --------------------------------------------------------------------- */

    /**
     * {@code AnyOfValidator} builds {@link ValidationMessage}s via a protected helper.
     * For deterministic testing we override that helper to create a simple message that
     * contains the supplied {@code type} and {@code at} values.
     */
    private static class TestableAnyOfValidator extends AnyOfValidator {
        TestableAnyOfValidator(String schemaPath,
                               JsonNode schemaNode,
                               JsonSchema parentSchema,
                               ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        protected ValidationMessage buildValidationMessage(String at, String message) {
            // Use the ANY_OF type for all messages created by the test subclass.
            return new ValidationMessage(ANY_OF, at, message);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Helper – builds a mock JsonSchema that either:
     *          * has a TypeValidator (or not)
     *          * returns a predefined validation result set
     * --------------------------------------------------------------------- */

    /**
     * Creates a mock {@link JsonSchema} whose {@code validate(...)} (or {@code walk(...)} )
     * returns {@code result}.  The schema can be configured to have a {@link TypeValidator}
     * that either matches or mismatches the supplied {@code nodeType}.
     *
     * @param result          the validation result to be returned.
     * @param hasTypeValidator whether the schema reports a type validator.
     * @param typeMatches      if {@code hasTypeValidator} is true, whether the type matches the node.
     * @return a fully‑configured mock {@link JsonSchema}.
     */
    private JsonSchema mockSchema(Set<ValidationMessage> result,
                                  boolean hasTypeValidator,
                                  boolean typeMatches) {
        JsonSchema schema = mock(JsonSchema.class);
        when(schema.validate(any(), any(), any(), any())).thenReturn(result);
        when(schema.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(result);
        when(schema.hasTypeValidator()).thenReturn(hasTypeValidator);
        if (hasTypeValidator) {
            TypeValidator tv = mock(TypeValidator.class);
            when(schema.getTypeValidator()).thenReturn(tv);
            // For the purpose of the test we only need the {@code equalsToSchemaType(...)} check.
            when(tv.equalsToSchemaType(any())).thenReturn(typeMatches);
            // The schema type used in the error message – any non‑UNION value works.
            when(tv.getSchemaType()).thenReturn(JsonType.STRING);
        }
        // The validator may have a child schema (parent) – not needed for these tests.
        when(schema.getParentSchema()).thenReturn(parentSchema);
        return schema;
    }

    /* --------------------------------------------------------------------- *
     *  Test Cases – validation without OpenAPI‑3 discriminators
     * --------------------------------------------------------------------- */

    @Test
    public void validate_typeMismatchAddsErrorAndSkipsSchema() {
        // -------------------------------------------------------------
        // 1️⃣  Build a schema whose type validator does NOT match the node.
        // -------------------------------------------------------------
        JsonSchema mismatchingSchema = mockSchema(Collections.emptySet(),
                true, /* has type validator */ false /* type does NOT match */);

        // The validator will be constructed with a single child schema.
        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode()); // any JSON – size = 1
        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);

        // Replace the automatically created child schema with our mock.
        // (The constructor adds a schema for each array element – we replace it via reflection.)
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(mismatchingSchema);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Execute validation.
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);

        // -------------------------------------------------------------
        // 3️⃣  Verify that a single type‑mismatch message is produced.
        // -------------------------------------------------------------
        assertEquals(1, messages.size());
        ValidationMessage msg = messages.iterator().next();
        assertEquals(ANY_OF, msg.getType());
        // The message contains the schema type (STRING) as defined in the mock.
        assertTrue(msg.getMessage().contains("STRING"));
    }

    @Test
    public void validate_successfulSchemaReturnsEmptySetImmediately() {
        // -------------------------------------------------------------
        // 1️⃣  Build a schema that validates successfully (no errors)
        //     and whose type validator matches the node.
        // -------------------------------------------------------------
        JsonSchema goodSchema = mockSchema(Collections.emptySet(),
                true, /* has type validator */ true /* type matches */);

        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode());
        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);

        // Inject the mock schema.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(goodSchema);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Execute validation.
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);

        // -------------------------------------------------------------
        // 3️⃣  Expect an empty result set.
        // -------------------------------------------------------------
        assertTrue(messages.isEmpty());
        // The validator must have set the matched‑node flag on the state.
        verify(validatorState).setMatchedNode(true);
    }

    @Test
    public void validate_successfulSchemaButMatchedNodeFalseIsIgnored() {
        // -------------------------------------------------------------
        // 1️⃣  The schema validates without errors, but the validator state
        //     reports that no node has been matched (hasMatchedNode == false).
        // -------------------------------------------------------------
        when(validatorState.hasMatchedNode()).thenReturn(false);
        JsonSchema schema = mockSchema(Collections.emptySet(),
                true, true);

        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode());
        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);

        // Inject mock schema.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(schema);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Execute validation.
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);

        // -------------------------------------------------------------
        // 3️⃣  No valid subschema was counted – the validator should return the
        //     (empty) error set generated by the loop (i.e. still empty) but
        //     it must NOT set the matched‑node flag.
        // -------------------------------------------------------------
        assertTrue(messages.isEmpty());
        verify(validatorState, never()).setMatchedNode(true);
    }

    @Test
    public void validate_mixedErrorsRequiredErrorsAreRemovedWhenValidSubschemaExists() {
        // -------------------------------------------------------------
        // 1️⃣  Two child schemas:
        //     • schemaA – fails with a "required" error.
        //     • schemaB – succeeds (empty error set).
        // -------------------------------------------------------------
        ValidationMessage requiredMsg = new ValidationMessage(REQUIRED, at, "missing required");
        Set<ValidationMessage> requiredSet = new LinkedHashSet<>(Collections.singleton(requiredMsg));

        JsonSchema schemaA = mockSchema(requiredSet, false, false);
        JsonSchema schemaB = mockSchema(Collections.emptySet(), false, false);

        JsonNode schemaNode = factory.arrayNode()
                .add(factory.objectNode())
                .add(factory.objectNode());

        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);

        // Inject the two schemas.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(schemaA);
            list.add(schemaB);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Execute validation.
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);

        // -------------------------------------------------------------
        // 3️⃣  Because schemaB validates successfully, the required‑error from
        //     schemaA must be stripped, resulting in an empty set.
        // -------------------------------------------------------------
        assertTrue(messages.isEmpty());
    }

    @Test
    public void validate_allSchemasFailAndDiscriminatorActiveReturnsDiscriminatorMessage() {
        // -------------------------------------------------------------
        // 1️⃣  Enable OpenAPI‑3 style discriminators.
        // -------------------------------------------------------------
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // The discriminator context must report that it is active but that no match was found.
        ValidationContext.DiscriminatorContext discriminatorContext = new ValidationContext.DiscriminatorContext();
        // We need a spy on the validator to expose the internal discriminator context.
        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode());

        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext) {
            @Override
            protected ValidationMessage buildValidationMessage(String at, String message) {
                return new ValidationMessage(ANY_OF, at, message);
            }
        };

        // Replace the automatically created discriminator context with our own.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discriminatorContext);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Add a single failing child schema (any error set).
        // -------------------------------------------------------------
        ValidationMessage err = new ValidationMessage(ANY_OF, at, "some error");
        JsonSchema failingSchema = mockSchema(new LinkedHashSet<>(Collections.singleton(err)),
                false, false);
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(failingSchema);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 3️⃣  Execute validation – because the discriminator is active but no
        //     match is found, the validator must return the generic discriminator
        //     error message.
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);
        assertEquals(1, messages.size());
        ValidationMessage returned = messages.iterator().next();
        assertTrue(returned.getMessage().contains("based on the provided discriminator"));
    }

    /* --------------------------------------------------------------------- *
     *  Test Cases – validation with OpenAPI‑3 discriminators enabled
     * --------------------------------------------------------------------- */

    @Test
    public void validate_discriminatorMatchFoundWithErrorsAddsRemark() {
        // -------------------------------------------------------------
        // 1️⃣  Enable discriminators and simulate a match being found.
        // -------------------------------------------------------------
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ValidationContext.DiscriminatorContext discriminatorContext = new ValidationContext.DiscriminatorContext();
        discriminatorContext.setDiscriminatorMatchFound(true); // simulate match

        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode());

        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);
        // inject the prepared discriminator context
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discriminatorContext);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Child schema returns a non‑empty error set.
        // -------------------------------------------------------------
        ValidationMessage childError = new ValidationMessage(ANY_OF, at, "child error");
        JsonSchema child = mockSchema(new LinkedHashSet<>(Collections.singleton(childError)),
                false, false);
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(child);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 3️⃣  Execute validation – the validator must return the child error
        //     plus the discriminator remark.
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);
        assertEquals(2, messages.size());

        boolean hasRemark = messages.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Expected discriminator remark", hasRemark);
    }

    @Test
    public void validate_discriminatorMatchFoundWithoutErrorsClearsAllErrors() {
        // -------------------------------------------------------------
        // 1️⃣  Enable discriminators and indicate a match.
        // -------------------------------------------------------------
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ValidationContext.DiscriminatorContext discriminatorContext = new ValidationContext.DiscriminatorContext();
        discriminatorContext.setDiscriminatorMatchFound(true);

        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode());

        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);
        // inject discriminator context
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
            f.setAccessible(true);
            f.set(validator, discriminatorContext);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Child schema validates with an empty error set.
        // -------------------------------------------------------------
        JsonSchema child = mockSchema(Collections.emptySet(), false, false);
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(child);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 3️⃣  Execute – the validator must return an empty set (no errors).
        // -------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, dummyNode, dummyRoot, at);
        assertTrue(messages.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  Test Cases – walk method
     * --------------------------------------------------------------------- */

    @Test
    public void walk_shouldValidateWhenFlagTrue() {
        // -------------------------------------------------------------
        // 1️⃣  Build a validator with a single child schema that returns a known error.
        // -------------------------------------------------------------
        ValidationMessage err = new ValidationMessage(ANY_OF, at, "walk error");
        JsonSchema child = mockSchema(new LinkedHashSet<>(Collections.singleton(err)), false, false);
        JsonNode schemaNode = factory.arrayNode().add(factory.objectNode());

        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(child);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Call walk with shouldValidateSchema = true – should delegate to validate().
        // -------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, dummyRoot, at, true);
        assertEquals(1, result.size());
        assertEquals("walk error", result.iterator().next().getMessage());
    }

    @Test
    public void walk_withoutValidationDelegatesToChildrenAndReturnsEmptySet() {
        // -------------------------------------------------------------
        // 1️⃣  Build validator with two child schemas – we only verify that
        //     their walk(...) methods are invoked.
        // -------------------------------------------------------------
        JsonSchema child1 = mock(JsonSchema.class);
        JsonSchema child2 = mock(JsonSchema.class);
        when(child1.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());
        when(child2.walk(any(), any(), any(), any(), eq(false))).thenReturn(Collections.emptySet());

        JsonNode schemaNode = factory.arrayNode()
                .add(factory.objectNode())
                .add(factory.objectNode());

        TestableAnyOfValidator validator = new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext);
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(child1);
            list.add(child2);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Invoke walk with shouldValidateSchema = false.
        // -------------------------------------------------------------
        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode, dummyRoot, at, false);
        assertTrue(result.isEmpty());

        // Verify that each child schema's walk method was called exactly once.
        verify(child1).walk(any(), any(), any(), any(), eq(false));
        verify(child2).walk(any(), any(), any(), any(), eq(false));
    }

    /* --------------------------------------------------------------------- *
     *  Test Cases – preloadJsonSchema method
     * --------------------------------------------------------------------- */

    @Test
    public void preloadJsonSchema_preloadsAllChildSchemas() {
        // -------------------------------------------------------------
        // 1️⃣  Create two child schemas and spy on the validator to intercept
        //     the protected preloadJsonSchemas(...) call.
        // -------------------------------------------------------------
        JsonSchema child1 = mock(JsonSchema.class);
        JsonSchema child2 = mock(JsonSchema.class);

        JsonNode schemaNode = factory.arrayNode()
                .add(factory.objectNode())
                .add(factory.objectNode());

        // Spy on a concrete subclass so we can verify the protected helper.
        TestableAnyOfValidator validator = Mockito.spy(new TestableAnyOfValidator(
                "anyOf", schemaNode, parentSchema, validationContext));

        // Replace the internal schema list with our mocks.
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            list.clear();
            list.add(child1);
            list.add(child2);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        // -------------------------------------------------------------
        // 2️⃣  Call preloadJsonSchema().
        // -------------------------------------------------------------
        validator.preloadJsonSchema();

        // -------------------------------------------------------------
        // 3️⃣  Verify that the protected helper was invoked with the exact list.
        // -------------------------------------------------------------
        // The helper is defined in BaseJsonValidator – we verify it via the spy.
        verify(validator, times(1)).preloadJsonSchemas(argThat(schemas -> schemas.size() == 2
                && schemas.containsAll(Arrays.asList(child1, child2))));
    }
}
```
###Test END##