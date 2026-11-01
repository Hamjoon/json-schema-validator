```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#fromRef(JsonSchema, JsonNodePath)}.
 * <p>
 * The test creates a real {@code JsonSchema} instance via reflection (the
 * constructor is private) and then invokes {@code fromRef}.  The returned
 * copy is examined with reflection to verify that the evaluation path and
 * parent schema are set correctly and that the mutable validator state is
 * reset.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_fromRef_Test {

    /** Common mocks used for all tests */
    @Mock ValidationContext validationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory jsonSchemaFactory;
    @Mock SchemaValidatorsConfig config;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNode schemaNode;

    /** The original schema on which {@code fromRef} will be called */
    JsonSchema originalSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // no global resources required
    }

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
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);

        // a minimal (empty) evaluation path – the concrete value is irrelevant for the
        // creation of the original schema.
        JsonNodePath emptyPath = new JsonNodePath(JsonNodePath.PathType.JSON);

        originalSchema = ctor.newInstance(
                validationContext,
                schemaLocation,
                emptyPath,
                schemaNode,
                null,                     // no parent
                false);                   // suppressSubSchemaRetrieval
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    @Test
    void fromRef_shouldCreateCopyWithCorrectEvaluationState() throws Exception {
        // ----- prepare parent schema (also via reflection) -----
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonNodePath parentPath = new JsonNodePath(JsonNodePath.PathType.JSON);
        JsonSchema parentSchema = ctor.newInstance(
                validationContext,
                schemaLocation,
                parentPath,
                schemaNode,
                null,
                false);

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
}
```