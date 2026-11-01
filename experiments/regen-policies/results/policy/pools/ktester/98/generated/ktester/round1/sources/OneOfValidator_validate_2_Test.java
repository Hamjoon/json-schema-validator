package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import java.util.*;
import com.networknt.schema.utils.SetView;
import org.junit.jupiter.api.*;
import org.mockito.*;

class OneOfValidator_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExecutionContext executionContext;

    private ValidationContext validationContext;

    private Config config;

    private ExecutionConfig executionConfig;

    private DiscriminatorContext discriminatorContext;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh execution context for each test.
        this.executionContext = new ExecutionContext();
    }

    /**
     * Helper to locate the {@code validate} method via reflection.
     */
    private Method getValidateMethod() throws Exception {
        Method m = OneOfValidator.class.getDeclaredMethod("validate", ExecutionContext.class, JsonNode.class, JsonNode.class, JsonNodePath.class, boolean.class);
        m.setAccessible(true);
        return m;
    }

    /**
     * Returns the {@link OneOfValidator} instance from a compiled {@link JsonSchema}.
     */
    private OneOfValidator getOneOfValidator(JsonSchema schema) {
        List<JsonValidator> validators = schema.getValidators();
        List<OneOfValidator> oneOfValidators = validators.stream().filter(v -> v instanceof OneOfValidator).map(v -> (OneOfValidator) v).collect(Collectors.toList());
        assertEquals(1, oneOfValidators.size(), "The test schema should contain exactly one OneOfValidator");
        return oneOfValidators.get(0);
    }

    /**
     * Prepares a mock {@link ExecutionContext} with the supplied flags.
     *
     * @param failFast               value returned by {@code isFailFast()}
     * @param discriminatorEnabled   whether discriminator is enabled in the config
     * @param reportChildErrors      value returned by {@code reportChildErrors(...)}
     * @param canShortCircuit        value returned by {@code canShortCircuit()}
     */
    private ExecutionContext mockExecutionContext(boolean failFast, boolean discriminatorEnabled, boolean reportChildErrors, boolean canShortCircuit) {
        // ----- mock the configuration hierarchy -----
        config = mock(Config.class);
        when(config.isDiscriminatorKeywordEnabled()).thenReturn(discriminatorEnabled);
        validationContext = mock(ValidationContext.class);
        when(validationContext.getConfig()).thenReturn(config);
        // the validator (OneOfValidator) reads this field directly, we will inject it later
        executionConfig = mock(ExecutionConfig.class);
        when(executionConfig.getLocale()).thenReturn(Locale.US);
        // ----- mock discriminator context (used only when discriminator is enabled) -----
        discriminatorContext = mock(DiscriminatorContext.class);
        // default behaviour: no match, not ignored, active when discriminator is enabled
        when(discriminatorContext.isDiscriminatorMatchFound()).thenReturn(false);
        when(discriminatorContext.isDiscriminatorIgnore()).thenReturn(false);
        when(discriminatorContext.isActive()).thenReturn(discriminatorEnabled);
        // ----- mock execution context -----
        executionContext = mock(ExecutionContext.class);
        when(executionContext.isFailFast()).thenReturn(failFast);
        when(executionContext.getExecutionConfig()).thenReturn(executionConfig);
        when(executionContext.getCurrentDiscriminatorContext()).thenReturn(discriminatorContext);
        // the following methods are void – just verify they are called, no behaviour needed
        doNothing().when(executionContext).setFailFast(anyBoolean());
        doNothing().when(executionContext).enterDiscriminatorContext(any(), any());
        doNothing().when(executionContext).leaveDiscriminatorContextImmediately(any());
        // ----- spy the validator later to stub canShortCircuit() / reportChildErrors() -----
        // the stubbing will be performed in each test case.
        return executionContext;
    }

    /**
     * Injects the mocked {@link ValidationContext} into the validator via reflection.
     */
    private void injectValidationContext(OneOfValidator validator) throws Exception {
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(validator, validationContext);
    }

    /**
     * Builds a minimal {@link DiscriminatorValidator} mock used in the
     * discriminator‑related tests.
     */
    private DiscriminatorValidator mockDiscriminatorValidator(String propertyName, Map<String, String> mapping, JsonNode schemaNode) {
        DiscriminatorValidator dv = mock(DiscriminatorValidator.class);
        when(dv.getKeyword()).thenReturn("discriminator");
        when(dv.getPropertyName()).thenReturn(propertyName);
        when(dv.getMapping()).thenReturn(mapping);
        when(dv.getSchemaNode()).thenReturn(schemaNode);
        when(dv.getSchemaLocation()).thenReturn("#/components/schemas/Disc");
        return dv;
    }

    /**
     * Mocks the parent schema so that {@code getParentSchema().getValidators()}
     * returns a list containing the supplied discriminator validator (or null).
     */
    private void mockParentSchema(OneOfValidator validator, DiscriminatorValidator discriminatorValidator) throws Exception {
        JsonSchema parent = mock(JsonSchema.class);
        List<JsonValidator> list = discriminatorValidator == null ? Collections.emptyList() : Collections.singletonList(discriminatorValidator);
        when(parent.getValidators()).thenReturn(list);
        // inject parent into the validator (BaseJsonValidator has a protected field)
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("parentSchema");
        f.setAccessible(true);
        f.set(validator, parent);
    }

    @Test
    void validateSingleMatchingSchemaReturnsNoErrors() throws Exception {
        // oneOf with two alternatives: first matches, second does not.
        String schemaJson = """
            {
              "oneOf": [
                { "type": "string" },
                { "type": "integer" }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // a string
        JsonNode instance = MAPPER.readTree("\"hello\"");
        Method validateMethod = getValidateMethod();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = (Set<ValidationMessage>) validateMethod.invoke(validator, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        assertTrue(messages.isEmpty(), "When exactly one subschema validates, no errors should be reported");
    }

    @Test
    void validateMultipleMatchingSchemasReturnsOneOfError() throws Exception {
        // both alternatives are satisfied (string also matches integer via coercion is not allowed,
        // so we use two schemas that both accept the same instance).
        String schemaJson = """
            {
              "oneOf": [
                { "type": "string", "minLength": 0 },
                { "type": "string", "maxLength": 10 }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // a string that satisfies both subschemas
        JsonNode instance = MAPPER.readTree("\"abc\"");
        Method validateMethod = getValidateMethod();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = (Set<ValidationMessage>) validateMethod.invoke(validator, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        // Expect a single OneOf error describing the two matching indexes (0 and 1)
        assertEquals(1, messages.size(), "When more than one subschema validates, a single OneOf error should be returned");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("2 are valid with indexes '0, 1'") || vm.getMessage().contains("2 are valid with indexes '0,1'"), "Message should mention that two schemas are valid and list their indexes");
    }

    @Test
    void validateNoMatchingSchemasReturnsOneOfError() throws Exception {
        // none of the alternatives match.
        String schemaJson = """
            {
              "oneOf": [
                { "type": "string", "pattern": "^foo$" },
                { "type": "integer", "minimum": 10 }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // does not match any subschema
        JsonNode instance = MAPPER.readTree("\"bar\"");
        Method validateMethod = getValidateMethod();
        @SuppressWarnings("unchecked")
        Set<ValidationMessage> messages = (Set<ValidationMessage>) validateMethod.invoke(validator, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        // Expect a single OneOf error indicating zero valid schemas.
        assertEquals(1, messages.size(), "When no subschema validates, a single OneOf error should be returned");
        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertTrue(vm.getMessage().contains("0 are valid") || vm.getMessage().contains("0 are valid"), "Message should indicate that zero schemas are valid");
    }

    /**
     * Helper that invokes the protected validate method and returns the result.
     */
    @SuppressWarnings("unchecked")
    private Set<ValidationMessage> invokeValidate(OneOfValidator validator, JsonNode node, boolean walk) throws Exception {
        Method m = getValidateMethod();
        return (Set<ValidationMessage>) m.invoke(validator, executionContext, node, node, new JsonNodePath(PathType.JSON_POINTER), walk);
    }

    @Test
    void noDiscriminator_singleValidSchema_happyPath() throws Exception {
        // one schema only, it validates – expect empty set
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // failFast true, discriminator disabled
        mockExecutionContext(true, false, false, true);
        injectValidationContext(validator);
        // spy to control canShortCircuit/reportChildErrors (both irrelevant here)
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(true).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        assertTrue(msgs.isEmpty(), "Exactly one subschema validates – no errors expected");
    }

    @Test
    void noDiscriminator_multipleValidSchemas_shortCircuitEnabled() throws Exception {
        // two schemas both accept the instance; short‑circuit after the second valid schema
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // failFast false
        mockExecutionContext(false, false, false, true);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        // enable short‑circuit
        doReturn(true).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        assertEquals(1, msgs.size(), "More than one subschema validates – expect a single OneOf error");
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getType());
        // messageKey should be "oneOf.indexes" – we check the presence of indexes 0 and 1
        assertTrue(vm.getMessage().contains("0") && vm.getMessage().contains("1"), "Message should list the indexes of the matching schemas");
    }

    @Test
    void noDiscriminator_multipleValidSchemas_shortCircuitDisabled() throws Exception {
        // three schemas all validate; short‑circuit disabled – all three indexes reported
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // short‑circuit disabled
        mockExecutionContext(false, false, false, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertTrue(vm.getMessage().contains("0") && vm.getMessage().contains("1") && vm.getMessage().contains("2"), "All three matching indexes should be listed");
    }

    @Test
    void noDiscriminator_noSchemaMatches_collectChildErrors() throws Exception {
        // two schemas that both fail; reportChildErrors = true -> child messages are unioned
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["b"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // reportChildErrors true
        mockExecutionContext(false, false, true, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(true).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"c\":3}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        // there should be at least one OneOf error + child errors from both subschemas
        assertTrue(msgs.size() > 1, "Expected OneOf error plus child errors");
        // verify that at least one message has type "oneOf"
        assertTrue(msgs.stream().anyMatch(m -> "oneOf".equals(m.getType())), "OneOf error must be present");
    }

    @Test
    void failFastFlag_propagatedInOneOfMessage() throws Exception {
        // both schemas fail; failFast true -> generated OneOf message must have failFast=true
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["b"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // failFast true
        mockExecutionContext(true, false, true, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(true).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"c\":3}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        ValidationMessage oneOfMsg = msgs.stream().filter(m -> "oneOf".equals(m.getType())).findFirst().orElseThrow(() -> new AssertionError("OneOf message missing"));
        assertTrue(oneOfMsg.isFailFast(), "OneOf message should carry failFast=true");
    }

    @Test
    void failFastFlag_false_propagatedInOneOfMessage() throws Exception {
        // same as previous but failFast false
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["b"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // failFast false
        mockExecutionContext(false, false, true, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(true).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"c\":3}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        ValidationMessage oneOfMsg = msgs.stream().filter(m -> "oneOf".equals(m.getType())).findFirst().orElseThrow(() -> new AssertionError("OneOf message missing"));
        assertFalse(oneOfMsg.isFailFast(), "OneOf message should carry failFast=false");
    }

    @Test
    void discriminatorEnabled_matchingProperty_success() throws Exception {
        // schema: oneOf with a $ref that matches the discriminator mapping
        String schemaJson = """
            {
              "discriminator": {
                "propertyName": "type",
                "mapping": { "A": "#/components/schemas/A" }
              },
              "oneOf": [
                { "$ref": "#/components/schemas/A" },
                { "type": "object", "required": ["b"] }
              ],
              "components": {
                "schemas": {
                  "A": { "type": "object", "required": ["a"] }
                }
              }
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // ExecutionContext with discriminator enabled
        mockExecutionContext(false, true, false, true);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(true).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        // mock the discriminator validator that the OneOfValidator will discover
        JsonNode discSchemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        DiscriminatorValidator discValidator = mockDiscriminatorValidator("type", Map.of("A", "#/components/schemas/A"), discSchemaNode);
        mockParentSchema(spy, discValidator);
        // instance with matching discriminator property
        JsonNode instance = MAPPER.readTree("{\"type\":\"A\", \"a\":1}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        assertTrue(msgs.isEmpty(), "Discriminator matches and exactly one subschema validates – no errors");
    }

    @Test
    void discriminatorEnabled_noMatchingRef_discriminatorErrorAdded() throws Exception {
        String schemaJson = """
            {
              "discriminator": {
                "propertyName": "type",
                "mapping": { "A": "#/components/schemas/A" }
              },
              "oneOf": [
                { "$ref": "#/components/schemas/A" }
              ],
              "components": {
                "schemas": {
                  "A": { "type": "object", "required": ["a"] }
                }
              }
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        mockExecutionContext(false, true, false, true);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(true).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode discSchemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        DiscriminatorValidator discValidator = mockDiscriminatorValidator("type", Map.of("A", "#/components/schemas/A"), discSchemaNode);
        mockParentSchema(spy, discValidator);
        // discriminator property present but value "B" does not map to any $ref
        JsonNode instance = MAPPER.readTree("{\"type\":\"B\"}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        assertEquals(2, msgs.size(), "One message for discriminator mismatch and one generic OneOf message expected");
        boolean hasDiscriminatorMsg = msgs.stream().anyMatch(m -> m.getMessage().contains("based on the provided discriminator"));
        assertTrue(hasDiscriminatorMsg, "Discriminator‑specific error must be present");
    }

    @Test
    void discriminatorEnabled_propertyMissing_ignoredPath() throws Exception {
        String schemaJson = """
            {
              "discriminator": {
                "propertyName": "type",
                "mapping": { "A": "#/components/schemas/A" }
              },
              "oneOf": [
                { "$ref": "#/components/schemas/A" }
              ],
              "components": {
                "schemas": {
                  "A": { "type": "object", "required": ["a"] }
                }
              }
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        mockExecutionContext(false, true, false, true);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(true).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode discSchemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        DiscriminatorValidator discValidator = mockDiscriminatorValidator("type", Map.of("A", "#/components/schemas/A"), discSchemaNode);
        mockParentSchema(spy, discValidator);
        // instance without the discriminator property
        JsonNode instance = MAPPER.readTree("{\"c\":3}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        // only the generic OneOf error (zero valid schemas) – no discriminator‑specific message
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertEquals("oneOf", vm.getType());
        assertFalse(vm.getMessage().contains("discriminator"), "Discriminator‑specific message should not be present when property is missing");
    }

    @Test
    void discriminatorEnabled_noValidatorBehavesLikeNormalOneOf() throws Exception {
        // schema contains discriminator keyword in config but parent schema has no validator
        String schemaJson = """
            {
              "oneOf": [
                { "type": "string", "minLength": 5 },
                { "type": "string", "maxLength": 2 }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // discriminator enabled but no validator
        mockExecutionContext(false, true, false, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        // parent schema without validators – we force null list to trigger NPE if code misbehaves
        // parent returns empty list
        mockParentSchema(spy, null);
        JsonNode instance = MAPPER.readTree("\"abc\"");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        // both subschemas are valid → generic OneOf error with indexes 0,1
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertTrue(vm.getMessage().contains("0") && vm.getMessage().contains("1"));
    }

    @Test
    void discriminatorEnabled_matchFoundButAnotherSchemaAlsoValid_indexesReported() throws Exception {
        // discriminator matches first schema, second schema also validates
        String schemaJson = """
            {
              "discriminator": {
                "propertyName": "type",
                "mapping": { "A": "#/components/schemas/A" }
              },
              "oneOf": [
                { "$ref": "#/components/schemas/A" },
                { "type": "object", "required": ["extra"] }
              ],
              "components": {
                "schemas": {
                  "A": { "type": "object", "required": ["type", "extra"] }
                }
              }
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        mockExecutionContext(false, true, false, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode discSchemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        DiscriminatorValidator discValidator = mockDiscriminatorValidator("type", Map.of("A", "#/components/schemas/A"), discSchemaNode);
        mockParentSchema(spy, discValidator);
        JsonNode instance = MAPPER.readTree("{\"type\":\"A\", \"extra\":1}");
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, false);
        assertEquals(1, msgs.size());
        ValidationMessage vm = msgs.iterator().next();
        assertTrue(vm.getMessage().contains("0") && vm.getMessage().contains("1"), "Both matching indexes should be reported even though discriminator matched");
    }

    @Test
    void walkFlagTrue_schemaValidatedViaWalk() throws Exception {
        // one schema validates only via walk()
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] },
                { "type": "object", "required": ["b"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        mockExecutionContext(false, false, false, true);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(true).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        // walk = true
        Set<ValidationMessage> msgs = invokeValidate(spy, instance, true);
        assertTrue(msgs.isEmpty(), "When walk=true and exactly one schema validates via walk(), no errors");
    }

    @Test
    void nullExecutionContext_throwsNPE() throws Exception {
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        Method m = getValidateMethod();
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        assertThrows(NullPointerException.class, () -> {
            m.invoke(validator, // executionContext = null
            null, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        });
    }

    @Test
    void nullNode_throwsNPE() throws Exception {
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        Method m = getValidateMethod();
        mockExecutionContext(false, false, false, false);
        injectValidationContext(validator);
        assertThrows(NullPointerException.class, () -> {
            m.invoke(validator, executionContext, // node = null
            null, null, new JsonNodePath(PathType.JSON_POINTER), false);
        });
    }

    @Test
    void nullParentSchema_throwsNPE() throws Exception {
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // force parentSchema to null via reflection
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("parentSchema");
        f.setAccessible(true);
        f.set(validator, null);
        mockExecutionContext(false, true, false, false);
        injectValidationContext(validator);
        Method m = getValidateMethod();
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        assertThrows(NullPointerException.class, () -> {
            m.invoke(validator, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        });
    }

    @Test
    void parentSchemaWithoutValidators_throwsNPE() throws Exception {
        String schemaJson = """
            {
              "oneOf": [
                { "type": "object", "required": ["a"] }
              ]
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        // mock a parent schema whose getValidators() returns null
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(null);
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("parentSchema");
        f.setAccessible(true);
        f.set(validator, parent);
        mockExecutionContext(false, true, false, false);
        injectValidationContext(validator);
        Method m = getValidateMethod();
        JsonNode instance = MAPPER.readTree("{\"a\":1}");
        assertThrows(NullPointerException.class, () -> {
            m.invoke(validator, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        });
    }

    @Test
    void discriminatorValidatorWrongType_throwsClassCastException() throws Exception {
        String schemaJson = """
            {
              "discriminator": {
                "propertyName": "type",
                "mapping": { "A": "#/components/schemas/A" }
              },
              "oneOf": [
                { "$ref": "#/components/schemas/A" }
              ],
              "components": {
                "schemas": {
                  "A": { "type": "object", "required": ["a"] }
                }
              }
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        mockExecutionContext(false, true, false, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        // create a dummy validator with keyword "discriminator" but NOT a DiscriminatorValidator
        JsonValidator bogus = mock(JsonValidator.class);
        when(bogus.getKeyword()).thenReturn("discriminator");
        // parent schema returns this bogus validator
        JsonSchema parent = mock(JsonSchema.class);
        when(parent.getValidators()).thenReturn(Collections.singletonList(bogus));
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("parentSchema");
        f.setAccessible(true);
        f.set(spy, parent);
        JsonNode instance = MAPPER.readTree("{\"type\":\"A\", \"a\":1}");
        Method m = getValidateMethod();
        assertThrows(ClassCastException.class, () -> {
            m.invoke(spy, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        });
    }

    @Test
    void discriminatorSchemaNodeNotObjectNode_throwsClassCastException() throws Exception {
        String schemaJson = """
            {
              "discriminator": {
                "propertyName": "type",
                "mapping": { "A": "#/components/schemas/A" }
              },
              "oneOf": [
                { "$ref": "#/components/schemas/A" }
              ],
              "components": {
                "schemas": {
                  "A": { "type": "object", "required": ["a"] }
                }
              }
            }
            """;
        JsonSchema schema = JsonSchemaFactory.getInstance(VersionFlag.V202012).getSchema(schemaJson);
        OneOfValidator validator = getOneOfValidator(schema);
        mockExecutionContext(false, true, false, false);
        injectValidationContext(validator);
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(false).when(spy).canShortCircuit();
        doReturn(false).when(spy).reportChildErrors(any());
        // discriminator validator whose schemaNode is an ArrayNode (not ObjectNode)
        JsonNode arrayNode = MAPPER.readTree("[1,2,3]");
        DiscriminatorValidator discValidator = mockDiscriminatorValidator("type", Map.of("A", "#/components/schemas/A"), arrayNode);
        mockParentSchema(spy, discValidator);
        JsonNode instance = MAPPER.readTree("{\"type\":\"A\", \"a\":1}");
        Method m = getValidateMethod();
        assertThrows(ClassCastException.class, () -> {
            m.invoke(spy, executionContext, instance, instance, new JsonNodePath(PathType.JSON_POINTER), false);
        });
    }
}
