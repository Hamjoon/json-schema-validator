package com.networknt.schema;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link PropertiesValidator}.  All public methods are exercised,
 * and the internal branches of {@code validate()} and {@code walk()} are hit
 * using carefully crafted Mockito stubs.
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertiesValidatorTest {

    /** The validator under test – created for each test method. */
    private PropertiesValidator validator;

    /** Mocked collaborators. */
    @Mock private ValidationContext validationContext;
    @Mock private JsonSchema parentSchema;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private ValidatorState validatorState;
    @Mock private WalkListenerRunner walkListenerRunner;               // used only for “walk” tests
    @Mock private Config config;                                         // part of ValidationContext
    @Mock private ApplyDefaultsStrategy applyDefaultsStrategy;          // part of BaseJsonValidator

    /** Helper objects used by several tests. */
    private ObjectNode schemaNode;       // the schema that defines the “properties”
    private ObjectNode instanceNode;     // the JSON document that is validated

    /** Constant used for the “at” path argument. */
    private static final String AT_ROOT = "$";

    @Before
    public void setUp() throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Build a minimal schemaNode that contains two properties:
        //    - “name” (no default)
        //    - “age”  (has a default value of 30)
        // -----------------------------------------------------------------
        schemaNode = JsonNodeFactory.instance.objectNode();

        ObjectNode nameProp = JsonNodeFactory.instance.objectNode();
        nameProp.put("type", "string");
        schemaNode.set("name", nameProp);

        ObjectNode ageProp = JsonNodeFactory.instance.objectNode();
        ageProp.put("type", "integer");
        ageProp.put("default", 30);
        schemaNode.set("age", ageProp);

        // -----------------------------------------------------------------
        // 2️⃣  Mock the ValidationContext – it must be able to create a
        //     JsonSchema for each property.  For simplicity we return a
        //     generic mock that we can later program with the desired
        //     behaviour.
        // -----------------------------------------------------------------
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenAnswer(invocation -> mock(JsonSchema.class));

        // -----------------------------------------------------------------
        // 3️⃣  Mock the configuration hierarchy that is required for the
        //     “apply defaults” logic.
        // -----------------------------------------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        when(config.getApplyDefaultsStrategy()).thenReturn(applyDefaultsStrategy);
        when(applyDefaultsStrategy.shouldApplyPropertyDefaults()).thenReturn(true);
        when(applyDefaultsStrategy.shouldApplyPropertyDefaultsIfNull()).thenReturn(true);

        // -----------------------------------------------------------------
        // 4️⃣  Mock the CollectorContext – it stores evaluated properties and
        //     the validator state.
        // -----------------------------------------------------------------
        Set<String> evaluatedProperties = new LinkedHashSet<>();
        when(collectorContext.getEvaluatedProperties()).thenReturn(evaluatedProperties);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.isComplexValidator()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // -----------------------------------------------------------------
        // 5️⃣  Create the validator instance that will be used by the tests.
        // -----------------------------------------------------------------
        validator = new PropertiesValidator("testSchema", schemaNode, parentSchema, validationContext);

        // -----------------------------------------------------------------
        // 6️⃣  Prepare an empty instance node – individual tests will mutate it.
        // -----------------------------------------------------------------
        instanceNode = JsonNodeFactory.instance.objectNode();
    }

    // -----------------------------------------------------------------
    // 1️⃣  Constructor – does it correctly populate the internal “schemas”
    //     map based on the supplied schemaNode?
    // -----------------------------------------------------------------
    @Test
    public void constructor_populatesSchemasMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        assertEquals("Two properties expected", 2, map.size());
        assertTrue("Property \"name\" missing", map.containsKey("name"));
        assertTrue("Property \"age\" missing", map.containsKey("age"));

        // verify that ValidationContext#newSchema was called with the proper paths
        verify(validationContext).newSchema(eq("testSchema/name"), eq(schemaNode.get("name")), eq(parentSchema));
        verify(validationContext).newSchema(eq("testSchema/age"), eq(schemaNode.get("age")), eq(parentSchema));
    }

    // -----------------------------------------------------------------
    // 2️⃣  getDefaultNode – static helper that extracts the “default”
    //     keyword from a property schema.
    // -----------------------------------------------------------------
    @Test
    public void getDefaultNode_returnsDefaultNodeIfPresent() {
        // Grab the internal entry for the “age” property
        Map.Entry<String, JsonSchema> ageEntry = validator.getSchemas().entrySet()
                .stream()
                .filter(e -> e.getKey().equals("age"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("age entry not found"));

        JsonNode defaultNode = invokeGetDefaultNode(ageEntry);
        assertNotNull("Default node should be present for \"age\"", defaultNode);
        assertTrue("Default node must be numeric", defaultNode.isNumber());
        assertEquals(30, defaultNode.intValue());
    }

    @Test
    public void getDefaultNode_returnsNullWhenNoDefault() {
        Map.Entry<String, JsonSchema> nameEntry = validator.getSchemas().entrySet()
                .stream()
                .filter(e -> e.getKey().equals("name"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("name entry not found"));

        JsonNode defaultNode = invokeGetDefaultNode(nameEntry);
        assertNull("No default defined for \"name\"", defaultNode);
    }

    /** Helper that uses reflection to call the private static method. */
    private static JsonNode invokeGetDefaultNode(Map.Entry<String, JsonSchema> entry) {
        try {
            java.lang.reflect.Method m = PropertiesValidator.class
                    .getDeclaredMethod("getDefaultNode", Map.Entry.class);
            m.setAccessible(true);
            return (JsonNode) m.invoke(null, entry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------
    // 3️⃣  validate() – property present → delegate to the child schema.
    // -----------------------------------------------------------------
    @Test
    public void validate_propertyPresent_delegatesToChildSchema() {
        // Arrange – put a value for “name” into the instance document
        instanceNode.put("name", "John Doe");

        // Mock the child schema for “name” to return a single validation message
        JsonSchema nameSchema = validator.getSchemas().get("name");
        ValidationMessage msg = mock(ValidationMessage.class);
        when(nameSchema.validate(any(), eq(instanceNode.get("name")), any(), anyString()))
                .thenReturn(Collections.singleton(msg));

        // Act
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, instanceNode, AT_ROOT);

        // Assert – the message from the child schema must be present
        assertEquals(1, result.size());
        assertTrue(result.contains(msg));

        // The evaluated‑properties set must contain the fully qualified path
        Set<String> evaluated = collectorContext.getEvaluatedProperties();
        assertTrue(evaluated.contains("/name"));
    }

    // -----------------------------------------------------------------
    // 4️⃣  validate() – missing required property → error from RequiredValidator.
    // -----------------------------------------------------------------
    @Test
    public void validate_missingRequiredProperty_returnsRequiredError() {
        // -------------------------------------------------------------
        // Parent schema must report that it *has* a required validator.
        // -------------------------------------------------------------
        RequiredValidator requiredValidator = mock(RequiredValidator.class);
        ValidationMessage requiredMsg = mock(ValidationMessage.class);
        when(requiredValidator.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(requiredMsg));
        when(parentSchema.hasRequiredValidator()).thenReturn(true);
        when(parentSchema.getRequiredValidator()).thenReturn(requiredValidator);
        // The validator under test holds a reference to its parent schema via
        // BaseJsonValidator#getParentSchema().  The field is private, so we set it via reflection.
        setParentSchemaViaReflection(validator, parentSchema);

        // Act – the instance does NOT contain the required property “name”.
        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, instanceNode, AT_ROOT);

        // Assert – the required‑error must be returned.
        assertEquals(1, result.size());
        assertTrue(result.contains(requiredMsg));
    }

    /** Helper to inject a parent schema into the BaseJsonValidator superclass. */
    private static void setParentSchemaViaReflection(PropertiesValidator validator, JsonSchema parent) {
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("parentSchema");
            f.setAccessible(true);
            f.set(validator, parent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------------------------------------------
    // 5️⃣  walk() – defaults are applied when the strategy says so.
    // -----------------------------------------------------------------
    @Test
    public void walk_appliesPropertyDefaults_whenStrategyEnabled() {
        // The instance currently lacks the “age” property.
        assertNull("Pre‑condition – age must be absent", instanceNode.get("age"));

        // Walk with validation disabled – we only care about defaults.
        Set<ValidationMessage> messages = validator.walk(executionContext, instanceNode, instanceNode, AT_ROOT, false);

        // No validation messages should be produced.
        assertTrue(messages.isEmpty());

        // After walking, the default value (30) must have been inserted.
        JsonNode ageNode = instanceNode.get("age");
        assertNotNull("Default for \"age\" should have been added", ageNode);
        assertTrue(ageNode.isInt());
        assertEquals(30, ageNode.intValue());
    }

    // -----------------------------------------------------------------
    // 6️⃣  walk() – when validation is requested, the validator delegates to
    //     its children (the child schemas are mocked to return a message).
    // -----------------------------------------------------------------
    @Test
    public void walk_withValidationEnabled_delegatesToChildValidate() {
        // Populate the instance with both properties.
        instanceNode.put("name", "Alice");
        instanceNode.put("age", 25);

        // Mock child schemas to return distinct messages.
        JsonSchema nameSchema = validator.getSchemas().get("name");
        JsonSchema ageSchema = validator.getSchemas().get("age");

        ValidationMessage nameMsg = mock(ValidationMessage.class);
        ValidationMessage ageMsg  = mock(ValidationMessage.class);

        when(nameSchema.validate(any(), eq(instanceNode.get("name")), any(), anyString()))
                .thenReturn(Collections.singleton(nameMsg));
        when(ageSchema.validate(any(), eq(instanceNode.get("age")), any(), anyString()))
                .thenReturn(Collections.singleton(ageMsg));

        // Walk with validation turned on.
        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, instanceNode, AT_ROOT, true);

        // Both messages must be present.
        assertEquals(2, result.size());
        assertTrue(result.contains(nameMsg));
        assertTrue(result.contains(ageMsg));
    }

    // -----------------------------------------------------------------
    // 7️⃣  preloadJsonSchema() – ensure it forwards the call to the protected
    //     helper in BaseJsonValidator.
    // -----------------------------------------------------------------
    @Test
    public void preloadJsonSchema_forwardsToBaseClass() throws Exception {
        // Spy on the validator to intercept the protected call.
        PropertiesValidator spyValidator = Mockito.spy(validator);

        // The BaseJsonValidator method is protected; we invoke the public wrapper.
        spyValidator.preloadJsonSchema();

        // Verify that the protected helper was called with the collection of schemas.
        verify(spyValidator, times(1)).preloadJsonSchemas(eq(spyValidator.getSchemas().values()));
    }
}
