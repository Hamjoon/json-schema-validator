```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#fromRef(JsonSchema, JsonNodePath)}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>basic copy creation and evaluation state handling</li>
 *   <li>reset of validator‑related fields</li>
 *   <li>behaviour when the parent schema is {@code null}</li>
 *   <li>deep‑copy semantics of {@link ValidationContext} collections and config</li>
 *   <li>isolation of the original schema</li>
 *   <li>multiple independent copies from the same parent</li>
 *   <li>integrity of the {@code evaluationParentSchema} reference</li>
 *   <li>exceptional situations caused by null internal structures</li>
 * </ul>
 *
 * <p>All interactions with the internal state are performed via reflection – the
 * class under test does not expose setters for the fields that need to be
 * pre‑configured.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /* --------------------------------------------------------------------- *
     *  Common test fixtures
     * --------------------------------------------------------------------- */

    @Mock ValidationContext parentValidationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory jsonSchemaFactory;
    @Mock SchemaValidatorsConfig parentConfig;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNode schemaNode;

    /** The original (template) schema on which {@code fromRef} will be invoked. */
    JsonSchema originalSchema;

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
        JsonNodePath emptyPath = new JsonNodePath(JsonNodePath.PathType.JSON);
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

    @BeforeEach
    void setUp() throws Exception {
        // -----------------------------------------------------------------
        // Stubbing a minimal ValidationContext that the original schema can use.
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
        // Build the original schema that will act as the template.
        // -----------------------------------------------------------------
        originalSchema = createSchema(parentValidationContext, null);
    }

    /* --------------------------------------------------------------------- *
     *  Basic functionality – evaluation state & validator reset
     * --------------------------------------------------------------------- */

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
        JsonNodePath path = JsonNodePath.of("/any");
        assertThrows(NullPointerException.class,
                () -> originalSchema.fromRef(null, path),
                "Passing a null parent schema should result in NPE");
    }

    /* --------------------------------------------------------------------- *
     *  Isolation of the original schema – original must stay unchanged
     * --------------------------------------------------------------------- */

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

    /* --------------------------------------------------------------------- *
     *  Deep copy of ValidationContext collections
     * --------------------------------------------------------------------- */

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
        mutableConfig.setTypeLoose(true); // mutable property
        when(parentValidationContext.getConfig()).thenReturn(mutableConfig);

        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, JsonNodePath.of("/email"));

        ValidationContext copyVc = (ValidationContext) getField(copy, "validationContext");
        SchemaValidatorsConfig copyConfig = copyVc.getConfig();

        // Ensure values are equal initially
        assertEquals(mutableConfig.isTypeLoose(), copyConfig.isTypeLoose());

        // Mutate copy's config and verify parent config unchanged
        copyConfig.setTypeLoose(false);
        assertTrue(mutableConfig.isTypeLoose(),
                "Changing the copy's config must not affect the parent's config");
    }

    /* --------------------------------------------------------------------- *
     *  Multiple independent copies from the same parent
     * --------------------------------------------------------------------- */

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
        assertFalse(vcSecond.getConfig().isFailFast(),
                "Second copy's config must stay unchanged after mutating the first copy");
    }

    /* --------------------------------------------------------------------- *
     *  EvaluationParentSchema reference integrity
     * --------------------------------------------------------------------- */

    @Test
    void fromRef_evaluationParentSchema_isExactArgumentInstance() throws Exception {
        // Parent that itself has an evaluationParentSchema set (simulating a $ref chain)
        JsonSchema grandParent = createSchema(parentValidationContext, null);
        JsonSchema parent = createSchema(parentValidationContext, null);
        setField(parent, "evaluationParentSchema", grandParent);

        JsonSchema copy = originalSchema.fromRef(parent, JsonNodePath.of("/nested"));
        assertSame(parent, getField(copy, "evaluationParentSchema"),
                "The copy must reference the supplied parent, not the parent's own parent");
    }

    /* --------------------------------------------------------------------- *
     *  Exceptional cases caused by null internal structures
     * --------------------------------------------------------------------- */

    @Test
    void fromRef_parentWithNullValidationContext_throwsNullPointerException() throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        // force internal validationContext to null
        setField(parent, "validationContext", null);

        assertThrows(NullPointerException.class,
                () -> originalSchema.fromRef(parent, JsonNodePath.of("/any")),
                "Null validationContext in parent should cause NPE");
    }

    @Test
    void fromRef_parentWithNullConfig_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getConfig()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);

        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, JsonNodePath.of("/props")),
                "Null config in parent ValidationContext should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaReferences_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getSchemaReferences()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);

        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, JsonNodePath.of("/defs")),
                "Null schemaReferences map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullSchemaResources_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getSchemaResources()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);

        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, JsonNodePath.of("/resources")),
                "Null schemaResources map should cause IAE");
    }

    @Test
    void fromRef_parentWithNullDynamicAnchors_throwsIllegalArgumentException() throws Exception {
        when(parentValidationContext.getDynamicAnchors()).thenReturn(null);
        JsonSchema parent = createSchema(parentValidationContext, null);

        assertThrows(IllegalArgumentException.class,
                () -> originalSchema.fromRef(parent, JsonNodePath.of("/anchors")),
                "Null dynamicAnchors set should cause IAE");
    }

    /* --------------------------------------------------------------------- *
     *  Handling of complex JsonNodePath values
     * --------------------------------------------------------------------- */

    @ParameterizedTest
    @MethodSource("complexPathProvider")
    void fromRef_handlesComplexJsonNodePath(JsonNodePath path) throws Exception {
        JsonSchema parent = createSchema(parentValidationContext, null);
        JsonSchema copy = originalSchema.fromRef(parent, path);
        assertSame(path, getField(copy, "evaluationPath"));
    }

    static Stream<JsonNodePath> complexPathProvider() {
        return Stream.of(
                JsonNodePath.of("/properties/name~1details"), // escaped '/'
                JsonNodePath.empty()                         // root path
        );
    }
}
```