
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import com.networknt.schema.i18n.MessageSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
        // Minimal but functional ValidationContext – the meta‑schema and factory are mocked
        JsonMetaSchema metaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
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

    @Test
    void fromRef_copiesRelevantState() {
        // -----------------------------------------------------------------
        // Arrange – create a “parent” schema that will be passed as the
        // evaluationParentSchema argument.
        // -----------------------------------------------------------------
        JsonSchema parentSchema = new TestJsonSchema(// validators
        Collections.emptyList(), // validatorsLoaded
        false, // recursiveAnchor
        false, // typeValidator
        null, // id
        "parent-id", // suppressSubSchemaRetrieval
        false, // schemaNode
        emptySchemaNode, // validationContext
        sharedValidationContext, // errorMessageType
        null, // errorMessageKeyword
        null, // messageSource
        null, // keyword
        null, // parentSchema
        null, // schemaLocation
        null, // evaluationPath
        null, // evaluationParentSchema
        null, // errorMessage map
        Collections.emptyMap());
        // -----------------------------------------------------------------
        // Arrange – create the schema on which we will invoke fromRef().
        // -----------------------------------------------------------------
        JsonSchema schemaUnderTest = new TestJsonSchema(Collections.emptyList(), false, false, null, "original-id", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        // The new evaluation path that we want the derived schema to carry.
        // we use null for simplicity
        JsonNodePath newEvalPath = null;
        // -----------------------------------------------------------------
        // Act – call the method under test.
        // -----------------------------------------------------------------
        JsonSchema derived = schemaUnderTest.fromRef(parentSchema, newEvalPath);
        // -----------------------------------------------------------------
        // Assert – verify the derived schema has the expected state.
        // -----------------------------------------------------------------
        assertNotNull(derived, "fromRef must return a non‑null JsonSchema");
        assertNotSame(schemaUnderTest, derived, "Derived schema must be a new instance");
        // The id should be copied from the original schema.
        assertEquals("original-id", ((TestJsonSchema) derived).getId(), "Derived schema should retain the original id");
        // The evaluation path supplied to fromRef must be stored (null in this test).
        assertNull(((TestJsonSchema) derived).getEvaluationPathPublic(), "EvaluationPath should be the one supplied to fromRef (null here)");
        // The evaluation parent schema should be stored.
        assertSame(parentSchema, ((TestJsonSchema) derived).getEvaluationParentSchemaPublic(), "EvaluationParentSchema should be the one supplied to fromRef");
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with recursive anchor enabled.                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_inheritsRecursiveAnchorFlag() {
        JsonSchema parentSchema = new TestJsonSchema(Collections.emptyList(), false, // recursiveAnchor = true
        true, null, "parent-id", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child-id", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema derived = schema.fromRef(parentSchema, null);
        // recursiveAnchor is private – use reflection to read it
        boolean recursiveAnchor = (boolean) getFieldValue(derived, "recursiveAnchor");
        assertTrue(recursiveAnchor, "Recursive anchor flag must be inherited from the parent");
    }

    /* --------------------------------------------------------------------- *
     * Null evaluation path – should not throw NPE.                          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullEvaluationPath_isHandledGracefully() {
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema derived = schema.fromRef(parent, null);
        assertNotNull(derived);
        assertNull(((TestJsonSchema) derived).getEvaluationPathPublic(), "Evaluation path may be null when a null argument is supplied");
    }

    /* --------------------------------------------------------------------- *
     * Null parent schema – should fail fast.                               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullParentSchema_throwsNPE() {
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(null, null), "Passing a null parent schema must result in NPE");
    }

    /* --------------------------------------------------------------------- *
     * Parent schema with custom ValidationContext configuration.          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_propagatesCustomValidationContext() {
        // custom config
        SchemaValidatorsConfig customConfig = SchemaValidatorsConfig.builder().typeLoose(true).build();
        // create a ValidationContext with custom config and non‑empty maps
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
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
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, parentVc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
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
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", // suppressSubSchemaRetrieval = true
        true, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema derived = schema.fromRef(parent, null);
        // suppressSubSchemaRetrieval lives in BaseJsonValidator (protected)
        boolean suppress = (boolean) getFieldValue(derived, "suppressSubSchemaRetrieval");
        assertTrue(suppress, "suppressSubSchemaRetrieval flag must be inherited from the parent");
    }

    /* --------------------------------------------------------------------- *
     * Id propagation – parent schema id retained.                          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_retainsParentId() {
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent-schema-id", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child-id", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema derived = schema.fromRef(parent, null);
        assertEquals("child-id", ((TestJsonSchema) derived).getId(), "The derived schema must keep its own id (not the parent’s).");
    }

    /* --------------------------------------------------------------------- *
     * Dynamic anchors propagation.                                         *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_copiesDynamicAnchors() {
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        JsonSchema anchorSchema = new TestJsonSchema(Collections.emptyList(), false, false, null, "anchor", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        Map<String, JsonSchema> anchors = new HashMap<>();
        anchors.put("myAnchor", anchorSchema);
        setField(vc, "dynamicAnchors", anchors);
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
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
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        Map<String, JsonNode> resources = new HashMap<>();
        resources.put("http://example.com/schema", JsonNodeFactory.instance.objectNode());
        setField(vc, "schemaResources", resources);
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
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
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
        JsonNodePath encodedPath = new JsonNodePath("/properties/%20name");
        JsonSchema derived = schema.fromRef(parent, encodedPath);
        assertSame(encodedPath, ((TestJsonSchema) derived).getEvaluationPathPublic(), "Encoded path must be preserved unchanged");
    }

    /* --------------------------------------------------------------------- *
     * Root evaluation path – empty JsonNodePath.                           *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_rootEvaluationPath_isEmpty() {
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
        JsonNodePath emptyPath = JsonNodePath.empty();
        JsonSchema derived = schema.fromRef(parent, emptyPath);
        assertSame(emptyPath, ((TestJsonSchema) derived).getEvaluationPathPublic(), "Root evaluation path should be the empty JsonNodePath instance");
    }

    /* --------------------------------------------------------------------- *
     * Validators list remains null until explicit initialization.          *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_validatorsList_isNullInitially() {
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema derived = schema.fromRef(parent, null);
        assertNull(derived.getValidators(), "Validators list should be null (or empty) before explicit initialization");
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null config in parent ValidationContext. *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullConfigInParent_throwsNPE() throws Exception {
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        // Force config to null via reflection
        setField(vc, "config", null);
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null), "A null config in the parent ValidationContext must cause NPE");
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null schemaReferences map.               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullSchemaReferences_throwsNPE() throws Exception {
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "schemaReferences", null);
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null schemaResources map.                  *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullSchemaResources_throwsNPE() throws Exception {
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "schemaResources", null);
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null dynamicAnchors map.                  *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullDynamicAnchors_throwsNPE() throws Exception {
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(meta, factory, SchemaValidatorsConfig.builder().build());
        setField(vc, "dynamicAnchors", null);
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null meta‑schema in parent.               *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullMetaSchema_throwsNPE() throws Exception {
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
        ValidationContext vc = new ValidationContext(null, factory, SchemaValidatorsConfig.builder().build());
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }

    /* --------------------------------------------------------------------- *
     * Defensive error handling – null JsonSchemaFactory in parent.        *
     * --------------------------------------------------------------------- */
    @Test
    void fromRef_nullFactory_throwsNPE() throws Exception {
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
        ValidationContext vc = new ValidationContext(meta, null, SchemaValidatorsConfig.builder().build());
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, vc, null, null, null, null, null, null, null, null, Collections.emptyMap());
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, null, null, Collections.emptyMap());
        assertThrows(NullPointerException.class, () -> schema.fromRef(parent, null));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:13: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:59: error: package org.mockito does not exist
        JsonMetaSchema metaSchema = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:60: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:184: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:185: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:237: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:238: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:257: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:258: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:276: error: cannot find symbol
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
                                                                                                                                                                                           ^
  symbol:   variable Mockito
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:277: error: cannot find symbol
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
                                                                                                                                                                                          ^
  symbol:   variable Mockito
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:278: error: incompatible types: String cannot be converted to PathType
        JsonNodePath encodedPath = new JsonNodePath("/properties/%20name");
                                                    ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:288: error: cannot find symbol
        JsonSchema parent = new TestJsonSchema(Collections.emptyList(), false, false, null, "parent", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
                                                                                                                                                                                           ^
  symbol:   variable Mockito
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:289: error: cannot find symbol
        JsonSchema schema = new TestJsonSchema(Collections.emptyList(), false, false, null, "child", false, emptySchemaNode, sharedValidationContext, null, null, null, null, null, null, Mockito.mock(JsonNodePath.class), null, Collections.emptyMap());
                                                                                                                                                                                          ^
  symbol:   variable Mockito
  location: class JsonSchema_fromRef_Test
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:290: error: cannot find symbol
        JsonNodePath emptyPath = JsonNodePath.empty();
                                             ^
  symbol:   method empty()
  location: class JsonNodePath
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:311: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:312: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:326: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:327: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:340: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:341: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:354: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:355: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:368: error: package org.mockito does not exist
        JsonSchemaFactory factory = org.mockito.Mockito.mock(JsonSchemaFactory.class);
                                               ^
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:380: error: package org.mockito does not exist
        JsonMetaSchema meta = org.mockito.Mockito.mock(JsonMetaSchema.class);
                                         ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
26 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method fromRef(JsonSchema, JsonNodePath)': '\n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.JsonSchema.id;\n\t\tboolean com.networknt.schema.JsonSchema.recursiveAnchor;\n\trelated functions:\n\t\tmethod `private JsonNode deserialize(String, InputFormat)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`, api document: \n     * Deserialize string to JsonNode.\n     * \n     * @param input the input\n     * @param inputFormat the format\n     * @return the JsonNode.\n     ;\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.ErrorMessageType com.networknt.schema.ValidationMessageHandler.errorMessageType;\n\t\tjava.lang.String com.networknt.schema.ValidationMessageHandler.errorMessageKeyword;\n\t\tjava.util.Map<java.lang.String, java.lang.String> com.networknt.schema.ValidationMessageHandler.errorMessage;\n\t\tcom.networknt.schema.Keyword com.networknt.schema.ValidationMessageHandler.keyword;\n\t\tcom.networknt.schema.i18n.MessageSource com.networknt.schema.ValidationMessageHandler.messageSource;\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;\n\trelated functions:\n\t\tmethod `protected MessageSourceValidationMessage.Builder message()` returns `com.networknt.schema.MessageSourceValidationMessage.Builder`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\tboolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `private ValidationContext withMetaSchema(ValidationContext, JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`, api document: \n     * Determines the validation context to use for the schema given the parent\n     * validation context.\n     * <p>\n     * This is typically the same validation context unless the schema has a\n     * different $schema from the parent.\n     * <p>\n     * If the schema does not define a $schema, the parent should be used.\n     * \n     * @param validationContext the parent validation context\n     * @param schemaNode        the schema node\n     * @return the validation context to use\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod ` static JsonSchemaRef getRefSchema(JsonSchema, ValidationContext, String, JsonNodePath)` returns `com.networknt.schema.JsonSchemaRef`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `private static void copySchemaResources(ValidationContext, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


