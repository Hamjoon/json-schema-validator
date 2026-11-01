```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests for {@link JsonSchema#fromRef(JsonSchema, JsonNodePath)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /* --------------------------------------------------------------------- */
    /** Common mocks used for all tests */
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

    /** The original schema on which {@code fromRef} will be called */
    JsonSchema originalSchema;

    @Mock
    ValidationContext parentValidationContext;

    @Mock
    SchemaValidatorsConfig parentConfig;

    /* --------------------------------------------------------------------- */
    /** Minimal {@link JsonValidator} implementation – does nothing. */
    private static class DummyValidator implements JsonValidator {
        @Override
        public Set<ValidationMessage> validate(JsonNode node, JsonNodePath at) {
            return Collections.emptySet();
        }

        @Override public void preloadJsonSchema() {}
        @Override public JsonSchema getSchema() { return null; }
        @Override public void setSchema(JsonSchema schema) {}
        @Override public String getErrorMessage() { return null; }
        @Override public String getMessage() { return null; }
        @Override public boolean isRequired() { return false; }
    }

    /** Minimal {@link TypeValidator} implementation – does nothing. */
    private static class DummyTypeValidator implements TypeValidator {
        @Override
        public Set<ValidationMessage> validate(JsonNode node, JsonNodePath at) {
            return Collections.emptySet();
        }

        @Override public void preloadJsonSchema() {}
        @Override public JsonSchema getSchema() { return null; }
        @Override public void setSchema(JsonSchema schema) {}
        @Override public String getErrorMessage() { return null; }
        @Override public String getMessage() { return null; }
        @Override public boolean isRequired() { return false; }
    }

    /** Simple {@link SchemaLocation} implementation – only {@code toString()} is used. */
    private static class DummySchemaLocation implements SchemaLocation {
        @Override public String toString() { return "mock:location"; }
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
        schemaNode = new ObjectNode(JsonNodeFactory.instance);

        // ---------- create the original schema (via private constructor) ----------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonNodePath emptyPath = new JsonNodePath(PathType.JSON);
        originalSchema = ctor.newInstance(validationContext, schemaLocation, emptyPath, schemaNode, null, false);
    }

    /** Helper to create a {@link JsonSchema} instance via the private constructor. */
    private JsonSchema createSchema(ValidationContext vc, JsonSchema parent) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonNodePath emptyPath = new JsonNodePath(PathType.JSON);
        return ctor.newInstance(vc, schemaLocation, emptyPath, schemaNode, parent, false);
    }

    /** Sets a private field on {@code target}. */
    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** Retrieves a private field from {@code target}. */
    private static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    static Stream<JsonNodePath> complexPathProvider() {
        return Stream.of(
                new JsonNodePath(PathType.JSON), // empty path
                new JsonNodePath(PathType.JSON));
    }

    /** Returns a distinct mock {@link JsonSchema} instance. */
    private static JsonSchema mock(JsonSchema ignored) {
        return Mockito.mock(JsonSchema.class);
    }

    @Test
    void fromRef_shouldCreateCopyWithCorrectEvaluationState() throws Exception {
        JsonSchema parentSchema = createSchema(validationContext, null);
        JsonNodePath newEvaluationPath = new JsonNodePath(PathType.JSON);
        JsonSchema copy = originalSchema.fromRef(parentSchema, newEvaluationPath);

        assertNotNull(copy, "The returned copy must not be null");
        assertNotSame(originalSchema, copy, "The copy must be a different instance");

        Field evalParentField = JsonSchema.class.getDeclaredField("evaluationParentSchema");
        evalParentField.setAccessible(true);
        assertSame(parentSchema, evalParentField.get(copy), "evaluationParentSchema must be the provided parent");

        Field evalPathField = JsonSchema.class.getDeclaredField("evaluationPath");
        evalPathField.setAccessible(true);
        assertSame(newEvaluationPath, evalPathField.get(copy), "evaluationPath must be the provided path");

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
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonValidator dummyValidator = new DummyValidator();
        TypeValidator dummyTypeValidator = new DummyTypeValidator();

        setField(parent, "validatorsLoaded", true);
        setField(parent, "requiredValidator", dummyValidator);
        setField(parent, "typeValidator", dummyTypeValidator);
        setField(parent, "validators", Arrays.asList(dummyValidator));

        JsonNodePath refPath = new JsonNodePath(PathType.JSON);
        JsonSchema copy = originalSchema.fromRef(parent, refPath);

        assertNotNull(copy);
        assertNotSame(originalSchema, copy);
        assertSame(parent, getField(copy, "evaluationParentSchema"));
        assertSame(refPath, getField(copy, "evaluationPath"));
        assertFalse((Boolean) getField(copy, "validatorsLoaded"));
        assertNull(getField(copy, "requiredValidator"));
        assertNull(getField(copy, "typeValidator"));
        assertNull(getField(copy, "validators"));

        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");
        ValidationContext parentVc = (ValidationContext) getField(parent, "validationContext");
        assertSame(parentVc.getConfig(), copyVc.getConfig(),
                "Config instance should be the same reference (the copy does not clone it here)");
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
        assertThrows(NullPointerException.class,
                () -> originalSchema.fromRef(null, path),
                "Passing a null parent schema should result in NPE");
    }

    @Test
    void fromRef_doesNotMutateOriginalSchema() throws Exception {
        JsonValidator origValidator = new DummyValidator();
        TypeValidator origTypeValidator = new DummyTypeValidator();

        setField(originalSchema, "validatorsLoaded", true);
        setField(originalSchema, "requiredValidator", origValidator);
        setField(originalSchema, "typeValidator", origTypeValidator);
        setField(originalSchema, "validators", Arrays.asList(origValidator));

        JsonNodePath originalPath = new JsonNodePath(PathType.JSON);
        setField(originalSchema, "evaluationPath", originalPath);

        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));

        assertSame(originalPath, getField(originalSchema, "evaluationPath"));
        assertTrue((Boolean) getField(originalSchema, "validatorsLoaded"));
        assertSame(origValidator, getField(originalSchema, "requiredValidator"));
        assertSame(origTypeValidator, getField(originalSchema, "typeValidator"));
        assertEquals(1, ((List<?>) getField(originalSchema, "validators")).size());
    }

    @Test
    void fromRef_copiesValidationContextCollections_shallowCopyButDistinctObjects() throws Exception {
        Map<String, JsonSchema> refs = new ConcurrentHashMap<>();
        refs.put("ref1", mock(null));
        Map<String, JsonSchema> resources = new ConcurrentHashMap<>();
        resources.put("res1", mock(null));
        Set<String> anchors = Collections.newSetFromMap(new ConcurrentHashMap<>());
        anchors.add("anchor1");

        setField(parentValidationContext, "schemaReferences", refs);
        setField(parentValidationContext, "schemaResources", resources);
        setField(parentValidationContext, "dynamicAnchors", anchors);

        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));

        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");

        assertEquals(refs, copyVc.getSchemaReferences());
        assertNotSame(refs, copyVc.getSchemaReferences());

        assertEquals(resources, copyVc.getSchemaResources());
        assertNotSame(resources, copyVc.getSchemaResources());

        assertEquals(anchors, copyVc.getDynamicAnchors());
        assertNotSame(anchors, copyVc.getDynamicAnchors());
    }

    @Test
    void fromRef_validationContextConfig_isDeepCopied() throws Exception {
        SchemaValidatorsConfig mutableConfig = new SchemaValidatorsConfig();
        mutableConfig.setTypeLoose(true);
        setField(parentValidationContext, "config", mutableConfig);

        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));

        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");
        SchemaValidatorsConfig copyConfig = copyVc.getConfig();

        assertEquals(mutableConfig.isTypeLoose(), copyConfig.isTypeLoose());

        copyConfig.setTypeLoose(false);
        assertTrue(mutableConfig.isTypeLoose(),
                "Changing the copy's config must not affect the parent's config");
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

        vcFirst.getConfig().setFailFast(true);
        assertFalse(vcSecond.getConfig().isFailFast(),
                "Second copy's config must stay unchanged after mutating the first copy");
    }

    @Test
    void fromRef_evaluationParentSchema_isExactArgumentInstance() throws Exception {
        JsonSchema grandParent = createSchema(parentValidationContext, null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        setField(parent, "evaluationParentSchema", grandParent);

        JsonSchema copy = originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON));
        assertSame(parent, getField(copy, "evaluationParentSchema"),
                "The copy must reference the supplied parent, not the parent's own parent");
    }

    @Test
    void fromRef_parentWithNullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        setField(parent, "validationContext", null);
        assertThrows(NullPointerException.class,
                () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)),
                "Null validationContext in parent should cause NPE");
    }

    @Test
    void fromRef_parentWithNullConfig_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "config", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)),
                "Null config in parent ValidationContext should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaReferences_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "schemaReferences", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)),
                "Null schemaReferences map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaResources_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "schemaResources", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)),
                "Null schemaResources map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullDynamicAnchors_throwsIllegalArgumentException() throws Exception {
        setField(parentValidationContext, "dynamicAnchors", null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, new JsonNodePath(PathType.JSON)),
                "Null dynamicAnchors set should cause IAE");
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