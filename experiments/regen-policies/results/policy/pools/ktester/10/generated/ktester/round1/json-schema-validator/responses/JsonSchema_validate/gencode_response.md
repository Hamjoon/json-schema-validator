```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * The tests use a heavily mocked environment – the {@link JsonSchema} instance is created
 * by the factory and then spied so that we can stub {@link JsonSchema#getValidators()} and
 * verify internal calls such as discriminator registration and matching.
 *
 * Each test corresponds to a logical group described in the JSON test‑case specification.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    /** The JsonSchema instance under test – a spy so we can stub/verify protected methods. */
    private JsonSchema jsonSchema;

    /** Mocked collaborators used across many test cases. */
    @Mock private ExecutionContext mockExecutionContext;
    @Mock private CollectorContext mockCollectorContext;
    @Mock private DiscriminatorContext mockDiscriminatorContext;
    @Mock private ValidationContext mockValidationContext;
    @Mock private SchemaValidatorsConfig mockConfig;
    @Mock private JsonValidator mockValidator; // generic validator
    @Mock private JsonValidator mockItemsValidator; // ItemsValidator‑like
    @Mock private JsonValidator mockPropertiesValidator; // PropertiesValidator‑like

    /** Common objects used as arguments. */
    private JsonNodePath instanceLocation;

    /** Helper to inject a field via reflection (used for protected fields in BaseJsonValidator). */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getSuperclass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Helper to create a simple schema node (ObjectNode) from a JSON string. */
    private static ObjectNode readObjectNode(String json) throws Exception {
        return (ObjectNode) JsonMapperFactory.getInstance().readTree(json);
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a minimal schema – we will replace the discriminator node in specific tests.
        JsonNode baseSchema = JsonMapperFactory.getInstance()
                .readTree("{\"type\":\"object\"}");

        jsonSchema = JsonSchemaFactory.getInstance(VersionFlag.V201909)
                .getSchema(baseSchema);

        // Spy the schema so we can stub getValidators() and verify protected calls.
        jsonSchema = Mockito.spy(jsonSchema);

        // Inject the mocked ValidationContext into the spy.
        setField(jsonSchema, "validationContext", mockValidationContext);
        // By default the config returns false for OpenAPI‑3 style discriminators.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // Default collector context – can be overridden per test.
        when(mockExecutionContext.getCollectorContext()).thenReturn(mockCollectorContext);

        // Default discriminator context – can be overridden per test.
        when(mockExecutionContext.getCurrentDiscriminatorContext()).thenReturn(null);

        // Instance location (JSON‑Pointer is the default path type).
        instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /* -------------------------------------------------------------------------
     *  Helper methods for building execution‑context variants
     * ---------------------------------------------------------------------- */

    /** Configures the execution context to have the given OpenAPI‑3 flag and discriminator context. */
    private void configureExecutionContext(boolean openAPI3, DiscriminatorContext ctx) {
        when(mockConfig.isOpenAPI3StyleDiscriminators()).thenReturn(openAPI3);
        when(mockExecutionContext.getCurrentDiscriminatorContext()).thenReturn(ctx);
    }

    /** Stubs the collector context to return a mock scope for enter/exit calls. */
    private Scope stubScopeForCollector(Scope parentScope, Scope childScope) {
        when(mockCollectorContext.enterDynamicScope(any(JsonSchema.class))).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        return parentScope;
    }

    /* -------------------------------------------------------------------------
     *  1. OpenAPI‑3 style discriminator registration (pre‑validation)
     * ---------------------------------------------------------------------- */

    @Test
    void preValidation_registerDiscriminator_whenOpenAPI3AndContextPresent() throws Exception {
        // Schema with a discriminator object.
        ObjectNode discriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        // ExecutionContext reports OpenAPI‑3 enabled and provides a discriminator context.
        configureExecutionContext(true, mockDiscriminatorContext);

        // No validators – we are only interested in the pre‑validation block.
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        // Invoke method under test.
        jsonSchema.validate(mockExecutionContext, null, null, null);

        // Verify that the discriminator was registered exactly once.
        verify(mockDiscriminatorContext, times(1))
                .registerDiscriminator(eq(jsonSchema.getSchemaLocation()), eq(discriminator));
    }

    @Test
    void preValidation_skipWhenDiscriminatorContextIsNull() throws Exception {
        ObjectNode discriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, null); // no discriminator context
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        jsonSchema.validate(mockExecutionContext, null, null, null);

        verifyNoInteractions(mockDiscriminatorContext);
    }

    @Test
    void preValidation_skipWhenOpenAPI3Disabled() throws Exception {
        ObjectNode discriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(false, mockDiscriminatorContext); // OpenAPI‑3 disabled
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        jsonSchema.validate(mockExecutionContext, null, null, null);

        verifyNoInteractions(mockDiscriminatorContext);
    }

    /* -------------------------------------------------------------------------
     *  2. Validator result handling and collector merging
     * ---------------------------------------------------------------------- */

    @Test
    void validatorLoop_allValidatorsReturnEmpty_mergeParentScope() throws Exception {
        // Two dummy validators that return null (treated as empty).
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        when(v1.validate(any(), any(), any(), any())).thenReturn(null);
        when(v2.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        when(jsonSchema.getValidators()).thenReturn(Arrays.asList(v1, v2));

        // Mock scopes for each validator iteration.
        Scope parent1 = mock(Scope.class);
        Scope child1 = mock(Scope.class);
        Scope parent2 = mock(Scope.class);
        Scope child2 = mock(Scope.class);

        // First validator iteration.
        when(mockCollectorContext.enterDynamicScope(jsonSchema)).thenReturn(parent1);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(child1);
        // Second validator iteration – need to change the return values.
        when(mockCollectorContext.enterDynamicScope(jsonSchema)).thenReturn(parent2);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(child2);

        // Execute.
        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext,
                JsonMapperFactory.getInstance().readTree("{\"a\":1}"),
                JsonMapperFactory.getInstance().readTree("{\"a\":1}"),
                instanceLocation);

        // No errors expected.
        assertTrue(errors.isEmpty());

        // Verify that mergeWith was called for both parent scopes because results were empty.
        verify(parent1, times(1)).mergeWith(child1);
        verify(parent2, times(1)).mergeWith(child2);
    }

    @Test
    void validatorLoop_itemsValidatorReturnsErrors_collectEvaluatedItems() throws Exception {
        // ItemsValidator‑like mock that returns a non‑empty error set.
        ValidationMessage msg = ValidationMessage.of("items.error", "items", "error");
        Set<ValidationMessage> errorSet = new LinkedHashSet<>(Collections.singletonList(msg));
        when(mockItemsValidator.validate(any(), any(), any(), any())).thenReturn(errorSet);

        when(jsonSchema.getValidators()).thenReturn(Collections.singletonList(mockItemsValidator));

        // Mock scopes.
        Scope parentScope = mock(Scope.class);
        Scope childScope = mock(Scope.class);
        when(mockCollectorContext.enterDynamicScope(jsonSchema)).thenReturn(parentScope);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childScope);
        // Simulate that the child scope evaluated some items.
        when(childScope.getEvaluatedItems()).thenReturn(new HashSet<>(Arrays.asList("item1", "item2")));

        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext,
                JsonMapperFactory.getInstance().readTree("[1,2,3]"),
                JsonMapperFactory.getInstance().readTree("[1,2,3]"),
                instanceLocation);

        assertEquals(1, errors.size());
        assertTrue(errors.contains(msg));

        // The parent scope should NOT be merged because errors were returned.
        verify(parentScope, never()).mergeWith(any());

        // Evaluated items must be added to the collector.
        verify(mockCollectorContext.getEvaluatedItems(), times(1)).addAll(childScope.getEvaluatedItems());
    }

    @Test
    void validatorLoop_multipleValidators_mixEmptyAndErrorful() throws Exception {
        // ItemsValidator returns errors, PropertiesValidator returns empty.
        ValidationMessage itemsMsg = ValidationMessage.of("items.error", "items", "error");
        Set<ValidationMessage> itemsErrors = new LinkedHashSet<>(Collections.singletonList(itemsMsg));
        when(mockItemsValidator.validate(any(), any(), any(), any())).thenReturn(itemsErrors);
        when(mockPropertiesValidator.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        when(jsonSchema.getValidators()).thenReturn(Arrays.asList(mockItemsValidator, mockPropertiesValidator));

        // Scopes for each validator.
        Scope parentItems = mock(Scope.class);
        Scope childItems = mock(Scope.class);
        Scope parentProps = mock(Scope.class);
        Scope childProps = mock(Scope.class);
        // First validator (items)
        when(mockCollectorContext.enterDynamicScope(jsonSchema)).thenReturn(parentItems);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childItems);
        when(childItems.getEvaluatedItems()).thenReturn(new HashSet<>(Arrays.asList("i1")));
        // Second validator (properties)
        when(mockCollectorContext.enterDynamicScope(jsonSchema)).thenReturn(parentProps);
        when(mockCollectorContext.exitDynamicScope()).thenReturn(childProps);
        when(childProps.getEvaluatedProperties()).thenReturn(new HashSet<>(Arrays.asList("p1")));

        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext,
                JsonMapperFactory.getInstance().readTree("{\"a\":1}"),
                JsonMapperFactory.getInstance().readTree("{\"a\":1}"),
                instanceLocation);

        // Both messages should be present.
        assertEquals(1, errors.size());
        assertTrue(errors.contains(itemsMsg));

        // Items evaluated should be added.
        verify(mockCollectorContext.getEvaluatedItems(), times(1)).addAll(childItems.getEvaluatedItems());
        // Properties evaluated should be added.
        verify(mockCollectorContext.getEvaluatedProperties(), times(1)).addAll(childProps.getEvaluatedProperties());

        // Parent scope for the properties validator (empty result) must be merged.
        verify(parentProps, times(1)).mergeWith(childProps);
        // Parent scope for items validator must NOT be merged.
        verify(parentItems, never()).mergeWith(any());
    }

    /* -------------------------------------------------------------------------
     *  3. OpenAPI‑3 style discriminator matching (post‑validation)
     * ---------------------------------------------------------------------- */

    @Test
    void postValidation_matchDiscriminator_whenPropertyPresent() throws Exception {
        // Schema with discriminator.
        ObjectNode discriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        // ExecutionContext with OpenAPI‑3 enabled and a discriminator context.
        configureExecutionContext(true, mockDiscriminatorContext);
        // No previously registered discriminator.
        when(mockDiscriminatorContext.getDiscriminatorForPath(any())).thenReturn(null);

        // Spy the jsonSchema to verify the protected checkDiscriminatorMatch call.
        doNothing().when(jsonSchema).checkDiscriminatorMatch(any(), any(), any(), any());

        // No validators – focus on post‑validation block.
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"type\":\"cat\"}");

        jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);

        // Verify that the discriminator was registered (since it was not present in the context).
        verify(mockDiscriminatorContext, times(1))
                .registerDiscriminator(eq(jsonSchema.getSchemaLocation()), eq(discriminator));

        // Verify that checkDiscriminatorMatch was called with the value "cat".
        verify(jsonSchema, times(1)).checkDiscriminatorMatch(eq(mockDiscriminatorContext),
                eq(discriminator), eq("cat"), eq(jsonSchema));
    }

    @Test
    void postValidation_useAlreadyRegisteredDiscriminator() throws Exception {
        // Schema discriminator.
        ObjectNode schemaDiscriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + schemaDiscriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        // Context already holds a discriminator for this schema location.
        ObjectNode registeredDiscriminator = readObjectNode("{\"propertyName\":\"type\",\"mapping\":{\"dog\":\"#/Dog\"}}");
        configureExecutionContext(true, mockDiscriminatorContext);
        when(mockDiscriminatorContext.getDiscriminatorForPath(eq(jsonSchema.getSchemaLocation()))).thenReturn(registeredDiscriminator);

        doNothing().when(jsonSchema).checkDiscriminatorMatch(any(), any(), any(), any());

        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"type\":\"dog\"}");

        jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);

        // No new registration because one already exists.
        verify(mockDiscriminatorContext, never()).registerDiscriminator(any(), any());

        // checkDiscriminatorMatch should receive the registered discriminator and value "dog".
        verify(jsonSchema, times(1)).checkDiscriminatorMatch(eq(mockDiscriminatorContext),
                eq(registeredDiscriminator), eq("dog"), eq(jsonSchema));
    }

    @Test
    void postValidation_nullPropertyValue_passesNullToMatch() throws Exception {
        // Schema discriminator with propertyName "type".
        ObjectNode discriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, mockDiscriminatorContext);
        when(mockDiscriminatorContext.getDiscriminatorForPath(any())).thenReturn(null);
        doNothing().when(jsonSchema).checkDiscriminatorMatch(any(), any(), any(), any());

        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        // Instance does NOT contain the "type" property.
        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"name\":\"John\"}");

        jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);

        // The discriminator is registered (first time) and then match is called with null value.
        verify(mockDiscriminatorContext, times(1))
                .registerDiscriminator(eq(jsonSchema.getSchemaLocation()), eq(discriminator));
        verify(jsonSchema, times(1)).checkDiscriminatorMatch(eq(mockDiscriminatorContext),
                eq(discriminator), isNull(), eq(jsonSchema));
    }

    @Test
    void postValidation_skipWhenDiscriminatorContextIsNull() throws Exception {
        // Schema with discriminator.
        ObjectNode discriminator = readObjectNode("{\"propertyName\":\"type\"}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, null); // no discriminator context

        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"type\":\"bird\"}");

        // Should not throw and should simply return an empty set.
        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);
        assertTrue(errors.isEmpty());

        // No interaction with discriminator context.
        verifyNoInteractions(mockDiscriminatorContext);
    }

    /* -------------------------------------------------------------------------
     *  4. No errors returned (empty set handling)
     * ---------------------------------------------------------------------- */

    @Test
    void returnsEmptySetWhenNoValidatorProducesErrors() throws Exception {
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
        // The implementation returns Collections.emptySet() – verify immutability.
        assertThrows(UnsupportedOperationException.class, () -> errors.add(null));
    }

    /* -------------------------------------------------------------------------
     *  5. Discriminator absent (schema without discriminator)
     * ---------------------------------------------------------------------- */

    @Test
    void schemaWithoutDiscriminator_skipsAllDiscriminatorLogic() throws Exception {
        // Schema without a "discriminator" property.
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\"}");
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, mockDiscriminatorContext);
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);
        assertTrue(errors.isEmpty());

        // No registration or matching should happen.
        verifyNoInteractions(mockDiscriminatorContext);
    }

    /* -------------------------------------------------------------------------
     *  6. Empty validator list
     * ---------------------------------------------------------------------- */

    @Test
    void emptyValidatorList_returnsEmptySetAndNoScopeMerges() throws Exception {
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"x\":10}");

        Set<ValidationMessage> errors = jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation);
        assertTrue(errors.isEmpty());

        // No interaction with collector context because the loop is never entered.
        verifyNoInteractions(mockCollectorContext);
    }

    /* -------------------------------------------------------------------------
     *  7. Null ValidationContext config (NPE)
     * ---------------------------------------------------------------------- */

    @Test
    void nullConfig_causesNullPointerException() throws Exception {
        // ValidationContext returns null config.
        when(mockValidationContext.getConfig()).thenReturn(null);
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     *  8. Null CollectorContext (NPE)
     * ---------------------------------------------------------------------- */

    @Test
    void nullCollectorContext_causesNullPointerException() throws Exception {
        when(mockExecutionContext.getCollectorContext()).thenReturn(null);
        when(jsonSchema.getValidators()).thenReturn(Collections.singletonList(mockValidator));

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     *  9. Null validators list (NPE)
     * ---------------------------------------------------------------------- */

    @Test
    void nullValidatorsList_causesNullPointerException() throws Exception {
        // Force getValidators() to return null.
        when(jsonSchema.getValidators()).thenReturn(null);

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     * 10. Validator throws unchecked exception (propagation)
     * ---------------------------------------------------------------------- */

    @Test
    void validatorThrowsRuntimeException_propagatesOut() throws Exception {
        RuntimeException boom = new RuntimeException("boom");
        when(mockValidator.validate(any(), any(), any(), any())).thenThrow(boom);
        when(jsonSchema.getValidators()).thenReturn(Collections.singletonList(mockValidator));

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
        assertSame(boom, ex);
    }

    /* -------------------------------------------------------------------------
     * 11. CollectorContext returns null scope (NPE on evaluated items/properties)
     * ---------------------------------------------------------------------- */

    @Test
    void collectorReturnsNullScope_andValidatorReturnsErrors_triggersNPE() throws Exception {
        // Validator returns a non‑empty error set.
        ValidationMessage msg = ValidationMessage.of("error", "path", "msg");
        when(mockValidator.validate(any(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(Collections.singletonList(msg)));
        when(jsonSchema.getValidators()).thenReturn(Collections.singletonList(mockValidator));

        // Collector returns null on exitDynamicScope().
        when(mockCollectorContext.enterDynamicScope(any())).thenReturn(mock(Scope.class));
        when(mockCollectorContext.exitDynamicScope()).thenReturn(null);

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     * 12. Discriminator without propertyName field (NPE on asText)
     * ---------------------------------------------------------------------- */

    @Test
    void discriminatorMissingPropertyName_causesNullPointerException() throws Exception {
        // Discriminator object without "propertyName".
        ObjectNode discriminator = readObjectNode("{\"mapping\":{}}");
        ObjectNode schemaNode = readObjectNode("{\"type\":\"object\",\"discriminator\":" + discriminator.toString() + "}");
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, mockDiscriminatorContext);
        when(mockDiscriminatorContext.getDiscriminatorForPath(any())).thenReturn(null);
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"type\":\"cat\"}");

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     * 13. Null schemaNode (NPE on discriminator access)
     * ---------------------------------------------------------------------- */

    @Test
    void nullSchemaNode_causesNullPointerExceptionOnDiscriminatorAccess() throws Exception {
        // Force the protected schemaNode field to be null.
        setField(jsonSchema, "schemaNode", null);

        configureExecutionContext(true, mockDiscriminatorContext);
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"type\":\"cat\"}");

        assertThrows(NullPointerException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     * 14. Pre‑validation discriminator cast exception (non‑ObjectNode)
     * ---------------------------------------------------------------------- */

    @Test
    void preValidation_discriminatorNotObjectNode_throwsClassCastException() throws Exception {
        // Schema with discriminator as an array node (invalid type for cast).
        JsonNode badDiscriminator = JsonMapperFactory.getInstance().readTree("[1,2,3]");
        ObjectNode schemaNode = JsonMapperFactory.getInstance()
                .createObjectNode()
                .set("type", JsonMapperFactory.getInstance().readTree("\"object\""))
                .set("discriminator", badDiscriminator);
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, mockDiscriminatorContext);
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"a\":1}");

        assertThrows(ClassCastException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }

    /* -------------------------------------------------------------------------
     * 15. Post‑validation discriminator cast exception (non‑ObjectNode)
     * ---------------------------------------------------------------------- */

    @Test
    void postValidation_discriminatorNotObjectNode_throwsClassCastException() throws Exception {
        // Schema with discriminator as an array node (invalid for post‑validation cast).
        JsonNode badDiscriminator = JsonMapperFactory.getInstance().readTree("[\"x\"]");
        ObjectNode schemaNode = JsonMapperFactory.getInstance()
                .createObjectNode()
                .set("type", JsonMapperFactory.getInstance().readTree("\"object\""))
                .set("discriminator", badDiscriminator);
        setField(jsonSchema, "schemaNode", schemaNode);

        configureExecutionContext(true, mockDiscriminatorContext);
        when(mockDiscriminatorContext.getDiscriminatorForPath(any())).thenReturn(null);
        when(jsonSchema.getValidators()).thenReturn(Collections.emptyList());

        JsonNode instance = JsonMapperFactory.getInstance().readTree("{\"type\":\"cat\"}");

        assertThrows(ClassCastException.class,
                () -> jsonSchema.validate(mockExecutionContext, instance, instance, instanceLocation));
    }
}
```