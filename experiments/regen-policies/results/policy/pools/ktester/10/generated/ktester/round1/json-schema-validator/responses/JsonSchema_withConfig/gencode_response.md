```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for {@link JsonSchema#withConfig(SchemaValidatorsConfig)} covering
 * edge‑cases described in the JSON test specification.
 *
 * <p>The test class lives in the same package as {@link JsonSchema} so that the
 * package‑private {@code JsonSchema.from(...)} factory method can be used.
 * Reflection is employed only where the test needs to inspect or modify private
 * state (e.g. forcing a {@code null} {@link ValidationContext}).</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_AdditionalTest {

    /** Shared objects used by many tests. */
    private static JsonMetaSchema metaSchema;
    private static JsonSchemaFactory factory;
    private static SchemaLocation schemaLocation;
    private static ObjectNode schemaNode;

    @BeforeAll
    static void beforeAll() {
        metaSchema = JsonMetaSchema.getV7();                 // draft‑07 meta‑schema
        factory = new JsonSchemaFactory();                    // default factory
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);
        schemaNode = new ObjectMapper().createObjectNode();   // empty schema node
    }

    /* --------------------------------------------------------------------- *
     * Helper utilities
     * --------------------------------------------------------------------- */

    /** Build a {@link JsonSchema} with the supplied {@link ValidationContext}. */
    private static JsonSchema buildSchema(ValidationContext ctx) {
        return JsonSchema.from(ctx,
                schemaLocation,
                null,               // evaluationPath – not needed for these tests
                schemaNode,
                null,               // parent
                false);             // suppressSubSchemaRetrieval
    }

    /** Reflectively read a private field from an object. */
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

    /** Reflectively set a private field on an object. */
    private static void setPrivateField(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new AssertionError("Unable to set private field '" + name + "'", e);
        }
    }

    /* --------------------------------------------------------------------- *
     * Test cases
     * --------------------------------------------------------------------- */

    @Test
    void withConfig_nullConfig_triggersCopyAndResetsFields() {
        // original context with a *non‑null* config
        SchemaValidatorsConfig originalConfig = new SchemaValidatorsConfig();
        ValidationContext originalCtx = new ValidationContext(
                metaSchema, factory, originalConfig,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        JsonSchema original = buildSchema(originalCtx);

        // invoke with null – should take the copy‑path
        JsonSchema copy = original.withConfig(null);

        assertNotSame(original, copy, "A new JsonSchema instance must be created");

        // the new ValidationContext must contain a *null* config
        assertNull(copy.getValidationContext().getConfig(),
                "The copied ValidationContext should hold a null config");

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
        ValidationContext ctx = new ValidationContext(
                metaSchema, factory, cfg,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        JsonSchema schema = buildSchema(ctx);

        // create a *different* instance that is equal according to .equals()
        SchemaValidatorsConfig equalButDistinct = new SchemaValidatorsConfig();
        // SchemaValidatorsConfig's equals is based on internal map; both are empty → equal
        assertEquals(cfg, equalButDistinct,
                "Pre‑condition: the two config instances must be equal");

        JsonSchema result = schema.withConfig(equalButDistinct);
        assertSame(schema, result,
                "When the supplied config is equal (even if distinct), the original instance must be returned");
    }

    @Test
    void withConfig_currentConfigNull_sameNull_throwsNPE() {
        // ValidationContext with a null config
        ValidationContext ctx = new ValidationContext(
                metaSchema, factory, null,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        JsonSchema schema = buildSchema(ctx);

        // calling withConfig(null) will attempt null.equals(null) → NPE
        assertThrows(NullPointerException.class,
                () -> schema.withConfig(null),
                "Calling withConfig(null) when the current config is null must throw NPE");
    }

    @Test
    void withConfig_currentConfigNull_nonNullProvided_throwsNPE() {
        // ValidationContext with a null config
        ValidationContext ctx = new ValidationContext(
                metaSchema, factory, null,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        JsonSchema schema = buildSchema(ctx);
        SchemaValidatorsConfig nonNull = new SchemaValidatorsConfig();

        // null.equals(nonNull) triggers NPE
        assertThrows(NullPointerException.class,
                () -> schema.withConfig(nonNull),
                "Calling withConfig(nonNull) when the current config is null must throw NPE");
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

        ValidationContext originalCtx = new ValidationContext(
                metaSchema, factory, new SchemaValidatorsConfig(),
                refs, resources, anchors);

        JsonSchema original = buildSchema(originalCtx);

        // supply a *different* config
        SchemaValidatorsConfig newCfg = new SchemaValidatorsConfig();

        JsonSchema copy = original.withConfig(newCfg);

        assertNotSame(original, copy, "A new instance must be created");

        ValidationContext copyCtx = copy.getValidationContext();

        // config must be the new one
        assertEquals(newCfg, copyCtx.getConfig());

        // all other fields must be the *same* objects (reference equality)
        assertSame(originalCtx.getMetaSchema(), copyCtx.getMetaSchema(),
                "MetaSchema must be preserved");
        assertSame(originalCtx.getJsonSchemaFactory(), copyCtx.getJsonSchemaFactory(),
                "JsonSchemaFactory must be preserved");
        assertSame(originalCtx.getSchemaReferences(), copyCtx.getSchemaReferences(),
                "SchemaReferences map must be preserved");
        assertSame(originalCtx.getSchemaResources(), copyCtx.getSchemaResources(),
                "SchemaResources map must be preserved");
        assertSame(originalCtx.getDynamicAnchors(), copyCtx.getDynamicAnchors(),
                "DynamicAnchors map must be preserved");

        // mutable validator fields must be reset (same checks as earlier)
        assertFalse(getPrivateField(copy, "validatorsLoaded"));
        assertNull(getPrivateField(copy, "requiredValidator"));
        assertNull(getPrivateField(copy, "typeValidator"));
        assertNull(getPrivateField(copy, "validators"));
    }

    @Test
    void withConfig_nullValidationContext_throwsNPE() throws Exception {
        // Build a normal schema first
        ValidationContext ctx = new ValidationContext(
                metaSchema, factory, new SchemaValidatorsConfig(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        JsonSchema schema = buildSchema(ctx);

        // Force the internal validationContext field to null via reflection
        setPrivateField(schema, "validationContext", null);

        // Any call to withConfig will now dereference a null field
        assertThrows(NullPointerException.class,
                () -> schema.withConfig(new SchemaValidatorsConfig()),
                "withConfig must throw NPE when internal validationContext is null");
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

        ValidationContext ctx = new ValidationContext(
                metaSchema, factory, badConfig,
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>(),
                new ConcurrentHashMap<>());

        JsonSchema schema = buildSchema(ctx);
        SchemaValidatorsConfig anyOther = new SchemaValidatorsConfig();

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> schema.withConfig(anyOther),
                "Exception from equals() must propagate");
        assertTrue(thrown.getMessage().contains("forced exception"),
                "The propagated exception should be the one thrown by equals()");
    }
}
```