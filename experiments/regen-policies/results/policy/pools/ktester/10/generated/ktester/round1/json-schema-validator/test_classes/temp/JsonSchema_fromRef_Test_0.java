package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

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

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---------- stub ValidationContext ----------
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        when(validationContext.getConfig()).thenReturn(config);
        when(validationContext.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // ---------- stub JsonMetaSchema ----------
        // readAnchor / readDynamicAnchor are used in the JsonSchema constructor
        when(metaSchema.readAnchor(any())).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any())).thenReturn(null);
        // ---------- stub SchemaLocation ----------
        // the constructor of JsonSchema will call toString() on this object,
        // so we provide a simple implementation.
        when(schemaLocation.toString()).thenReturn("mock:location");
        // ---------- create a real JsonSchema instance ----------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // a minimal (empty) evaluation path – the concrete value is irrelevant for the
        // creation of the original schema.
        JsonNodePath emptyPath = new JsonNodePath(JsonNodePath.PathType.JSON);
        originalSchema = ctor.newInstance(validationContext, schemaLocation, emptyPath, schemaNode, // no parent
        null, // suppressSubSchemaRetrieval
        false);
        // -----------------------------------------------------------------
        when(parentValidationContext.getMetaSchema()).thenReturn(metaSchema);
        when(parentValidationContext.getJsonSchemaFactory()).thenReturn(jsonSchemaFactory);
        when(parentValidationContext.getConfig()).thenReturn(parentConfig);
        when(parentValidationContext.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());
        when(parentValidationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(parentValidationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // The JsonMetaSchema methods are accessed during construction.
        when(metaSchema.readAnchor(any())).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any())).thenReturn(null);
        when(schemaLocation.toString()).thenReturn("mock:location");
        // -----------------------------------------------------------------
        originalSchema = createSchema(parentValidationContext, null);
    }

    /**
     * Helper to create a {@link JsonSchema} instance via the private constructor.
     */
    private JsonSchema createSchema(ValidationContext vc, JsonSchema parent) throws Exception {
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonNodePath emptyPath = new JsonNodePath(JsonNodePath.PathType.JSON);
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
        return Stream.of(// escaped '/'
        JsonNodePath.of("/properties/name~1details"), // root path
        JsonNodePath.empty());
    }

    @Test
    void fromRef_shouldCreateCopyWithCorrectEvaluationState() throws Exception {
        // ----- prepare parent schema (also via reflection) -----
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonNodePath parentPath = new JsonNodePath(JsonNodePath.PathType.JSON);
        JsonSchema parentSchema = ctor.newInstance(validationContext, schemaLocation, parentPath, schemaNode, null, false);
        // ----- the evaluation path that will be set on the copy -----
        JsonNodePath newEvaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON);
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
        JsonValidator dummyValidator = mock(JsonValidator.class);
        TypeValidator dummyTypeValidator = mock(TypeValidator.class);
        setField(parent, "validatorsLoaded", true);
        setField(parent, "requiredValidator", dummyValidator);
        setField(parent, "typeValidator", dummyTypeValidator);
        setField(parent, "validators", Arrays.asList(dummyValidator));
        JsonNodePath refPath = JsonNodePath.of("/properties/name");
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
        JsonNodePath path = JsonNodePath.of("/any");
        assertThrows(NullPointerException.class, () -> originalSchema.fromRef(null, path), "Passing a null parent schema should result in NPE");
    }

    @Test
    void fromRef_doesNotMutateOriginalSchema() throws Exception {
        // Prepare original with a known state
        JsonValidator origValidator = mock(JsonValidator.class);
        TypeValidator origTypeValidator = mock(TypeValidator.class);
        setField(originalSchema, "validatorsLoaded", true);
        setField(originalSchema, "requiredValidator", origValidator);
        setField(originalSchema, "typeValidator", origTypeValidator);
        setField(originalSchema, "validators", Arrays.asList(origValidator));
        JsonNodePath originalPath = JsonNodePath.of("/original");
        setField(originalSchema, "evaluationPath", originalPath);
        JsonSchema parent = createSchema(parentValidationContext, null);
        // Call fromRef
        JsonSchema copy = originalSchema.fromRef(parent, JsonNodePath.of("/sub"));
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
        when(parentValidationContext.getSchemaReferences()).thenReturn(refs);
        when(parentValidationContext.getSchemaResources()).thenReturn(resources);
        when(parentValidationContext.getDynamicAnchors()).thenReturn(anchors);
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, JsonNodePath.of("/def"));
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
        // mutable property
        mutableConfig.setTypeLoose(true);
        when(parentValidationContext.getConfig()).thenReturn(mutableConfig);
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, JsonNodePath.of("/email"));
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
        JsonSchema first = originalSchema.fromRef(parent, JsonNodePath.of("/a"));
        JsonSchema second = originalSchema.fromRef(parent, JsonNodePath.of("/a"));
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
        JsonSchema first = originalSchema.fromRef(parent, JsonNodePath.of("/a"));
        JsonSchema second = originalSchema.fromRef(parent, JsonNodePath.of("/b"));
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
        JsonSchema copy = originalSchema.fromRef(parent, JsonNodePath.of("/nested"));
        assertSame(parent, getField(copy, "evaluationParentSchema"), "The copy must reference the supplied parent, not the parent's own parent");
    }

    @Test
    void fromRef_parentWithNullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        // force internal validationContext to null
        setField(parent, "validationContext", null);
        assertThrows(NullPointerException.class, () -> originalSchema.fromRef(parent, JsonNodePath.of("/any")), "Null validationContext in parent should cause NPE");
    }

    @Test
    void fromRef_parentWithNullConfig_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getConfig()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, JsonNodePath.of("/props")), "Null config in parent ValidationContext should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaReferences_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getSchemaReferences()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, JsonNodePath.of("/defs")), "Null schemaReferences map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaResources_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getSchemaResources()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, JsonNodePath.of("/resources")), "Null schemaResources map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullDynamicAnchors_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getDynamicAnchors()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        assertThrows(IllegalArgumentException.class, () -> originalSchema.fromRef(parent, JsonNodePath.of("/anchors")), "Null dynamicAnchors set should cause IAE");
    }

    @ParameterizedTest
    @MethodSource("complexPathProvider")
    void fromRef_handlesComplexJsonNodePath(JsonNodePath path) throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, path);
        assertSame(path, getField(copy, "evaluationPath"));
    }
}
