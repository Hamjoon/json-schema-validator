```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tests for {@link JsonSchema#getValidationContext()} and related behaviour.
 * <p>
 * The original test relied on Mockito, which is not available in this
 * compilation environment.  The tests have been rewritten to use plain
 * reflection and real objects where possible.
 */
@DisplayName("JsonSchema.getValidationContext tests")
class JsonSchema_getValidationContext_Test {

    /** a dummy {@link SchemaLocation} – the constructor tolerates null arguments */
    private final SchemaLocation dummyLocation = new SchemaLocation(null, null);

    /** a simple {@link ValidationContext} instance created with null meta‑schema and factory */
    private ValidationContext validationContext;

    /** the primary {@link JsonSchema} instance used by most tests */
    private JsonSchema jsonSchema;

    /**
     * Helper that creates a {@link JsonSchema} instance via reflection.
     * The constructor of {@code JsonSchema} is package‑private and expects many
     * arguments; we bypass this by passing {@code null} for all parameters
     * and then manually inject the desired {@code ValidationContext}.
     *
     * @param ctx the {@code ValidationContext} to associate with the schema
     * @return a new {@code JsonSchema} instance
     * @throws Exception if reflection fails
     */
    private JsonSchema createJsonSchema(Object ctx) throws Exception {
        // Find any constructor (there is at least one) and make it accessible.
        Constructor<?> ctor = JsonSchema.class.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        // Fill the argument array with nulls – the constructor tolerates them.
        Object[] args = new Object[ctor.getParameterCount()];
        JsonSchema instance = (JsonSchema) ctor.newInstance(args);
        // Inject the validationContext field.
        Field f = JsonSchema.class.getDeclaredField("validationContext");
        f.setAccessible(true);
        f.set(instance, ctx);
        return instance;
    }

    @BeforeEach
    void setUp() throws Exception {
        // ValidationContext has a public constructor that accepts (JsonMetaSchema,
        // JsonSchemaFactory, SchemaValidatorsConfig).  We can safely pass nulls for the
        // first two arguments because the tests never exercise those parts.
        validationContext = new ValidationContext(null, null, new SchemaValidatorsConfig());

        // Create the primary JsonSchema instance with the real validationContext.
        jsonSchema = createJsonSchema(validationContext);
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContext, returned,
                "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCalls_returnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second,
                "Multiple invocations of getValidationContext should return the identical instance");
    }

    @Test
    void getValidationContext_whenContextIsNull_returnsNull() throws Exception {
        // Build a schema where the internal field is deliberately set to null.
        JsonSchema schemaWithNullContext = createJsonSchema(null);
        ValidationContext ctx = schemaWithNullContext.getValidationContext();
        assertNull(ctx, "When constructed with a null ValidationContext the getter should return null");
    }

    @Test
    void childSchema_inheritsValidationContextFromParent() throws Exception {
        // Create a child schema with a null context.
        JsonSchema child = createJsonSchema(null);

        // The JsonSchema class stores its parent in a field named “parentSchema”.
        // If the field name changes in the library, this reflection will need to be updated.
        try {
            Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
            parentField.setAccessible(true);
            parentField.set(child, jsonSchema);
        } catch (NoSuchFieldException e) {
            // If the field does not exist, the fallback behaviour is not testable.
            // In that case we simply fail the test.
            fail("Unable to locate parentSchema field in JsonSchema: " + e.getMessage());
        }

        assertSame(validationContext, child.getValidationContext(),
                "A child JsonSchema created with a null context should inherit the parent's ValidationContext");
    }

    @Test
    void withConfig_retainsOriginalValidationContext() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema configured = jsonSchema.withConfig(cfg);
        assertSame(validationContext, configured.getValidationContext(),
                "withConfig must keep the original ValidationContext instance");
    }

    @Test
    void getValidationContext_identityCheck() {
        ValidationContext ctx = jsonSchema.getValidationContext();
        assertTrue(ctx == validationContext,
                "The returned ValidationContext must be the exact same object (==) as supplied");
    }

    @Test
    void customValidationContextSubclass_isPreserved() throws Exception {
        // Build a real (non‑mock) ValidationContext subclass instance.
        class MyValidationContext extends ValidationContext {
            MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
                super(meta, null, cfg);
            }

            @Override
            public SchemaValidatorsConfig getConfig() {
                return new SchemaValidatorsConfig();
            }
        }

        MyValidationContext customCtx = new MyValidationContext(null, new SchemaValidatorsConfig());
        JsonSchema schema = createJsonSchema(customCtx);
        ValidationContext returned = schema.getValidationContext();
        assertSame(customCtx, returned,
                "The getter must return the exact custom ValidationContext instance supplied");
        assertTrue(returned instanceof MyValidationContext,
                "The concrete type of the returned context should be the custom subclass");
    }

    @Test
    void fromRef_retainsOriginalValidationContext() {
        // The fromRef method expects a JsonNodePath; passing null is acceptable for this test.
        JsonSchema refSchema = jsonSchema.fromRef(jsonSchema, null);
        assertSame(validationContext, refSchema.getValidationContext(),
                "A schema obtained via fromRef should retain the original ValidationContext");
    }
}
```