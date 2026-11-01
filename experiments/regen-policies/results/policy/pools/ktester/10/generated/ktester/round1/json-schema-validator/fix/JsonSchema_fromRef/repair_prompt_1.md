
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /**
     * Common mocks used for all tests
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonSchemaFactory jsonSchemaFactory;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNode schemaNode;

    /**
     * The original schema on which {@code fromRef} will be called
     */
    JsonSchema originalSchema;

    @Mock
    ValidationContext parentValidationContext;

    @Mock
    SchemaValidatorsConfig parentConfig;

    /**
     * Minimal {@link JsonValidator} implementation – does nothing.
     */
    private static class DummyValidator implements JsonValidator {

        @Override
        public Set<ValidationMessage> validate(JsonNode node, JsonNodePath at) {
            return Collections.emptySet();
        }

        @Override
        public void preloadJsonSchema() {
        }

        @Override
        public JsonSchema getSchema() {
            return null;
        }

        @Override
        public void setSchema(JsonSchema schema) {
        }

        @Override
        public String getErrorMessage() {
            return null;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public boolean isRequired() {
            return false;
        }
    }

    /**
     * Minimal {@link TypeValidator} implementation – does nothing.
     */
    private static class DummyTypeValidator implements TypeValidator {

        @Override
        public Set<ValidationMessage> validate(JsonNode node, JsonNodePath at) {
            return Collections.emptySet();
        }

        @Override
        public void preloadJsonSchema() {
        }

        @Override
        public JsonSchema getSchema() {
            return null;
        }

        @Override
        public void setSchema(JsonSchema schema) {
        }

        @Override
        public String getErrorMessage() {
            return null;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public boolean isRequired() {
            return false;
        }
    }

    /**
     * Simple {@link SchemaLocation} implementation – only {@code toString()} is used.
     */
    private static class DummySchemaLocation implements SchemaLocation {

        @Override
        public String toString() {
            return "mock:location";
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---------- real objects (no Mockito) ----------
        metaSchema = JsonMetaSchema.getInstance("http://json-schema.org/draft-07/schema#");
        jsonSchemaFactory = new JsonSchemaFactory();
        config = new SchemaValidatorsConfig();
        parentConfig = new SchemaValidatorsConfig();
        // ---------- ValidationContext instances ----------
        validationContext = new ValidationContext(metaSchema, jsonSchemaFactory, config);
        parentValidationContext = new ValidationContext(metaSchema, jsonSchemaFactory, parentConfig);
        // ---------- other simple collaborators ----------
        schemaLocation = new DummySchemaLocation();
        // empty node – sufficient for construction
        schemaNode = new ObjectNode(JsonNodeFactory.instance);
        // ---------- create the original schema (via private constructor) ----------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonNodePath emptyPath = new JsonNodePath(PathType.JSON);
        originalSchema = ctor.newInstance(validationContext, schemaLocation, emptyPath, schemaNode, null, false);
    }

    /**
     * Helper to create a {@link JsonSchema} instance via the private constructor.
     */
    private JsonSchema createSchema(ValidationContext vc, JsonSchema parent) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonNodePath emptyPath = new JsonNodePath(PathType.JSON);
        return ctor.newInstance(vc, schemaLocation, emptyPath, schemaNode, parent, false);
    }

    /**
     * Sets a private field on {@code target}.
     */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Retrieves a private field from {@code target}.
     */
    private static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    static Stream<JsonNodePath> complexPathProvider() {
        return Stream.of(// represents a “complex” path – the actual value is irrelevant for the test
        new JsonNodePath(PathType.JSON), // empty path
        new JsonNodePath(PathType.JSON));
    }

    /* --------------------------------------------------------------------- */
    private static JsonSchema mock(JsonSchema ignored) {
        // The actual instance is irrelevant; we just need a distinct object.
        return new JsonSchema(null, null, null, null, null, false);
    }

    @Test
    void fromRef_shouldCreateCopyWithCorrectEvaluationState() throws Exception {
        // ----- prepare parent schema (also via reflection) -----
        JsonSchema parentSchema = createSchema(validationContext, null);
        // ----- the evaluation path that will be set on the copy -----
        JsonNodePath newEvaluationPath = new JsonNodePath(PathType.JSON);
        // ----- invoke the method under test -----
        JsonSchema copy = originalSchema.fromRef(parentSchema, newEvaluationPath);
        // ----- basic sanity checks -----
        assertNotNull(copy, "The returned copy must not be null");
        assertNotSame(originalSchema, copy, "The copy must be a different instance");
        // ----- use reflection to inspect private fields -----
        Field evalParentField = JsonSchema.class.getDeclaredField("evaluationParentSchema");
        evalParentField.setAccessible(true);
        Object evalParentValue = evalParentField.get(copy);
        assertSame(parentSchema, evalParentValue, "evaluationParentSchema must be the provided parent");
        Field evalPathField = JsonSchema.class.getDeclaredField("evaluationPath");
        evalPathField.setAccessible(true);
        Object evalPathValue = evalPathField.get(copy);
        assertSame(newEvaluationPath, evalPathValue, "evaluationPath must be the provided path");
        Field validatorsLoadedField = JsonSchema.class.getDeclaredField("validatorsLoaded");
        validatorsLoadedField.setAccessible(true);
        assertFalse((Boolean) validatorsLoadedField.get(copy), "validatorsLoaded must be reset to false");
        Field requiredValidatorField = JsonSchema.class.getDeclaredField("requiredValidator");
        requiredValidatorField.setAccessible(true);
        assertNull(requiredValidatorField.get(copy), "requiredValidator must be reset to null");
        Field typeValidatorField = JsonSchema.class.getDeclaredField("typeValidator");
        typeValidatorField.setAccessible(true);
        assertNull(typeValidatorField.get(copy), "typeValidator must be reset to null");
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        assertNull(validatorsField.get(copy), "validators list must be reset to null");
    }

    @Test
    void fromRef_basicFunctionality_resetsValidatorStateAndSetsEvaluationInfo() throws Exception {
        // ----- parent schema (with a custom config) -----
        JsonSchema parent = createSchema(parentValidationContext, null);
        // simulate that the parent already has validator state
        JsonValidator dummyValidator = new DummyValidator();
        TypeValidator dummyTypeValidator = new DummyTypeValidator();
        setField(parent, "validatorsLoaded", true);
        setField(parent, "requiredValidator", dummyValidator);
        setField(parent, "typeValidator", dummyTypeValidator);
        setField(parent, "validators", Arrays.asList(dummyValidator));
        JsonNodePath refPath = new JsonNodePath(PathType.JSON);
        // ----- invoke fromRef -----
        JsonSchema copy = originalSchema.fromRef(parent, refPath);
        // ----- assertions on copy -----
        assertNotNull(copy);
        assertNotSame(originalSchema, copy);
        assertSame(parent, getField(copy, "evaluationParentSchema"));
        assertSame(refPath, getField(copy, "evaluationPath"));
        assertFalse((Boolean) getField(copy, "validatorsLoaded"));
        assertNull(getField(copy, "requiredValidator"));
        assertNull(getField(copy, "typeValidator"));
        assertNull(getField(copy, "validators"));
        // ----- validationContext config must be identical (by value) -----
        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");
        ValidationContext parentVc = (ValidationContext) getField(parent, "validationContext");
        assertSame(parentVc.getConfig(), copyVc.getConfig(), "Config instance should be the same reference (the copy does not clone it here)");
    }

    @Test
    void fromRef_nullEvaluationPath_isAccepted() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, null);
        assertNull(getField(copy, "evaluationPath"));
        assertSame(parent, getField(copy, "evaluationParentSchema"));
    }

    @Test
    void fromRef_nullParent_throwsNullPointerException() {
        JsonNodePath path = new JsonNodePath(PathType.JSON);
        assertThrows(NullPointerException.class, () -> originalSchema.fromRef(null, path), "Passing a null parent schema should result in NPE");
    }

    @Test
    void fromRef_doesNotMutateOriginalSchema() throws Exception {
        // Prepare original with a known state
        JsonValidator origValidator = new DummyValidator();
        TypeValidator origTypeValidator = new DummyTypeValidator();
        setField(originalSchema, "validatorsLoaded", true);
        setField(originalSchema, "requiredValidator", origValidator);
        setField(originalSchema, "typeValidator", origTypeValidator);
        setField(originalSchema, "validators", Arrays.asList(origValidator));
        JsonNodePath originalPath = new JsonNodePath(PathType.JSON);
        setField(originalSchema, "evaluationPath", originalPath);
        JsonSchema parent = createSchema(parentValidationContext, null);
        // Call fromRef
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        // Verify original unchanged
        assertSame(originalPath, getField(originalSchema, "evaluationPath"));
        assertTrue((Boolean) getField(originalSchema, "validatorsLoaded"));
        assertSame(origValidator, getField(originalSchema, "requiredValidator"));
        assertSame(origTypeValidator, getField(originalSchema, "typeValidator"));
        assertEquals(1, ((List<?>) getField(originalSchema, "validators")).size());
    }

    @Test
    void fromRef_copiesValidationContextCollections_shallowCopyButDistinctObjects() throws Exception {
        // Prepare parent ValidationContext with non‑empty maps/sets
        Map<String, JsonSchema> refs = new ConcurrentHashMap<>();
        refs.put("ref1", mock(JsonSchema.class));
        Map<String, JsonSchema> resources = new ConcurrentHashMap<>();
        resources.put("res1", mock(JsonSchema.class));
        Set<String> anchors = Collections.newSetFromMap(new ConcurrentHashMap<>());
        anchors.add("anchor1");
        // Populate the parent ValidationContext via reflection
        setField(parentValidationContext, "schemaReferences", refs);
        setField(parentValidationContext, "schemaResources", resources);
        setField(parentValidationContext, "dynamicAnchors", anchors);
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");
        // Collections must contain same entries but be different objects
        assertEquals(refs, copyVc.getSchemaReferences());
        assertNotSame(refs, copyVc.getSchemaReferences());
        assertEquals(resources, copyVc.getSchemaResources());
        assertNotSame(resources, copyVc.getSchemaResources());
        assertEquals(anchors, copyVc.getDynamicAnchors());
        assertNotSame(anchors, copyVc.getDynamicAnchors());
    }

    @Test
    void fromRef_validationContextConfig_isDeepCopied() throws Exception {
        // Parent config with a mutable property
        SchemaValidatorsConfig mutableConfig = new SchemaValidatorsConfig();
        mutableConfig.setTypeLoose(true);
        // replace the config inside the parent ValidationContext
        setField(parentValidationContext, "config", mutableConfig);
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");
        SchemaValidatorsConfig copyConfig = copyVc.getConfig();
        // Ensure values are equal initially
        assertEquals(mutableConfig.isTypeLoose(), copyConfig.isTypeLoose());
        // Mutate copy's config and verify parent config unchanged
        copyConfig.setTypeLoose(false);
        assertTrue(mutableConfig.isTypeLoose(), "Changing the copy's config must not affect the parent's config");
    }

    @Test
    void fromRef_multipleCallsProduceIndependentCopies() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema first = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        JsonSchema second = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        assertNotSame(first, second);
        assertSame(parent, getField(first, "evaluationParentSchema"));
        assertSame(parent, getField(second, "evaluationParentSchema"));
        ValidationContext vc1 = (ValidationContext) getField(first, "validationContext");
        ValidationContext vc2 = (ValidationContext) getField(second, "validationContext");
        assertNotSame(vc1, vc2);
    }

    @Test
    void fromRef_modifyingFirstCopyConfigDoesNotAffectSecondCopy() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema first = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        JsonSchema second = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        ValidationContext vcFirst = (ValidationContext) getField(first, "validationContext");
        ValidationContext vcSecond = (ValidationContext) getField(second, "validationContext");
        // Change a config value on the first copy
        vcFirst.getConfig().setFailFast(true);
        assertFalse(vcSecond.getConfig().isFailFast(), "Second copy's config must stay unchanged after mutating the first copy");
    }

    @Test
    void fromRef_evaluationParentSchema_isExactArgumentInstance() throws Exception {
        // Parent that itself has an evaluationParentSchema set (simulating a $ref chain)
        JsonSchema grandParent = createSchema(parentValidationContext, null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        setField(parent, "evaluationParentSchema", grandParent);
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        assertSame(parent, getField(copy, "evaluationParentSchema"), "The copy must reference the supplied parent, not the parent's own parent");
    }

    @Test
    void fromRef_parentWithNullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        // force internal validationContext to null
        setField(parent, "validationContext", null);
        assertThrows(NullPointerException.class, () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)), "Null validationContext in parent should cause NPE");
    }

    @Test
    void fromRef_parentWithNullConfig_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "config", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)), "Null config in parent ValidationContext should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaReferences_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "schemaReferences", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)), "Null schemaReferences map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaResources_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "schemaResources", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)), "Null schemaResources map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullDynamicAnchors_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "dynamicAnchors", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)), "Null dynamicAnchors set should cause IAE");
    }

    @ParameterizedTest
    @MethodSource("complexPathProvider")
    void fromRef_handlesComplexJsonNodePath(JsonNodePath path) throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, path);
        assertSame(path, getField(copy, "evaluationPath"));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_fromRef_Test.java:15: error: ';' expected
