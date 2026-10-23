
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import com.networknt.schema.CollectorContext.Scope;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_Test {

    /**
     * shared test fixtures **
     */
    private static final String SCHEMA_PATH = "#";

    private static final URI DUMMY_URI = URI.create("http://example.com/schema.json");

    /**
     * objects created for every test
     */
    private ValidationContext validationContext;

    private ExecutionContext executionContext;

    // class under test
    private JsonSchema jsonSchema;

    // minimal schema (no validators)
    private ObjectNode schemaNode;

    @BeforeEach
    void setupBeforeEach() {
        /* -------------------------------------------------
         * 1.  Mock the ValidationContext and its configuration
         * ------------------------------------------------- */
        validationContext = mock(ValidationContext.class);
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
        // By default we do **not** enable OpenAPI‑3 style discriminators
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        // {} – no validators, no discriminator
        schemaNode = JsonNodeFactory.instance.objectNode();
        /* -------------------------------------------------
         * 3.  Build the JsonSchema instance via the package‑private
         *     factory method.  All constructor arguments are
         *     supplied explicitly.
         * ------------------------------------------------- */
        jsonSchema = JsonSchema.from(validationContext, SCHEMA_PATH, DUMMY_URI, schemaNode, // no parent
        null, // do not suppress sub‑schema retrieval
        false);
        /* -------------------------------------------------
         * 4.  Prepare a fresh ExecutionContext (real object, not a mock)
         * ------------------------------------------------- */
        executionContext = new ExecutionContext();
        /* -------------------------------------------------
         * 5.  Ensure the internal validator map is empty.
         *    The map lives in the superclass BaseJsonValidator
         *    (protected field `validators`).  Using reflection keeps
         *    us from calling any private methods/fields directly.
         * ------------------------------------------------- */
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
            // start with a clean slate for each test
            map.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /* -------------------------------------------------
         * 2.  Create a very small (empty) schema node.
         * ------------------------------------------------- */
        /* -------------------------------------------------
         * 1.  Mock the ValidationContext and its configuration
         * ------------------------------------------------- */
        validationContext = mock(ValidationContext.class);
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
        // default: discriminator handling disabled
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        // {}
        schemaNode = JsonNodeFactory.instance.objectNode();
        /* -------------------------------------------------
         * 3.  Build the JsonSchema instance via the package‑private
         *    factory method.
         * ------------------------------------------------- */
        jsonSchema = JsonSchema.from(validationContext, SCHEMA_PATH, DUMMY_URI, schemaNode, // no parent
        null, // do not suppress sub‑schema retrieval
        false);
        /* -------------------------------------------------
         * 4.  Fresh ExecutionContext (real object)
         * ------------------------------------------------- */
        executionContext = new ExecutionContext();
        /* -------------------------------------------------
         * 5.  Ensure the internal validator map is empty.
         * ------------------------------------------------- */
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
        map.clear();
    }

    /**
     * inject a discriminator object into the schemaNode of the {@link JsonSchema}
     */
    private void injectDiscriminator(ObjectNode discriminatorNode) throws Exception {
        schemaNode.set("discriminator", discriminatorNode);
        // schemaNode is a protected field in BaseJsonValidator
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
        f.setAccessible(true);
        f.set(jsonSchema, schemaNode);
    }

    /**
     * create a mock Scope that returns given evaluated items / properties
     */
    private Scope mockScope(Set<String> items, Set<String> properties) {
        Scope scope = mock(Scope.class);
        when(scope.getEvaluatedItems()).thenReturn(items);
        when(scope.getEvaluatedProperties()).thenReturn(properties);
        return scope;
    }

    /**
     * create a mock CollectorContext that returns a fresh mock Scope for each call
     */
    private CollectorContext mockCollectorContext(Scope... scopes) {
        CollectorContext cc = mock(CollectorContext.class);
        // each call to enterDynamicScope returns the next mock scope
        final Queue<Scope> q = new ArrayDeque<>(Arrays.asList(scopes));
        when(cc.enterDynamicScope(any())).thenAnswer(inv -> q.poll());
        // not used directly
        when(cc.exitDynamicScope()).thenReturn(mock(Scope.class));
        when(cc.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(cc.getEvaluatedProperties()).thenReturn(new HashSet<>());
        return cc;
    }

    /* -------------------------------------------------
     *  TEST 1 – No validators → validate returns an empty set
     * ------------------------------------------------- */
    @Test
    void testValidate_NoValidators_ReturnsEmptySet() {
        // any JSON payload
        JsonNode data = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");
        assertNotNull(result, "Result must never be null");
        assertTrue(result.isEmpty(), "When there are no validators the result set should be empty");
    }

    /* -------------------------------------------------
     *  TEST 2 – A validator that reports errors → those errors are returned
     * ------------------------------------------------- */
    @Test
    void testValidate_ValidatorReturnsErrors_ErrorsCollected() {
        /* -------------------------------------------------
         * 1.  Create a mock JsonValidator that will be stored
         *     in the internal validator map.
         * ------------------------------------------------- */
        JsonValidator mockValidator = mock(JsonValidator.class);
        ValidationMessage err1 = ValidationMessage.of("ERR001", "$", "first error");
        ValidationMessage err2 = ValidationMessage.of("ERR002", "$", "second error");
        Set<ValidationMessage> validatorErrors = new LinkedHashSet<>(Arrays.asList(err1, err2));
        when(mockValidator.validate(any(), any(), any(), anyString())).thenReturn(validatorErrors);
        /* -------------------------------------------------
         * 2.  Insert the mock into the private `validators` map.
         * ------------------------------------------------- */
        try {
            java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
            map.put("mockKeyword", mockValidator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /* -------------------------------------------------
         * 3.  Execute the method under test.
         * ------------------------------------------------- */
        // payload does not matter for this test
        JsonNode data = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");
        /* -------------------------------------------------
         * 4.  Verify interaction and outcome.
         * ------------------------------------------------- */
        // The validator should have been invoked exactly once.
        verify(mockValidator, times(1)).validate(eq(executionContext), eq(data), eq(data), eq("$"));
        // The result must contain the two messages supplied by the mock.
        assertEquals(2, result.size(), "Exactly the messages produced by the validator should be returned");
        assertTrue(result.containsAll(validatorErrors), "Result set should contain all validator errors");
    }

    /* -------------------------------------------------
     *  TEST 3 – Multiple validators returning empty sets → scopes merged
     * ------------------------------------------------- */
    @Test
    void testValidate_MultipleEmptyValidators_MergeScopes() throws Exception {
        // mock two validators that return empty sets
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        when(v1.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
        when(v2.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
        // each validator gets its own child scope
        Scope childScope1 = mockScope(new HashSet<>(Arrays.asList("item1")), Collections.emptySet());
        Scope childScope2 = mockScope(Collections.emptySet(), new HashSet<>(Arrays.asList("propA")));
        // parent scope that will receive merges
        Scope parentScope = mock(Scope.class);
        when(parentScope.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(parentScope.getEvaluatedProperties()).thenReturn(new HashSet<>());
        // collector context that returns the above scopes in order
        CollectorContext collectorContext = mock(CollectorContext.class);
        when(collectorContext.enterDynamicScope(any())).thenReturn(parentScope, childScope1, childScope2);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope1, childScope2);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        // inject collector context into executionContext
        java.lang.reflect.Field ecField = ExecutionContext.class.getDeclaredField("collectorContext");
        ecField.setAccessible(true);
        ecField.set(executionContext, collectorContext);
        // inject validators
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
        map.put("v1", v1);
        map.put("v2", v2);
        JsonNode data = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");
        assertTrue(result.isEmpty(), "No validator reported errors");
        // verify merges
        verify(parentScope, times(2)).mergeWith(any());
    }

    /* -------------------------------------------------
     *  TEST 4 – PrefixItemsValidator returns errors → evaluated items are copied
     * ------------------------------------------------- */
    @Test
    void testValidate_PrefixItemsValidator_WithErrors_CopiesEvaluatedItems() throws Exception {
        PrefixItemsValidator prefixValidator = mock(PrefixItemsValidator.class);
        ValidationMessage err = ValidationMessage.of("ERRX", "$", "prefix error");
        when(prefixValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
        // child scope that pretends to have evaluated items
        Scope childScope = mockScope(new HashSet<>(Arrays.asList("itemA", "itemB")), Collections.emptySet());
        CollectorContext collectorContext = mockCollectorContext(childScope);
        // make collectorContext.exitDynamicScope return the same childScope (simplify)
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);
        // inject collector context
        java.lang.reflect.Field ecField = ExecutionContext.class.getDeclaredField("collectorContext");
        ecField.setAccessible(true);
        ecField.set(executionContext, collectorContext);
        // inject validator
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
        map.put("prefix", prefixValidator);
        JsonNode data = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");
        assertEquals(1, result.size());
        assertTrue(result.contains(err));
        // evaluated items must be added to collector context
        verify(collectorContext.getEvaluatedItems(), times(1)).addAll(childScope.getEvaluatedItems());
    }

    /* -------------------------------------------------
     *  TEST 5 – PropertiesValidator returns errors → evaluated properties are copied
     * ------------------------------------------------- */
    @Test
    void testValidate_PropertiesValidator_WithErrors_CopiesEvaluatedProperties() throws Exception {
        PropertiesValidator propValidator = mock(PropertiesValidator.class);
        ValidationMessage err = ValidationMessage.of("ERRP", "$", "property error");
        when(propValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
        Scope childScope = mockScope(Collections.emptySet(), new HashSet<>(Arrays.asList("propX", "propY")));
        CollectorContext collectorContext = mockCollectorContext(childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);
        java.lang.reflect.Field ecField = ExecutionContext.class.getDeclaredField("collectorContext");
        ecField.setAccessible(true);
        ecField.set(executionContext, collectorContext);
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
        map.put("props", propValidator);
        JsonNode data = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");
        assertEquals(1, result.size());
        assertTrue(result.contains(err));
        verify(collectorContext.getEvaluatedProperties(), times(1)).addAll(childScope.getEvaluatedProperties());
    }

    /* -------------------------------------------------
     *  TEST 7 – Discriminator node missing → checkDiscriminatorMatch called with null value
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_PropertyMissing_NullValuePassed() throws Exception {
        // enable discriminator handling
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // mock discriminator context
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // discriminator object with propertyName "type"
        ObjectNode discNode = JsonNodeFactory.instance.objectNode().put("propertyName", "type");
        injectDiscriminator(discNode);
        // json payload without "type"
        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("name", "bob");
        // spy on jsonSchema to verify protected method call
        JsonSchema spySchema = Mockito.spy(jsonSchema);
        // make the private call real (it is protected in BaseJsonValidator)
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
        Set<ValidationMessage> result = spySchema.validate(executionContext, payload, payload, "$");
        assertTrue(result.isEmpty());
        // verify that checkDiscriminatorMatch was invoked with a null discriminator property value
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), isNull(), eq(spySchema));
    }

    /* -------------------------------------------------
     *  TEST 8 – No discriminator context → discriminator block skipped
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_NoContext_Skipped() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(null);
        ObjectNode discNode = JsonNodeFactory.instance.objectNode().put("propertyName", "type");
        injectDiscriminator(discNode);
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("type", "cat");
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, payload, payload, "$");
        assertTrue(result.isEmpty(), "No errors expected and discriminator block should be skipped");
    }

    /* -------------------------------------------------
     *  TEST 9 – Register new discriminator (no prior registration)
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_RegisterNew() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // no prior registration
        when(discCtx.getDiscriminatorForPath(SCHEMA_PATH)).thenReturn(null);
        ObjectNode discNode = JsonNodeFactory.instance.objectNode().put("propertyName", "petType");
        injectDiscriminator(discNode);
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("petType", "dog");
        JsonSchema spySchema = Mockito.spy(jsonSchema);
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
        Set<ValidationMessage> result = spySchema.validate(executionContext, payload, payload, "$");
        assertTrue(result.isEmpty());
        // verify registration and match check
        verify(discCtx, times(1)).registerDiscriminator(eq(SCHEMA_PATH), eq(discNode));
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), eq("dog"), eq(spySchema));
    }

    /* -------------------------------------------------
     *  TEST 10 – Use already registered discriminator
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_UseExisting() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // existing discriminator for this path
        ObjectNode existingDisc = JsonNodeFactory.instance.objectNode().put("propertyName", "petType");
        when(discCtx.getDiscriminatorForPath(SCHEMA_PATH)).thenReturn(existingDisc);
        // schema also contains a discriminator (could be different)
        ObjectNode schemaDisc = JsonNodeFactory.instance.objectNode().put("propertyName", "ignored");
        injectDiscriminator(schemaDisc);
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("petType", "bird");
        JsonSchema spySchema = Mockito.spy(jsonSchema);
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
        Set<ValidationMessage> result = spySchema.validate(executionContext, payload, payload, "$");
        assertTrue(result.isEmpty());
        // registration must NOT happen
        verify(discCtx, never()).registerDiscriminator(anyString(), any());
        // match check receives the *existing* discriminator
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(existingDisc), eq("bird"), eq(spySchema));
    }

    /* -------------------------------------------------
     *  TEST 11 – Mixed validators: one error, one success
     * ------------------------------------------------- */
    @Test
    void testValidate_MixedValidators_ErrorAndSuccess() throws Exception {
        // error validator (not a collection type)
        JsonValidator errorValidator = mock(JsonValidator.class);
        ValidationMessage err = ValidationMessage.of("ERRM", "$", "mixed error");
        when(errorValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singleton(err)));
        // success validator returning empty set
        JsonValidator successValidator = mock(JsonValidator.class);
        when(successValidator.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
        // scopes
        Scope parentScope = mock(Scope.class);
        Scope childScope = mockScope(new HashSet<>(Arrays.asList("i1")), new HashSet<>(Arrays.asList("p1")));
        CollectorContext collectorContext = mock(CollectorContext.class);
        when(collectorContext.enterDynamicScope(any())).thenReturn(parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        java.lang.reflect.Field ecField = ExecutionContext.class.getDeclaredField("collectorContext");
        ecField.setAccessible(true);
        ecField.set(executionContext, collectorContext);
        // inject validators
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validators");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> map = (Map<String, JsonValidator>) f.get(jsonSchema);
        map.put("error", errorValidator);
        map.put("ok", successValidator);
        JsonNode data = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, data, data, "$");
        assertEquals(1, result.size());
        assertTrue(result.contains(err));
        // error validator should NOT cause evaluated items/properties to be added
        verify(collectorContext.getEvaluatedItems(), never()).addAll(any());
        verify(collectorContext.getEvaluatedProperties(), never()).addAll(any());
        // successful validator merges its scope into parent
        verify(parentScope, times(1)).mergeWith(childScope);
    }

    /* -------------------------------------------------
     *  TEST 12 – Discriminator node absent in schema → block skipped
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_NoDiscriminatorNode() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // schemaNode has no "discriminator" field (already the case)
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("type", "dog");
        Set<ValidationMessage> result = jsonSchema.validate(executionContext, payload, payload, "$");
        assertTrue(result.isEmpty());
    }

    /* -------------------------------------------------
     *  TEST 13 – Discriminator object missing propertyName → NPE
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_MissingPropertyName_NPE() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // discriminator without propertyName
        // empty
        ObjectNode badDisc = JsonNodeFactory.instance.objectNode();
        injectDiscriminator(badDisc);
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("any", "value");
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(executionContext, payload, payload, "$"), "Missing propertyName should cause NPE");
    }

    /* -------------------------------------------------
     *  TEST 14 – Null executionContext → NPE
     * ------------------------------------------------- */
    @Test
    void testValidate_NullExecutionContext_NPE() {
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(null, payload, payload, "$"));
    }

    /* -------------------------------------------------
     *  TEST 15 – Null collectorContext inside ExecutionContext → NPE
     * ------------------------------------------------- */
    @Test
    void testValidate_NullCollectorContext_NPE() throws Exception {
        // ExecutionContext with null collectorContext
        // default collectorContext is null
        ExecutionContext ec = new ExecutionContext();
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(ec, payload, payload, "$"));
    }

    /* -------------------------------------------------
     *  TEST 16 – Null validationContext inside JsonSchema → NPE
     * ------------------------------------------------- */
    @Test
    void testValidate_NullValidationContext_NPE() throws Exception {
        // use reflection to null out the validationContext field (protected in BaseJsonValidator)
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(jsonSchema, null);
        JsonNode payload = JsonNodeFactory.instance.objectNode();
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(executionContext, payload, payload, "$"));
    }

    /* -------------------------------------------------
     *  TEST 17 – Null schemaNode → NPE when discriminator block accessed
     * ------------------------------------------------- */
    @Test
    void testValidate_NullSchemaNode_NPE() throws Exception {
        // enable discriminator handling
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // set schemaNode to null via reflection
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("schemaNode");
        f.setAccessible(true);
        f.set(jsonSchema, null);
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("type", "cat");
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(executionContext, payload, payload, "$"));
    }

    /* -------------------------------------------------
     *  TEST 18 – Discriminator field not an ObjectNode → ClassCastException
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorNotObjectNode_ClassCastException() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // discriminator is a plain text node (invalid)
        schemaNode.set("discriminator", TextNode.valueOf("not-an-object"));
        // need to inject the modified schemaNode (already the same instance)
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("type", "cat");
        assertThrows(ClassCastException.class, () -> jsonSchema.validate(executionContext, payload, payload, "$"));
    }

    /* -------------------------------------------------
     *  TEST 19 – Null jsonNode during discriminator processing → NPE
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorEnabled_NullJsonNode_NPE() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        ObjectNode discNode = JsonNodeFactory.instance.objectNode().put("propertyName", "type");
        injectDiscriminator(discNode);
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(executionContext, null, null, "$"));
    }

    /* -------------------------------------------------
     *  TEST 20 – Discriminator propertyName node is null (explicit NPE)
     * ------------------------------------------------- */
    @Test
    void testValidate_DiscriminatorPropertyNameNodeNull_NPE() throws Exception {
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        // discriminator object with propertyName set to null (explicitly add null node)
        ObjectNode discNode = JsonNodeFactory.instance.objectNode();
        discNode.set("propertyName", NullNode.instance);
        injectDiscriminator(discNode);
        JsonNode payload = JsonNodeFactory.instance.objectNode().put("any", "value");
        assertThrows(NullPointerException.class, () -> jsonSchema.validate(executionContext, payload, payload, "$"));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:10: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:14: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:6: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:46: error: cannot find symbol
        validationContext = mock(ValidationContext.class);
                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:47: error: cannot find symbol
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
                                        ^
  symbol:   method mock(Class<SchemaValidatorsConfig>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:49: error: cannot find symbol
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:50: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:51: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:51: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
                                                           ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:88: error: cannot find symbol
        validationContext = mock(ValidationContext.class);
                            ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:89: error: variable config is already defined in method setupBeforeEach()
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
                               ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:89: error: cannot find symbol
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
                                        ^
  symbol:   method mock(Class<SchemaValidatorsConfig>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:91: error: cannot find symbol
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:92: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(config);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:93: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:93: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(mock(JsonMetaSchema.class));
                                                           ^
  symbol:   method mock(Class<JsonMetaSchema>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:132: error: cannot find symbol
        Scope scope = mock(Scope.class);
                      ^
  symbol:   method mock(Class<Scope>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:133: error: cannot find symbol
        when(scope.getEvaluatedItems()).thenReturn(items);
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:134: error: cannot find symbol
        when(scope.getEvaluatedProperties()).thenReturn(properties);
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:142: error: cannot find symbol
        CollectorContext cc = mock(CollectorContext.class);
                              ^
  symbol:   method mock(Class<CollectorContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:145: error: cannot find symbol
        when(cc.enterDynamicScope(any())).thenAnswer(inv -> q.poll());
                                  ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:147: error: cannot find symbol
        when(cc.exitDynamicScope()).thenReturn(mock(Scope.class));
        ^
  symbol:   method when(Scope)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:147: error: cannot find symbol
        when(cc.exitDynamicScope()).thenReturn(mock(Scope.class));
                                               ^
  symbol:   method mock(Class<Scope>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:148: error: cannot find symbol
        when(cc.getEvaluatedItems()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:149: error: cannot find symbol
        when(cc.getEvaluatedProperties()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:174: error: cannot find symbol
        JsonValidator mockValidator = mock(JsonValidator.class);
                                      ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:175: error: incompatible types: String cannot be converted to ErrorMessageType
        ValidationMessage err1 = ValidationMessage.of("ERR001", "$", "first error");
                                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:176: error: incompatible types: String cannot be converted to ErrorMessageType
        ValidationMessage err2 = ValidationMessage.of("ERR002", "$", "second error");
                                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:178: error: cannot find symbol
        when(mockValidator.validate(any(), any(), any(), anyString())).thenReturn(validatorErrors);
                                    ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:178: error: cannot find symbol
        when(mockValidator.validate(any(), any(), any(), anyString())).thenReturn(validatorErrors);
                                           ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:178: error: cannot find symbol
        when(mockValidator.validate(any(), any(), any(), anyString())).thenReturn(validatorErrors);
                                                  ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:178: error: cannot find symbol
        when(mockValidator.validate(any(), any(), any(), anyString())).thenReturn(validatorErrors);
                                                         ^
  symbol:   method anyString()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:201: error: cannot find symbol
        verify(mockValidator, times(1)).validate(eq(executionContext), eq(data), eq(data), eq("$"));
                              ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:201: error: cannot find symbol
        verify(mockValidator, times(1)).validate(eq(executionContext), eq(data), eq(data), eq("$"));
                                                 ^
  symbol:   method eq(ExecutionContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:201: error: cannot find symbol
        verify(mockValidator, times(1)).validate(eq(executionContext), eq(data), eq(data), eq("$"));
                                                                       ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:201: error: cannot find symbol
        verify(mockValidator, times(1)).validate(eq(executionContext), eq(data), eq(data), eq("$"));
                                                                                 ^
  symbol:   method eq(JsonNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:201: error: cannot find symbol
        verify(mockValidator, times(1)).validate(eq(executionContext), eq(data), eq(data), eq("$"));
                                                                                           ^
  symbol:   method eq(String)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:213: error: cannot find symbol
        JsonValidator v1 = mock(JsonValidator.class);
                           ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:214: error: cannot find symbol
        JsonValidator v2 = mock(JsonValidator.class);
                           ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:215: error: cannot find symbol
        when(v1.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                         ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:215: error: cannot find symbol
        when(v1.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                                ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:215: error: cannot find symbol
        when(v1.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                                       ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:215: error: cannot find symbol
        when(v1.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                                              ^
  symbol:   method anyString()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:216: error: cannot find symbol
        when(v2.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                         ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:216: error: cannot find symbol
        when(v2.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                                ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:216: error: cannot find symbol
        when(v2.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                                       ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:216: error: cannot find symbol
        when(v2.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());
                                              ^
  symbol:   method anyString()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:221: error: cannot find symbol
        Scope parentScope = mock(Scope.class);
                            ^
  symbol:   method mock(Class<Scope>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:222: error: cannot find symbol
        when(parentScope.getEvaluatedItems()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:223: error: cannot find symbol
        when(parentScope.getEvaluatedProperties()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:225: error: cannot find symbol
        CollectorContext collectorContext = mock(CollectorContext.class);
                                            ^
  symbol:   method mock(Class<CollectorContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:226: error: cannot find symbol
        when(collectorContext.enterDynamicScope(any())).thenReturn(parentScope, childScope1, childScope2);
                                                ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:227: error: cannot find symbol
        when(collectorContext.exitDynamicScope()).thenReturn(childScope1, childScope2);
        ^
  symbol:   method when(Scope)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:228: error: cannot find symbol
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:229: error: cannot find symbol
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());
        ^
  symbol:   method when(Collection<String>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:245: error: cannot find symbol
        verify(parentScope, times(2)).mergeWith(any());
                            ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:245: error: cannot find symbol
        verify(parentScope, times(2)).mergeWith(any());
                                                ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:253: error: cannot find symbol
        PrefixItemsValidator prefixValidator = mock(PrefixItemsValidator.class);
                                               ^
  symbol:   method mock(Class<PrefixItemsValidator>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:254: error: incompatible types: String cannot be converted to ErrorMessageType
        ValidationMessage err = ValidationMessage.of("ERRX", "$", "prefix error");
                                                             ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        when(prefixValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                      ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        when(prefixValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                             ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        when(prefixValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                                    ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:255: error: cannot find symbol
        when(prefixValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                                           ^
  symbol:   method anyString()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:260: error: cannot find symbol
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);
        ^
  symbol:   method when(Scope)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:276: error: cannot find symbol
        verify(collectorContext.getEvaluatedItems(), times(1)).addAll(childScope.getEvaluatedItems());
                                                     ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:284: error: cannot find symbol
        PropertiesValidator propValidator = mock(PropertiesValidator.class);
                                            ^
  symbol:   method mock(Class<PropertiesValidator>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:285: error: incompatible types: String cannot be converted to ErrorMessageType
        ValidationMessage err = ValidationMessage.of("ERRP", "$", "property error");
                                                             ^
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:286: error: cannot find symbol
        when(propValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                    ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:286: error: cannot find symbol
        when(propValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                           ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:286: error: cannot find symbol
        when(propValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                                  ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:286: error: cannot find symbol
        when(propValidator.validate(any(), any(), any(), anyString())).thenReturn(new LinkedHashSet<>(Collections.singletonList(err)));
                                                         ^
  symbol:   method anyString()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:289: error: cannot find symbol
        when(collectorContext.exitDynamicScope()).thenReturn(childScope);
        ^
  symbol:   method when(Scope)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:302: error: cannot find symbol
        verify(collectorContext.getEvaluatedProperties(), times(1)).addAll(childScope.getEvaluatedProperties());
                                                          ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:311: error: cannot find symbol
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:313: error: cannot find symbol
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
                                       ^
  symbol:   method mock(Class<DiscriminatorContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:314: error: cannot find symbol
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:321: error: cannot find symbol
        JsonSchema spySchema = Mockito.spy(jsonSchema);
                               ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:323: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:323: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                            ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:323: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:323: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                                          ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:323: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:327: error: cannot find symbol
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), isNull(), eq(spySchema));
                          ^
  symbol:   method times(int)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:327: error: cannot find symbol
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), isNull(), eq(spySchema));
                                                            ^
  symbol:   method eq(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:327: error: cannot find symbol
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), isNull(), eq(spySchema));
                                                                         ^
  symbol:   method eq(ObjectNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:327: error: cannot find symbol
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), isNull(), eq(spySchema));
                                                                                       ^
  symbol:   method isNull()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:327: error: cannot find symbol
        verify(spySchema, times(1)).checkDiscriminatorMatch(eq(discCtx), eq(discNode), isNull(), eq(spySchema));
                                                                                                 ^
  symbol:   method eq(JsonSchema)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:335: error: cannot find symbol
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:336: error: cannot find symbol
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(null);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:349: error: cannot find symbol
        when(validationContext.getConfig().isOpenAPI3StyleDiscriminators()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:350: error: cannot find symbol
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
                                       ^
  symbol:   method mock(Class<DiscriminatorContext>)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:351: error: cannot find symbol
        when(validationContext.getCurrentDiscriminatorContext()).thenReturn(discCtx);
        ^
  symbol:   method when(DiscriminatorContext)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:353: error: cannot find symbol
        when(discCtx.getDiscriminatorForPath(SCHEMA_PATH)).thenReturn(null);
        ^
  symbol:   method when(ObjectNode)
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:357: error: cannot find symbol
        JsonSchema spySchema = Mockito.spy(jsonSchema);
                               ^
  symbol:   variable Mockito
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:358: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
        ^
  symbol:   method doNothing()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:358: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                            ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:358: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                                   ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:358: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                                          ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
src/test/java/com/networknt/schema/JsonSchema_validate_Test.java:358: error: cannot find symbol
        doNothing().when(spySchema).checkDiscriminatorMatch(any(), any(), any(), any());
                                                                                 ^
  symbol:   method any()
  location: class JsonSchema_validate_Test
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
100 errors
only showing the first 100 errors, of 162 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.CollectorContext`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\trelated functions:\n\t\tmethod `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, String)` returns `void`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`, api document: \n     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine\n     * the <code>propertyName</code> or mappings.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param schema                      the value of the <code>discriminator/propertyName</code> field\n     * @param at                          the logging prefix\n     ;\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.ValidationMessageHandler.schemaPath;\n\t\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`, api document: \n     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.\n     ;\n\t\tmethod `private URI combineCurrentUriWithIds(URI, JsonNode)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;\n\t\tmethod `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`, api document: \n     * This method both validates and collects the data in a CollectorContext.\n     * Unlike others this methods cleans and removes everything from collector\n     * context before returning.\n     * @param executionContext ExecutionContext\n     * @param jsonNode JsonNode\n     * @param rootNode JsonNode\n     * @param at       String path\n     *\n     * @return ValidationResult\n     ;\nclass `com.networknt.schema.TypeValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


