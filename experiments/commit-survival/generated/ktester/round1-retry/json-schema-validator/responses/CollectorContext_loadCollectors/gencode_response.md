```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link CollectorContext#loadCollectors()}.
 *
 * <p>The tests exercise all scenarios described in the JSON test‑case list,
 * including empty maps, single/multiple collectors, null returns, exception
 * propagation, overwriting previous loads and concurrent modification.</p>
 *
 * <p>Only the public API of {@link CollectorContext} ( {@code add},
 * {@code get} ) is used to set up the pre‑condition.  When verification of the
 * internal {@code collectorLoadMap} is required, reflection is employed – this
 * is permissible because {@code loadCollectors()} itself is not private.</p>
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_loadCollectors_Test {

    private CollectorContext collectorContext;

    // Generic mock – the actual type parameter is irrelevant for the tests.
    @Mock
    private Collector<List<String>> mockCollector1;
    @Mock
    private Collector<List<String>> mockCollector2;
    @Mock
    private Collector<List<String>> mockCollector3;
    @Mock
    private Collector<Object> mockNullCollector;
    @Mock
    private Collector<Object> mockExceptionCollector;

    private static final String C1 = "c1";
    private static final String C2 = "c2";
    private static final String C3 = "c3";
    private static final String NON_COLLECTOR = "plainObject";

    @BeforeEach
    void setup() {
        collectorContext = new CollectorContext();

        // Default stubbing – individual tests will override as needed.
        when(mockCollector1.collect()).thenReturn(Arrays.asList("collected1"));
        when(mockCollector2.collect()).thenReturn(Arrays.asList("v2"));
        when(mockCollector3.collect()).thenReturn(Arrays.asList("v3"));
        when(mockNullCollector.collect()).thenReturn(null);
        when(mockExceptionCollector.collect()).thenThrow(new RuntimeException("collector failure"));
    }

    /* --------------------------------------------------------------------- */
    /* Helper to read the private collectorLoadMap via reflection            */
    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getCollectorLoadMap() throws Exception {
        Field f = CollectorContext.class.getDeclaredField("collectorLoadMap");
        f.setAccessible(true);
        return (Map<String, Object>) f.get(collectorContext);
    }

    /* --------------------------------------------------------------------- */
    /* 1. loadCollectors – empty collectorMap                               */
    /* --------------------------------------------------------------------- */
    @Test
    void emptyCollectorMapShouldLeaveLoadMapEmpty() throws Exception {
        // No entries are added to the context.

        collectorContext.loadCollectors();

        Map<String, Object> loadMap = getCollectorLoadMap();
        assertTrue(loadMap.isEmpty(),
                "collectorLoadMap should remain empty when collectorMap has no entries");
    }

    /* --------------------------------------------------------------------- */
    /* 2. loadCollectors – single collector                                  */
    /* --------------------------------------------------------------------- */
    @Test
    void singleCollectorShouldPopulateLoadMap() throws Exception {
        collectorContext.add(C1, mockCollector1);

        collectorContext.loadCollectors();

        // Verify via public getter.
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) collectorContext.get(C1);
        assertNotNull(result);
        assertEquals(Collections.singletonList("collected1"), result);

        // Verify internal map size via reflection.
        Map<String, Object> loadMap = getCollectorLoadMap();
        assertEquals(1, loadMap.size(), "Exactly one entry should be present");
        assertTrue(loadMap.containsKey(C1), "Key 'c1' must be present");
        assertEquals(result, loadMap.get(C1));

        verify(mockCollector1, times(1)).collect();
    }

    /* --------------------------------------------------------------------- */
    /* 3. loadCollectors – multiple collectors (parameterized)              */
    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("multipleCollectorsProvider")
    void multipleCollectorsShouldAllBeLoaded(Map<String, Collector<?>> collectors,
                                             Map<String, List<String>> expected) throws Exception {
        // Populate context with the supplied collectors.
        collectors.forEach(collectorContext::add);

        collectorContext.loadCollectors();

        // Verify each expected entry via the public getter.
        expected.forEach((key, expectedValue) -> {
            @SuppressWarnings("unchecked")
            List<String> actual = (List<String>) collectorContext.get(key);
            assertNotNull(actual, "Result for key " + key + " should not be null");
            assertEquals(expectedValue, actual,
                    "Collector result mismatch for key " + key);
        });

        // Verify internal map size.
        Map<String, Object> loadMap = getCollectorLoadMap();
        assertEquals(expected.size(), loadMap.size(),
                "collectorLoadMap size must match number of collectors");

        // Verify each mock's collect() was invoked once.
        collectors.values().forEach(c -> verify(c, times(1)).collect());
    }

    static Stream<Arguments> multipleCollectorsProvider() {
        Collector<List<String>> c1 = mock(Collector.class);
        Collector<List<String>> c2 = mock(Collector.class);
        Collector<List<String>> c3 = mock(Collector.class);
        when(c1.collect()).thenReturn(Arrays.asList("v1"));
        when(c2.collect()).thenReturn(Arrays.asList("v2"));
        when(c3.collect()).thenReturn(Arrays.asList("v3"));

        Map<String, Collector<?>> map = new LinkedHashMap<>();
        map.put("c1", c1);
        map.put("c2", c2);
        map.put("c3", c3);

        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("c1", Arrays.asList("v1"));
        expected.put("c2", Arrays.asList("v2"));
        expected.put("c3", Arrays.asList("v3"));

        return Stream.of(Arguments.of(map, expected));
    }

    /* --------------------------------------------------------------------- */
    /* 4. loadCollectors – non‑collector entries ignored                     */
    /* --------------------------------------------------------------------- */
    @Test
    void nonCollectorEntriesShouldBeIgnored() throws Exception {
        List<String> plainList = new ArrayList<>(Collections.singletonList("plain"));
        collectorContext.add(NON_COLLECTOR, plainList);
        collectorContext.add(C1, mockCollector1);

        collectorContext.loadCollectors();

        // Non‑collector value stays the same.
        Object nonCollectorResult = collectorContext.get(NON_COLLECTOR);
        assertSame(plainList, nonCollectorResult,
                "Non‑collector entry must remain unchanged after loadCollectors()");

        // Collector entry is loaded.
        @SuppressWarnings("unchecked")
        List<String> collectorResult = (List<String>) collectorContext.get(C1);
        assertNotNull(collectorResult);
        assertEquals(Collections.singletonList("collected1"), collectorResult);

        // Internal map must contain only the collector key.
        Map<String, Object> loadMap = getCollectorLoadMap();
        assertEquals(1, loadMap.size());
        assertTrue(loadMap.containsKey(C1));
        assertFalse(loadMap.containsKey(NON_COLLECTOR));

        verify(mockCollector1, times(1)).collect();
    }

    /* --------------------------------------------------------------------- */
    /* 5. loadCollectors – mixed collectors and non‑collectors              */
    /* --------------------------------------------------------------------- */
    @Test
    void mixedCollectorsAndNonCollectorsShouldProcessOnlyCollectors() throws Exception {
        // collectors
        collectorContext.add(C1, mockCollector1);
        collectorContext.add(C3, mockCollector3);
        // non‑collector
        collectorContext.add(NON_COLLECTOR, "just a string");

        // Stub distinct return values.
        when(mockCollector1.collect()).thenReturn(Arrays.asList("val1"));
        when(mockCollector3.collect()).thenReturn(Arrays.asList("val3"));

        collectorContext.loadCollectors();

        // Verify collector results.
        @SuppressWarnings("unchecked")
        List<String> r1 = (List<String>) collectorContext.get(C1);
        @SuppressWarnings("unchecked")
        List<String> r3 = (List<String>) collectorContext.get(C3);
        assertEquals(Arrays.asList("val1"), r1);
        assertEquals(Arrays.asList("val3"), r3);

        // Verify non‑collector unchanged.
        assertEquals("just a string", collectorContext.get(NON_COLLECTOR));

        // Internal map should contain only the two collector keys.
        Map<String, Object> loadMap = getCollectorLoadMap();
        assertEquals(2, loadMap.size());
        assertTrue(loadMap.containsKey(C1));
        assertTrue(loadMap.containsKey(C3));
        assertFalse(loadMap.containsKey(NON_COLLECTOR));
    }

    /* --------------------------------------------------------------------- */
    /* 6. loadCollectors – collector returns null                           */
    /* --------------------------------------------------------------------- */
    @Test
    void collectorReturningNullShouldStoreNullValue() throws Exception {
        collectorContext.add("nullCollector", mockNullCollector);

        collectorContext.loadCollectors();

        Object result = collectorContext.get("nullCollector");
        assertNull(result, "collectorLoadMap must contain a null value for a collector that returns null");

        Map<String, Object> loadMap = getCollectorLoadMap();
        assertTrue(loadMap.containsKey("nullCollector"));
        assertNull(loadMap.get("nullCollector"));
    }

    /* --------------------------------------------------------------------- */
    /* 7. loadCollectors – collector throws exception                       */
    /* --------------------------------------------------------------------- */
    @Test
    void collectorThrowingExceptionShouldPropagate() {
        collectorContext.add("badCollector", mockExceptionCollector);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> collectorContext.loadCollectors(),
                "Exception thrown by Collector.collect() must propagate");
        assertEquals("collector failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 8. loadCollectors – overwrite existing collectorLoadMap entries      */
    /* --------------------------------------------------------------------- */
    @Test
    void subsequentLoadCollectorsCallShouldOverwritePreviousValues() throws Exception {
        // First collector that returns "new1"
        Collector<String> first = mock(Collector.class);
        when(first.collect()).thenReturn("new1");
        collectorContext.add(C1, first);

        collectorContext.loadCollectors();

        assertEquals("new1", collectorContext.get(C1));

        // Change stub to return a different value
        when(first.collect()).thenReturn("new2");

        collectorContext.loadCollectors();

        assertEquals("new2", collectorContext.get(C1));

        // Verify collect() called twice in total.
        verify(first, times(2)).collect();

        // Internal map size stays 1.
        Map<String, Object> loadMap = getCollectorLoadMap();
        assertEquals(1, loadMap.size());
        assertEquals("new2", loadMap.get(C1));
    }

    /* --------------------------------------------------------------------- */
    /* 9. loadCollectors – concurrent modification during collection        */
    /* --------------------------------------------------------------------- */
    @Test
    void collectorModifyingCollectorMapDuringLoadShouldThrowConcurrentModification() {
        // Real collector that adds a new entry when collect() is invoked.
        Collector<Object> mutatingCollector = new Collector<Object>() {
            @Override
            public Object collect() {
                // This modification will trigger ConcurrentModificationException
                // on the next iteration of the for‑each loop inside loadCollectors().
                collectorContext.add("newDuringCollect", "newValue");
                return "mutated";
            }
        };

        collectorContext.add(C1, mutatingCollector);
        // Add another collector to ensure iteration proceeds beyond the first entry.
        collectorContext.add(C2, mockCollector2); // stub already defined

        // Expect ConcurrentModificationException.
        assertThrows(ConcurrentModificationException.class,
                () -> collectorContext.loadCollectors(),
                "Modifying collectorMap during iteration must result in ConcurrentModificationException");
    }

    /* --------------------------------------------------------------------- */
    /* Existing tests from the starter (kept unchanged)                     */
    /* --------------------------------------------------------------------- */
    @Test
    void shouldLoadCollectorResultIntoContext() {
        collectorContext.add("myCollector", mockCollector1);
        when(mockCollector1.collect()).thenReturn(Arrays.asList("value1", "value2"));

        collectorContext.loadCollectors();

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) collectorContext.get("myCollector");
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get(0));
        assertEquals("value2", result.get(1));

        verify(mockCollector1, times(1)).collect();
    }

    @Test
    void shouldIgnoreNonCollectorEntries() {
        List<String> plainList = new ArrayList<>();
        plainList.add("plain");
        collectorContext.add(NON_COLLECTOR, plainList);
        collectorContext.add("myCollector", mockCollector1);
        when(mockCollector1.collect()).thenReturn(Arrays.asList("value1", "value2"));

        collectorContext.loadCollectors();

        Object nonCollectorResult = collectorContext.get(NON_COLLECTOR);
        assertSame(plainList, nonCollectorResult);

        @SuppressWarnings("unchecked")
        List<String> collectorResult = (List<String>) collectorContext.get("myCollector");
        assertNotNull(collectorResult);
        assertEquals(2, collectorResult.size());

        verify(mockCollector1, times(1)).collect();
    }
}
```