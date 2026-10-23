package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectorContext_add_Test {

    private CollectorContext collectorContext;

    /**
     * Minimal stub of the Collector interface – the real interface has no
     *  behaviour required for the add/get tests.
     */
    private interface Collector<E> {
    }

    private static class SimpleCollector<E> implements Collector<E> {

        // optional identifier for debugging
        private final String id;

        SimpleCollector() {
            this.id = null;
        }

        SimpleCollector(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return "SimpleCollector{" + (id != null ? "id='" + id + '\'' : "") + '}';
        }
    }

    private static class CustomCollector<E> extends SimpleCollector<E> {

        CustomCollector() {
            super("custom");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test to avoid cross‑test contamination.
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the internal map to help the garbage collector.
        // The CollectorContext does not expose a clear method, so we simply
        // discard the instance; this method is kept for symmetry with the
        // template.
        collectorContext = null;
    }

    static Stream<Arguments> basicCollectorProvider() {
        return Stream.of(// normal collector
        Arguments.of("testCollector", new SimpleCollector<>()), // null name (HashMap permits null keys)
        Arguments.of(null, new SimpleCollector<>()), // generic String collector
        Arguments.of("genericString", new SimpleCollector<String>()), // generic Integer collector
        Arguments.of("genericInteger", new SimpleCollector<Integer>()));
    }

    static Stream<Arguments> edgeKeyProvider() {
        String veryLongName = String.join("", Collections.nCopies(5_000, "a"));
        String special = "!@#$%^&*()_+|~=`{}[]:\\;'<>,.?/";
        return Stream.of(// empty string
        Arguments.of(""), // whitespace only
        Arguments.of("   "), // special characters
        Arguments.of(special), // 5,000‑character name
        Arguments.of(veryLongName));
    }

    @Test
    @DisplayName("add() stores a collector and makes it retrievable via get()")
    void testAddStoresCollector() {
        // GIVEN
        String name = "myCollector";
        List<String> collector = new ArrayList<>();
        // WHEN
        collectorContext.add(name, collector);
        // THEN
        Object retrieved = collectorContext.get(name);
        assertNotNull(retrieved, "Collector should be present in the context");
        assertSame(collector, retrieved, "The stored collector instance should be returned");
    }

    @Test
    @DisplayName("add() replaces an existing collector with the same name")
    void testAddReplacesExistingCollector() {
        // GIVEN
        String name = "replaceCollector";
        List<String> firstCollector = new ArrayList<>();
        List<String> secondCollector = new ArrayList<>();
        collectorContext.add(name, firstCollector);
        assertSame(firstCollector, collectorContext.get(name), "First collector should be stored initially");
        // WHEN
        collectorContext.add(name, secondCollector);
        // THEN
        Object retrieved = collectorContext.get(name);
        assertSame(secondCollector, retrieved, "Second collector should have replaced the first one");
        assertNotSame(firstCollector, retrieved, "The original collector should no longer be referenced");
    }

    @Test
    @DisplayName("add() correctly updates the internal map size")
    void testAddUpdatesMapSize() {
        // GIVEN
        String name1 = "collectorOne";
        String name2 = "collectorTwo";
        List<String> collector1 = new ArrayList<>();
        List<String> collector2 = new ArrayList<>();
        // WHEN
        collectorContext.add(name1, collector1);
        collectorContext.add(name2, collector2);
        // THEN
        Map<String, Object> all = collectorContext.getAll();
        assertEquals(2, all.size(), "Two distinct collectors should be present");
        assertTrue(all.containsKey(name1), "Map should contain first collector name");
        assertTrue(all.containsKey(name2), "Map should contain second collector name");
    }

    @Test
    @DisplayName("add() with null collector stores null value")
    void testAddNullCollector() {
        // GIVEN
        String name = "nullCollector";
        // WHEN
        // using raw type to allow null
        collectorContext.add(name, (Collector<?>) null);
        // THEN
        assertTrue(collectorContext.getAll().containsKey(name), "The key should exist even if the value is null");
        assertNull(collectorContext.get(name), "Stored value should be null");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: name={0}, collector={1}")
    @MethodSource("basicCollectorProvider")
    @DisplayName("add(String, Collector) stores and retrieves various collector types")
    void testAddBasicFunctionality(String name, Collector<?> collector) {
        // WHEN
        collectorContext.add(name, collector);
        // THEN
        Object retrieved = collectorContext.get(name);
        assertSame(collector, retrieved, () -> "Collector retrieved for name [" + name + "] should be the same instance that was added");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: name=\"{0}\"")
    @MethodSource("edgeKeyProvider")
    @DisplayName("add() accepts various edge‑case key strings")
    void testAddEdgeCaseKeys(String name) {
        Collector<?> collector = new SimpleCollector<>();
        // WHEN
        collectorContext.add(name, collector);
        // THEN
        assertSame(collector, collectorContext.get(name), () -> "Collector should be retrievable for edge‑case key: \"" + name + "\"");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("add() correctly stores and retrieves a subclass collector")
    void testAddCustomCollector() {
        String name = "subCollector";
        CustomCollector<?> collector = new CustomCollector<>();
        collectorContext.add(name, collector);
        Object retrieved = collectorContext.get(name);
        assertSame(collector, retrieved, "CustomCollector instance should be stored and retrieved unchanged");
        assertTrue(retrieved instanceof CustomCollector, "Retrieved instance should retain its subclass type");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("add() overwrites a previously stored null value")
    void testOverwriteNullWithRealCollector() {
        String name = "nullThenReal";
        // first add – null collector
        collectorContext.add(name, (Collector<?>) null);
        assertNull(collectorContext.get(name), "First retrieval should be null");
        // second add – real collector
        SimpleCollector<?> real = new SimpleCollector<>();
        collectorContext.add(name, real);
        assertSame(real, collectorContext.get(name), "After overwrite, the real collector should be returned");
        assertEquals(1, collectorContext.getAll().size(), "Map size must stay 1 because the key was the same");
    }

    @Test
    @DisplayName("add() with the identical instance does not change map size")
    void testAddSameInstanceTwice() {
        String name = "sameInstance";
        SimpleCollector<?> collector = new SimpleCollector<>();
        collectorContext.add(name, collector);
        // add the same instance again
        collectorContext.add(name, collector);
        assertSame(collector, collectorContext.get(name), "Stored instance must remain the same");
        assertEquals(1, collectorContext.getAll().size(), "Map size should stay 1 when the same instance overwrites itself");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("add() properly handles Unicode characters in the key")
    void testAddUnicodeKey() {
        String name = "ユニコード";
        SimpleCollector<?> collector = new SimpleCollector<>();
        collectorContext.add(name, collector);
        assertSame(collector, collectorContext.get(name), "Collector stored under a Unicode key should be retrievable");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("add() overwrites when a different String object with the same content is used as key")
    void testAddDuplicateKeyStringEquality() {
        String key1 = new String("duplicateKey");
        Collector<?> first = new SimpleCollector<>("first");
        Collector<?> second = new SimpleCollector<>("second");
        collectorContext.add(key1, first);
        assertSame(first, collectorContext.get("duplicateKey"), "First collector should be stored");
        // different object, same content
        String key2 = new String("duplicateKey");
        collectorContext.add(key2, second);
        assertSame(second, collectorContext.get("duplicateKey"), "Second collector should have overwritten the first due to equal content");
        assertEquals(1, collectorContext.getAll().size(), "Map size must remain 1 because the logical key is the same");
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("add() distinct keys result in correct map size and content")
    void testAddMultipleDistinctCollectors() {
        Collector<?> c1 = new SimpleCollector<>("c1");
        Collector<?> c2 = new SimpleCollector<>("c2");
        collectorContext.add("c1", c1);
        collectorContext.add("c2", c2);
        Map<String, Object> all = collectorContext.getAll();
        assertEquals(2, all.size(), "Two distinct keys should produce map size 2");
        assertSame(c1, all.get("c1"));
        assertSame(c2, all.get("c2"));
    }
}
