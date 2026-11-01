package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import com.networknt.schema.i18n.MessageSource;

@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /**
     * Shared objects used by all tests.
     */
    private static ValidationContext sharedValidationContext;

    private static JsonNode emptySchemaNode;

    /**
     * Minimal concrete subclass that forwards to the protected constructor
     * and provides public accessors for the evaluation‑path information.
     */
    private static final class TestJsonSchema extends JsonSchema {

        TestJsonSchema(List<JsonValidator> validators, boolean validatorsLoaded, boolean recursiveAnchor, TypeValidator typeValidator, String id, boolean suppressSubSchemaRetrieval, JsonNode schemaNode, ValidationContext validationContext, ErrorMessageType errorMessageType, String errorMessageKeyword, MessageSource messageSource, Keyword keyword, JsonSchema parentSchema, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonSchema evaluationParentSchema, Map<String, String> errorMessage) {
            super(validators, validatorsLoaded, recursiveAnchor, typeValidator, id, suppressSubSchemaRetrieval, schemaNode, validationContext, errorMessageType, errorMessageKeyword, messageSource, keyword, parentSchema, schemaLocation, evaluationPath, evaluationParentSchema, errorMessage);
        }

        /**
         * expose the protected getter from {@link BaseJsonValidator}
         */
        JsonNodePath getEvaluationPathPublic() {
            return super.getEvaluationPath();
        }

        /**
         * expose the protected getter from {@link BaseJsonValidator}
         */
        JsonSchema getEvaluationParentSchemaPublic() {
            return super.getEvaluationParentSchema();
        }

        /**
         * expose the id (package‑private field) via the public accessor
         */
        @Override
        public String getId() {
            return super.getId();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use real meta‑schema and factory instances – they are cheap and avoid
        // the need for a mocking framework.
        JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        sharedValidationContext = new ValidationContext(metaSchema, factory, config);
        // an empty object node
        emptySchemaNode = JsonNodeFactory.instance.objectNode();
    }

    /* --------------------------------------------------------------------- *
     * Utility to inject a private field via reflection.                     *
     * --------------------------------------------------------------------- */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- *
     * Helper to read private fields via reflection (used for flags).       *
     * --------------------------------------------------------------------- */
    private static Object getFieldValue(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper that creates a {@link TestJsonSchema} with sensible defaults.
     */
    private static TestJsonSchema newTestSchema(String id, boolean recursiveAnchor, boolean suppressSubSchemaRetrieval, ValidationContext vc, JsonNode schemaNode, JsonNodePath evalPath, JsonSchema evalParent) {
        return new TestJsonSchema(// validators
        Collections.emptyList(), // validatorsLoaded
        false, // recursiveAnchor
        recursiveAnchor, // typeValidator
        null, // id
        id, // suppressSubSchemaRetrieval
        suppressSubSchemaRetrieval, // schemaNode
        schemaNode, // validationContext
        vc, // errorMessageType
        null, // errorMessageKeyword
        null, // messageSource
        null, // keyword
        null, // parentSchema
        null, // schemaLocation
        null, // evaluationPath
        evalPath, // evaluationParentSchema
        evalParent, // errorMessage map
        Collections.emptyMap());
    }

    @Test
    void fromRef_copiesRelevantState() {
        JsonSchema parentSchema = newTestSchema("parent-id", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schemaUnderTest = newTestSchema("original-id", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonNodePath newEvalPath = null;
        JsonSchema derived = schemaUnderTest.fromRef(parentSchema, newEvalPath);
        assertNotNull(derived, "fromRef must return a non‑null JsonSchema");
        assertNotSame(schemaUnderTest, derived, "Derived schema must be a new instance");
        assertEquals("original-id", ((TestJsonSchema) derived).getId(), "Derived schema should retain the original id");
        assertNull(((TestJsonSchema) derived).getEvaluationPathPublic(), "EvaluationPath should be the one supplied to fromRef (null here)");
        assertSame(parentSchema, ((TestJsonSchema) derived).getEvaluationParentSchemaPublic(), "EvaluationParentSchema should be the one supplied to fromRef");
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with recursive anchor enabled.                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_inheritsRecursiveAnchorFlag() {
        JsonSchema parentSchema = newTestSchema("parent-id", true, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child-id", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parentSchema, null);
        boolean recursiveAnchor = (boolean) getFieldValue(derived, "recursiveAnchor");
        assertTrue(recursiveAnchor, "Recursive anchor flag must be inherited from the parent");
    }

    /* --------------------------------------------------------------------- *
     * Null evaluation path – should not throw NPE.                          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullEvaluationPath_isHandledGracefully() {
        JsonSchema parent = newTestSchema("parent", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        assertNotNull(derived);
        assertNull(((TestJsonSchema) derived).getEvaluationPathPublic(), "Evaluation path may be null when a null argument is supplied");
    }

    /* --------------------------------------------------------------------- *
     * Null parent schema – should fail fast.                               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullParentSchema_throwsNPE() {
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(null, null), "Passing a null parent schema must result in NPE");
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with custom ValidationContext configuration.          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_propagatesCustomValidationContext() {
        // custom config
        SchemaValidatorsConfig customConfig = SchemaValidatorsConfig.builder().typeLoose(true).build();
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext parentVc = new ValidationContext(meta, factory, customConfig);
        // inject non‑empty maps via reflection
        Map<String, JsonSchema> references = new HashMap<>();
        references.put("ref1", null);
        setField(parentVc, "schemaReferences", references);
        Map<String, JsonNode> resources = new HashMap<>();
        resources.put("http://example.com", JsonNodeFactory.instance.objectNode());
        setField(parentVc, "schemaResources", resources);
        Map<String, JsonSchema> anchors = new HashMap<>();
        anchors.put("myAnchor", null);
        setField(parentVc, "dynamicAnchors", anchors);
        JsonSchema parent = newTestSchema("parent", false, false, parentVc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        ValidationContext derivedVc = derived.getValidationContext();
        assertSame(customConfig, derivedVc.getConfig(), "Custom config must be propagated");
        assertSame(references, derivedVc.getSchemaReferences(), "Schema references map must be the same instance");
        assertSame(resources, derivedVc.getSchemaResources(), "Schema resources map must be the same instance");
        assertSame(anchors, derivedVc.getDynamicAnchors(), "Dynamic anchors map must be the same instance");
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with suppressed sub‑schema retrieval flag set.         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_inheritsSuppressSubSchemaRetrievalFlag() throws Exception {
        JsonSchema parent = newTestSchema("parent", false, true, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        boolean suppress = (boolean) getFieldValue(derived, "suppressSubSchemaRetrieval");
        assertTrue(suppress, "suppressSubSchemaRetrieval flag must be inherited from the parent");
    }

    /* --------------------------------------------------------------------- *
     * Id propagation – parent schema id retained.                          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_retainsParentId() {
        JsonSchema parent = newTestSchema("parent-schema-id", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child-id", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        assertEquals("child-id", ((TestJsonSchema) derived).getId(), "The derived schema must keep its own id (not the parent’s).");
    }

    /* --------------------------------------------------------------------- *
     * Dynamic anchors propagation.                                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_copiesDynamicAnchors() {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        JsonSchema anchorSchema = newTestSchema("anchor", false, false, vc, emptySchemaNode, null, null);
        Map<String, JsonSchema> anchors = new HashMap<>();
        anchors.put("myAnchor", anchorSchema);
        setField(vc, "dynamicAnchors", anchors);
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        Map<String, JsonSchema> derivedAnchors = derived.getValidationContext().getDynamicAnchors();
        assertSame(anchors, derivedAnchors, "Dynamic anchors map must be the same instance");
        assertEquals(anchorSchema, derivedAnchors.get("myAnchor"));
    }

    /* --------------------------------------------------------------------- *
     * Schema resources map propagation.                                    *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_copiesSchemaResources() {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        Map<String, JsonNode> resources = new HashMap<>();
        resources.put("http://example.com/schema", JsonNodeFactory.instance.objectNode());
        setField(vc, "schemaResources", resources);
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        Map<?, ?> derivedResources = derived.getValidationContext().getSchemaResources();
        assertSame(resources, derivedResources);
        assertTrue(derivedResources.containsKey("http://example.com/schema"));
    }

    /* --------------------------------------------------------------------- *
     * Evaluation path with URL‑encoded characters.                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_preservesUrlEncodedEvaluationPath() {
        JsonSchema parent = newTestSchema("parent", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        // content not relevant for the test
        JsonNodePath encodedPath = new JsonNodePath();
        JsonSchema derived = schema.fromRef(parent, encodedPath);
        assertSame(encodedPath, ((TestJsonSchema) derived).getEvaluationPathPublic(), "Encoded path must be preserved unchanged");
    }

    /* --------------------------------------------------------------------- *
     * Root evaluation path – empty JsonNodePath.                           *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_rootEvaluationPath_isEmpty() {
        JsonSchema parent = newTestSchema("parent", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        // represents the empty path
        JsonNodePath emptyPath = new JsonNodePath();
        JsonSchema derived = schema.fromRef(parent, emptyPath);
        assertSame(emptyPath, ((TestJsonSchema) derived).getEvaluationPathPublic(), "Root evaluation path should be the empty JsonNodePath instance");
    }

    /* --------------------------------------------------------------------- *
     * Validators list remains null until explicit initialization.          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_validatorsList_isNullInitially() {
        JsonSchema parent = newTestSchema("parent", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        JsonSchema derived = schema.fromRef(parent, null);
        assertNull(derived.getValidators(), "Validators list should be null (or empty) before explicit initialization");
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null config in parent ValidationContext. *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullConfigInParent_throwsNPE() throws Exception {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "config", null);
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null), "A null config in the parent ValidationContext must cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null schemaReferences map.               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullSchemaReferences_throwsNPE() throws Exception {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "schemaReferences", null);
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null schemaResources map.                  *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullSchemaResources_throwsNPE() throws Exception {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "schemaResources", null);
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null dynamicAnchors map.                  *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullDynamicAnchors_throwsNPE() throws Exception {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "dynamicAnchors", null);
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null meta‑schema in parent.               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullMetaSchema_throwsNPE() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(null, factory, SchemaValidatorsConfig.builder().build());
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null JsonSchemaFactory in parent.        *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullFactory_throwsNPE() throws Exception {
        JsonMetaSchema meta = JsonMetaSchema.getInstance(VersionFlag.V7);
        ValidationContext vc = new ValidationContext(meta, null, SchemaValidatorsConfig.builder().build());
        JsonSchema parent = newTestSchema("parent", false, false, vc, emptySchemaNode, null, null);
        JsonSchema schema = newTestSchema("child", false, false, sharedValidationContext, emptySchemaNode, null, null);
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }
}
