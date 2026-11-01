package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

//     @Mock
    private ValidationContext mockValidationContext;

//     @Mock
    private JsonMetaSchema mockMetaSchema;

//     @Mock
    private JsonSchemaFactory mockFactory;

//     @Mock
    private JsonNode mockSchemaNode;

//     @Mock
    private JsonNodePath mockEvaluationPath;

    private JsonSchema jsonSchema;

//     @Mock
    private SchemaValidatorsConfig mockConfig;

    private SimpleValidationContext validationContext;

    private SimpleMetaSchema metaSchema;

    private SimpleFactory factory;

    private SimpleNodePath evaluationPath;

    /**
     * Simple stub for {@link ValidationContext}.
     */
    static class SimpleValidationContext extends ValidationContext {

        private JsonMetaSchema metaSchema;

        SimpleValidationContext(JsonMetaSchema metaSchema, JsonSchemaFactory factory) {
//             super(metaSchema, factory);
            this.metaSchema = metaSchema;
        }

        @Override
        public JsonMetaSchema getMetaSchema() {
            return metaSchema;
        }

        void setMetaSchema(JsonMetaSchema metaSchema) {
            this.metaSchema = metaSchema;
        }
    }

    /**
     * Minimal stub for {@link JsonMetaSchema}.
     */
//     static class SimpleMetaSchema extends JsonMetaSchema {
        // No additional behaviour required for the tests.
    }

    /**
     * Minimal stub for {@link JsonSchemaFactory}.
     */
//     static class SimpleFactory extends JsonSchemaFactory {
        // No behaviour required for the tests.
    }

    /**
     * Minimal stub for {@link JsonNodePath}.
     */
//     static class SimpleNodePath extends JsonNodePath {
        // No behaviour required for the tests.
    }

    /**
     * Minimal stub for {@link SchemaValidatorsConfig}.
     */
    static class SimpleConfig extends SchemaValidatorsConfig {
        // No behaviour required for the tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the library’s default meta‑schema and factory for a concrete ValidationContext.
//         JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V4);
//         JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V4);
//         validationContext = new ValidationContext(metaSchema, factory, new SchemaValidatorsConfig());
        // Build the JsonSchema instance using the package‑private factory method.
        SchemaLocation schemaLocation = new SchemaLocation(null, null);
        jsonSchema = JsonSchema.from(validationContext, schemaLocation, // evaluationPath – not required for these tests
        null, // schema node – not needed for these tests
        null, // parent schema
        null, false);
    }

    @Test
    void getValidationContext_returnsSameInstance() {
        ValidationContext returned = jsonSchema.getValidationContext();
        assertSame(validationContext, returned, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
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
        JsonSchema schema = JsonSchema.from(validationContext, new SchemaLocation(null, null), null, null, null, false);
        // Force the private field in BaseJsonValidator to null via reflection.
        Field field = BaseJsonValidator.class.getDeclaredField("validationContext");
        field.setAccessible(true);
        field.set(schema, null);
        // The getter should now return null.
        assertNull(schema.getValidationContext(), "When the internal validationContext field is null, getValidationContext() must return null");
    }

    @Test
    void getValidationContext_doesNotCreateCopy() {
        ValidationContext ctx = jsonSchema.getValidationContext();
        // Retrieve again and verify we still have the same reference.
        ValidationContext again = jsonSchema.getValidationContext();
        assertSame(ctx, again, "getValidationContext should return the stored reference, not a copy");
    }

    @Test
    void getValidationContext_inheritedSchemaViaFromRefSharesContext() {
        // Simulate a child schema creation via fromRef.
        JsonSchema child = jsonSchema.fromRef(jsonSchema, null);
        assertNotNull(child, "fromRef should produce a non‑null child schema");
        assertSame(validationContext, child.getValidationContext(), "Child schema created via fromRef must share the same ValidationContext as the parent");
    }

    @Test
    void getValidationContext_unchangedAfterWithConfig() {
        // Apply a custom configuration; the method returns a new JsonSchema instance.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonSchema configured = jsonSchema.withConfig(config);
        assertNotNull(configured, "withConfig should return a new JsonSchema instance");
        assertSame(validationContext, configured.getValidationContext(), "withConfig must preserve the original ValidationContext instance");
    }

//     @Test
//     void getValidationContext_unchangedAfterValidationExecution() {
//         // Build a schema with an empty object node – no validators will be generated.
//         ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();
//         JsonSchema simpleSchema = JsonSchema.from(validationContext, new SchemaLocation(null, null), null, emptySchemaNode, null, false);
//         // Perform a validation on a trivial JSON instance (empty object).
//         ObjectNode instance = JsonNodeFactory.instance.objectNode();
//         Set<ValidationMessage> messages = simpleSchema.validate(instance);
//         // Expect no validation messages.
//         assertTrue(messages.isEmpty(), "Validation of an empty schema against an empty instance should succeed");
//         // Ensure the ValidationContext is still the same instance.
//         assertSame(validationContext, simpleSchema.getValidationContext(), "Validation execution must not replace or modify the stored ValidationContext");
//     }

//     @Test
//     void getValidationContext_threadSafetyOfConcurrentReads() throws Exception {
//         final int THREAD_COUNT = 10;
//         ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
//         List<Future<ValidationContext>> futures = new ArrayList<>();
//         // Submit concurrent tasks that invoke getValidationContext().
//         for (int i = 0; i < THREAD_COUNT; i++) {
//             futures.add(executor.submit(() -> jsonSchema.getValidationContext()));
//         }
//         // Collect results.
//         List<ValidationContext> results = new ArrayList<>();
//         for (Future<ValidationContext> f : futures) {
//             results.add(f.get(5, TimeUnit.SECONDS));
//         }
//         executor.shutdownNow();
//         // All returned references must be identical to the original instance.
//         for (ValidationContext ctx : results) {
//             assertSame(validationContext, ctx, "All concurrent calls to getValidationContext must return the identical ValidationContext instance");
//         }
//     }
}