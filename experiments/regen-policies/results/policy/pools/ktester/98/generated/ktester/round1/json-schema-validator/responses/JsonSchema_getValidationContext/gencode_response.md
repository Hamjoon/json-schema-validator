```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit tests for {@link JsonSchema#getValidationContext()}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic return of the supplied ValidationContext (including null)</li>
 *   <li>inheritance of the context by child schemas created via {@code JsonSchema.from(...)}</li>
 *   <li>that {@code withConfig(...)} does not replace the context</li>
 *   <li>identity semantics (the same object reference is returned)</li>
 *   <li>preservation of a custom {@link ValidationContext} subclass</li>
 *   <li>that reference‑resolution helpers keep the original context</li>
 * </ul>
 */
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

    private JsonSchema jsonSchema;               // root schema used by many tests
    private SchemaLocation dummyLocation;        // simple placeholder – constructor tolerates nulls

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // ----- stub behaviour of ValidationContext and its collaborators -----
        when(validationContextMock.getMetaSchema()).thenReturn(metaSchemaMock);
        when(metaSchemaMock.readAnchor(any())).thenReturn(null);
        when(metaSchemaMock.readDynamicAnchor(any())).thenReturn(null);

        // Use real concurrent maps to avoid NPEs on internal puts
        when(validationContextMock.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(validationContextMock.getSchemaReferences()).thenReturn(new ConcurrentHashMap<>());

        // other methods used in the constructor
        when(validationContextMock.resolveSchemaId(any())).thenReturn(null);

        // ----- create a root JsonSchema via the package‑private factory method -----
        dummyLocation = new SchemaLocation(null, null);
        jsonSchema = JsonSchema.from(
                validationContextMock,
                dummyLocation,
                evaluationPathMock,
                schemaNodeMock,
                null,               // no parent – this is the root
                false               // suppressSubSchemaRetrieval
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------
     *  Basic return‑value tests
     * -------------------------------------------------------------- */

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContextMock, returned,
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
        JsonSchema schemaWithNullContext = JsonSchema.from(
                null,                     // no ValidationContext supplied
                dummyLocation,
                evaluationPathMock,
                schemaNodeMock,
                null,
                false);

        // The factory does not initialise the field, so we expect null.
        ValidationContext ctx = schemaWithNullContext.getValidationContext();
        assertNull(ctx, "When constructed with a null ValidationContext the getter should return null");
    }

    /* --------------------------------------------------------------
     *  Child schema inheritance
     * -------------------------------------------------------------- */

    @Test
    void childSchema_inheritsValidationContextFromParent() {
        // Create a child schema via the same factory method, passing the parent.
        JsonSchema child = JsonSchema.from(
                null,                         // explicit null – factory should fall back to parent
                dummyLocation,
                evaluationPathMock,
                schemaNodeMock,
                jsonSchema,                   // parent schema
                false);

        assertSame(validationContextMock, child.getValidationContext(),
                "A child JsonSchema created with a null context should inherit the parent's ValidationContext");
    }

    /* --------------------------------------------------------------
     *  withConfig does not replace the context
     * -------------------------------------------------------------- */

    @Test
    void withConfig_retainsOriginalValidationContext() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        JsonSchema configured = jsonSchema.withConfig(cfg);
        assertSame(validationContextMock, configured.getValidationContext(),
                "withConfig must keep the original ValidationContext instance");
    }

    /* --------------------------------------------------------------
     *  Identity check (already covered, but kept for explicitness)
     * -------------------------------------------------------------- */

    @Test
    void getValidationContext_identityCheck() {
        ValidationContext ctx = jsonSchema.getValidationContext();
        assertTrue(ctx == validationContextMock,
                "The returned ValidationContext must be the exact same object (==) as supplied");
    }

    /* --------------------------------------------------------------
     *  Custom ValidationContext subclass preservation
     * -------------------------------------------------------------- */

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

    @Test
    void customValidationContextSubclass_isPreserved() {
        // Build a real (non‑mock) ValidationContext subclass instance.
        MyValidationContext customCtx = new MyValidationContext(metaSchemaMock, new SchemaValidatorsConfig());

        JsonSchema schema = JsonSchema.from(
                customCtx,
                dummyLocation,
                evaluationPathMock,
                schemaNodeMock,
                null,
                false);

        ValidationContext returned = schema.getValidationContext();
        assertSame(customCtx, returned,
                "The getter must return the exact custom ValidationContext instance supplied");
        assertTrue(returned instanceof MyValidationContext,
                "The concrete type of the returned context should be the custom subclass");
    }

    /* --------------------------------------------------------------
     *  Reference‑resolution helpers keep the original context
     * -------------------------------------------------------------- */

    @Test
    void fromRef_retainsOriginalValidationContext() {
        // Simulate a $ref resolution using the public fromRef helper.
        JsonSchema refSchema = jsonSchema.fromRef(jsonSchema, evaluationPathMock);
        assertSame(validationContextMock, refSchema.getValidationContext(),
                "A schema obtained via fromRef should retain the original ValidationContext");
    }
}
```