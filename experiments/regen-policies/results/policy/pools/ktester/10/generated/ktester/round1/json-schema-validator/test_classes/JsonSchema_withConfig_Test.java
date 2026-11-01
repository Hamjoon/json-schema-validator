package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    /**
     * Shared objects used by all tests.
     */
    private static ValidationContext validationContext;

    private static SchemaLocation schemaLocation;

    private static ObjectNode schemaNode;

    /**
     * Shared objects used by many tests.
     */
    private static JsonMetaSchema metaSchema;

    private static JsonSchemaFactory factory;

    class BadEqualsConfig extends SchemaValidatorsConfig {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("forced exception from equals");
        }
    }

    class BadEqualsConfig extends SchemaValidatorsConfig {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("forced exception from equals");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // a minimal, real ValidationContext – it holds a meta‑schema, a factory and a config
        // default draft‑07 meta‑schema
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        // default factory
        JsonSchemaFactory factory = new JsonSchemaFactory();
        // default (empty) config
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, cfg, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        // a simple absolute IRI for the schema location
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);
        // an empty schema node – sufficient for construction
        schemaNode = new ObjectMapper().createObjectNode();
        // draft‑07 meta‑schema
        metaSchema = JsonMetaSchema.getV7();
        // default factory
        factory = new JsonSchemaFactory();
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);
        // empty schema node
        schemaNode = new ObjectMapper().createObjectNode();
    }

    /**
     * Build a {@link JsonSchema} with the supplied {@link ValidationContext}.
     */
    private static JsonSchema buildSchema(ValidationContext ctx) {
        return JsonSchema.from(ctx, schemaLocation, // evaluationPath – not needed for these tests
        null, schemaNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
    }

    /**
     * Reflectively set a private field on an object.
     */
    private static void setPrivateField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new AssertionError("Unable to set private field '" + name + "'", e);
        }
    }

    /**
     * When the supplied configuration is the same (by {@code equals}) as the one
     * stored in the schema's {@link ValidationContext}, {@code withConfig}
     * must return the original instance.
     */
    @Test
    void withConfig_sameConfig_returnsSameInstance() {
        // the schema is built with the *same* config object that we will pass later
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, // evaluationPath – not needed for this test
        null, schemaNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
        // identical object
        SchemaValidatorsConfig sameConfig = validationContext.getConfig();
        JsonSchema result = schema.withConfig(sameConfig);
        assertSame(schema, result, "withConfig should return the original JsonSchema when the config is unchanged");
    }

    /**
     * When a different configuration is supplied, a new {@link JsonSchema}
     * instance must be created. The new instance must contain the supplied
     * configuration.
     */
    @Test
    void withConfig_differentConfig_returnsNewInstance() {
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, null, schemaNode, null, false);
        // a *different* configuration object – default config is fine, just a new instance
        SchemaValidatorsConfig newConfig = new SchemaValidatorsConfig();
        // ensure the new config is not equal to the original one (different reference)
        assertNotEquals(validationContext.getConfig(), newConfig, "pre‑condition: the new config must differ from the original config");
        JsonSchema result = schema.withConfig(newConfig);
        assertNotSame(schema, result, "withConfig should create a new JsonSchema when the config differs");
        // verify that the new schema really carries the new configuration
        assertEquals(newConfig, result.getValidationContext().getConfig(), "The new JsonSchema must contain the supplied configuration");
    }

    /**
     * Reflectively read a private field from an object.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Unable to read private field '" + name + "'", e);
        }
    }

    @Test
    void withConfig_nullConfig_triggersCopyAndResetsFields() {
        // original context with a *non‑null* config
        SchemaValidatorsConfig originalConfig = new SchemaValidatorsConfig();
        ValidationContext originalCtx = new ValidationContext(metaSchema, factory, originalConfig, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonSchema original = buildSchema(originalCtx);
        // invoke with null – should take the copy‑path
        JsonSchema copy = original.withConfig(null);
        assertNotSame(original, copy, "A new JsonSchema instance must be created");
        // the new ValidationContext must contain a *null* config
        assertNull(copy.getValidationContext().getConfig(), "The copied ValidationContext should hold a null config");
        // mutable validator fields must be reset
        assertFalse(getPrivateField(copy, "validatorsLoaded"));
        assertNull(getPrivateField(copy, "requiredValidator"));
        assertNull(getPrivateField(copy, "typeValidator"));
        assertNull(getPrivateField(copy, "validators"));
    }

    @Test
    void withConfig_equalDifferentInstance_returnsSameInstance() {
        // original config
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        ValidationContext ctx = new ValidationContext(metaSchema, factory, cfg, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonSchema schema = buildSchema(ctx);
        // create a *different* instance that is equal according to .equals()
        SchemaValidatorsConfig equalButDistinct = new SchemaValidatorsConfig();
        // SchemaValidatorsConfig's equals is based on internal map; both are empty → equal
        assertEquals(cfg, equalButDistinct, "Pre‑condition: the two config instances must be equal");
        JsonSchema result = schema.withConfig(equalButDistinct);
        assertSame(schema, result, "When the supplied config is equal (even if distinct), the original instance must be returned");
    }

    @Test
    void withConfig_currentConfigNull_sameNull_throwsNPE() {
        // ValidationContext with a null config
        ValidationContext ctx = new ValidationContext(metaSchema, factory, null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonSchema schema = buildSchema(ctx);
        // calling withConfig(null) will attempt null.equals(null) → NPE
        assertThrows(NullPointerException.class, () -> schema.withConfig(null), "Calling withConfig(null) when the current config is null must throw NPE");
    }

    @Test
    void withConfig_currentConfigNull_nonNullProvided_throwsNPE() {
        // ValidationContext with a null config
        ValidationContext ctx = new ValidationContext(metaSchema, factory, null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonSchema schema = buildSchema(ctx);
        SchemaValidatorsConfig nonNull = new SchemaValidatorsConfig();
        // null.equals(nonNull) triggers NPE
        assertThrows(NullPointerException.class, () -> schema.withConfig(nonNull), "Calling withConfig(nonNull) when the current config is null must throw NPE");
    }

    @Test
    void withConfig_copyPreservesOtherContextFields() {
        // original context with some non‑empty maps to be able to check reference equality
        ConcurrentHashMap<String, JsonSchema> refs = new ConcurrentHashMap<>();
        refs.put("ref", null);
        ConcurrentHashMap<String, JsonSchema> resources = new ConcurrentHashMap<>();
        resources.put("res", null);
        ConcurrentHashMap<String, JsonSchema> anchors = new ConcurrentHashMap<>();
        anchors.put("anchor", null);
        ValidationContext originalCtx = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig(), refs, resources, anchors);
        JsonSchema original = buildSchema(originalCtx);
        // supply a *different* config
        SchemaValidatorsConfig newCfg = new SchemaValidatorsConfig();
        JsonSchema copy = original.withConfig(newCfg);
        assertNotSame(original, copy, "A new instance must be created");
        ValidationContext copyCtx = copy.getValidationContext();
        // config must be the new one
        assertEquals(newCfg, copyCtx.getConfig());
        // all other fields must be the *same* objects (reference equality)
        assertSame(originalCtx.getMetaSchema(), copyCtx.getMetaSchema(), "MetaSchema must be preserved");
        assertSame(originalCtx.getJsonSchemaFactory(), copyCtx.getJsonSchemaFactory(), "JsonSchemaFactory must be preserved");
        assertSame(originalCtx.getSchemaReferences(), copyCtx.getSchemaReferences(), "SchemaReferences map must be preserved");
        assertSame(originalCtx.getSchemaResources(), copyCtx.getSchemaResources(), "SchemaResources map must be preserved");
        assertSame(originalCtx.getDynamicAnchors(), copyCtx.getDynamicAnchors(), "DynamicAnchors map must be preserved");
        // mutable validator fields must be reset (same checks as earlier)
        assertFalse(getPrivateField(copy, "validatorsLoaded"));
        assertNull(getPrivateField(copy, "requiredValidator"));
        assertNull(getPrivateField(copy, "typeValidator"));
        assertNull(getPrivateField(copy, "validators"));
    }

    @Test
    void withConfig_nullValidationContext_throwsNPE() throws Exception {
        // Build a normal schema first
        ValidationContext ctx = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonSchema schema = buildSchema(ctx);
        // Force the internal validationContext field to null via reflection
        setPrivateField(schema, "validationContext", null);
        // Any call to withConfig will now dereference a null field
        assertThrows(NullPointerException.class, () -> schema.withConfig(new SchemaValidatorsConfig()), "withConfig must throw NPE when internal validationContext is null");
    }

    @Test
    void withConfig_currentConfigEqualsThrows_propagatesRuntimeException() {
        // Custom config whose equals throws RuntimeException
        class BadEqualsConfig extends SchemaValidatorsConfig {

            @Override
            public boolean equals(Object o) {
                throw new RuntimeException("forced exception from equals");
            }
        }
        BadEqualsConfig badConfig = new BadEqualsConfig();
        ValidationContext ctx = new ValidationContext(metaSchema, factory, badConfig, new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
        JsonSchema schema = buildSchema(ctx);
        SchemaValidatorsConfig anyOther = new SchemaValidatorsConfig();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> schema.withConfig(anyOther), "Exception from equals() must propagate");
        assertTrue(thrown.getMessage().contains("forced exception"), "The propagated exception should be the one thrown by equals()");
    }
}
