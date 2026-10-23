```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_add_Test {

    private CollectorContext collectorContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
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

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
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
        assertSame(firstCollector, collectorContext.get(name),
                "First collector should be stored initially");

        // WHEN
        collectorContext.add(name, secondCollector);

        // THEN
        Object retrieved = collectorContext.get(name);
        assertSame(secondCollector, retrieved,
                "Second collector should have replaced the first one");
        assertNotSame(firstCollector, retrieved,
                "The original collector should no longer be referenced");
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
        collectorContext.add(name, (Collector<?>) null); // using raw type to allow null

        // THEN
        assertTrue(collectorContext.getAll().containsKey(name),
                "The key should exist even if the value is null");
        assertNull(collectorContext.get(name), "Stored value should be null");
    }
}
```