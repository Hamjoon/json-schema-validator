/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * The test class follows the steps requested:
 *
 * 1. **Public API extraction**
 *    <pre>
 *    public AnyOfValidator(String schemaPath,
 *                           JsonNode schemaNode,
 *                           JsonSchema parentSchema,
 *                           ValidationContext validationContext)
 *
 *    public Set<ValidationMessage> validate(ExecutionContext executionContext,
 *                                            JsonNode node,
 *                                            JsonNode rootNode,
 *                                            String at)
 *
 *    public Set<ValidationMessage> walk(ExecutionContext executionContext,
 *                                         JsonNode node,
 *                                         JsonNode rootNode,
 *                                         String at,
 *                                         boolean shouldValidateSchema)
 *
 *    public void preloadJsonSchema()
 *    </pre>
 *
 * 2. **Basic test cases** – one test per public method that checks the “happy‑path”.
 *
 * 3. **Edge‑case & exception scenarios** – type‑mismatch handling, mixed‑error/valid
 *    subschemas, discriminator disabled/enabled branches and the `walk`‑flag behaviour.
 *
 * 4. **All tests merged** into a single file that can be compiled and run with JUnit‑4.
 *
 * The tests rely heavily on Mockito to stub the complex collaborators
 * (`ValidationContext`, `ExecutionContext`, `JsonSchema`, `CollectorContext`, …).
 * Only the behaviour that is exercised by the validator is stubbed.
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    // -------------------------------------------------------------------------
    //  Helper fields – common collaborators used by many tests
    // -------------------------------------------------------------------------
    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonNode schemaNode;          // the “anyOf” array node

    @Mock
    private JsonNode instanceNode;        // the JSON instance being validated

    @Mock
    private JsonNode rootNode;            // root of the document (not used in tests)

    @Captor
    private ArgumentCaptor<Set<ValidationMessage>> messageCaptor;

    private static final String SCHEMA_PATH = "$.anyOf";
    private static final String AT = "$";

    // -------------------------------------------------------------------------
    //  Common stubbing performed before each test
    // -------------------------------------------------------------------------
    @Before
    public void setUp() {
        // ValidationContext returns its config and creates JsonSchema objects on demand
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(invocation -> mock(JsonSchema.class));

        // ExecutionContext provides a CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // CollectorContext returns a fresh ValidatorState for the validator key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // Dynamic scopes are no‑ops for the purpose of these tests
        Scope dummyScope = mock(Scope.class);
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
    }

    // -------------------------------------------------------------------------
    //  Factory method – builds an AnyOfValidator with a given number of subschemas
    // -------------------------------------------------------------------------
    private AnyOfValidator createValidator(int numberOfSchemas, boolean openApiDiscriminator) {
        // make the config answer the discriminator flag
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(openApiDiscriminator);

        // mock the array node – size() and get(i) must behave
        when(schemaNode.size()).thenReturn(numberOfSchemas);
        for (int i = 0; i < numberOfSchemas; i++) {
            when(schemaNode.get(i)).thenReturn(mock(JsonNode.class));
        }

        // Build the validator (the constructor will call validationContext.newSchema)
        return new AnyOfValidator(SCHEMA_PATH, schemaNode, null, validationContext);
    }

    // -------------------------------------------------------------------------
    //  1. Basic happy‑path test – all subschemas validate without errors
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_AllSchemasPass_ReturnsEmptySet() {
        AnyOfValidator validator = createValidator(2, false);

        // each subschema returns an empty error set
        for (JsonSchema schema : getInternalSchemas(validator)) {
            when(schema.validate(executionContext, instanceNode, rootNode, AT)).thenReturn(Collections.emptySet());
            when(schema.walk(executionContext, instanceNode, rootNode, AT, true))
                    .thenReturn(Collections.emptySet());
        }

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertTrue("When all subschemas pass, result must be empty", result.isEmpty());

        // verify that each schema was asked to validate exactly once
        for (JsonSchema schema : getInternalSchemas(validator)) {
            verify(schema, times(1)).validate(executionContext, instanceNode, rootNode, AT);
        }
    }

    // -------------------------------------------------------------------------
    //  2. Mixed error / success – one schema fails, another succeeds
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_MixedSchemas_ValidSubschemaClearsErrors() {
        AnyOfValidator validator = createValidator(2, false);

        List<JsonSchema> schemas = getInternalSchemas(validator);
        // first schema – produces an error
        ValidationMessage err = mock(ValidationMessage.class);
        when(err.getType()).thenReturn("type");
        Set<ValidationMessage> firstErrors = new LinkedHashSet<>(Collections.singleton(err));
        when(schemas.get(0).validate(executionContext, instanceNode, rootNode, AT)).thenReturn(firstErrors);
        when(schemas.get(0).walk(executionContext, instanceNode, rootNode, AT, true)).thenReturn(firstErrors);

        // second schema – succeeds
        when(schemas.get(1).validate(executionContext, instanceNode, rootNode, AT))
                .thenReturn(Collections.emptySet());
        when(schemas.get(1).walk(executionContext, instanceNode, rootNode, AT, true))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertTrue("A successful subschema should cause the final error set to be empty", result.isEmpty());

        // verify both schemas were invoked
        verify(schemas.get(0), times(1)).validate(executionContext, instanceNode, rootNode, AT);
        verify(schemas.get(1), times(1)).validate(executionContext, instanceNode, rootNode, AT);
    }

    // -------------------------------------------------------------------------
    //  3. Edge case – type validator mismatch leads to a type‑error message
    // -------------------------------------------------------------------------
    @Test
    public void testValidate_TypeValidatorMismatch_AddsTypeError() {
        AnyOfValidator validator = createValidator(1, false);

        JsonSchema schema = getInternalSchemas(validator).get(0);

        // mock a TypeValidator that does NOT match the instance node
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING); // expects a string
        when(typeValidator.equalsToSchemaType(instanceNode)).thenReturn(false);
        when(schema.hasTypeValidator()).thenReturn(true);
        when(schema.getTypeValidator()).thenReturn(typeValidator);

        // even though the type does not match, the validator should add a message
        // (the actual text is produced by BaseJsonValidator.buildValidationMessage)
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, AT);
        assertEquals("Exactly one validation message (the type error) is expected", 1, result.size());

        // ensure the underlying schema.validate() was never called because of early continue
        verify(schema, never()).validate(any(), any(), any(), anyString());
    }

    // -------------------------------------------------------------------------
    //  4. walk(…, shouldValidateSchema = true) delegates to validate()
    // -------------------------------------------------------------------------
    @Test
    public void testWalk_ShouldValidateTrue_DelegatesToValidate() {
        AnyOfValidator validator = spy(createValidator(1, false));

        // stub the validate method to return a known set
        Set<ValidationMessage> fakeResult = Collections.singleton(mock(ValidationMessage.class));
        doReturn(fakeResult).when(validator).validate(executionContext, instanceNode, rootNode, AT);

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, AT, true);
        assertSame("walk with shouldValidateSchema=true must return the result of validate()", fakeResult, result);

        // verify that validate() was indeed called
        verify(validator, times(1)).validate(executionContext, instanceNode, rootNode, AT);
    }

    // -------------------------------------------------------------------------
    //  5. walk(…, shouldValidateSchema = false) iterates over subschemas
    // -------------------------------------------------------------------------
    @Test
    public void testWalk_ShouldValidateFalse_InvokesSchemaWalk() {
        AnyOfValidator validator = createValidator(3, false);
        List<JsonSchema> schemas = getInternalSchemas(validator);

        // make each schema return an empty set when walk is called with shouldValidate = false
        for (JsonSchema schema : schemas) {
            when(schema.walk(executionContext, instanceNode, rootNode, AT, false))
                    .thenReturn(Collections.emptySet());
        }

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, AT, false);
        assertTrue("walk with shouldValidateSchema=false must return an empty set", result.isEmpty());

        // verify each schema.walk was called exactly once with shouldValidate = false
        for (JsonSchema schema : schemas) {
            verify(schema, times(1)).walk(executionContext, instanceNode, rootNode, AT, false);
        }
    }

    // -------------------------------------------------------------------------
    //  6. preloadJsonSchema() should delegate to BaseJsonValidator.preloadJsonSchemas()
    // -------------------------------------------------------------------------
    @Test
    public void testPreloadJsonSchema_DelegatesToBaseClass() {
        // Create a spy that overrides the protected method to capture the argument
        AnyOfValidator validator = spy(new AnyOfValidator(SCHEMA_PATH, schemaNode, null, validationContext) {
            @Override
            protected void preloadJsonSchemas(List<JsonSchema> schemas) {
                // capture the argument for verification
                assertEquals("The internal schema list size must be the same as the number of child schemas",
                        schemaNode.size(), schemas.size());
                // No further action required for the test
            }
        });

        // invoke the method under test
        validator.preloadJsonSchema();

        // verify that the overridden protected method was indeed called
        verify(validator, times(1)).preloadJsonSchemas(anyList());
    }

    // -------------------------------------------------------------------------
    //  Helper – retrieve the private “schemas” field via reflection
    // -------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private List<JsonSchema> getInternalSchemas(AnyOfValidator validator) {
        try {
            java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            return (List<JsonSchema>) f.get(validator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
