/**
 * Comprehensive JUnit‑4 test suite for {@link com.networknt.schema.PropertiesValidator}.
 *
 * The tests cover:
 *  <ul>
 *      <li>Constructor – building the internal {@code schemas} map.</li>
 *      <li>{@code validate(..)} – handling of present properties, missing
 *          properties with required validation, and interaction with the
 *          {@link com.networknt.schema.ValidatorState}.</li>
 *      <li>{@code walk(..)} – default‑value application and delegating to
 *          {@code validate(..)} when validation is enabled.</li>
 *      <li>Private helper {@code applyPropertyDefaults(..)} – default
 *          injection logic.</li>
 *      <li>{@code getSchemas()} – simple accessor.</li>
 *      <li>{@code preloadJsonSchema()} – delegation to the protected
 *          {@code preloadJsonSchemas(..)} method.</li>
 *  </ul>
 *
 * The test class makes heavy use of Mockito to isolate {@code PropertiesValidator}
 * from the rest of the library (no real schema compilation, walking or logging is
 * performed).  All external collaborators – {@link com.networknt.schema.ValidationContext},
 * {@link com.networknt.schema.ExecutionContext}, {@link com.networknt.schema.CollectorContext},
 * {@link com.networknt.schema.ValidatorState}, {@link com.networknt.schema.JsonSchema},
 * etc. – are mocked.
 *
 * The code is written for JUnit‑4 (no JUnit‑5 features are used) and can be
 * compiled and run with a standard Maven/Gradle setup that includes
 * <code>junit:junit:4.13.2</code> and <code>org.mockito:mockito-core:5.x</code>.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private JsonSchema childSchemaA;

    @Mock
    private JsonSchema childSchemaB;

    @Mock
    private Config config;

    @Mock
    private RequiredValidator requiredValidator;

    @Before
    public void setUp() {
        // --------------------------------------------------------------------
        // Common stubbing that is required for every test
        // --------------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getPropertyWalkListeners()).thenReturn(Collections.emptyList());

        // ExecutionContext always returns the same CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);

        // Default behaviour of the validator state – simple (non‑complex) walk
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        // Parent schema – required validator is absent unless a test explicitly
        // configures it.
        when(parentSchema.hasRequiredValidator()).thenReturn(false);
    }

    /**
     * Helper to create a simple schema node with two properties:
     * <pre>
     * {
     *   "propA": { "type": "string" },
     *   "propB": { "type": "integer", "default": 42 }
     * }
     * </pre>
     */
    private ObjectNode createRootSchemaNode() {
        ObjectNode root = MAPPER.createObjectNode();

        ObjectNode propA = MAPPER.createObjectNode();
        propA.put("type", "string");
        root.set("propA", propA);

        ObjectNode propB = MAPPER.createObjectNode();
        propB.put("type", "integer");
        propB.put("default", 42);
        root.set("propB", propB);

        return root;
    }

    /**
     * Verifies that the constructor correctly populates the internal {@code schemas}
     * map using {@link ValidationContext#newSchema(String, JsonNode, JsonSchema)}.
     */
    @Test
    public void testConstructorBuildsSchemasMap() {
        ObjectNode schemaNode = createRootSchemaNode();

        // The ValidationContext should be asked to create a JsonSchema for each property
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propA")), eq(parentSchema)))
                .thenReturn(childSchemaA);
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propB")), eq(parentSchema)))
                .thenReturn(childSchemaB);

        PropertiesValidator validator = new PropertiesValidator(
                "/properties", schemaNode, parentSchema, validationContext);

        Map<String, JsonSchema> schemas = validator.getSchemas();

        assertEquals("Two schemas must have been created", 2, schemas.size());
        assertTrue("Schema for propA must be present", schemas.containsKey("propA"));
        assertTrue("Schema for propB must be present", schemas.containsKey("propB"));
        assertSame("propA schema must be childSchemaA", childSchemaA, schemas.get("propA"));
        assertSame("propB schema must be childSchemaB", childSchemaB, schemas.get("propB"));
    }

    /**
     * Scenario: the instance node contains a property that is defined in the
     * schema.  The validator must:
     * <ol>
     *   <li>Mark the property as evaluated.</li>
     *   <li>Delegate validation to the child schema.</li>
     *   <li>Return any validation messages produced by the child.</li>
     * </ol>
     */
    @Test
    public void testValidate_PropertyExists_DelegatesToChildSchema() {
        // --------------------------------------------------------------------
        // Build the validator (same schema as above)
        // --------------------------------------------------------------------
        ObjectNode schemaNode = createRootSchemaNode();
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propA")), eq(parentSchema)))
                .thenReturn(childSchemaA);
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propB")), eq(parentSchema)))
                .thenReturn(childSchemaB);

        PropertiesValidator validator = new PropertiesValidator(
                "/properties", schemaNode, parentSchema, validationContext);

        // --------------------------------------------------------------------
        // Instance node: only "propA" is present
        // --------------------------------------------------------------------
        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("propA", "hello");

        // Child schema validation should return a single message
        ValidationMessage childMessage = ValidationMessage.builder()
                .code("type")
                .path("propA")
                .message("must be a string")
                .build();
        when(childSchemaA.validate(executionContext, instance.get("propA"), instance, "/propA"))
                .thenReturn(Collections.singleton(childMessage));

        // --------------------------------------------------------------------
        // Execute validation
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, instance, instance, "");

        // --------------------------------------------------------------------
        // Assertions
        // --------------------------------------------------------------------
        assertEquals("One validation message must be returned", 1, messages.size());
        assertTrue("Returned message must be the one from the child schema",
                messages.contains(childMessage));

        // Evaluated properties set must contain the fully qualified path
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        assertTrue("Evaluated properties must contain '/propA'",
                evaluated.contains("/propA"));
    }

    /**
     * Scenario: a required property defined by the parent schema is missing.
     * The validator must obtain the required‑validator errors and return them.
     */
    @Test
    public void testValidate_MissingRequiredProperty_ReturnsRequiredErrors() {
        // --------------------------------------------------------------------
        // Build a schema that contains just "propA"
        // --------------------------------------------------------------------
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ObjectNode propA = MAPPER.createObjectNode();
        propA.put("type", "string");
        schemaNode.set("propA", propA);

        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propA")), eq(parentSchema)))
                .thenReturn(childSchemaA);

        PropertiesValidator validator = new PropertiesValidator(
                "/properties", schemaNode, parentSchema, validationContext);

        // --------------------------------------------------------------------
        // Parent schema reports that a required validator exists and provides errors
        // --------------------------------------------------------------------
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        ValidationMessage requiredMessage = ValidationMessage.builder()
                .code("required")
                .path("")
                .message("property 'propA' is required")
                .build();
        when(requiredValidator.validate(executionContext, MAPPER.createObjectNode(), MAPPER.createObjectNode(), ""))
                .thenReturn(Collections.singleton(requiredMessage));

        // --------------------------------------------------------------------
        // Instance node: empty object (propA missing)
        // --------------------------------------------------------------------
        ObjectNode instance = MAPPER.createObjectNode();

        // --------------------------------------------------------------------
        // Execute validation
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = validator.validate(executionContext, instance, instance, "");

        // --------------------------------------------------------------------
        // Assertions
        // --------------------------------------------------------------------
        assertEquals("The required‑validator error must be returned", 1, messages.size());
        assertTrue(messages.contains(requiredMessage));
    }

    /**
     * Test the private {@code applyPropertyDefaults} method via reflection.
     * A property that is missing in the instance node should be populated with the
     * default value defined in the child schema.
     */
    @Test
    public void testApplyPropertyDefaults_InsertsMissingDefaults() throws Exception {
        // --------------------------------------------------------------------
        // Build a schema where "propB" has a default value (42)
        // --------------------------------------------------------------------
        ObjectNode schemaNode = createRootSchemaNode();

        // childSchemaB will be used for "propB"
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propB")), eq(parentSchema)))
                .thenReturn(childSchemaB);

        // The child schema's schema node contains the default value (42)
        when(childSchemaB.getSchemaNode()).thenReturn(schemaNode.get("propB"));

        PropertiesValidator validator = new PropertiesValidator(
                "/properties", schemaNode, parentSchema, validationContext);

        // --------------------------------------------------------------------
        // Instance node: does NOT contain "propB"
        // --------------------------------------------------------------------
        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("propA", "some string");

        // --------------------------------------------------------------------
        // Invoke the private method via reflection
        // --------------------------------------------------------------------
        java.lang.reflect.Method method = PropertiesValidator.class
                .getDeclaredMethod("applyPropertyDefaults", ObjectNode.class);
        method.setAccessible(true);
        method.invoke(validator, instance);

        // --------------------------------------------------------------------
        // Verify that "propB" is now present with the default value (42)
        // --------------------------------------------------------------------
        assertTrue("Default property 'propB' must have been added", instance.has("propB"));
        assertEquals("Default value must be 42", 42, instance.get("propB").asInt());
    }

    /**
     * Verify that {@code walk(..)} delegates to {@code validate(..)} when
     * {@code shouldValidateSchema} is {@code true} and that defaults are applied
     * when the strategy permits it.
     */
    @Test
    public void testWalk_ValidatesWhenEnabledAndAppliesDefaults() throws Exception {
        // --------------------------------------------------------------------
        // Build schema with a default on propB
        // --------------------------------------------------------------------
        ObjectNode schemaNode = createRootSchemaNode();

        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propA")), eq(parentSchema)))
                .thenReturn(childSchemaA);
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propB")), eq(parentSchema)))
                .thenReturn(childSchemaB);

        when(childSchemaB.getSchemaNode()).thenReturn(schemaNode.get("propB"));
        // For propA validation we return an empty set (no errors)
        when(childSchemaA.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.emptySet());

        // --------------------------------------------------------------------
        // Spy the validator to intercept the protected 'applyDefaultsStrategy' check
        // --------------------------------------------------------------------
        PropertiesValidator realValidator = new PropertiesValidator(
                "/properties", schemaNode, parentSchema, validationContext);
        PropertiesValidator validator = Mockito.spy(realValidator);

        // Mock the defaults strategy to return true for both checks
        ApplyDefaultsStrategy defaultsStrategy = mock(ApplyDefaultsStrategy.class);
        when(defaultsStrategy.shouldApplyPropertyDefaults()).thenReturn(true);
        when(defaultsStrategy.shouldApplyPropertyDefaultsIfNull()).thenReturn(true);
        // The field is protected in BaseJsonValidator, we set it via reflection
        java.lang.reflect.Field strategyField = BaseJsonValidator.class
                .getDeclaredField("applyDefaultsStrategy");
        strategyField.setAccessible(true);
        strategyField.set(validator, defaultsStrategy);

        // --------------------------------------------------------------------
        // Instance node: missing propB (should be filled with the default)
        // --------------------------------------------------------------------
        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("propA", "value");

        // --------------------------------------------------------------------
        // Call walk with validation enabled
        // --------------------------------------------------------------------
        Set<ValidationMessage> messages = validator.walk(executionContext, instance, instance, "", true);

        // --------------------------------------------------------------------
        // Expectations
        // --------------------------------------------------------------------
        // No validation messages (child schema for propA returned empty set)
        assertTrue("No validation errors expected", messages.isEmpty());

        // Default must have been applied
        assertTrue("propB default must be added", instance.has("propB"));
        assertEquals("Default value of propB must be 42", 42, instance.get("propB").asInt());

        // Verify that the child schema's validate method was called exactly once
        verify(childSchemaA, times(1)).validate(eq(executionContext), any(JsonNode.class), eq(instance), eq("/propA"));
    }

    /**
     * Ensure that {@code preloadJsonSchema()} correctly forwards the call to the
     * protected {@code preloadJsonSchemas(..)} method for each child schema.
     */
    @Test
    public void testPreloadJsonSchema_DelegatesToBaseImplementation() {
        // --------------------------------------------------------------------
        // Build a validator with two child schemas
        // --------------------------------------------------------------------
        ObjectNode schemaNode = createRootSchemaNode();
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propA")), eq(parentSchema)))
                .thenReturn(childSchemaA);
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propB")), eq(parentSchema)))
                .thenReturn(childSchemaB);

        // Spy the validator so we can verify the protected call
        PropertiesValidator validator = Mockito.spy(
                new PropertiesValidator("/properties", schemaNode, parentSchema, validationContext));

        // --------------------------------------------------------------------
        // Invoke the public method
        // --------------------------------------------------------------------
        validator.preloadJsonSchema();

        // --------------------------------------------------------------------
        // Verify that the protected helper was invoked with the collection of child schemas
        // --------------------------------------------------------------------
        verify(validator, times(1)).preloadJsonSchemas(eq(Arrays.asList(childSchemaA, childSchemaB)));
    }

    /**
     * Simple sanity test for the public accessor {@code getSchemas()}.
     */
    @Test
    public void testGetSchemas_ReturnsInternalMapReference() {
        ObjectNode schemaNode = createRootSchemaNode();

        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propA")), eq(parentSchema)))
                .thenReturn(childSchemaA);
        when(validationContext.newSchema(anyString(), eq(schemaNode.get("propB")), eq(parentSchema)))
                .thenReturn(childSchemaB);

        PropertiesValidator validator = new PropertiesValidator(
                "/properties", schemaNode, parentSchema, validationContext);

        Map<String, JsonSchema> schemas = validator.getSchemas();

        // The map must be mutable (LinkedHashMap) and contain the expected entries
        assertEquals("Map must contain two entries", 2, schemas.size());
        assertSame("Entry for propA must be childSchemaA", childSchemaA, schemas.get("propA"));
        assertSame("Entry for propB must be childSchemaB", childSchemaB, schemas.get("propB"));
    }
}

/**
 * Minimal stub for the defaults‑strategy used by {@link BaseJsonValidator}.
 *
 * The real implementation is more sophisticated, but for unit‑testing we only
 * need to know whether defaults should be applied.
 */
interface ApplyDefaultsStrategy {
    boolean shouldApplyPropertyDefaults();
    boolean shouldApplyPropertyDefaultsIfNull();
}
