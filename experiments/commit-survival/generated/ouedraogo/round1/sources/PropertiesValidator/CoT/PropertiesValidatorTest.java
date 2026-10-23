package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PropertiesValidator}.  The tests use Mockito
 * to isolate the validator from the rest of the schema library.
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertiesValidatorTest {

    // -----------------------------------------------------------------
    // Mocks that are shared by many tests
    // -----------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock JsonNode schemaNode;               // the "properties" object supplied to the constructor
    @Mock JsonSchema parentSchema;          // the parent schema (used for required handling)
    @Mock ExecutionContext executionContext;
    @Mock CollectorContext collectorContext;
    @Mock ValidatorState validatorState;
    @Mock Config config;                    // returned by validationContext.getConfig()
    @Mock JsonSchema propertySchema;        // a child schema for a single property
    @Mock JsonSchema requiredValidator;     // mock for the required validator
    @Mock ValidationMessage validationMessage; // a dummy validation message

    private static final String SCHEMA_PATH = "/test";

    @Before
    public void setUp() {
        // -----------------------------------------------------------------
        // Basic wiring that is required for *all* tests
        // -----------------------------------------------------------------
        MockitoAnnotations.initMocks(this);
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getPropertyWalkListeners()).thenReturn(Collections.emptyList());

        // ExecutionContext always returns a CollectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        // CollectorContext returns the validator state under the well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // The set that records evaluated properties – we use a real Set for simplicity
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // Stub the parent schema’s required validator handling
        when(parentSchema.hasRequiredValidator()).thenReturn(false);
    }

    // -----------------------------------------------------------------
    // Helper to build a PropertiesValidator with a given map of property names
    // -----------------------------------------------------------------
    private PropertiesValidator buildValidatorWithProperties(String... propertyNames) {
        // Mock the iterator returned by schemaNode.fieldNames()
        Iterator<String> fieldNamesIter = Arrays.asList(propertyNames).iterator();
        when(schemaNode.fieldNames()).thenReturn(fieldNamesIter);
        // For each property name, schemaNode.get(name) returns a dummy JsonNode
        for (String name : propertyNames) {
            JsonNode propNode = mock(JsonNode.class);
            when(schemaNode.get(name)).thenReturn(propNode);
            // validationContext creates a new JsonSchema for each property
            when(validationContext.newSchema(eq(SCHEMA_PATH + "/" + name), eq(propNode), eq(parentSchema)))
                    .thenReturn(propertySchema);
        }
        return new PropertiesValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
    }

    // -----------------------------------------------------------------
    // 1. Constructor – it must populate the internal map
    // -----------------------------------------------------------------
    @Test
    public void constructor_populatesSchemasMap() {
        PropertiesValidator validator = buildValidatorWithProperties("p1", "p2", "p3");

        Map<String, JsonSchema> map = validator.getSchemas();
        assertEquals("Three entries expected", 3, map.size());
        assertTrue(map.containsKey("p1"));
        assertTrue(map.containsKey("p2"));
        assertTrue(map.containsKey("p3"));
        // All entries must point to the same mocked propertySchema (the test set‑up returns the same mock)
        for (JsonSchema js : map.values()) {
            assertSame(propertySchema, js);
        }
    }

    // -----------------------------------------------------------------
    // 2. validate – property present → delegate to child schema
    // -----------------------------------------------------------------
    @Test
    public void validate_propertyPresent_delegatesToChildSchema() {
        PropertiesValidator validator = buildValidatorWithProperties("alpha");

        // Build a node that *has* the property "alpha"
        JsonNode instanceNode = mock(JsonNode.class);
        JsonNode alphaNode = mock(JsonNode.class);
        when(instanceNode.get("alpha")).thenReturn(alphaNode);

        // Child schema returns a single validation message
        Set<ValidationMessage> childMessages = Collections.singleton(validationMessage);
        when(propertySchema.validate(executionContext, alphaNode, mock(JsonNode.class), "/alpha"))
                .thenReturn(childMessages);

        // Simple state – not a complex validator, walk disabled
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode,
                mock(JsonNode.class), "/");

        // The result must contain the child message
        assertEquals(1, result.size());
        assertTrue(result.contains(validationMessage));

        // The evaluated‑properties set must contain the full JSON‑Pointer
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        assertTrue(evaluated.contains("/alpha"));
    }

    // -----------------------------------------------------------------
    // 3a. validate – missing property, required → required validator messages added
    // -----------------------------------------------------------------
    @Test
    public void validate_missingPropertyWhenRequired_addsRequiredErrors() {
        // Build validator with a single property "must"
        PropertiesValidator validator = buildValidatorWithProperties("must");

        // The instance node does *not* contain the property
        JsonNode instanceNode = mock(JsonNode.class);
        when(instanceNode.get("must")).thenReturn(null);

        // Parent schema reports that it has a required validator
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        // Required validator returns a single error
        Set<ValidationMessage> requiredErrors = Collections.singleton(validationMessage);
        when(requiredValidator.validate(executionContext, instanceNode, mock(JsonNode.class), "/"))
                .thenReturn(requiredErrors);

        // Simple (non‑complex) state
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode,
                mock(JsonNode.class), "/");

        assertEquals(1, result.size());
        assertTrue(result.contains(validationMessage));
    }

    // -----------------------------------------------------------------
    // 3b. validate – missing property, not required → no errors
    // -----------------------------------------------------------------
    @Test
    public void validate_missingPropertyWhenOptional_returnsEmptySet() {
        PropertiesValidator validator = buildValidatorWithProperties("opt");

        JsonNode instanceNode = mock(JsonNode.class);
        when(instanceNode.get("opt")).thenReturn(null);

        // parentSchema.hasRequiredValidator() already returns false (set in @Before)

        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode,
                mock(JsonNode.class), "/");

        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------
    // 4. validate – complex‑validator handling (state flag manipulation)
    // -----------------------------------------------------------------
    @Test
    public void validate_complexValidator_stateManipulation() {
        PropertiesValidator validator = buildValidatorWithProperties("cplx");

        JsonNode instanceNode = mock(JsonNode.class);
        JsonNode cplxNode = mock(JsonNode.class);
        when(instanceNode.get("cplx")).thenReturn(cplxNode);

        // Child schema returns no messages
        when(propertySchema.validate(executionContext, cplxNode, mock(JsonNode.class), "/cplx"))
                .thenReturn(Collections.emptySet());

        // Simulate a *complex* validator (e.g. oneOf)
        when(validatorState.isComplexValidator()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        // Run validation
        validator.validate(executionContext, instanceNode, mock(JsonNode.class), "/");

        // Expected sequence:
        // 1) original complex flag saved (true)
        // 2) because it is complex, setMatchedNode(true) is called before child validation
        // 3) after child validation the flag is reset to original value (true)
        // 4) because it is still complex, setMatchedNode(true) is called again
        InOrder inOrder = inOrder(validatorState);
        // first call – before delegating to child
        inOrder.verify(validatorState).setMatchedNode(true);
        // flag cleared for child validation
        inOrder.verify(validatorState).setComplexValidator(false);
        // after child validation – flag restored
        inOrder.verify(validatorState).setComplexValidator(true);
        // final matched‑node flag
        inOrder.verify(validatorState, times(2)).setMatchedNode(true);
    }

    // -----------------------------------------------------------------
    // 5. walk – default values are applied when the strategy says so
    // -----------------------------------------------------------------
    @Test
    public void walk_appliesDefaultValuesWhenStrategyAllows() throws Exception {
        // Build a validator with a property that has a "default" entry
        PropertiesValidator validator = buildValidatorWithProperties("defProp");

        // Mock the default node inside the child schema
        JsonNode defaultNode = JsonNodeFactory.instance.textNode("default-value");
        JsonNode propertySchemaNode = mock(JsonNode.class);
        when(propertySchema.getSchemaNode()).thenReturn(propertySchemaNode);
        when(propertySchemaNode.get("default")).thenReturn(defaultNode);

        // The property schema is still returned by the ValidationContext (see setUp)
        // – we already have propertySchema mocked, just make sure its getSchemaNode() works

        // Create an empty ObjectNode (no "defProp" field)
        ObjectNode instance = JsonNodeFactory.instance.objectNode();

        // Mock an ApplyDefaultsStrategy that says *yes* to both checks
        ApplyDefaultsStrategy defaultsStrategy = mock(ApplyDefaultsStrategy.class);
        when(defaultsStrategy.shouldApplyPropertyDefaults()).thenReturn(true);
        when(defaultsStrategy.shouldApplyPropertyDefaultsIfNull()).thenReturn(true);

        // Inject the strategy via reflection (field is protected in BaseJsonValidator)
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        f.setAccessible(true);
        f.set(validator, defaultsStrategy);

        // Simple state – validation not required because we only test the default‑application path
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);

        Set<ValidationMessage> result = validator.walk(executionContext, instance,
                mock(JsonNode.class), "/", true);

        // After walk the ObjectNode must contain the default value
        assertTrue(instance.has("defProp"));
        assertEquals("default-value", instance.get("defProp").asText());

        // No validation messages are expected (the child schema was never invoked)
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------
    // 5b. walk – node is null or not an OBJECT → defaults are NOT applied
    // -----------------------------------------------------------------
    @Test
    public void walk_doesNotApplyDefaultsWhenNodeIsNullOrNotObject() throws Exception {
        PropertiesValidator validator = buildValidatorWithProperties("any");

        // Strategy that would apply defaults if it could
        ApplyDefaultsStrategy defaultsStrategy = mock(ApplyDefaultsStrategy.class);
        when(defaultsStrategy.shouldApplyPropertyDefaults()).thenReturn(true);
        when(defaultsStrategy.shouldApplyPropertyDefaultsIfNull()).thenReturn(true);
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        f.setAccessible(true);
        f.set(validator, defaultsStrategy);

        // CASE 1 – node == null
        Set<ValidationMessage> r1 = validator.walk(executionContext, null,
                mock(JsonNode.class), "/", true);
        assertTrue(r1.isEmpty());

        // CASE 2 – node is an ARRAY (not OBJECT)
        JsonNode arrayNode = JsonNodeFactory.instance.arrayNode();
        Set<ValidationMessage> r2 = validator.walk(executionContext, arrayNode,
                mock(JsonNode.class), "/", true);
        assertTrue(r2.isEmpty());
    }

    // -----------------------------------------------------------------
    // 6. walk – shouldValidateSchema == false → walkSchema is called for each entry
    // -----------------------------------------------------------------
    @Test
    public void walk_whenValidateFlagFalse_delegatesToWalkSchema() {
        PropertiesValidator validator = buildValidatorWithProperties("pA", "pB");

        // Spy on the validator to capture the protected walkSchema call
        PropertiesValidator spyValidator = Mockito.spy(validator);

        JsonNode instanceNode = mock(JsonNode.class);
        // make sure node.get(any) returns null (missing properties) – we are not interested in child validation
        when(instanceNode.get(anyString())).thenReturn(null);

        // Run with shouldValidateSchema == false
        Set<ValidationMessage> result = spyValidator.walk(executionContext, instanceNode,
                mock(JsonNode.class), "/", false);

        // walkSchema should have been called twice (once per property)
        verify(spyValidator, times(2)).walkSchema(eq(executionContext), anyMapEntry(),
                eq(instanceNode), any(JsonNode.class), eq("/"), eq(false),
                anySet(), any(WalkListenerRunner.class));
        // No validation messages expected because the child schemas were never invoked
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------
    // 7. getSchemas – simple getter test
    // -----------------------------------------------------------------
    @Test
    public void getSchemas_returnsInternalMap() {
        PropertiesValidator validator = buildValidatorWithProperties("x", "y");
        Map<String, JsonSchema> map = validator.getSchemas();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
    }

    // -----------------------------------------------------------------
    // 8. preloadJsonSchema – ensure delegation to superclass method
    // -----------------------------------------------------------------
    @Test
    public void preloadJsonSchema_delegatesToBaseClass() throws Exception {
        // Create a subclass that exposes the protected method for verification
        class TestableValidator extends PropertiesValidator {
            TestableValidator(String schemaPath, JsonNode schemaNode, JsonSchema parent,
                              ValidationContext ctx) {
                super(schemaPath, schemaNode, parent, ctx);
            }

            @Override
            protected void preloadJsonSchemas(Collection<JsonSchema> schemas) {
                // we just capture the argument
                captured = schemas;
            }

            Collection<JsonSchema> captured;
        }

        TestableValidator validator = new TestableValidator(SCHEMA_PATH, schemaNode,
                parentSchema, validationContext);
        // Populate the map with a single entry (the setUp already ensures newSchema returns propertySchema)
        when(schemaNode.fieldNames()).thenReturn(Collections.singleton("z").iterator());
        when(schemaNode.get("z")).thenReturn(mock(JsonNode.class));
        when(validationContext.newSchema(eq(SCHEMA_PATH + "/z"), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(propertySchema);

        // invoke the method under test
        validator.preloadJsonSchema();

        // The captured collection must be the same as validator.getSchemas().values()
        assertNotNull(validator.captured);
        assertEquals(validator.getSchemas().values().size(), validator.captured.size());
        assertTrue(validator.captured.containsAll(validator.getSchemas().values()));
    }
}
