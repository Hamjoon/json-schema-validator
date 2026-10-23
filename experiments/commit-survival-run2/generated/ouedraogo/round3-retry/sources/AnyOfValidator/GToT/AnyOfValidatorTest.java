/****************************************************************************************
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}
 *
 * The tests cover:
 *   1. Public API extraction
 *   2. Basic functionality of each public method
 *   3. Edge‑case and exception scenarios (type‑mismatch, discriminator handling,
 *      required‑property filtering, walk‑mode flag, etc.)
 *
 * The suite uses Mockito for all external collaborators (ExecutionContext,
 * CollectorContext, ValidationContext, JsonSchema, JsonNode, etc.) and therefore
 * does not depend on the full schema‑validation runtime.
 *
 * Author:  ChatGPT – simulated “three‑expert” collaboration
 * Date:    2026‑10‑23
 ****************************************************************************************/

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link AnyOfValidator}.
 *
 * The test file is demarcated with the required markers:
 *   - starts with {@code ###Test START##}
 *   - ends   with {@code ###Test END##}
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /* -------------------------------------------------------------------------
     *  PUBLIC METHODS OF ANYOFVALIDATOR (extracted for reference)
     * -------------------------------------------------------------------------
     *
     * 1. AnyOfValidator(String schemaPath,
     *                  JsonNode schemaNode,
     *                  JsonSchema parentSchema,
     *                  ValidationContext validationContext)
     *
     * 2. Set<ValidationMessage> validate(ExecutionContext executionContext,
     *                                   JsonNode node,
     *                                   JsonNode rootNode,
     *                                   String at)
     *
     * 3. Set<ValidationMessage> walk(ExecutionContext executionContext,
     *                                 JsonNode node,
     *                                 JsonNode rootNode,
     *                                 String at,
     *                                 boolean shouldValidateSchema)
     *
     * 4. void preloadJsonSchema()
     *
     * -------------------------------------------------------------------------
     */

    // -------------------------------------------------------------------------
    //  Mocks shared by all test cases
    // -------------------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private JsonNode schemaNode;          // the node that holds the anyOf array
    @Mock private JsonNode instanceNode;        // node being validated
    @Mock private JsonNode rootNode;            // root of the document
    @Mock private JsonSchema parentSchema;      // not used directly in tests
    @Mock private ValidatorState validatorState; // stored inside CollectorContext

    // Helper objects created per test
    private AnyOfValidator validator;
    private List<JsonSchema> stubSchemas; // schemas we inject via reflection

    // -------------------------------------------------------------------------
    //  Common setup
    // -------------------------------------------------------------------------
    @Before
    public void setUp() throws Exception {
        // ---------------------------------------------------------------------
        //  Default behaviour for the ValidationContext hierarchy
        // ---------------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        // By default we *disable* OpenAPI‑3 style discriminators (most tests)
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // ExecutionContext must return a CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext must supply a fresh ValidatorState under a well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Dynamic scope handling – we use simple mocks that do nothing on merge
        Scope dummyScope = mock(Scope.class);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        doNothing().when(dummyScope).mergeWith(any(Scope.class));

        // ---------------------------------------------------------------------
        //  Build a validator instance with an *empty* anyOf array.  We will
        //  inject our own stub schemas later (via reflection) because the
        //  constructor creates schemas from the supplied JsonNode.
        // ---------------------------------------------------------------------
        when(schemaNode.size()).thenReturn(0);
        validator = new AnyOfValidator("#/anyOf", schemaNode, parentSchema, validationContext);

        // Prepare a mutable list that we will push into the private field
        stubSchemas = new ArrayList<>();
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, stubSchemas);
    }

    // -------------------------------------------------------------------------
    //  Utility methods
    // -------------------------------------------------------------------------

    /** Creates a mock {@link JsonSchema} that returns the supplied error set
     *  when its {@code validate(...)} or {@code walk(...)} method is invoked. */
    private JsonSchema mockSchema(Set<ValidationMessage> errors) {
        JsonSchema schema = mock(JsonSchema.class);
        // Both validate() and walk() are used by AnyOfValidator; make them equivalent
        when(schema.validate(any(), any(), any(), any())).thenReturn(errors);
        when(schema.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(errors);
        // By default a schema has no TypeValidator – this bypasses the type‑check logic
        when(schema.hasTypeValidator()).thenReturn(false);
        return schema;
    }

    /** Helper to build a simple ValidationMessage with a custom type. */
    private ValidationMessage vm(String type, String path, String message) {
        ValidationMessage vm = mock(ValidationMessage.class);
        when(vm.getType()).thenReturn(type);
        when(vm.getPath()).thenReturn(path);
        when(vm.getMessage()).thenReturn(message);
        return vm;
    }

    // -------------------------------------------------------------------------
    //  BASIC FUNCTIONALITY TESTS
    // -------------------------------------------------------------------------

    /** 1️⃣  validate() – all subschemas succeed → empty error set */
    @Test
    public void testValidate_AllSubSchemasPass() {
        // two passing schemas (empty error set)
        stubSchemas.add(mockSchema(Collections.emptySet()));
        stubSchemas.add(mockSchema(Collections.emptySet()));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        assertTrue("When at least one subschema validates, AnyOfValidator must return an empty set",
                result.isEmpty());
        verify(validatorState).setMatchedNode(true); // state should be marked as matched
    }

    /** 2️⃣  validate() – one subschema passes, another fails → empty error set */
    @Test
    public void testValidate_OnePassOneFail() {
        ValidationMessage failMsg = vm("type", "$.foo", "type mismatch");
        stubSchemas.add(mockSchema(new LinkedHashSet<>(Arrays.asList(failMsg))));
        stubSchemas.add(mockSchema(Collections.emptySet())); // passing schema

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        assertTrue("A single successful subschema should suppress all other errors", result.isEmpty());
        verify(validatorState).setMatchedNode(true);
    }

    /** 3️⃣  validate() – *no* subschema passes → combined error set */
    @Test
    public void testValidate_NoSubSchemaPasses() {
        ValidationMessage err1 = vm("required", "$", "missing required property");
        ValidationMessage err2 = vm("type", "$", "type mismatch");
        stubSchemas.add(mockSchema(new LinkedHashSet<>(Collections.singleton(err1))));
        stubSchemas.add(mockSchema(new LinkedHashSet<>(Collections.singleton(err2))));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Both errors should be present because there is no valid subschema
        assertEquals(2, result.size());
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));
        verify(validatorState, never()).setMatchedNode(true);
    }

    /** 4️⃣  walk() with {@code shouldValidateSchema = true} must delegate to validate() */
    @Test
    public void testWalk_ValidateFlagTrue() {
        // one passing schema → validate() returns empty set
        stubSchemas.add(mockSchema(Collections.emptySet()));

        // spy the validator to verify that validate() is invoked
        AnyOfValidator spyValidator = spy(validator);
        Set<ValidationMessage> result = spyValidator.walk(executionContext, instanceNode, rootNode, "$", true);

        verify(spyValidator, times(1)).validate(executionContext, instanceNode, rootNode, "$");
        assertTrue(result.isEmpty());
    }

    /** 5️⃣  walk() with {@code shouldValidateSchema = false} must walk each subschema
     *       (but must not aggregate errors) */
    @Test
    public void testWalk_ValidateFlagFalse() {
        // two schemas that would each return an error if validated
        ValidationMessage err = vm("type", "$", "type error");
        JsonSchema s1 = mockSchema(new LinkedHashSet<>(Collections.singleton(err)));
        JsonSchema s2 = mockSchema(new LinkedHashSet<>(Collections.singleton(err)));

        stubSchemas.add(s1);
        stubSchemas.add(s2);

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "$", false);

        // walk() should *not* return any errors when the flag is false
        assertTrue(result.isEmpty());

        // verify that each schema's walk() method was called with shouldValidate = false
        verify(s1, times(1)).walk(executionContext, instanceNode, rootNode, "$", false);
        verify(s2, times(1)).walk(executionContext, instanceNode, rootNode, "$", false);
    }

    /** 6️⃣  preloadJsonSchema() must invoke the protected helper method
     *       {@code preloadJsonSchemas(List<JsonSchema>)} */
    @Test
    public void testPreloadJsonSchema_InvokesHelper() throws Exception {
        // Add two dummy schemas
        stubSchemas.add(mock(JsonSchema.class));
        stubSchemas.add(mock(JsonSchema.class));

        // Spy the validator to capture the protected call
        AnyOfValidator spyValidator = spy(validator);
        // Use reflection to make the protected method visible for verification
        doNothing().when(spyValidator).preloadJsonSchemas(anyList());

        spyValidator.preloadJsonSchema();

        // Verify that the helper method was called exactly once with the internal list
        verify(spyValidator, times(1)).preloadJsonSchemas(same(stubSchemas));
    }

    // -------------------------------------------------------------------------
    //  EDGE‑CASE & DISCRIMINATOR‑SPECIFIC TESTS
    // -------------------------------------------------------------------------

    /** 7️⃣  When OpenAPI‑3 style discriminators are enabled and a discriminator match
     *       is found *and* the chosen subschema fails validation, the returned set
     *       must contain the original errors **plus** the discriminator remark. */
    @Test
    public void testValidate_DiscriminatorMatchFound_WithErrors() {
        // Enable discriminator handling in the configuration
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Re‑create validator so that it creates a non‑null DiscriminatorContext
        validator = new AnyOfValidator("$/anyOf", schemaNode, parentSchema, validationContext);
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        stubSchemas = new ArrayList<>();
        f.set(validator, stubSchemas);

        // Prepare a schema that returns an error
        ValidationMessage subErr = vm("type", "$", "type error");
        JsonSchema errorSchema = mockSchema(new LinkedHashSet<>(Collections.singleton(subErr)));
        stubSchemas.add(errorSchema);

        // Mock the discrimination context to signal that a match was found
        ValidationContext.DiscriminatorContext dc = mock(ValidationContext.DiscriminatorContext.class);
        when(dc.isDiscriminatorMatchFound()).thenReturn(true);
        // inject the mocked context – it is created inside the constructor, so we replace it via reflection
        java.lang.reflect.Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        dcField.set(validator, dc);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Expect original error + the remark about the discriminator
        assertEquals(2, result.size());
        assertTrue(result.contains(subErr));
        boolean remarkFound = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result must contain the discriminator‑remark message", remarkFound);
    }

    /** 8️⃣  When discriminators are active but *no* match is found, the validator must
     *       return a single message explaining that no alternative could be chosen. */
    @Test
    public void testValidate_DiscriminatorActive_NoMatchFound() {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        // Re‑instantiate to get a fresh DiscriminatorContext
        validator = new AnyOfValidator("$/anyOf", schemaNode, parentSchema, validationContext);
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        stubSchemas = new ArrayList<>();
        f.set(validator, stubSchemas);
        // Add a schema that would pass (empty error set)
        stubSchemas.add(mockSchema(Collections.emptySet()));

        // Mock the discriminator context to be active but never report a match
        ValidationContext.DiscriminatorContext dc = mock(ValidationContext.DiscriminatorContext.class);
        when(dc.isActive()).thenReturn(true);
        when(dc.isDiscriminatorMatchFound()).thenReturn(false);
        java.lang.reflect.Field dcField = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        dcField.setAccessible(true);
        dcField.set(validator, dc);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Exactly one message describing the discriminator failure must be returned
        assertEquals(1, result.size());
        ValidationMessage onlyMsg = result.iterator().next();
        assertTrue("Message must mention the discriminator", onlyMsg.getMessage()
                .contains("based on the provided discriminator"));
    }

    /** 9️⃣  Type‑validator shortcut – if a subschema defines a concrete type that does
     *       not match the instance node, the validator must skip that subschema and
     *       add a type‑mismatch error for the *anyOf* keyword. */
    @Test
    public void testValidate_TypeValidatorMismatch() {
        // Create a mock TypeValidator that claims the schema type is STRING
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        // Assume the instance node is NOT a string → equalsToSchemaType returns false
        when(typeValidator.equalsToSchemaType(instanceNode)).thenReturn(false);

        // Mock a schema that reports it has a type validator
        JsonSchema schemaWithType = mock(JsonSchema.class);
        when(schemaWithType.hasTypeValidator()).thenReturn(true);
        when(schemaWithType.getTypeValidator()).thenReturn(typeValidator);
        // Validation of such a schema is never reached, but we still need a return value
        when(schemaWithType.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        stubSchemas.add(schemaWithType);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // The AnyOfValidator adds a type‑mismatch message for the ANY_OF validator
        assertEquals(1, result.size());
        ValidationMessage vm = result.iterator().next();
        assertEquals(ValidatorTypeCode.ANY_OF.getValue(), vm.getType());
    }

    /** 10️⃣  Required‑property filtering – when at least one subschema validates
     *        successfully, any “required” errors coming from the *other* subschemas
     *        must be discarded. */
    @Test
    public void testValidate_RequiredErrorFiltering() {
        // Subschema #1 fails with a required‑property error
        ValidationMessage requiredErr = vm(ValidatorTypeCode.REQUIRED.getValue(),
                "$", "required property missing");
        stubSchemas.add(mockSchema(new LinkedHashSet<>(Collections.singleton(requiredErr))));

        // Subschema #2 passes (empty set)
        stubSchemas.add(mockSchema(Collections.emptySet()));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Because a valid subschema exists, the required‑error must be filtered out
        assertTrue("Result must be empty – required errors are removed when any subschema passes",
                result.isEmpty());
    }

    // -------------------------------------------------------------------------
    //  Helper class to expose the protected method for verification (used only in the
    //  preloadJsonSchema test).  This class lives inside the test file so it does not
    //  affect production code.
    // -------------------------------------------------------------------------
    private static abstract class AnyOfValidatorTestHook extends AnyOfValidator {
        AnyOfValidatorTestHook(String schemaPath, JsonNode schemaNode,
                               JsonSchema parentSchema, ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validationContext);
        }

        // expose the protected method
        @Override
        public void preloadJsonSchemas(List<JsonSchema> schemas) {
            super.preloadJsonSchemas(schemas);
        }
    }
}
