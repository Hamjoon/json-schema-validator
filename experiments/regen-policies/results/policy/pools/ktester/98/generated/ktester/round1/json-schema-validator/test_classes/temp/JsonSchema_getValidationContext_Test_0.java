package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
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
     * A minimal concrete subclass of {@link ValidationContext}. The real
     * ValidationContext class has a public constructor that takes a
     * {@link JsonMetaSchema} and a {@link SchemaValidatorsConfig}. For the
     * purpose of this test we use the same constructor signature.
     */
    static class MyValidationContext extends ValidationContext {

        MyValidationContext(JsonMetaSchema meta, SchemaValidatorsConfig cfg) {
            super(meta, cfg);
        }

        @Override
        public SchemaValidatorsConfig getConfig() {
            // Return a custom config to prove the subclass is really used.
            return new SchemaValidatorsConfig();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
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
        jsonSchema = JsonSchema.from(validationContextMock, schemaLocation, evaluationPathMock, schemaNodeMock, // parent schema – none for this test
        null, // suppressSubSchemaRetrieval
        false);
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContextMock, returned, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCalls_returnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second, "Multiple invocations of getValidationContext should return the identical instance");
    }

    @Test
    void getValidationContext_whenContextIsNull_returnsNull() throws Exception {
        // Build a schema where the internal field is deliberately set to null.
        JsonSchema schemaWithNullContext = JsonSchema.from(// no ValidationContext supplied
        null, dummyLocation, evaluationPathMock, schemaNodeMock, null, false);
        // The factory does not initialise the field, so we expect null.
        ValidationContext ctx = schemaWithNullContext.getValidationContext();
        assertNull(ctx, "When constructed with a null ValidationContext the getter should return null");
    }

    @Test
    void childSchema_inheritsValidationContextFromParent() {
        // Create a child schema via the same factory method, passing the parent.
        JsonSchema child = JsonSchema.from(// explicit null – factory should fall back to parent
        null, dummyLocation, evaluationPathMock, schemaNodeMock, // parent schema
        jsonSchema, false);
        assertSame(validationContextMock, child.getValidationContext(), "A child JsonSchema created with a null context should inherit the parent's ValidationContext");
    }

    @Test
    void withConfig_retainsOriginalValidationContext() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema configured = jsonSchema.withConfig(cfg);
        assertSame(validationContextMock, configured.getValidationContext(), "withConfig must keep the original ValidationContext instance");
    }

    @Test
    void getValidationContext_identityCheck() {
        ValidationContext ctx = jsonSchema.getValidationContext();
        assertTrue(ctx == validationContextMock, "The returned ValidationContext must be the exact same object (==) as supplied");
    }

    @Test
    void customValidationContextSubclass_isPreserved() {
        // Build a real (non‑mock) ValidationContext subclass instance.
        MyValidationContext customCtx = new MyValidationContext(metaSchemaMock, new SchemaValidatorsConfig());
        JsonSchema schema = JsonSchema.from(customCtx, dummyLocation, evaluationPathMock, schemaNodeMock, null, false);
        ValidationContext returned = schema.getValidationContext();
        assertSame(customCtx, returned, "The getter must return the exact custom ValidationContext instance supplied");
        assertTrue(returned instanceof MyValidationContext, "The concrete type of the returned context should be the custom subclass");
    }

    @Test
    void fromRef_retainsOriginalValidationContext() {
        // Simulate a $ref resolution using the public fromRef helper.
        JsonSchema refSchema = jsonSchema.fromRef(jsonSchema, evaluationPathMock);
        assertSame(validationContextMock, refSchema.getValidationContext(), "A schema obtained via fromRef should retain the original ValidationContext");
    }
}
