package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link PropertiesValidator}.
 *
 * <p>
 * The tests use Mockito to mock the heavy‑weight collaborators
 * ( {@link ValidationContext}, {@link ExecutionContext},
 * {@link CollectorContext}, {@link ValidatorState},
 * {@link JsonSchema}, {@link ApplyDefaultsStrategy} etc.).
 * Only the logic inside {@code PropertiesValidator} is exercised.
 * </p>
 */
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
    private ApplyDefaultsStrategy applyDefaultsStrategy;

    @Mock
    private Config config;

    @Mock
    private JsonSchemaFactory jsonSchemaFactory;

    @Captor
    private ArgumentCaptor<Set<ValidationMessage>> messageCaptor;

    private PropertiesValidator validator;
    private JsonNode schemaNode; // the "properties" object supplied to the validator

    @Before
    public void setUp() throws Exception {
        // ---------- build a simple schema node ----------
        // {
        //   "name": { "type": "string", "default": "John" },
        //   "age" : { "type": "integer" }
        // }
        ObjectNode propertiesNode = MAPPER.createObjectNode();
        ObjectNode nameProp = MAPPER.createObjectNode();
        nameProp.put("type", "string");
        nameProp.put("default", "John");
        ObjectNode ageProp = MAPPER.createObjectNode();
        ageProp.put("type", "integer");
        propertiesNode.set("name", nameProp);
        propertiesNode.set("age", ageProp);
        this.schemaNode = propertiesNode;

        // ---------- mock ValidationContext ----------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getPropertyWalkListeners()).thenReturn(Collections.emptyList());
        when(validationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);

        // ---------- mock newSchema() to return a simple JsonSchema mock ----------
        // we need a separate mock per property key so that we can verify interactions.
        JsonSchema nameSchema = mock(JsonSchema.class);
        JsonSchema ageSchema = mock(JsonSchema.class);
        when(nameSchema.getSchemaNode()).thenReturn(nameProp);
        when(ageSchema.getSchemaNode()).thenReturn(ageProp);
        when(nameSchema.getSchemaPath()).thenReturn("#/properties/name");
        when(ageSchema.getSchemaPath()).thenReturn("#/properties/age");
        when(validationContext.newSchema(anyString(), eq(nameProp), eq(parentSchema))).thenReturn(nameSchema);
        when(validationContext.newSchema(anyString(), eq(ageProp), eq(parentSchema))).thenReturn(ageSchema);

        // ---------- mock ExecutionContext ----------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(collectorContext.getEvaluatedProperties()).thenReturn(new LinkedHashSet<>());

        // ---------- mock ValidatorState defaults ----------
        // By default we treat the validator as a simple (non‑complex) validator,
        // walk disabled and validation enabled.
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        // The matchedNode flag is not used in our happy‑path tests.
        when(validatorState.isMatchedNode()).thenReturn(false);

        // ---------- create the validator under test ----------
        validator = new PropertiesValidator("#/properties", schemaNode, parentSchema, validationContext);
        // inject the applyDefaultsStrategy (a protected field in BaseJsonValidator)
        // using reflection as there is no public setter.
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        f.setAccessible(true);
        f.set(validator, applyDefaultsStrategy);
    }

    /** -----------------------------------------------------------------------
     *  Constructor behaviour
     * ----------------------------------------------------------------------- */
    @Test
    public void constructorShouldPopulateSchemasMap() {
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertEquals("Two property schemas must be present", 2, schemas.size());
        assertTrue("Schema for 'name' must exist", schemas.containsKey("name"));
        assertTrue("Schema for 'age' must exist", schemas.containsKey("age"));
        // Verify that ValidationContext.newSchema() has been called with the expected paths.
        verify(validationContext).newSchema(eq("#/properties/name"), eq(schemaNode.get("name")), eq(parentSchema));
        verify(validationContext).newSchema(eq("#/properties/age"), eq(schemaNode.get("age")), eq(parentSchema));
    }

    /** -----------------------------------------------------------------------
     *  validate() – property present, walk disabled, non‑complex
     * ----------------------------------------------------------------------- */
    @Test
    public void validateWhenPropertyExistsShouldValidateChildAndMarkEvaluated() {
        // Build a sample instance: { "name": "Bob", "age": 30 }
        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("name", "Bob");
        instance.put("age", 30);

        // Mock child schema validation – return an empty error set.
        JsonSchema nameSchema = validator.getSchemas().get("name");
        JsonSchema ageSchema = validator.getSchemas().get("age");
        when(nameSchema.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());
        when(ageSchema.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, "#");

        // No errors expected.
        assertTrue(errors.isEmpty());

        // Verify that both child schemas were invoked with the correct at‑paths.
        verify(nameSchema).validate(eq(executionContext), eq(instance.get("name")), eq(instance), eq("#/name"));
        verify(ageSchema).validate(eq(executionContext), eq(instance.get("age")), eq(instance), eq("#/age"));

        // Evaluated properties must contain the full JSON‑Pointer paths.
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        assertTrue(evaluated.contains("#/name"));
        assertTrue(evaluated.contains("#/age"));
    }

    /** -----------------------------------------------------------------------
     *  validate() – missing required property should produce required‑error
     * ----------------------------------------------------------------------- */
    @Test
    public void validateMissingRequiredPropertyShouldAddRequiredError() {
        // Configure the parent schema to claim it has a required validator.
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);

        // The required validator will report a single error for the missing "age".
        ValidationMessage requiredMsg = ValidationMessage.builder()
                .path("#/age")
                .message("required property")
                .type(ValidatorTypeCode.REQUIRED.getValue())
                .build();
        when(requiredValidator.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.singleton(requiredMsg));

        // Instance only contains "name".
        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("name", "Alice");

        // Child "name" schema validation returns empty set.
        JsonSchema nameSchema = validator.getSchemas().get("name");
        when(nameSchema.validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> errors = validator.validate(executionContext, instance, instance, "#");

        // The required error must be present.
        assertEquals(1, errors.size());
        assertTrue(errors.contains(requiredMsg));

        // The present property ("name") should still have been validated.
        verify(nameSchema).validate(eq(executionContext), eq(instance.get("name")), eq(instance), eq("#/name"));
    }

    /** -----------------------------------------------------------------------
     *  walk() – apply property defaults when strategy permits it
     * ----------------------------------------------------------------------- */
    @Test
    public void walkShouldApplyDefaultsWhenStrategySaysSo() {
        // Strategy says we should apply defaults for properties.
        when(applyDefaultsStrategy.shouldApplyPropertyDefaults()).thenReturn(true);
        // For null values the strategy also says to apply defaults.
        when(applyDefaultsStrategy.shouldApplyPropertyDefaultsIfNull()).thenReturn(true);

        // Instance missing the "name" property (which has a default) and having null "age".
        ObjectNode instance = MAPPER.createObjectNode();
        instance.putNull("age"); // age present but null – defaults not defined for age.

        // Walk with validation disabled – we only want the default to be injected.
        Set<ValidationMessage> messages = validator.walk(executionContext, instance, instance, "#", false);

        // No validation messages should be produced (validation disabled).
        assertTrue(messages.isEmpty());

        // The "name" property must have been added with its default value.
        assertTrue(instance.has("name"));
        assertEquals("John", instance.get("name").asText());

        // The "age" property should remain null (no default defined).
        assertTrue(instance.has("age"));
        assertTrue(instance.get("age").isNull());
    }

    /** -----------------------------------------------------------------------
     *  walk() – when shouldValidateSchema == true it delegates to validate()
     * ----------------------------------------------------------------------- */
    @Test
    public void walkWhenShouldValidateShouldDelegateToValidate() {
        // Spy on the validator to capture the call to validate().
        PropertiesValidator spyValidator = Mockito.spy(validator);
        // Stub validate() to return a known set.
        ValidationMessage msg = ValidationMessage.builder()
                .path("#/name")
                .message("dummy")
                .type(ValidatorTypeCode.TYPE.getValue())
                .build();
        Set<ValidationMessage> dummySet = Collections.singleton(msg);
        doReturn(dummySet).when(spyValidator).validate(eq(executionContext), any(JsonNode.class), any(JsonNode.class), anyString());

        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("name", "Bob");

        Set<ValidationMessage> result = spyValidator.walk(executionContext, instance, instance, "#", true);

        // The result must be exactly the dummy set returned by validate().
        assertEquals(dummySet, result);
        verify(spyValidator, times(1)).validate(eq(executionContext), eq(instance), eq(instance), eq("#"));
    }

    /** -----------------------------------------------------------------------
     *  getDefaultNode() – static helper returns the "default" field if present
     * ----------------------------------------------------------------------- */
    @Test
    public void getDefaultNodeShouldReturnDefaultWhenPresent() throws Exception {
        // Use reflection to invoke the private static method.
        java.lang.reflect.Method method = PropertiesValidator.class.getDeclaredMethod("getDefaultNode", Map.Entry.class);
        method.setAccessible(true);

        Map.Entry<String, JsonSchema> entry = validator.getSchemas().entrySet().stream()
                .filter(e -> e.getKey().equals("name"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("name entry not found"));

        JsonNode defaultNode = (JsonNode) method.invoke(null, entry);
        assertNotNull("Default node must be present for 'name'", defaultNode);
        assertEquals("John", defaultNode.asText());
    }

    /** -----------------------------------------------------------------------
     *  preloadJsonSchema() – should delegate to preloadJsonSchemas()
     * ----------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchemaShouldDelegateToBaseImplementation() throws Exception {
        // Create a spy of the validator to verify the protected method call.
        PropertiesValidator spyValidator = Mockito.spy(validator);
        // The protected method preloadJsonSchemas(Collection) is defined in BaseJsonValidator.
        // We verify that it is invoked with the collection of property schemas.
        doNothing().when(spyValidator).preloadJsonSchemas(anyCollection());

        spyValidator.preloadJsonSchema();

        // Capture the argument passed to preloadJsonSchemas.
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());

        Collection<?> passed = captor.getValue();
        assertEquals("All property schemas must be passed", validator.getSchemas().size(), passed.size());
        assertTrue(passed.containsAll(validator.getSchemas().values()));
    }

    /** -----------------------------------------------------------------------
     *  walkSchema() – ensure pre‑walk listener decides execution and post‑walk
     *                  listener receives the accumulated messages.
     * ----------------------------------------------------------------------- */
    @Test
    public void walkSchemaExecutesPreAndPostListenersCorrectly() throws Exception {
        // Create a dummy listener that records calls.
        WalkListener listener = mock(WalkListener.class);
        when(listener.preWalk(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any(ValidationContext.class),
                any(JsonSchemaFactory.class))).thenReturn(true);
        when(listener.postWalk(any(ExecutionContext.class), anyString(), any(JsonNode.class), any(JsonNode.class),
                anyString(), anyString(), any(JsonNode.class), any(JsonSchema.class), any(ValidationContext.class),
                any(JsonSchemaFactory.class), anySet())).thenReturn(null);

        // Replace the config to return our custom listener.
        when(config.getPropertyWalkListeners()).thenReturn(Collections.singletonList(listener));

        // Re‑create validator so that it picks up the new config.
        validator = new PropertiesValidator("#/properties", schemaNode, parentSchema, validationContext);
        // Inject the applyDefaultsStrategy again (required for later tests).
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        f.setAccessible(true);
        f.set(validator, applyDefaultsStrategy);

        // Build a simple instance node.
        ObjectNode instance = MAPPER.createObjectNode();
        instance.put("name", "Bob");

        // Mock the child schema's walk() to return a dummy message.
        JsonSchema nameSchema = validator.getSchemas().get("name");
        ValidationMessage childMsg = ValidationMessage.builder()
                .path("#/name")
                .message("child")
                .type(ValidatorTypeCode.TYPE.getValue())
                .build();
        when(nameSchema.walk(eq(executionContext), eq(instance.get("name")), eq(instance), eq("#/name"), anyBoolean()))
                .thenReturn(Collections.singleton(childMsg));

        // Invoke walk() with validation enabled; this will call walkSchema internally.
        Set<ValidationMessage> result = validator.walk(executionContext, instance, instance, "#", true);

        // Result should contain the child message.
        assertTrue(result.contains(childMsg));

        // Verify that preWalk and postWalk were each called once for the "name" property.
        verify(listener, times(1)).preWalk(eq(executionContext), eq(ValidatorTypeCode.PROPERTIES.getValue()),
                eq(instance.get("name")), eq(instance), eq("#/name"),
                eq(nameSchema.getSchemaPath()), eq(nameSchema.getSchemaNode()),
                eq(nameSchema), eq(validationContext), eq(jsonSchemaFactory));

        verify(listener, times(1)).postWalk(eq(executionContext), eq(ValidatorTypeCode.PROPERTIES.getValue()),
                eq(instance.get("name")), eq(instance), eq("#/name"),
                eq(nameSchema.getSchemaPath()), eq(nameSchema.getSchemaNode()),
                eq(nameSchema), eq(validationContext), eq(jsonSchemaFactory), anySet());
    }
}
