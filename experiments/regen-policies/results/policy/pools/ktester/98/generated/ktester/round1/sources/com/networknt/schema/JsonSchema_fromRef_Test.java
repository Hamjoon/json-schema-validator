package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import com.networknt.schema.PathType;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

// @ExtendWith(MockitoExtension.class)
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

    /* --------------------------------------------------------------------
     * Helper concrete ValidationContext used in place of Mockito mocks.
     * -------------------------------------------------------------------- */
    private static class SimpleValidationContext extends ValidationContext {

        SimpleValidationContext(JsonMetaSchema metaSchema, JsonSchemaFactory factory, SchemaValidatorsConfig config) {
            super(metaSchema, factory, config);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft – the concrete version does not matter for the test.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // a minimal valid schema – we only need an instance, not validation logic
        String schemaJson = "{\"type\":\"object\"}";
        sourceSchema = factory.getSchema(schemaJson);
        currentSchema = factory.getSchema(schemaJson);
        parentSchema = factory.getSchema(schemaJson);
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);
        // --------------------------------------------------------------------
        // Prepare a simple ValidationContext for the parent schema – this is used
        // in many test cases.  The context is injected via reflection because the
        // field is private.
        // --------------------------------------------------------------------
        SimpleValidationContext parentVc = new SimpleValidationContext(null, null, new SchemaValidatorsConfig());
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
        JsonSchema derived = sourceSchema.fromRef(parentSchema, evaluationPath);
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
//     @Test
//     void basic_successfulCreation_copiesValidationContextAndPath() throws Exception {
//         JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V202012);
//         JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         SimpleValidationContext currentVc = new SimpleValidationContext(metaSchema, jsonSchemaFactory, new SchemaValidatorsConfig());
//         setField(currentSchema, "validationContext", currentVc);
//         JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
//         assertSame(evaluationPath, getField(derived, "evaluationPath"));
//         assertSame(parentSchema, getField(derived, "evaluationParentSchema"));
//         ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
//         assertNotNull(derivedVc);
//         assertSame(metaSchema, derivedVc.getMetaSchema(), "Meta‑schema must come from the *current* schema");
//         assertSame(jsonSchemaFactory, derivedVc.getJsonSchemaFactory(), "Factory must come from the *current* schema");
//         ValidationContext parentVc = (ValidationContext) getField(parentSchema, "validationContext");
//         assertSame(parentVc.getConfig(), derivedVc.getConfig(), "Config must be copied from the parent schema");
//         assertSame(parentVc.getSchemaReferences(), derivedVc.getSchemaReferences(), "Schema references must be copied from the parent schema");
//         assertSame(parentVc.getSchemaResources(), derivedVc.getSchemaResources(), "Schema resources must be copied from the parent schema");
//         assertSame(parentVc.getDynamicAnchors(), derivedVc.getDynamicAnchors(), "Dynamic anchors must be copied from the parent schema");
//     }

    /* --------------------------------------------------------------------
     * 2. recursiveAnchor flag is taken from the *current* schema, not the parent
     * -------------------------------------------------------------------- */
    @Test
    void recursiveAnchor_isTakenFromCurrentSchema() throws Exception {
        setField(currentSchema, "recursiveAnchor", true);
        JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
        assertTrue((Boolean) getField(derived, "recursiveAnchor"), "recursiveAnchor must reflect the value of the current schema");
    }

    /* --------------------------------------------------------------------
     * 3. suppressSubSchemaRetrieval flag is taken from the current schema
     * -------------------------------------------------------------------- */
    @Test
    void suppressSubSchemaRetrieval_isCopiedFromCurrentSchema() throws Exception {
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
        setField(currentSchema, "validatorsLoaded", true);
        setField(currentSchema, "validators", Collections.singletonList(null));
        setField(currentSchema, "typeValidator", null);
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
        assertNull(getField(derived, "evaluationPath"), "evaluationPath should be null when passed null");
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
//     @Test
//     void parentSchemaWithCustomConfig_isCopied() throws Exception {
//         SchemaValidatorsConfig customConfig = new SchemaValidatorsConfig();
//         SimpleValidationContext parentVc = new SimpleValidationContext(null, null, customConfig);
//         setField(parentSchema, "validationContext", parentVc);
//         JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V202012);
//         JsonSchemaFactory factoryObj = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         SimpleValidationContext currentVc = new SimpleValidationContext(meta, factoryObj, new SchemaValidatorsConfig());
//         setField(currentSchema, "validationContext", currentVc);
//         JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
//         ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
//         assertSame(customConfig, derivedVc.getConfig(), "The custom config from the parent must be present in the new ValidationContext");
//     }

    /* --------------------------------------------------------------------
     * 9. Current schema with null ValidationContext causes NPE
     * -------------------------------------------------------------------- */
    @Test
    void currentSchemaWithNullValidationContext_throwsNPE() throws Exception {
        setField(currentSchema, "validationContext", null);
        assertThrows(NullPointerException.class, () -> currentSchema.fromRef(parentSchema, evaluationPath), "If the current schema's validationContext is null, fromRef must throw NPE");
    }

    /* --------------------------------------------------------------------
     * 10. Parent schema with null ValidationContext field leads to NPE
     * -------------------------------------------------------------------- */
    @Test
    void parentSchemaWithNullValidationContextField_throwsNPE() throws Exception {
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
        JsonNodePath emptyPath = new JsonNodePath(PathType.JSON_POINTER);
        JsonSchema derived = currentSchema.fromRef(parentSchema, emptyPath);
        assertSame(emptyPath, getField(derived, "evaluationPath"), "Empty JsonNodePath should be stored unchanged");
    }

    /* --------------------------------------------------------------------
     * 13. Parent schema with null config – the new ValidationContext gets null config
     * -------------------------------------------------------------------- */
//     @Test
//     void parentSchemaWithNullConfig_allowsNullConfigInNewContext() throws Exception {
//         SimpleValidationContext parentVc = new SimpleValidationContext(null, null, null);
//         setField(parentSchema, "validationContext", parentVc);
//         JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V202012);
//         JsonSchemaFactory factoryObj = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         SimpleValidationContext currentVc = new SimpleValidationContext(meta, factoryObj, new SchemaValidatorsConfig());
//         setField(currentSchema, "validationContext", currentVc);
//         JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
//         ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
//         assertNull(derivedVc.getConfig(), "When parent config is null, the new ValidationContext should contain null");
//     }

    /* --------------------------------------------------------------------
     * 14. Parent schema with non‑empty dynamicAnchors – they are copied verbatim
     * -------------------------------------------------------------------- */
//     @Test
//     void parentSchemaWithDynamicAnchors_areCopied() throws Exception {
//         ConcurrentMap<String, JsonSchema> anchors = new ConcurrentHashMap<>();
//         anchors.put("anchor1", null);
//         SimpleValidationContext parentVc = new SimpleValidationContext(null, null, new SchemaValidatorsConfig());
//         // inject the dynamicAnchors map via reflection
//         setField(parentVc, "dynamicAnchors", anchors);
//         setField(parentSchema, "validationContext", parentVc);
//         JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V202012);
//         JsonSchemaFactory factoryObj = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         SimpleValidationContext currentVc = new SimpleValidationContext(meta, factoryObj, new SchemaValidatorsConfig());
//         setField(currentSchema, "validationContext", currentVc);
//         JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
//         ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
//         assertSame(anchors, derivedVc.getDynamicAnchors(), "Dynamic anchors map must be the same instance as in the parent");
//     }

    /* --------------------------------------------------------------------
     * 15. Parent schema with custom meta‑schema and factory – they are *not* copied,
     *     only the current schema's values are used.
     * -------------------------------------------------------------------- */
//     @Test
//     void metaSchemaAndFactory_areTakenFromCurrentSchemaOnly() throws Exception {
//         // parent context (should be ignored for meta‑schema/factory)
//         SimpleValidationContext parentVc = new SimpleValidationContext(null, null, new SchemaValidatorsConfig());
//         setField(parentSchema, "validationContext", parentVc);
//         // current context with distinct meta‑schema / factory
//         JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V202012);
//         JsonSchemaFactory factoryObj = JsonSchemaFactory.getInstance(VersionFlag.V202012);
//         SimpleValidationContext currentVc = new SimpleValidationContext(meta, factoryObj, new SchemaValidatorsConfig());
//         setField(currentSchema, "validationContext", currentVc);
//         JsonSchema derived = currentSchema.fromRef(parentSchema, evaluationPath);
//         ValidationContext derivedVc = (ValidationContext) getField(derived, "validationContext");
//         assertSame(meta, derivedVc.getMetaSchema(), "Meta‑schema must come from the current schema");
//         assertSame(factoryObj, derivedVc.getJsonSchemaFactory(), "JsonSchemaFactory must come from the current schema");
//     }
}