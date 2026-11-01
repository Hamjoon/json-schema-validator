package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;

class AnyOfValidator_preloadJsonSchema_Test {

    //     @Mock
    ValidationContext validationContext;

    //     @Mock
    SchemaValidatorsConfig config;

    //     @Mock
    JsonSchema mockSchema1;

    //     @Mock
    JsonSchema mockSchema2;

    AnyOfValidator anyOfValidator;

    /**
     * Minimal {@link JsonSchema} implementation that records calls to
     * {@code preloadJsonSchema()}.
     */
    private static class SimpleJsonSchema extends JsonSchema {

        private static final ValidationContext DUMMY_CONTEXT = new StubValidationContext(new StubSchemaValidatorsConfig(), new ArrayDeque<>());

        private int preloadCount = 0;

        SimpleJsonSchema() {
            // The current library constructor is:
            // JsonSchema(JsonSchemaFactory factory,
            //           JsonNode schemaNode,
            //           JsonSchema parentSchema,
            //           ValidationContext validationContext,
            //           boolean collectAnnotations)
            // All arguments except the validation context are irrelevant for our tests.
            super(null, null, null, DUMMY_CONTEXT, false);
        }

        @Override
        public void preloadJsonSchema() {
            preloadCount++;
        }

        int getPreloadCount() {
            return preloadCount;
        }

        /* ----------------------------------------------------------------- */
        /* Trivial implementations of the remaining abstract methods.        */
        /* ----------------------------------------------------------------- */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> walk(ExecutionContext ctx, JsonNode node, JsonNode rootNode, String at, boolean walkAll) {
            return Collections.emptySet();
        }

        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public String getSchemaPath() {
            return null;
        }

        @Override
        public JsonSchema getParentSchema() {
            return null;
        }
    }

    /**
     * Minimal {@link SchemaValidatorsConfig} implementation used by the tests.
     */
    private static class StubSchemaValidatorsConfig extends SchemaValidatorsConfig {

        @Override
        public boolean isOpenAPI3StyleDiscriminators() {
            return false;
        }
        /* All other methods inherit the default behaviour. */
    }

    /**
     * Minimal {@link ValidationContext} implementation that returns
     * pre‑programmed schemas.
     */
    private static class StubValidationContext extends ValidationContext {

        private final Queue<JsonSchema> queue;

        StubValidationContext(SchemaValidatorsConfig config, Queue<JsonSchema> queue) {
            // Newer versions of the library require a richer constructor.
            // The first four arguments are not needed for these tests.
            super(null, null, null, null, config);
            this.queue = queue;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode schemaNode, JsonSchema parent) {
            // Return the next pre‑programmed mock
            return queue.poll();
        }
    }

    /**
     * Subclass of {@link AnyOfValidator} that delegates the preload behaviour
     * to a supplied {@link Consumer}.
     */
    private static class TestAnyOfValidator extends AnyOfValidator {

        private final Consumer<List<JsonSchema>> behaviour;

        TestAnyOfValidator(String path, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext, Consumer<List<JsonSchema>> behaviour) {
            super(path, schemaNode, parentSchema, validationContext);
            this.behaviour = behaviour;
        }

        @Override
        protected void preloadJsonSchemas(List<JsonSchema> schemas) {
            // The original implementation loads schemas from the schema node.
            // For the purpose of these tests we replace that logic with the
            // supplied consumer.
            behaviour.accept(schemas);
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- stubbed behaviour for ValidationContext -----
        config = new StubSchemaValidatorsConfig();
        mockSchema1 = new SimpleJsonSchema();
        mockSchema2 = new SimpleJsonSchema();
        Queue<JsonSchema> queue = new ArrayDeque<>();
        queue.add(mockSchema1);
        queue.add(mockSchema2);
        validationContext = new StubValidationContext(config, queue);
        // ----- build a simple schema node that contains two subschemas -----
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());
        anyOfValidator = new AnyOfValidator("testPath", schemaArray, null, validationContext);
    }

