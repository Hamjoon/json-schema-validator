package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    private ValidationContext mockValidationContext;

    @Mock
    private JsonMetaSchema mockMetaSchema;

    @Mock
    private JsonSchemaFactory mockFactory;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonNodePath mockEvaluationPath;

    private JsonSchema jsonSchema;

    @Mock
    private SchemaValidatorsConfig mockConfig;

    @BeforeEach
    void setupBeforeEach() {
        // Stub the ValidationContext behaviour that is exercised by the JsonSchema constructor.
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);
        when(mockValidationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(mockValidationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(mockValidationContext.resolveSchemaId(any())).thenReturn(null);
        // The meta‑schema is queried for anchors; we return null to keep the flow simple.
        when(mockMetaSchema.readAnchor(any())).thenReturn(null);
        when(mockMetaSchema.readDynamicAnchor(any())).thenReturn(null);
        // A minimal SchemaLocation – both arguments may be null for the purpose of this test.
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        // Build the JsonSchema instance using the package‑private factory method.
        jsonSchema = JsonSchema.from(mockValidationContext, schemaLocation, mockEvaluationPath, mockSchemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval
        false);
        // Stub the minimal behaviour required by the JsonSchema constructor.
        when(mockValidationContext.getMetaSchema()).thenReturn(mockMetaSchema);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(mockFactory);
        when(mockValidationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(mockValidationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        when(mockValidationContext.resolveSchemaId(any())).thenReturn(null);
        when(mockMetaSchema.readAnchor(any())).thenReturn(null);
        when(mockMetaSchema.readDynamicAnchor(any())).thenReturn(null);
        // Minimal SchemaLocation – both arguments may be null for these tests.
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        // Build a JsonSchema instance using the package‑private factory method.
        jsonSchema = JsonSchema.from(mockValidationContext, schemaLocation, mockEvaluationPath, mockSchemaNode, // no parent schema
        null, // suppressSubSchemaRetrieval
        false);
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(mockValidationContext, returned, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    @Test
    void getValidationContext_multipleCallsReturnSameInstance() {
        ValidationContext first = jsonSchema.getValidationContext();
        ValidationContext second = jsonSchema.getValidationContext();
        assertSame(first, second, "Multiple invocations of getValidationContext should return the identical instance");
    }

    @Test
    void getValidationContext_returnsNullWhenFieldIsNull() throws Exception {
        // Create a fresh JsonSchema instance.
        JsonSchema schema = JsonSchema.from(mockValidationContext, new SchemaLocation(null, null), mockEvaluationPath, mockSchemaNode, null, false);
        // Force the private field in BaseJsonValidator to null via reflection.
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(schema, null);
        // The getter should now return null.
        assertNull(schema.getValidationContext(), "When the internal validationContext field is null, getValidationContext() must return null");
    }

    @Test
    void getValidationContext_doesNotCreateCopy() {
        // The mock ValidationContext is mutable (we can add behaviour later if needed).
        ValidationContext ctx = jsonSchema.getValidationContext();
        // Mutate the mock by adding a stubbed method call.
        when(ctx.getMetaSchema()).thenReturn(mockMetaSchema);
        // Retrieve again and verify we still have the same reference (hence the mutation is visible).
        ValidationContext again = jsonSchema.getValidationContext();
        assertSame(ctx, again, "getValidationContext should return the stored reference, not a copy");
        // Verify that the mutation (stub) is effective.
        assertSame(mockMetaSchema, again.getMetaSchema(), "Mutations on the returned ValidationContext must be reflected on subsequent calls");
    }

    @Test
    void getValidationContext_inheritedSchemaViaFromRefSharesContext() {
        // Simulate a child schema creation via fromRef.
        // The method signature: JsonSchema fromRef(JsonSchema refEvaluationParentSchema, JsonNodePath refEvaluationPath)
        JsonSchema child = jsonSchema.fromRef(jsonSchema, mockEvaluationPath);
        assertNotNull(child, "fromRef should produce a non‑null child schema");
        assertSame(mockValidationContext, child.getValidationContext(), "Child schema created via fromRef must share the same ValidationContext as the parent");
    }

    @Test
    void getValidationContext_unchangedAfterWithConfig() {
        // Apply a custom configuration; the method returns a new JsonSchema instance.
        JsonSchema configured = jsonSchema.withConfig(mockConfig);
        assertNotNull(configured, "withConfig should return a new JsonSchema instance");
        assertSame(mockValidationContext, configured.getValidationContext(), "withConfig must preserve the original ValidationContext instance");
    }

    @Test
    void getValidationContext_unchangedAfterValidationExecution() {
        // Build a schema with an empty object node – no validators will be generated.
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema simpleSchema = JsonSchema.from(mockValidationContext, new SchemaLocation(null, null), mockEvaluationPath, emptySchemaNode, null, false);
        // Perform a validation on a trivial JSON instance.
        // empty JSON object
        ObjectNode instance = JsonNodeFactory.instance.objectNode();
        Set<ValidationMessage> messages = simpleSchema.validate(instance);
        // Expect no validation messages.
        assertTrue(messages.isEmpty(), "Validation of an empty schema against an empty instance should succeed");
        // Ensure the ValidationContext is still the same instance.
        assertSame(mockValidationContext, simpleSchema.getValidationContext(), "Validation execution must not replace or modify the stored ValidationContext");
    }

    @Test
    void getValidationContext_threadSafetyOfConcurrentReads() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ValidationContext>> futures = new ArrayList<>();
        // Submit concurrent tasks that invoke getValidationContext().
        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> jsonSchema.getValidationContext()));
        }
        // Collect results.
        List<ValidationContext> results = new ArrayList<>();
        for (Future<ValidationContext> f : futures) {
            results.add(f.get(5, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        // All returned references must be identical to the original mock.
        for (ValidationContext ctx : results) {
            assertSame(mockValidationContext, ctx, "All concurrent calls to getValidationContext must return the identical ValidationContext instance");
        }
    }
}
