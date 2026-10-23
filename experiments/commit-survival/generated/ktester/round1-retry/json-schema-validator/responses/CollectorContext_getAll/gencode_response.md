```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_getAll_Test {

    private CollectorContext collectorContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // The default constructor creates a fresh context for every test
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        collectorContext = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /* --------------------------------------------------------------------- */
    /* Helper utilities for reflection manipulation                           */
    /* --------------------------------------------------------------------- */
    private static void setPrivateMap(CollectorContext ctx, String fieldName, Map<String, Object> map) {
        try {
            Field field = CollectorContext.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(ctx, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> getPrivateMap(CollectorContext ctx, String fieldName) {
        try {
            Field field = CollectorContext.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) field.get(ctx);
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* Existing simple tests (kept unchanged)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllMergesCollectorsAddedViaAdd() {
        // GIVEN – two different collector entries are added
        List<String> listA = new ArrayList<>(Arrays.asList("a1", "a2"));
        List<String> listB = new ArrayList<>(Collections.singletonList("b1"));

        collectorContext.add("collectorA", listA);
        collectorContext.add("collectorB", listB);

        // WHEN – the merged map is retrieved
        Map<String, Object> merged = collectorContext.getAll();

        // THEN – both entries are present and values are the same objects added earlier
        assertEquals(2, merged.size(), "Merged map should contain exactly two entries");
        assertTrue(merged.containsKey("collectorA"));
        assertTrue(merged.containsKey("collectorB"));
        assertSame(listA, merged.get("collectorA"));
        assertSame(listB, merged.get("collectorB"));
    }

    @Test
    void testGetAllReturnsIndependentCopy() {
        // GIVEN – a collector is added
        List<String> list = new ArrayList<>(Collections.singletonList("value"));
        collectorContext.add("myCollector", list);

        // WHEN – we obtain the merged map and mutate it
        Map<String, Object> firstCall = collectorContext.getAll();
        firstCall.put("newKey", "newValue");

        // AND – we obtain the map a second time
        Map<String, Object> secondCall = collectorContext.getAll();

        // THEN – the internal state of CollectorContext has not been altered by the external mutation
        assertFalse(secondCall.containsKey("newKey"),
                "Subsequent calls to getAll() must not reflect modifications made to a previously returned map");
        assertEquals(1, secondCall.size(), "Only the originally added collector should be present");
        assertSame(list, secondCall.get("myCollector"));
    }

    @Test
    void testGetAllKeyCollisionResolution() {
        // GIVEN – an entry is added via the public add method (populates collectorMap)
        List<String> mapValue = new ArrayList<>(Collections.singletonList("map"));
        collectorContext.add("collisionKey", mapValue);

        // Simulate loading a second map entry that would reside in collectorLoadMap.
        // We directly set the private collectorLoadMap via reflection.
        Map<String, Object> loadMap = new HashMap<>();
        List<String> loadValue = new ArrayList<>(Collections.singletonList("load"));
        loadMap.put("collisionKey", loadValue);
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        // WHEN – we retrieve the merged map
        Map<String, Object> merged = collectorContext.getAll();

        // THEN – collectorLoadMap overrides collectorMap for the colliding key
        assertEquals(1, merged.size());
        assertSame(loadValue, merged.get("collisionKey"));
    }

    /* --------------------------------------------------------------------- */
    /* 1. Merging behavior – various distinct scenarios                        */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllWhenBothMapsEmpty() {
        // both internal maps start empty (default state)
        Map<String, Object> result = collectorContext.getAll();
        assertTrue(result.isEmpty(), "Result should be empty when both internal maps are empty");
    }

    @Test
    void testGetAllCollectorMapOnly() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("a", 1);
        collectorMap.put("b", 2);
        setPrivateMap(collectorContext, "collectorMap", collectorMap);
        // leave collectorLoadMap empty (default)

        Map<String, Object> result = collectorContext.getAll();
        assertEquals(collectorMap, result, "Result should exactly match collectorMap when loader map is empty");
    }

    @Test
    void testGetAllCollectorLoadMapOnly() {
        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put("x", "foo");
        loadMap.put("y", "bar");
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);
        // collectorMap stays empty

        Map<String, Object> result = collectorContext.getAll();
        assertEquals(loadMap, result, "Result should exactly match collectorLoadMap when collectorMap is empty");
    }

    @Test
    void testGetAllDistinctKeysFromBothMaps() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("k1", 10);
        collectorMap.put("k2", 20);
        setPrivateMap(collectorContext, "collectorMap", collectorMap);

        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put("k3", 30);
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();
        Map<String, Object> expected = new HashMap<>();
        expected.putAll(collectorMap);
        expected.putAll(loadMap);
        assertEquals(expected, result, "All distinct entries from both maps should be present");
    }

    @Test
    void testGetAllDuplicateKeyResolutionWithUniqueEntry() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("dup", "collector");
        collectorMap.put("unique", "collectorOnly");
        setPrivateMap(collectorContext, "collectorMap", collectorMap);

        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put("dup", "load");
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();

        assertEquals(2, result.size(), "Result map should contain exactly two keys");
        assertEquals("load", result.get("dup"), "Value from collectorLoadMap should win on key collision");
        assertEquals("collectorOnly", result.get("unique"), "Unique entry from collectorMap must be retained");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Null key handling                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllNullKeyInCollectorMap() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put(null, "collectorNull");
        setPrivateMap(collectorContext, "collectorMap", collectorMap);

        Map<String, Object> result = collectorContext.getAll();
        assertEquals(1, result.size());
        assertTrue(result.containsKey(null));
        assertEquals("collectorNull", result.get(null));
    }

    @Test
    void testGetAllNullKeyInCollectorLoadMap() {
        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put(null, "loadNull");
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();
        assertEquals(1, result.size());
        assertTrue(result.containsKey(null));
        assertEquals("loadNull", result.get(null));
    }

    @Test
    void testGetAllNullKeyCollisionLoadOverridesCollector() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put(null, "collectorOverride");
        setPrivateMap(collectorContext, "collectorMap", collectorMap);

        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put(null, "loadOverride");
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();
        assertEquals(1, result.size());
        assertEquals("loadOverride", result.get(null), "collectorLoadMap value should win for null key");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Null value handling                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllNullValueInCollectorMap() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("k1", null);
        setPrivateMap(collectorContext, "collectorMap", collectorMap);

        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put("k2", "val2");
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();

        assertEquals(2, result.size());
        assertNull(result.get("k1"));
        assertEquals("val2", result.get("k2"));
    }

    @Test
    void testGetAllNullValueOverridesNonNullOnCollision() {
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("k1", "collectorVal");
        setPrivateMap(collectorContext, "collectorMap", collectorMap);

        Map<String, Object> loadMap = new HashMap<>();
        loadMap.put("k1", null);
        loadMap.put("k2", null);
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();

        assertEquals(2, result.size());
        assertNull(result.get("k1"), "Null from collectorLoadMap should override the non‑null value");
        assertNull(result.get("k2"));
    }

    /* --------------------------------------------------------------------- */
    /* 4. Map instance isolation & independence                               */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllReturnsDistinctMapInstances() {
        Map<String, Object> map1 = collectorContext.getAll();
        Map<String, Object> map2 = collectorContext.getAll();

        assertNotSame(map1, map2, "Each call to getAll() must return a new Map instance");
    }

    @Test
    void testReturnedMapDoesNotReflectLaterModifications() {
        // initial state – empty
        Map<String, Object> first = collectorContext.getAll();

        // mutate internal state after first retrieval
        collectorContext.add("newKey", "newValue");

        Map<String, Object> second = collectorContext.getAll();

        assertFalse(first.containsKey("newKey"), "First returned map must not see later additions");
        assertTrue(second.containsKey("newKey"), "Second call should see the newly added entry");
    }

    /* --------------------------------------------------------------------- */
    /* 5. Large merge stress test                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllLargeMergeStress() {
        Map<String, Object> collectorMap = new HashMap<>();
        Map<String, Object> loadMap = new HashMap<>();

        // populate 100 entries each
        IntStream.range(0, 100).forEach(i -> collectorMap.put("c" + i, i));
        IntStream.range(0, 100).forEach(i -> loadMap.put("l" + i, i + 100));

        setPrivateMap(collectorContext, "collectorMap", collectorMap);
        setPrivateMap(collectorContext, "collectorLoadMap", loadMap);

        Map<String, Object> result = collectorContext.getAll();

        assertEquals(200, result.size(), "Result should contain 200 distinct entries");
        // Spot‑check a few keys
        assertEquals(0, result.get("c0"));
        assertEquals(99, result.get("c99"));
        assertEquals(100, result.get("l0"));
        assertEquals(199, result.get("l99"));
    }

    /* --------------------------------------------------------------------- */
    /* 6. Internal map null handling (expect NullPointerException)           */
    /* --------------------------------------------------------------------- */

    @Test
    void testGetAllWhenCollectorMapIsNull() throws Exception {
        // make collectorMap null via reflection
        setPrivateMap(collectorContext, "collectorMap", null);

        Executable call = () -> collectorContext.getAll();
        assertThrows(NullPointerException.class, call,
                "Calling getAll() with a null collectorMap should throw NPE");
    }

    @Test
    void testGetAllWhenCollectorLoadMapIsNull() throws Exception {
        setPrivateMap(collectorContext, "collectorLoadMap", null);

        Executable call = () -> collectorContext.getAll();
        assertThrows(NullPointerException.class, call,
                "Calling getAll() with a null collectorLoadMap should throw NPE");
    }

    @Test
    void testGetAllWhenBothMapsAreNull() throws Exception {
        setPrivateMap(collectorContext, "collectorMap", null);
        setPrivateMap(collectorContext, "collectorLoadMap", null);

        Executable call = () -> collectorContext.getAll();
        assertThrows(NullPointerException.class, call,
                "Calling getAll() with both internal maps null should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    /* 7. Faulty map implementation – exception propagation                    */
    /* --------------------------------------------------------------------- */

    /**
     * A Map implementation whose {@code entrySet().iterator()} throws a RuntimeException.
     */
    private static class FaultyMap extends AbstractMap<String, Object> {
        private final Map<String, Object> delegate = new HashMap<>();

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<String, Object>> iterator() {
                    throw new RuntimeException("Faulty iterator");
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }

        @Override
        public Object put(String key, Object value) {
            return delegate.put(key, value);
        }
    }

    @Test
    void testGetAllWithFaultyCollectorMapThrowsRuntimeException() {
        FaultyMap faulty = new FaultyMap();
        faulty.put("a", 1);
        setPrivateMap(collector