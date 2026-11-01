package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    JsonSchema mockSchema1;

    @Mock
    JsonSchema mockSchema2;

    AnyOfValidator anyOfValidator;

    /**
     * Simple subclass that delegates the {@code preloadJsonSchemas} behaviour
     * to a {@link Consumer}. This allows each test case to customise how the
     * supplied list is mutated.
     */
    private static class TestAnyOfValidator extends AnyOfValidator {

        private final Consumer<List<JsonSchema>> behaviour;

        TestAnyOfValidator(String path, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext, Consumer<List<JsonSchema>> behaviour) {
            super(path, schemaNode, parentSchema, validationContext);
            this.behaviour = behaviour;
        }

        @Override
        protected void preloadJsonSchemas(List<JsonSchema> schemas) {
            behaviour.accept(schemas);
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- stubbed behaviour for ValidationContext -----
        // config required by the constructor
        when(validationContext.getConfig()).thenReturn(config);
        // discriminator flag – not needed for this test, just return false
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // newSchema is called once per element in the schema array.
        // Return different mocks for successive calls.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSchema1, mockSchema2);
        // ----- build a simple schema node that contains two subschemas -----
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaArray = mapper.createArrayNode();
        // each subschema can be an empty object – the content is irrelevant for this test
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());
        // parent schema is not used in the constructor for this test; pass null
        anyOfValidator = new AnyOfValidator("testPath", schemaArray, null, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(validationContext, config, mockSchema1, mockSchema2);
    }

    @Test
    void testPreloadJsonSchemaInvokesPreloadOnAllContainedSchemas() {
        // Act – invoke the method under test
        anyOfValidator.preloadJsonSchema();
        // Assert – each JsonSchema created by the constructor must have its
        // preloadJsonSchema() method called exactly once.
        verify(mockSchema1, times(1)).preloadJsonSchema();
        verify(mockSchema2, times(1)).preloadJsonSchema();
        // Also verify that no extra interactions happened.
        verifyNoMoreInteractions(mockSchema1, mockSchema2);
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
    void preloadWithEmptySchemasList() throws Exception {
        // Build an AnyOfValidator with an empty schema array → internal list empty.
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode emptyArray = mapper.createArrayNode();
        AnyOfValidator validator = new AnyOfValidator("emptyPath", emptyArray, null, validationContext);
        // pre‑condition: list is empty
        assertTrue(getSchemas(validator).isEmpty());
        // Act
        validator.preloadJsonSchema();
        // Assert – list is still empty and no schema preload was attempted.
        assertTrue(getSchemas(validator).isEmpty());
        verifyNoInteractions(mockSchema1, mockSchema2);
    }

    @Test
    void preloadWithPrePopulatedSchemasList() throws Exception {
        // Pre‑populate the internal list with two mock schemas.
        Consumer<List<JsonSchema>> addOneMore = list -> list.add(mockSchema1);
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addOneMore);
        // using reflection to inject two mocks (mockSchema1 & mockSchema2)
        List<JsonSchema> internal = getSchemas(validator);
        internal.add(mockSchema1);
        internal.add(mockSchema2);
        assertEquals(2, internal.size());
        // Act
        validator.preloadJsonSchema();
        // Expect one additional schema added by the overridden method.
        assertEquals(3, internal.size());
        assertTrue(internal.contains(mockSchema1));
        assertTrue(internal.contains(mockSchema2));
    }

    @Test
    void exceptionPropagationFromPreloadJsonSchemas() {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            throw new RuntimeException("boom");
        });
        RuntimeException ex = assertThrows(RuntimeException.class, validator::preloadJsonSchema);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void multipleConsecutivePreloadCalls() throws Exception {
        // Each call adds two distinct mock schemas.
        Consumer<List<JsonSchema>> addTwo = list -> {
            JsonSchema s1 = mock(JsonSchema.class);
            JsonSchema s2 = mock(JsonSchema.class);
            list.add(s1);
            list.add(s2);
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addTwo);
        // First call -> size 2
        validator.preloadJsonSchema();
        assertEquals(2, getSchemas(validator).size());
        // Second call -> size 4
        validator.preloadJsonSchema();
        assertEquals(4, getSchemas(validator).size());
    }

    @Test
    void preloadClearsExistingSchemas() throws Exception {
        Consumer<List<JsonSchema>> clear = List::clear;
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, clear);
        // Pre‑populate with three mocks.
        List<JsonSchema> internal = getSchemas(validator);
        internal.add(mock());
        internal.add(mock());
        internal.add(mock());
        assertEquals(3, internal.size());
        // Act
        validator.preloadJsonSchema();
        // Assert list is empty.
        assertTrue(internal.isEmpty());
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
                list.add(mock(JsonSchema.class));
            }
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, addHundred);
        validator.preloadJsonSchema();
        assertEquals(100, getSchemas(validator).size());
    }

    @Test
    void preloadWithNoModifications() throws Exception {
        Consumer<List<JsonSchema>> noop = list -> {
            /* do nothing */
        };
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, noop);
        // pre‑populate with five mocks
        List<JsonSchema> internal = getSchemas(validator);
        for (int i = 0; i < 5; i++) internal.add(mock(JsonSchema.class));
        assertEquals(5, internal.size());
        validator.preloadJsonSchema();
        // size must stay unchanged
        assertEquals(5, internal.size());
    }

    @Test
    void nullInternalSchemasList() throws Exception {
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, list -> {
            /* no‑op */
        });
        // Force the private field to null via reflection.
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(validator, null);
        assertThrows(NullPointerException.class, validator::preloadJsonSchema);
    }

    @Test
    void unmodifiableSchemasList() throws Exception {
        Consumer<List<JsonSchema>> tryAdd = list -> list.add(mock(JsonSchema.class));
        TestAnyOfValidator validator = new TestAnyOfValidator("path", new ObjectMapper().createArrayNode(), null, validationContext, tryAdd);
        // Replace internal list with an unmodifiable version.
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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, validator::preloadJsonSchema);
        assertEquals("bad arg", ex.getMessage());
    }
}