    /**
     * Reflectively obtains the private {@code schemas} field from the validator.
     */
    @SuppressWarnings("unchecked")
    private List<JsonSchema> getSchemas(AnyOfValidator validator) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        return (List<JsonSchema>) f.get(validator);
    }

    @Test
    void preloadWithPrePopulatedSchemasList() throws Exception {
        Consumer<List<JsonSchema>> addOneMore = list -> list.add(mockSchema1);
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addOneMore);
        List<JsonSchema> internal = getSchemas(validator);
        internal.add(mockSchema1);
        internal.add(mockSchema2);
        assertEquals(2, internal.size());
        validator.preloadJsonSchema();
        // the overridden behaviour adds one more element
        assertEquals(3, internal.size());
        assertTrue(internal.contains(mockSchema1));
        assertTrue(internal.contains(mockSchema2));
    }

    @Test
    void exceptionPropagationFromPreloadJsonSchemas() {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            throw new RuntimeException("boom");
        });
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, validator::preloadJsonSchema);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void multipleConsecutivePreloadCalls() throws Exception {
        Consumer<List<JsonSchema>> addTwo = list -> {
            SimpleJsonSchema s1 = new SimpleJsonSchema();
            SimpleJsonSchema s2 = new SimpleJsonSchema();
            list.add(s1);
            list.add(s2);
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addTwo);
        validator.preloadJsonSchema();
        assertEquals(2, getSchemas(validator).size());
        validator.preloadJsonSchema();
        assertEquals(4, getSchemas(validator).size());
    }

    @Test
    void preloadClearsExistingSchemas() throws Exception {
        Consumer<List<JsonSchema>> clear = List::clear;
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, clear);
        List<JsonSchema> internal = getSchemas(validator);
        internal.add(new SimpleJsonSchema());
        internal.add(new SimpleJsonSchema());
        internal.add(new SimpleJsonSchema());
        assertEquals(3, internal.size());
        validator.preloadJsonSchema();
        assertTrue(internal.isEmpty(), "list should be empty after clear");
    }

    @Test
    void preloadAddsNullSchema() throws Exception {
        Consumer<List<JsonSchema>> addNull = list -> list.add(null);
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addNull);
        int before = getSchemas(validator).size();
        validator.preloadJsonSchema();
        List<JsonSchema> after = getSchemas(validator);
        assertEquals(before + 1, after.size());
        assertNull(after.get(after.size() - 1));
    }

    @Test
    void preloadWithLargeNumberOfSchemas() throws Exception {
        Consumer<List<JsonSchema>> addHundred = list -> {
            for (int i = 0; i < 100; i++) {
                list.add(new SimpleJsonSchema());
            }
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addHundred);
        validator.preloadJsonSchema();
        assertEquals(100, getSchemas(validator).size());
    }

    @Test
    void preloadWithNoModifications() throws Exception {
        Consumer<List<JsonSchema>> noop = list -> {
            /* no‑op */
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, noop);
        List<JsonSchema> internal = getSchemas(validator);
        for (int i = 0; i < 5; i++) internal.add(new SimpleJsonSchema());
        assertEquals(5, internal.size());
        validator.preloadJsonSchema();
        assertEquals(5, internal.size(), "size must stay unchanged");
    }

    @Test
    void nullInternalSchemasList() throws Exception {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            /* no‑op */
        });
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, null);
        assertThrows(NullPointerException.class, validator::preloadJsonSchema);
    }

    @Test
    void unmodifiableSchemasList() throws Exception {
        Consumer<List<JsonSchema>> tryAdd = list -> list.add(new SimpleJsonSchema());
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, tryAdd);
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, Collections.unmodifiableList(new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, validator::preloadJsonSchema);
    }

    @Test
    void specificUncheckedExceptionPropagation() {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            throw new IllegalArgumentException("bad arg");
        });
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, validator::preloadJsonSchema);
        assertEquals("bad arg", ex.getMessage());
    }
}
