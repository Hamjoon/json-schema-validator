```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for {@link JsonSchema#getValidationContext()}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JsonSchema.getValidationContext tests")
class JsonSchema_getValidationContext_Test {

    @Mock
    private ValidationContext validationContextMock;

    @Mock
    private JsonMetaSchema metaSchemaMock;

    @Mock
    private JsonNode schemaNodeMock;

    @Mock
    private JsonNodePath evaluationPathMock;

    private JsonSchema jsonSchema;

    // simple placeholder – constructor tolerates nulls
    private SchemaLocation dummyLocation;

    /**
     * a simple {@link ValidationContext} instance created with null meta‑schema and factory
     */
    private ValidationContext validationContext;

    /**
     * Simple concrete subclass used in a few tests.
     */
    class MyValidationContext extends ValidationContext {
        MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
            super(meta, null, cfg);
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            return new SchemaValidatorsConfig();
        }
    }

    /**
     * Simple concrete subclass used in a few tests.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- stub behaviour of ValidationContext and its collaborators -----
        // meta‑schema related calls
        when(validationContextMock.getMetaSchema()).thenReturn(metaSchemaMock);
        when(metaSchemaMock.readAnchor(any())).thenReturn(null);
        when(metaSchemaMock.readDynamicAnchor(any())).thenReturn(null);
        // map getters – use real concurrent maps to avoid NPEs on putIfAbsent
        when(validationContextMock.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());
        // other methods used in the constructor
        when(validationContextMock.resolveSchemaId(any())).thenReturn(null);

        // ----- create a JsonSchema instance via the package‑private factory method -----
        // SchemaLocation can be constructed with null arguments (the constructor does not enforce non‑null)
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        // parent schema – none for this test
        jsonSchema = JsonSchema.from(validationContextMock, schemaLocation, evaluationPathMock,
                schemaNodeMock, null, false);

        // first two arguments because the tests never exercise those parts.
        validationContext = new ValidationContext(null, null, new SchemaValidatorsConfig());

        // Create the primary JsonSchema instance with the real validationContext.
        jsonSchema = createJsonSchema(validationContext);
    }

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