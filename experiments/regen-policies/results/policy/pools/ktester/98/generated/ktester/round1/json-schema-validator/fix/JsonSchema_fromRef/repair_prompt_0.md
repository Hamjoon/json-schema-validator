
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.PathType;
import com.fasterxml.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /**
     * Factory used to create a baseline {@link JsonSchema}.
     */
    private static JsonSchemaFactory factory;

    /**
     * A simple schema that will be the source of the {@code fromRef} call.
     */
    private JsonSchema sourceSchema;

    /**
     * The parent schema that will be passed to {@code fromRef}.
     */
    private JsonSchema parentSchema;

    /**
     * Evaluation path that will be passed to {@code fromRef}.
     */
    private JsonNodePath evaluationPath;

    /**
     * The schema on which {@code fromRef} is invoked (the “current” schema).
     */
    private JsonSchema currentSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft – the concrete version does not matter for the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a minimal valid schema – we only need an instance, not validation logic
        String schemaJson = "{\"type\":\"object\"}";
        currentSchema = factory.getSchema(schemaJson);
        parentSchema = factory.getSchema(schemaJson);
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
        // --------------------------------------------------------------------
        // Prepare mock ValidationContext for the parent schema – this is used in
        // many test cases.  The mock is injected via reflection because the
        // field is private.
        // --------------------------------------------------------------------
        ValidationContext parentVc = mock(ValidationContext.class);
        when(parentVc.getConfig()).thenReturn(new SchemaValidatorsConfig());
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        setField(parentSchema, "validationContext", parentVc);
    }

    /* --------------------------------------------------------------------
     * Helper methods for reflective field access
     * -------------------------------------------------------------------- */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Walks the class hierarchy to find a declared field.
     */
    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> cur = clazz;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Verifies that {@code fromRef} returns a new {@link JsonSchema} whose
     * {@code evaluationPath} and {@code evaluationParentSchema} fields are the
     * exact objects supplied to the method, and that the {@code validationContext}
     * is shared with the original schema.
     */
    @Test
    void fromRef_copiesStateCorrectly() throws Exception {
        // Act – create a new schema via the method under test
        JsonSchema derived = sourceSchema.fromRef(parentSchema, evaluationPath);
        // --------------------------------------------------------------------
        // Use reflection to read the private fields of JsonSchema.
        // --------------------------------------------------------------------
        Field evalPathField = JsonSchema.class.getDeclaredField("evaluationPath");
        evalPathField.setAccessible(true);
        Object actualEvalPath = evalPathField.get(derived);
        assertSame(evaluationPath, actualEvalPath, "evaluationPath should be the same instance that was passed to fromRef");
        Field evalParentField = JsonSchema.class.getDeclaredField("evaluationParentSchema");
        evalParentField.setAccessible(true);
        Object actualEvalParent = evalParentField.get(derived);
        assertSame(parentSchema, actualEvalParent, "evaluationParentSchema should be the same instance that was passed to fromRef");
        Field vcField = JsonSchema.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        Object actualVc = vcField.get(derived);
        Field originalVcField = JsonSchema.class.getDeclaredField("validationContext");
        originalVcField.setAccessible(true);
        Object originalVc = originalVcField.get(sourceSchema);
        assertSame(originalVc, actualVc, "validationContext should be carried over from the original JsonSchema");
    }

    /* --------------------------------------------------------------------
     * 1. Basic successful creation – all pieces of ValidationContext are copied
     * -------------------------------------------------------------------- */
    @Test
    void basic_successfulCreation_copiesValidationContextAndPath() throws Exception {
        // ----- arrange ----------------------------------------------------
        // mock meta‑schema and factory for the *current* schema
        ValidationContext currentVc = mock(ValidationContext.class);
        Object metaSchema = new Object();
        Object jsonSchemaFactory = new Object();
        when(currentVc.getMetaSchema()).thenReturn(metaSchema);
        when(currentVc.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        setField(currentSchema, "validationContext", currentVc);
        // ----- act --------------------------------------------------------
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        // ----- assert -----------------------------------------------------
        // evaluationPath and evaluationParentSchema must be the exact objects supplied
        assertSame(evaluationPath, getField(derived, "evaluationPath"));
        assertSame(parentSchema, getField(derived, "evaluationParentSchema"));
        // validationContext must be a new instance but with the expected components
        ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
        assertNotNull(derivedVc);
        assertSame(metaSchema, derivedVc.getMetaSchema(), "Meta‑schema must come from the *current* schema");
        assertSame(jsonSchemaFactory, derivedVc.getJsonSchemaFactory(), "Factory must come from the *current* schema");
        ValidationContext parentVc = (ValidationContext) getField(parentSchema, "validationContext");
        assertSame(parentVc.getConfig(), derivedVc.getConfig(), "Config must be copied from the parent schema");
        assertSame(parentVc.getSchemaReferences(), derivedVc.getSchemaReferences(), "Schema references must be copied from the parent schema");
        assertSame(parentVc.getSchemaResources(), derivedVc.getSchemaResources(), "Schema resources must be copied from the parent schema");
        assertSame(parentVc.getDynamicAnchors(), derivedVc.getDynamicAnchors(), "Dynamic anchors must be copied from the parent schema");
    }

    /* --------------------------------------------------------------------
     * 2. recursiveAnchor flag is taken from the *current* schema, not the parent
     * -------------------------------------------------------------------- */
    @Test
    void recursiveAnchor_isTakenFromCurrentSchema() throws Exception {
        // make current schema recursive
        setField(currentSchema, "recursiveAnchor", true);
        // parent schema may have any value – we leave it false (default)
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        assertTrue((Boolean) getField(derived, "recursiveAnchor"), "recursiveAnchor must reflect the value of the current schema");
    }

    /* --------------------------------------------------------------------
     * 3. suppressSubSchemaRetrieval flag is taken from the current schema
     * -------------------------------------------------------------------- */
    @Test
    void suppressSubSchemaRetrieval_isCopiedFromCurrentSchema() throws Exception {
        // set the flag on the current schema (protected field in BaseJsonValidator)
        setField(currentSchema, "suppressSubSchemaRetrieval", true);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        assertTrue((Boolean) getField(derived, "suppressSubSchemaRetrieval"), "suppressSubSchemaRetrieval must be copied from the current schema");
    }

    /* --------------------------------------------------------------------
     * 4. id field is propagated from the current schema
     * -------------------------------------------------------------------- */
    @Test
    void id_isPropagatedFromCurrentSchema() throws Exception {
        String expectedId = "my-schema-id";
        setField(currentSchema, "id", expectedId);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        assertEquals(expectedId, getField(derived, "id"));
    }

    /* --------------------------------------------------------------------
     * 5. Validators and typeValidator are reset (null / false)
     * -------------------------------------------------------------------- */
    @Test
    void validatorsAndTypeValidator_areReset() throws Exception {
        // simulate a schema that already has validators loaded
        setField(currentSchema, "validatorsLoaded", true);
        setField(currentSchema, "validators", Collections.singletonList(mock(JsonValidator.class)));
        setField(currentSchema, "typeValidator", mock(TypeValidator.class));
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        assertFalse((Boolean) getField(derived, "validatorsLoaded"), "validatorsLoaded must be false");
        assertNull(getField(derived, "validators"), "validators must be null");
        assertNull(getField(derived, "typeValidator"), "typeValidator must be null");
    }

    /* --------------------------------------------------------------------
     * 6. Null evaluationPath is accepted and stored as null
     * -------------------------------------------------------------------- */
    @Test
    void nullEvaluationPath_isStoredAsNull() {
        JsonSchema derived = currentSchema.fromRef(parentSchema, null);
        assertNull(derived.evaluationPath, "evaluationPath should be null when passed null");
    }

    /* --------------------------------------------------------------------
     * 7. Null parent schema leads to NPE (dereferencing its validationContext)
     * -------------------------------------------------------------------- */
    @Test
    void nullParentSchema_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> currentSchema.fromRef(null, evaluationPath), "Calling fromRef with a null parent schema must throw NPE");
    }

    /* --------------------------------------------------------------------
     * 8. Parent schema with custom config – config object is copied
     * -------------------------------------------------------------------- */
    @Test
    void parentSchemaWithCustomConfig_isCopied() throws Exception {
        SchemaValidatorsConfig customConfig = new SchemaValidatorsConfig();
        ValidationContext parentVc = mock(ValidationContext.class);
        when(parentVc.getConfig()).thenReturn(customConfig);
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        setField(parentSchema, "validationContext", parentVc);
        // current schema needs a non‑null meta‑schema/factory, otherwise constructor fails
        ValidationContext currentVc = mock(ValidationContext.class);
        when(currentVc.getMetaSchema()).thenReturn(new Object());
        when(currentVc.getJsonSchemaFactory()).thenReturn(new Object());
        setField(currentSchema, "validationContext", currentVc);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
        assertSame(customConfig, derivedVc.getConfig(), "The custom config from the parent must be present in the new ValidationContext");
    }

    /* --------------------------------------------------------------------
     * 9. Current schema with null ValidationContext causes NPE
     * -------------------------------------------------------------------- */
    @Test
    void currentSchemaWithNullValidationContext_throwsNPE() throws Exception {
        // force the field to null (bypassing getter)
        setField(currentSchema, "validationContext", null);
        assertThrows(NullPointerException.class, () -> currentSchema.fromRef(parentSchema, evaluationPath), "If the current schema's validationContext is null, fromRef must throw NPE");
    }

    /* --------------------------------------------------------------------
     * 10. Parent schema with null ValidationContext field leads to NPE
     * -------------------------------------------------------------------- */
    @Test
    void parentSchemaWithNullValidationContextField_throwsNPE() throws Exception {
        // set the field directly to null (the getter would also return null)
        setField(parentSchema, "validationContext", null);
        assertThrows(NullPointerException.class, () -> currentSchema.fromRef(parentSchema, evaluationPath), "If the parent schema's validationContext field is null, fromRef must throw NPE");
    }

    /* --------------------------------------------------------------------
     * 11. EvaluationParentSchema reference is stored exactly as supplied
     * -------------------------------------------------------------------- */
    @Test
    void evaluationParentSchema_isStoredExactly() throws Exception {
        JsonSchema differentParent = factory.getSchema("{\"type\":\"string\"}");
        JsonSchema derived = currentSchema.fromRef(differentParent, evaluationPath);
        assertSame(differentParent, getField(derived, "evaluationParentSchema"), "evaluationParentSchema must be the exact object supplied to fromRef");
    }

    /* --------------------------------------------------------------------
     * 12. Empty JsonNodePath is accepted (non‑null)
     * -------------------------------------------------------------------- */
    @Test
    void emptyJsonNodePath_isAccepted() throws Exception {
        JsonNodePath emptyPath = JsonNodePath.empty();
        JsonSchema derived = currentSchema.fromRef(parentSchema, emptyPath);
        assertSame(emptyPath, getField(derived, "evaluationPath"), "Empty JsonNodePath should be stored unchanged");
    }

    /* --------------------------------------------------------------------
     * 13. Parent schema with null config – the new ValidationContext gets null config
     * -------------------------------------------------------------------- */
    @Test
    void parentSchemaWithNullConfig_allowsNullConfigInNewContext() throws Exception {
        ValidationContext parentVc = mock(ValidationContext.class);
        when(parentVc.getConfig()).thenReturn(null);
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        setField(parentSchema, "validationContext", parentVc);
        // current schema must have non‑null meta‑schema/factory
        ValidationContext currentVc = mock(ValidationContext.class);
        when(currentVc.getMetaSchema()).thenReturn(new Object());
        when(currentVc.getJsonSchemaFactory()).thenReturn(new Object());
        setField(currentSchema, "validationContext", currentVc);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
        assertNull(derivedVc.getConfig(), "When parent config is null, the new ValidationContext should contain null");
    }

    /* --------------------------------------------------------------------
     * 14. Parent schema with non‑empty dynamicAnchors – they are copied verbatim
     * -------------------------------------------------------------------- */
    @Test
    void parentSchemaWithDynamicAnchors_areCopied() throws Exception {
        Map<String, JsonNode> anchors = Collections.singletonMap("anchor1", mock(JsonNode.class));
        ValidationContext parentVc = mock(ValidationContext.class);
        when(parentVc.getConfig()).thenReturn(new SchemaValidatorsConfig());
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(parentVc.getDynamicAnchors()).thenReturn(anchors);
        setField(parentSchema, "validationContext", parentVc);
        // current schema needs meta‑schema/factory
        ValidationContext currentVc = mock(ValidationContext.class);
        when(currentVc.getMetaSchema()).thenReturn(new Object());
        when(currentVc.getJsonSchemaFactory()).thenReturn(new Object());
        setField(currentSchema, "validationContext", currentVc);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
        assertSame(anchors, derivedVc.getDynamicAnchors(), "Dynamic anchors map must be the same instance as in the parent");
    }

    /* --------------------------------------------------------------------
     * 15. Parent schema with custom meta‑schema and factory – they are *not* copied,
     *     only the current schema's values are used.
     * -------------------------------------------------------------------- */
    @Test
    void metaSchemaAndFactory_areTakenFromCurrentSchemaOnly() throws Exception {
        // custom meta‑schema / factory on parent (should be ignored)
        ValidationContext parentVc = mock(ValidationContext.class);
        when(parentVc.getConfig()).thenReturn(new SchemaValidatorsConfig());
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        setField(parentSchema, "validationContext", parentVc);
        // current schema with distinct meta‑schema / factory
        Object meta = new Object();
        Object factoryObj = new Object();
        ValidationContext currentVc = mock(ValidationContext.class);
        when(currentVc.getMetaSchema()).thenReturn(meta);
        when(currentVc.getJsonSchemaFactory()).thenReturn(factoryObj);
        setField(currentSchema, "validationContext", currentVc);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
        assertSame(meta, derivedVc.getMetaSchema(), "Meta‑schema must come from the current schema");
        assertSame(factoryObj, derivedVc.getJsonSchemaFactory(), "JsonSchemaFactory must come from the current schema");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:7: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:14: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:60: error: cannot find symbol
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);
                                                      ^
  symbol:   variable PathType
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:66: error: cannot find symbol
        ValidationContext parentVc = mock(ValidationContext.class);
                                     ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:67: error: cannot find symbol
        when(parentVc.getConfig()).thenReturn(new SchemaValidatorsConfig());
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:68: error: cannot find symbol
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:69: error: cannot find symbol
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:70: error: cannot find symbol
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:141: error: cannot find symbol
        ValidationContext currentVc = mock(ValidationContext.class);
                                      ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:144: error: cannot find symbol
        when(currentVc.getMetaSchema()).thenReturn(metaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:145: error: cannot find symbol
        when(currentVc.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:206: error: cannot find symbol
        setField(currentSchema, "validators", Collections.singletonList(mock(JsonValidator.class)));
                                                                        ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:207: error: cannot find symbol
        setField(currentSchema, "typeValidator", mock(TypeValidator.class));
                                                 ^
  symbol:   method mock(Class<TypeValidator>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:237: error: cannot find symbol
        ValidationContext parentVc = mock(ValidationContext.class);
                                     ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:238: error: cannot find symbol
        when(parentVc.getConfig()).thenReturn(customConfig);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:239: error: cannot find symbol
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:240: error: cannot find symbol
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:241: error: cannot find symbol
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:244: error: cannot find symbol
        ValidationContext currentVc = mock(ValidationContext.class);
                                      ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:245: error: cannot find symbol
        when(currentVc.getMetaSchema()).thenReturn(new Object());
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:246: error: cannot find symbol
        when(currentVc.getJsonSchemaFactory()).thenReturn(new Object());
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:288: error: cannot find symbol
        JsonNodePath emptyPath = JsonNodePath.empty();
                                             ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:298: error: cannot find symbol
        ValidationContext parentVc = mock(ValidationContext.class);
                                     ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:299: error: cannot find symbol
        when(parentVc.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:300: error: cannot find symbol
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:301: error: cannot find symbol
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:302: error: cannot find symbol
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:305: error: cannot find symbol
        ValidationContext currentVc = mock(ValidationContext.class);
                                      ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:306: error: cannot find symbol
        when(currentVc.getMetaSchema()).thenReturn(new Object());
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:307: error: cannot find symbol
        when(currentVc.getJsonSchemaFactory()).thenReturn(new Object());
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:319: error: cannot find symbol
        Map<String, JsonNode> anchors = Collections.singletonMap("anchor1", mock(JsonNode.class));
                                                                            ^
  symbol:   method mock(Class<JsonNode>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:320: error: cannot find symbol
        ValidationContext parentVc = mock(ValidationContext.class);
                                     ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:321: error: cannot find symbol
        when(parentVc.getConfig()).thenReturn(new SchemaValidatorsConfig());
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:322: error: cannot find symbol
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:323: error: cannot find symbol
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:324: error: cannot find symbol
        when(parentVc.getDynamicAnchors()).thenReturn(anchors);
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:327: error: cannot find symbol
        ValidationContext currentVc = mock(ValidationContext.class);
                                      ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:328: error: cannot find symbol
        when(currentVc.getMetaSchema()).thenReturn(new Object());
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:329: error: cannot find symbol
        when(currentVc.getJsonSchemaFactory()).thenReturn(new Object());
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:343: error: cannot find symbol
        ValidationContext parentVc = mock(ValidationContext.class);
                                     ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:344: error: cannot find symbol
        when(parentVc.getConfig()).thenReturn(new SchemaValidatorsConfig());
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:345: error: cannot find symbol
        when(parentVc.getSchemaReferences()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:346: error: cannot find symbol
        when(parentVc.getSchemaResources()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:347: error: cannot find symbol
        when(parentVc.getDynamicAnchors()).thenReturn(Collections.emptyMap());
        ^
  symbol:   method when(ConcurrentMap<String,JsonSchema>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:352: error: cannot find symbol
        ValidationContext currentVc = mock(ValidationContext.class);
                                      ^
  symbol:   method mock(Class<ValidationContext>)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:353: error: cannot find symbol
        when(currentVc.getMetaSchema()).thenReturn(meta);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:354: error: cannot find symbol
        when(currentVc.getJsonSchemaFactory()).thenReturn(factoryObj);
        ^
  symbol:   method when(JsonSchemaFactory)
  location: class JsonSchema_fromRef_Test
Note: src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
50 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method fromRef(JsonSchema, JsonNodePath)': '\n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.JsonSchema.recursiveAnchor;\n\t\tjava.lang.String com.networknt.schema.JsonSchema.id;\n\trelated functions:\n\t\tmethod `private JsonNode deserialize(String, InputFormat)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`, api document: \n     * Deserialize string to JsonNode.\n     * \n     * @param input the input\n     * @param inputFormat the format\n     * @return the JsonNode.\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;\n\t\tcom.networknt.schema.Keyword com.networknt.schema.ValidationMessageHandler.keyword;\n\t\tcom.networknt.schema.ErrorMessageType com.networknt.schema.ValidationMessageHandler.errorMessageType;\n\t\tcom.networknt.schema.i18n.MessageSource com.networknt.schema.ValidationMessageHandler.messageSource;\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\tjava.lang.String com.networknt.schema.ValidationMessageHandler.errorMessageKeyword;\n\t\tjava.util.Map<java.lang.String, java.lang.String> com.networknt.schema.ValidationMessageHandler.errorMessage;\n\trelated functions:\n\t\tmethod `protected MessageSourceValidationMessage.Builder message()` returns `com.networknt.schema.MessageSourceValidationMessage.Builder`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\tboolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `private ValidationContext withMetaSchema(ValidationContext, JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`, api document: \n     * Determines the validation context to use for the schema given the parent\n     * validation context.\n     * <p>\n     * This is typically the same validation context unless the schema has a\n     * different $schema from the parent.\n     * <p>\n     * If the schema does not define a $schema, the parent should be used.\n     * \n     * @param validationContext the parent validation context\n     * @param schemaNode        the schema node\n     * @return the validation context to use\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchemaRef getRefSchema(JsonSchema, ValidationContext, String, JsonNodePath)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `private static void copySchemaResources(ValidationContext, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


