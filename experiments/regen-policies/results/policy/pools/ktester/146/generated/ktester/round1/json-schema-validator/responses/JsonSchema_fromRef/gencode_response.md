```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#fromRef(JsonSchema, JsonNodePath)}.
 *
 * The tests cover normal behaviour, propagation of configuration flags,
 * handling of {@code null} arguments and defensive error handling for
 * missing internal objects.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /* --------------------------------------------------------------------- *
     * Helper concrete subclass exposing the protected constructor and the *
     * evaluation‑path getters.                                            *
     * --------------------------------------------------------------------- */
    private static final class TestJsonSchema extends JsonSchema {
        TestJsonSchema(List<JsonValidator> validators,
                       boolean validatorsLoaded,
                       boolean recursiveAnchor,
                       TypeValidator typeValidator,
                       String id,
                       boolean suppressSubSchemaRetrieval,
                       JsonNode schemaNode,
                       ValidationContext validationContext,
                       ErrorMessageType errorMessageType,
                       String errorMessageKeyword,
                       MessageSource messageSource,
                       Keyword keyword,
                       JsonSchema parentSchema,
                       SchemaLocation schemaLocation,
                       JsonNodePath evaluationPath,
                       JsonSchema evaluationParentSchema,
                       Map<String, String> errorMessage) {
            super(validators, validatorsLoaded, recursiveAnchor, typeValidator, id,
                    suppressSubSchemaRetrieval, schemaNode, validationContext,
                    errorMessageType, errorMessageKeyword, messageSource, keyword,
                    parentSchema, schemaLocation, evaluationPath, evaluationParentSchema,
                    errorMessage);
        }

        JsonNodePath getEvaluationPathPublic() {
            return super.getEvaluationPath();
        }

        JsonSchema getEvaluationParentSchemaPublic() {
            return super.getEvaluationParentSchema();
        }

        @Override
        public String getId() {
            return super.getId();
        }
    }

    /* --------------------------------------------------------------------- *
     * Shared objects used by all tests.                                   *
     * --------------------------------------------------------------------- */
    private static ValidationContext sharedValidationContext;
    private static JsonNode emptySchemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Minimal but functional ValidationContext – meta‑schema and factory are mocked.
        JsonMetaSchema metaSchema = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        sharedValidationContext = new ValidationContext(metaSchema, factory, config);
        emptySchemaNode = JsonNodeFactory.instance.objectNode(); // empty {}
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
     * Normal operation – typical reference resolution (already provided).   *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_copiesRelevantState() {
        JsonNodePath parentEvalPath = Mockito.mock(JsonNodePath.class);
        JsonSchema parentSchema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                parentEvalPath,
                null,
                Collections.emptyMap()
        );

        JsonNodePath originalEvalPath = Mockito.mock(JsonNodePath.class);
        JsonSchema schemaUnderTest = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "original-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                originalEvalPath,
                null,
                Collections.emptyMap()
        );

        JsonNodePath newEvalPath = Mockito.mock(JsonNodePath.class);
        JsonSchema derived = schemaUnderTest.fromRef(parentSchema, newEvalPath);

        assertNotNull(derived);
        assertNotSame(schemaUnderTest, derived);
        assertEquals("original-id", ((TestJsonSchema) derived).getId());
        assertSame(newEvalPath, ((TestJsonSchema) derived).getEvaluationPathPublic());
        assertSame(parentSchema, ((TestJsonSchema) derived).getEvaluationParentSchemaPublic());
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with recursive anchor enabled.                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_inheritsRecursiveAnchorFlag() {
        JsonSchema parentSchema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                true, // recursiveAnchor = true
                null,
                "parent-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parentSchema, Mockito.mock(JsonNodePath.class));

        // recursiveAnchor is private – use reflection to read it
        boolean recursiveAnchor = (boolean) getFieldValue(derived, "recursiveAnchor");
        assertTrue(recursiveAnchor, "Recursive anchor flag must be inherited from the parent");
    }

    /* --------------------------------------------------------------------- *
     * Null evaluation path – should not throw NPE.                          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullEvaluationPath_isHandledGracefully() {
        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, null);
        assertNotNull(derived);
        assertNull(((TestJsonSchema) derived).getEvaluationPathPublic(),
                "Evaluation path may be null (or empty) when a null argument is supplied");
    }

    /* --------------------------------------------------------------------- *
     * Null parent schema – should fail fast.                               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullParentSchema_throwsNPE() {
        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(null, Mockito.mock(JsonNodePath.class)),
                "Passing a null parent schema must result in NPE");
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with custom ValidationContext configuration.          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_propagatesCustomValidationContext() {
        // custom config
        SchemaValidatorsConfig customConfig = SchemaValidatorsConfig.builder()
                .typeLoose(true)
                .build();

        // create a ValidationContext with custom config and non‑empty maps
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
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

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                parentVc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, Mockito.mock(JsonNodePath.class));
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
        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                true, // suppressSubSchemaRetrieval = true
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, Mockito.mock(JsonNodePath.class));

        // suppressSubSchemaRetrieval lives in BaseJsonValidator (protected)
        boolean suppress = (boolean) getFieldValue(derived, "suppressSubSchemaRetrieval");
        assertTrue(suppress, "suppressSubSchemaRetrieval flag must be inherited from the parent");
    }

    /* --------------------------------------------------------------------- *
     * Id propagation – parent schema id retained.                          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_retainsParentId() {
        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent-schema-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child-id",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, Mockito.mock(JsonNodePath.class));
        assertEquals("child-id", ((TestJsonSchema) derived).getId(),
                "The derived schema must keep its own id (not the parent’s).");
    }

    /* --------------------------------------------------------------------- *
     * Dynamic anchors propagation.                                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_copiesDynamicAnchors() {
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());

        Map<String, JsonSchema> anchors = new HashMap<>();
        JsonSchema anchorSchema = new TestJsonSchema(
                Collections.emptyList(), false, false, null, "anchor", false,
                emptySchemaNode, vc, null, null, null, null, null, null,
                Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
        anchors.put("myAnchor", anchorSchema);
        setField(vc, "dynamicAnchors", anchors);

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, Mockito.mock(JsonNodePath.class));
        Map<String, JsonSchema> derivedAnchors = derived.getValidationContext().getDynamicAnchors();
        assertSame(anchors, derivedAnchors, "Dynamic anchors map must be the same instance");
        assertEquals(anchorSchema, derivedAnchors.get("myAnchor"));
    }

    /* --------------------------------------------------------------------- *
     * Schema resources map propagation.                                    *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_copiesSchemaResources() {
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());

        Map<String, JsonNode> resources = new HashMap<>();
        resources.put("http://example.com/schema", JsonNodeFactory.instance.objectNode());
        setField(vc, "schemaResources", resources);

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, Mockito.mock(JsonNodePath.class));
        Map<String, JsonNode> derivedResources = derived.getValidationContext().getSchemaResources();
        assertSame(resources, derivedResources);
        assertTrue(derivedResources.containsKey("http://example.com/schema"));
    }

    /* --------------------------------------------------------------------- *
     * Evaluation path with URL‑encoded characters.                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_preservesUrlEncodedEvaluationPath() {
        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonNodePath encodedPath = new JsonNodePath("/properties/%20name");
        JsonSchema derived = schema.fromRef(parent, encodedPath);
        assertSame(encodedPath, ((TestJsonSchema) derived).getEvaluationPathPublic(),
                "Encoded path must be preserved unchanged");
    }

    /* --------------------------------------------------------------------- *
     * Root evaluation path – empty JsonNodePath.                           *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_rootEvaluationPath_isEmpty() {
        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonNodePath emptyPath = JsonNodePath.empty();
        JsonSchema derived = schema.fromRef(parent, emptyPath);
        assertSame(emptyPath, ((TestJsonSchema) derived).getEvaluationPathPublic(),
                "Root evaluation path should be the empty JsonNodePath instance");
    }

    /* --------------------------------------------------------------------- *
     * Validators list remains null until explicit initialization.          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_validatorsList_isNullInitially() {
        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema derived = schema.fromRef(parent, Mockito.mock(JsonNodePath.class));
        assertNull(derived.getValidators(),
                "Validators list should be null (or empty) before explicit initialization");
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null config in parent ValidationContext. *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullConfigInParent_throwsNPE() throws Exception {
        // Build a valid ValidationContext first
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());

        // Force config to null via reflection
        setField(vc, "config", null);

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(parent, Mockito.mock(JsonNodePath.class)),
                "A null config in the parent ValidationContext must cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null schemaReferences map.               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullSchemaReferences_throwsNPE() throws Exception {
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());

        setField(vc, "schemaReferences", null);

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(parent, Mockito.mock(JsonNodePath.class)));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null schemaResources map.                  *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullSchemaResources_throwsNPE() throws Exception {
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());

        setField(vc, "schemaResources", null);

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(parent, Mockito.mock(JsonNodePath.class)));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null dynamicAnchors map.                  *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullDynamicAnchors_throwsNPE() throws Exception {
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());

        setField(vc, "dynamicAnchors", null);

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(parent, Mockito.mock(JsonNodePath.class)));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null meta‑schema in parent.               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullMetaSchema_throwsNPE() throws Exception {
        JsonSchemaFactory factory = Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(null, factory, SchemaValidatorsConfig.builder().build());

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(parent, Mockito.mock(JsonNodePath.class)));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null JsonSchemaFactory in parent.        *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullFactory_throwsNPE() throws Exception {
        JsonMetaSchema meta = Mockito.mock(JsonMetaSchema.class);
        ValidationContext vc = new ValidationContext(meta, null, SchemaValidatorsConfig.builder().build());

        JsonSchema parent = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "parent",
                false,
                emptySchemaNode,
                vc,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        JsonSchema schema = new TestJsonSchema(
                Collections.emptyList(),
                false,
                false,
                null,
                "child",
                false,
                emptySchemaNode,
                sharedValidationContext,
                null,
                null,
                null,
                null,
                null,
                null,
                Mockito.mock(JsonNodePath.class),
                null,
                Collections.emptyMap()
        );

        assertThrows(NullPointerException.class,
                () -> schema.fromRef(parent, Mockito.mock(JsonNodePath.class)));
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
}
```