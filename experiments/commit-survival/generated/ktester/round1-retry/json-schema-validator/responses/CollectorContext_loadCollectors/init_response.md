```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CollectorContext#loadCollectors()}.
 *
 * The tests focus on the behaviour of {@code loadCollectors()} without touching
 * private fields directly.  A {@link Collector} is mocked so that the
 * {@code collect()} method returns a known value.  The {@link CollectorContext}
 * API ( {@code add}, {@code get} ) is used to set up the pre‑condition and to
 * verify the post‑condition.
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_loadCollectors_Test {

    private CollectorContext collectorContext;

    @Mock
    private Collector<List<String>> mockCollector; // generic type is irrelevant for the test

    private static final String COLLECTOR_NAME = "myCollector";
    private static final String NON_COLLECTOR_NAME = "plainObject";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh context for each test to avoid cross‑test contamination.
        collectorContext = new CollectorContext();

        // Configure the mock collector to return a deterministic value.
        List<String> collectedData = Arrays.asList("value1", "value2");
        when(mockCollector.collect()).thenReturn(collectedData);
    }

    @AfterEach
    void teardownAfterEach() {
        // Mockito automatically resets @Mock fields between tests, nothing else needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    /**
     * Verifies that when a {@link Collector} is present in the context,
     * {@code loadCollectors()} stores the result of {@code collect()} in the
     * internal load map and makes it accessible via {@code get()}.
     */
    @Test
    void shouldLoadCollectorResultIntoContext() {
        // GIVEN – a collector added to the context
        collectorContext.add(COLLECTOR_NAME, mockCollector);

        // WHEN – collectors are loaded
        collectorContext.loadCollectors();

        // THEN – the context should contain the collected data under the same key
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) collectorContext.get(COLLECTOR_NAME);
        assertNotNull(result, "Result should not be null after loading collectors");
        assertEquals(2, result.size(), "Collected list should contain exactly two elements");
        assertEquals("value1", result.get(0));
        assertEquals("value2", result.get(1));

        // Also verify that the collector's collect() method was invoked exactly once.
        verify(mockCollector, times(1)).collect();
    }

    /**
     * Verifies that non‑collector objects stored in the context are ignored by
     * {@code loadCollectors()} – they must not appear in the load map.
     */
    @Test
    void shouldIgnoreNonCollectorEntries() {
        // GIVEN – a plain object (not a Collector) added to the context
        List<String> plainList = new ArrayList<>();
        plainList.add("plain");
        collectorContext.add(NON_COLLECTOR_NAME, plainList);

        // Also add a proper collector to ensure the method still works for valid entries.
        collectorContext.add(COLLECTOR_NAME, mockCollector);

        // WHEN – collectors are loaded
        collectorContext.loadCollectors();

        // THEN – the plain object should not be present in the load map (get returns the original object)
        Object nonCollectorResult = collectorContext.get(NON_COLLECTOR_NAME);
        assertSame(plainList, nonCollectorResult,
                "Non‑collector entries must remain unchanged after loadCollectors()");

        // The collector entry must still be populated with the collected data.
        @SuppressWarnings("unchecked")
        List<String> collectorResult = (List<String>) collectorContext.get(COLLECTOR_NAME);
        assertNotNull(collectorResult);
        assertEquals(2, collectorResult.size());

        // Verify collect() was called once for the collector and never for the non‑collector.
        verify(mockCollector, times(1)).collect();
    }
}
```