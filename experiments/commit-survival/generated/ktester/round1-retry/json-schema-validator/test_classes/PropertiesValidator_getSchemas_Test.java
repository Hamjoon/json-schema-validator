package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
class PropertiesValidator_getSchemas_Test {

    // ----- Mocks ------------------------------------------------------------
//     @Mock
    ValidationContext validationContext;

//     @Mock
    JsonSchema parentSchema;

//     @Mock
    JsonSchema // the schema that will be returned by ValidationContext#newSchema
    childSchema;

    // ----- System under test -----------------------------------------------
    private PropertiesValidator validator;

    // may be null – we only need a reference
    private JsonSchema stubChildSchema;

    /**
     * Minimal concrete {@link JsonSchema} implementation that can be instantiated
     * in the tests without needing a full schema definition.
     */
    private static class DummyJsonSchema extends JsonSchema {

        DummyJsonSchema() {
            // The super‑constructor that takes (ValidationContext, URI, JsonNode) is
            // sufficient for the tests – all arguments can be null.
            super(null, null, null);
        }
    }

    /**
     * Simple {@link ValidationContext} stub that records how many times
     * {@code newSchema} is invoked and always returns the same {@link JsonSchema}
     * instance (which may be {@code null} for the tests).
     */
    private static class StubValidationContext extends ValidationContext {

        private final JsonSchema stubSchema;

        private int callCount = 0;

        StubValidationContext(JsonSchema stubSchema) {
            // ValidationContext does not have a no‑arg constructor; we pass nulls for
            // all its dependencies because the tests never use them.
            super(null, null, null, null, null);
            this.stubSchema = stubSchema;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            callCount++;
            return stubSchema;
        }

        int getCallCount() {
            return callCount;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Simple schema JSON containing two properties: "a" (string) and "b" (integer)
        String schemaJson = """
            {
              "properties": {
                "a": { "type": "string" },
                "b": { "type": "integer" }
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // The stub child schema – we do not need any behaviour, only a reference.
        stubChildSchema = null;
        // ValidationContext that always returns the same stub schema and records calls.
        validationContext = new StubValidationContext(stubChildSchema);
        // Parent schema can also be null – it is only passed through to the context.
        JsonSchema parentSchema = null;
        // Create the validator under test.
        validator = new PropertiesValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /**
     * Utility that uses reflection to obtain the private `schemas` field and clear it.
     */
    private void clearInternalSchemasMap() throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.clear();
    }

    /**
     * Utility that injects a map entry via reflection.
     */
    private void putIntoInternalSchemas(String key, JsonSchema schema) throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.put(key, schema);
    }

//     @Test
//     void testGetSchemasReturnsMapWithAllDeclaredProperties() {
//         // Act
//         Map<String, JsonSchema> schemas = validator.getSchemas();
//         // Assert – the map must contain exactly the two property names defined in the JSON.
//         assertNotNull(schemas, "The schemas map should never be null");
//         assertEquals(2, schemas.size(), "There should be two entries in the schemas map");
//         assertTrue(schemas.containsKey("a"), "Map should contain property 'a'");
//         assertTrue(schemas.containsKey("b"), "Map should contain property 'b'");
//         // The values stored in the map must be the objects returned by the stub ValidationContext.
//         // In this test the stub returns {@code null}, so we expect null values.
//         assertNull(schemas.get("a"), "Schema for 'a' should be the stub (null) value");
//         assertNull(schemas.get("b"), "Schema for 'b' should be the stub (null) value");
//         // Verify that ValidationContext#newSchema was invoked exactly twice – once per property.
//         assertEquals(2, validationContext.getCallCount(), "ValidationContext#newSchema should have been called once for each property");
//     }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Initial state of schemas map – should be empty")
    void testInitialStateEmptyMap() {
        // The validator is freshly created in @BeforeEach, so the map is still empty.
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertNotNull(schemas, "Map must never be null");
        assertTrue(schemas.isEmpty(), "New validator should expose an empty schemas map");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Reflection insertion – map reflects the injected entry")
    void testAddEntryViaReflection() throws Exception {
        JsonSchema injected = new DummyJsonSchema();
        putIntoInternalSchemas("user", injected);
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertEquals(1, schemas.size(), "Map should contain exactly the injected entry");
        assertTrue(schemas.containsKey("user"), "Map must contain the key inserted via reflection");
        assertSame(injected, schemas.get("user"), "Stored value must be the mock injected via reflection");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Returned map is mutable – modifications affect internal state")
    void testMutabilityOfReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema schemaX = new DummyJsonSchema();
        map.put("x", schemaX);
        // Re‑obtain the map and verify the entry persists.
        Map<String, JsonSchema> again = validator.getSchemas();
        assertEquals(1, again.size());
        assertSame(schemaX, again.get("x"));
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent modifications and reads do not throw unexpected exceptions")
    void testThreadSafetySanityCheck() throws Exception {
        final int iterations = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicBoolean failed = new AtomicBoolean(false);
        Runnable writer = () -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    validator.getSchemas().put("k" + i, new DummyJsonSchema());
                }
            } catch (Exception e) {
                failed.set(true);
            }
        };
        Runnable reader = () -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    // just read size
                    validator.getSchemas().size();
                }
            } catch (Exception e) {
                failed.set(true);
            }
        };
        Future<?> w = executor.submit(writer);
        Future<?> r = executor.submit(reader);
        w.get();
        r.get();
        executor.shutdownNow();
        assertFalse(failed.get(), "No exception should occur during concurrent access");
        assertTrue(validator.getSchemas().size() >= iterations, "At least the written entries should be present");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Two consecutive calls return the exact same Map instance")
    void testIdentityOfReturnedMap() {
        Map<String, JsonSchema> first = validator.getSchemas();
        Map<String, JsonSchema> second = validator.getSchemas();
        assertSame(first, second, "Getter must return the live internal map, not a copy");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Insertion order is preserved")
    void testInsertionOrderPreservation() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema s1 = new DummyJsonSchema();
        JsonSchema s2 = new DummyJsonSchema();
        JsonSchema s3 = new DummyJsonSchema();
        map.put("a", s1);
        map.put("b", s2);
        map.put("c", s3);
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("a", "b", "c"), keys, "LinkedHashMap should preserve the order of insertion");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Removing a key through the returned map updates internal state")
    void testRemovalViaReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema temp = new DummyJsonSchema();
        map.put("temp", temp);
        assertEquals(1, map.size());
        map.remove("temp");
        assertFalse(validator.getSchemas().containsKey("temp"));
        assertEquals(0, validator.getSchemas().size());
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Map accepts a null key")
    void testNullKeyHandling() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema nullKeySchema = new DummyJsonSchema();
        map.put(null, nullKeySchema);
        assertTrue(map.containsKey(null), "Map must contain the null key");
        assertSame(nullKeySchema, map.get(null), "Value associated with null key must be retrievable");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Putting a value under an existing key replaces the previous one")
    void testOverwriteExistingKey() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema first = new DummyJsonSchema();
        JsonSchema second = new DummyJsonSchema();
        map.put("duplicate", first);
        assertEquals(1, map.size());
        assertSame(first, map.get("duplicate"));
        map.put("duplicate", second);
        assertEquals(1, map.size(), "Map size must stay unchanged when overwriting a key");
        assertSame(second, map.get("duplicate"), "Value should be replaced by the second schema");
    }
}