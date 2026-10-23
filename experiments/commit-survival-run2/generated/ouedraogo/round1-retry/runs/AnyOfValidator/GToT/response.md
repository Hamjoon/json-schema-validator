###Test START##
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AnyOfValidator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Constructor behaviour (schema list creation, discriminator handling)</li>
 *   <li>Normal validation flow (type‑mismatch, successful validation, required‑error filtering)</li>
 *   <li>Discriminator‑related edge cases (when the discriminator is enabled)</li>
 *   <li>walk() delegation logic</li>
 *   <li>preloadJsonSchema() execution path</li>
 * </ul>
 *
 * All external collaborators are heavily mocked (Jackson nodes, ValidationContext,
 * ExecutionContext, CollectorContext, JsonSchema, TypeValidator, etc.) so that the
 * tests focus exclusively on the logic inside {@code AnyOfValidator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    private static final String SCHEMA_PATH = "$.anyOf";

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.Config config;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private CollectorContext.Scope grandParentScope;

    @Mock
    private CollectorContext.Scope parentScope;

    @Mock
    private CollectorContext.Scope childScope;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonSchema childSchema;

    @Mock
    private TypeValidator typeValidator;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        // ValidationContext configuration
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false); // default – overridden per test

        // ExecutionContext provides a CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // Dynamic scope handling – a simple stub that returns the same mock scope each time
        when(collectorContext.enterDynamicScope()).thenReturn(parentScope);
        when(parentScope.mergeWith(any(Scope.class))).thenReturn(parentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope);
        when(grandParentScope.mergeWith(any(Scope.class))).thenReturn(grandParentScope);
    }

    /**
     * Helper that creates an {@code AnyOfValidator} with the supplied {@code schemaNode}
     * and the current mock {@link ValidationContext}.
     */
    private AnyOfValidator createValidator(JsonNode schemaNode) {
        // the constructor will call validationContext.newSchema for each child node
        // we simply make it return the same mock childSchema for every call
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema);
        return new AnyOfValidator(SCHEMA_PATH, schemaNode, mock(JsonSchema.class), validationContext);
    }

    /**
     * 1. Constructor – empty schema array (no subschemas)
     */
    @Test
    public void testConstructorWithEmptyArray() {
        ArrayNode emptyArray = mapper.createArrayNode(); // size = 0
        AnyOfValidator validator = createValidator(emptyArray);

        // No subschemas should have been added
        assertTrue("Validator should contain zero subschemas", validator.schemas.isEmpty());

        // Discriminator context must be null when the config flag is false
        assertNull("Discriminator context should be null when disabled", validator.discriminatorContext);
    }

    /**
     * 2. Constructor – discriminator flag enabled creates a non‑null context.
     */
    @Test
    public void testConstructorWithDiscriminatorEnabled() {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        assertNotNull("Discriminator context must be created when feature is enabled",
                validator.discriminatorContext);
    }

    /**
     * 3. validate() – type‑mismatch should produce a validation message and skip the schema.
     */
    @Test
    public void testValidate_TypeMismatchAddsError() {
        // schema node with a single subschema
        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        // Mock the child schema to claim it has a type validator
        when(childSchema.hasTypeValidator()).thenReturn(true);
        when(childSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false); // mismatch

        // The node we validate – an integer node (type does NOT match STRING)
        JsonNode node = mapper.readTree("123");

        // State starts with matchedNode = false
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // Because of the type mismatch an error message should be present
        assertFalse("Validation should return at least one error due to type mismatch", result.isEmpty());
        // The error type should be ANY_OF (coming from BaseJsonValidator.buildValidationMessage)
        ValidationMessage msg = result.iterator().next();
        assertEquals("Validator type should be ANY_OF",
                ValidatorTypeCode.ANY_OF.getValue(), msg.getType());
    }

    /**
     * 4. validate() – a subschema that validates without errors should result in an empty set.
     */
    @Test
    public void testValidate_SubSchemaValidReturnsEmptySet() {
        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        // Subschema does NOT have a type validator
        when(childSchema.hasTypeValidator()).thenReturn(false);
        // Validation succeeds (empty error set)
        when(childSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode node = mapper.readTree("\"a string\"");

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true); // simulate successful match after validation

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // No errors expected
        assertTrue("Result should be empty when a subschema validates successfully", result.isEmpty());
        // Ensure the validator state was marked as matched
        verify(validatorState).setMatchedNode(true);
    }

    /**
     * 5. validate() – when at least one subschema is valid but other subschemas report
     *    “required” errors only, those required errors are filtered out.
     */
    @Test
    public void testValidate_FiltersRequiredErrorsWhenOneSubschemaValid() {
        // Two subschemas: first valid, second reports a required‑field error
        ArrayNode arr = mapper.createArrayNode()
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        // First subschema – succeeds
        JsonSchema first = mock(JsonSchema.class);
        when(first.hasTypeValidator()).thenReturn(false);
        when(first.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // Second subschema – fails with a REQUIRED error
        JsonSchema second = mock(JsonSchema.class);
        when(second.hasTypeValidator()).thenReturn(false);
        ValidationMessage requiredMsg = ValidationMessage.of("required", "$.missing", "required");
        when(second.validate(any(), any(), any(), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(requiredMsg)));

        // newSchema must return the two mocks in order
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(first)
                .thenReturn(second);

        // Re‑create validator to pick up the new schema mocks
        validator = new AnyOfValidator(SCHEMA_PATH, arr, mock(JsonSchema.class), validationContext);

        JsonNode node = mapper.readTree("{\"present\":\"value\"}");

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // The required error from the second subschema must be removed because the first subschema succeeded
        assertTrue("All errors should be filtered out when at least one subschema validates", result.isEmpty());
    }

    /**
     * 6. walk() – when shouldValidateSchema = true it must delegate to {@code validate()}.
     */
    @Test
    public void testWalk_DelegatesToValidateWhenShouldValidateTrue() {
        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        AnyOfValidator validator = spy(createValidator(arr));

        JsonNode node = mapper.readTree("{}");
        Set<ValidationMessage> expected = new LinkedHashSet<>(Collections.singleton(
                ValidationMessage.of("anyOf", "$", "anyOf error")));

        // Stub validate() to return a known set
        doReturn(expected).when(validator).validate(executionContext, node, node, "$");

        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", true);

        assertEquals("walk() should forward to validate() when flag is true", expected, result);
        verify(validator, times(1)).validate(executionContext, node, node, "$");
    }

    /**
     * 7. walk() – when shouldValidateSchema = false each subschema's walk() is invoked,
     *    and the returned set is empty.
     */
    @Test
    public void testWalk_InvokesSubSchemaWalkWhenShouldValidateFalse() {
        // Two subschemas
        ArrayNode arr = mapper.createArrayNode()
                .add(mapper.createObjectNode())
                .add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        // Replace the internal list with two mocks that we can verify
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        validator.schemas.clear();
        validator.schemas.add(s1);
        validator.schemas.add(s2);

        JsonNode node = mapper.readTree("{}");

        Set<ValidationMessage> result = validator.walk(executionContext, node, node, "$", false);

        // walk() on each subschema should have been called with shouldValidate = false
        verify(s1).walk(executionContext, node, node, "$", false);
        verify(s2).walk(executionContext, node, node, "$", false);
        assertTrue("walk() should return an empty set when validation is disabled", result.isEmpty());
    }

    /**
     * 8. preloadJsonSchema() – should delegate to BaseJsonValidator.preloadJsonSchemas().
     *    We verify that the internal list of schemas is passed unchanged.
     */
    @Test
    public void testPreloadJsonSchema_DelegatesToBaseMethod() {
        // Subclass AnyOfValidator to expose the protected method call
        class TestableAnyOfValidator extends AnyOfValidator {
            boolean called = false;
            Set<JsonSchema> received = null;

            TestableAnyOfValidator(String schemaPath, JsonNode schemaNode,
                                   JsonSchema parentSchema, ValidationContext ctx) {
                super(schemaPath, schemaNode, parentSchema, ctx);
            }

            @Override
            protected void preloadJsonSchemas(Set<JsonSchema> schemas) {
                called = true;
                received = schemas;
            }
        }

        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        TestableAnyOfValidator validator = new TestableAnyOfValidator(SCHEMA_PATH, arr,
                mock(JsonSchema.class), validationContext);

        // The constructor already added one mock schema (childSchema)
        validator.preloadJsonSchema();

        assertTrue("preloadJsonSchemas should have been invoked", validator.called);
        assertEquals("The set passed to preloadJsonSchemas must contain the internal schemas",
                validator.schemas, validator.received);
    }

    /**
     * 9. validate() – discriminator enabled and a discriminator match is found,
     *    but the selected subschema returns errors. The error set must contain
     *    the original errors plus the discriminator remark.
     */
    @Test
    public void testValidate_DiscriminatorMatchFoundWithErrorsAddsRemark() {
        // Enable discriminator handling
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        // Mock the discriminator context to indicate a match has been found
        ValidationContext.DiscriminatorContext dContext = mock(ValidationContext.DiscriminatorContext.class);
        when(dContext.isDiscriminatorMatchFound()).thenReturn(true);
        validator.discriminatorContext = dContext; // inject the mocked context

        // Subschema validation returns a non‑empty error set
        ValidationMessage subError = ValidationMessage.of("type", "$", "type error");
        when(childSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(new LinkedHashSet<>(Collections.singleton(subError)));

        JsonNode node = mapper.readTree("\"value\"");

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        // Expect the original error plus the discriminator remark
        assertEquals("Two messages expected (original error + discriminator remark)", 2, result.size());
        boolean remarkFound = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("The discriminator remark must be present", remarkFound);
    }

    /**
     * 10. validate() – discriminator enabled, match found, and subschema validates cleanly.
     *     The validator must return an empty set (no errors) and not add the remark.
     */
    @Test
    public void testValidate_DiscriminatorMatchFoundWithoutErrorsReturnsEmpty() {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ArrayNode arr = mapper.createArrayNode().add(mapper.createObjectNode());
        AnyOfValidator validator = createValidator(arr);

        ValidationContext.DiscriminatorContext dContext = mock(ValidationContext.DiscriminatorContext.class);
        when(dContext.isDiscriminatorMatchFound()).thenReturn(true);
        validator.discriminatorContext = dContext;

        // Subschema validates successfully
        when(childSchema.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        JsonNode node = mapper.readTree("\"ok\"");

        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, node, node, "$");

        assertTrue("When discriminator match succeeds without errors, result must be empty", result.isEmpty());
    }
}
```
###Test END##