import org.junit.jupiter.params.provider.MethodSource
                                                     ^
1 error

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'api document of method fromRef(JsonSchema, JsonNodePath)': '\n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonValidator com.networknt.schema.JsonSchema.requiredValidator;\n\t\tboolean com.networknt.schema.JsonSchema.validatorsLoaded;\n\t\tcom.networknt.schema.TypeValidator com.networknt.schema.JsonSchema.typeValidator;\n\t\tjava.util.List<com.networknt.schema.JsonValidator> com.networknt.schema.JsonSchema.validators;\n\trelated functions:\n\t\tmethod `private List<JsonValidator> read(JsonNode)` returns `java.util.List<com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`, api document: \n     * Please note that the key in {@link #validators} map is the evaluation path.\n     ;\n\t\tmethod `public TypeValidator getTypeValidator()` returns `com.networknt.schema.TypeValidator`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonNodePath com.networknt.schema.ValidationMessageHandler.evaluationPath;\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.evaluationParentSchema;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `private ValidationContext withMetaSchema(ValidationContext, JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`, api document: \n     * Determines the validation context to use for the schema given the parent\n     * validation context.\n     * <p>\n     * This is typically the same validation context unless the schema has a\n     * different $schema from the parent.\n     * <p>\n     * If the schema does not define a $schema, the parent should be used.\n     * \n     * @param validationContext the parent validation context\n     * @param schemaNode        the schema node\n     * @return the validation context to use\n     ;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static void copySchemaResources(ValidationContext, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.fromRef(JsonSchema, JsonNodePath)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


