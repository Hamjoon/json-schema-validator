```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PropertiesValidator#getSchemas()}.
 *
 * The tests cover various aspects of the live map returned by the getter:
 * <ul>
 *   <li>Initial emptiness</li>
 *   <li>Reflection based insertion</li>
 *   <li>Mutability (the map is not a defensive copy)</li>
 *   <li>Identity – the same instance is returned on every call</li>
 *   <li>Insertion‑order preservation of {@link LinkedHashMap}</li>
 *   <li>Removal, null key handling and overwriting existing keys</li>
 *   <li>A simple thread‑safety sanity check</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_getSchemas_Test {

    // ----- Mocks ------------------------------------------------------------
    @Mock ValidationContext validationContext;
    @Mock JsonSchema parentSchema;
    @Mock JsonSchema childSchema; // returned by ValidationContext#newSchema

    // ----- System under test -----------------------------------------------
    private PropertiesValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Minimal schema JSON – an empty object so the constructor does not
        // automatically populate the schemas map.
        String schemaJson = "{}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(schemaJson);

        // Stub newSchema – not really used in the tests that manipulate the map via reflection,
        // but required for the constructor.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(childSchema);

        validator = new PropertiesValidator("testPath", schemaNode, parentSchema, validationContext);
        // Ensure the internal map is empty before each test.
        clearInternalSchemasMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /** Utility that uses reflection to obtain the private `schemas` field and clear it. */
    private void clearInternalSchemasMap() throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.clear();
    }

    /** Utility that injects a map entry via reflection. */
    private void putIntoInternalSchemas(String key, JsonSchema schema) throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.put(key, schema);
    }

    // -----------------------------------------------------------------------
    // 1. Initial state of schemas map
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Initial state of schemas map – should be empty")
    void testInitialStateEmptyMap() {
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertNotNull(schemas, "Map must never be null");
        assertTrue(schemas.isEmpty(), "New validator should expose an empty schemas map");
    }

    // -----------------------------------------------------------------------
    // 2. Schemas map after adding entries via reflection
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Reflection insertion – map reflects the injected entry")
    void testAddEntryViaReflection() throws Exception {
        JsonSchema mockSchema = mock(JsonSchema.class);
        putIntoInternalSchemas("user", mockSchema);

        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertEquals(1, schemas.size(), "Map should contain exactly the injected entry");
        assertTrue(schemas.containsKey("user"), "Map must contain the key inserted via reflection");
        assertSame(mockSchema, schemas.get("user"), "Stored value must be the mock injected via reflection");
    }

    // -----------------------------------------------------------------------
    // 3. Mutability of the returned map
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Returned map is mutable – modifications affect internal state")
    void testMutabilityOfReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema schemaX = mock(JsonSchema.class);
        map.put("x", schemaX);

        // Re‑obtain the map and verify the entry persists.
        Map<String, JsonSchema> again = validator.getSchemas();
        assertEquals(1, again.size());
        assertSame(schemaX, again.get("x"));
    }

    // -----------------------------------------------------------------------
    // 4. Thread‑safety sanity check (basic concurrent modifications)
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
                    validator.getSchemas().put("k" + i, mock(JsonSchema.class));
                }
            } catch (Exception e) {
                failed.set(true);
            }
        };

        Runnable reader = () -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    validator.getSchemas().size(); // just read size
                }
            } catch (Exception e) {
                failed.set(true);
            }
        };

        Future<?> w = executor.submit(writer);
        Future<?> r = executor.submit(reader);
        w.get(); r.get(); // wait for completion

        executor.shutdownNow();
        assertFalse(failed.get(), "No exception should occur during concurrent access");
        assertTrue(validator.getSchemas().size() >= iterations,
                "At least the written entries should be present");
    }

    // -----------------------------------------------------------------------
    // 5. Identity of returned map
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Two consecutive calls return the exact same Map instance")
    void testIdentityOfReturnedMap() {
        Map<String, JsonSchema> first = validator.getSchemas();
        Map<String, JsonSchema> second = validator.getSchemas();
        assertSame(first, second, "Getter must return the live internal map, not a copy");
    }

    // -----------------------------------------------------------------------
    // 6. Insertion order preservation (LinkedHashMap behaviour)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Insertion order is preserved")
    void testInsertionOrderPreservation() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        map.put("a", s1);
        map.put("b", s2);
        map.put("c", s3);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("a", "b", "c"), keys,
                "LinkedHashMap should preserve the order of insertion");
    }

    // -----------------------------------------------------------------------
    // 7. Removal of entries via returned map
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Removing a key through the returned map updates internal state")
    void testRemovalViaReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema temp = mock(JsonSchema.class);
        map.put("temp", temp);
        assertEquals(1, map.size());

        map.remove("temp");
        assertFalse(validator.getSchemas().containsKey("temp"));
        assertEquals(0, validator.getSchemas().size());
    }

    // -----------------------------------------------------------------------
    // 8. Handling of null key (LinkedHashMap permits a single null key)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Map accepts a null key")
    void testNullKeyHandling() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema nullKeySchema = mock(JsonSchema.class);
        map.put(null, nullKeySchema);

        assertTrue(map.containsKey(null), "Map must contain the null key");
        assertSame(nullKeySchema, map.get(null), "Value associated with null key must be retrievable");
    }

    // -----------------------------------------------------------------------
    // 9. Overwrite existing key
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Putting a value under an existing key replaces the previous one")
    void testOverwriteExistingKey() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema first = mock(JsonSchema.class);
        JsonSchema second = mock(JsonSchema.class);

        map.put("duplicate", first);
        assertEquals(1, map.size());
        assertSame(first, map.get("duplicate"));

        map.put("duplicate", second);
        assertEquals(1, map.size(), "Map size must stay unchanged when overwriting a key");
        assertSame(second, map.get("duplicate"), "Value should be replaced by the second schema");
    }
}
```