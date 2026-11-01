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
