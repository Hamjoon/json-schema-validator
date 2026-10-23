```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_combineWithCollector_Test {

    private static final String COLLECTOR_NAME = "myCollector";

    private CollectorContext collectorContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CollectorContext for every test to avoid state leakage.
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for now.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global tear‑down needed.
    }

    /**
     * Verify that when a {@link Collector} is registered under the supplied name,
     * {@code combineWithCollector} forwards the data to the collector's {@code combine}
     * method.
     */
    @Test
    void shouldCombineDataWhenCollectorIsPresent() {
        // given
        @SuppressWarnings("unchecked")
        Collector<Object> mockCollector = mock(Collector.class);
        collectorContext.add(COLLECTOR_NAME, mockCollector);
        String data = "sample-data";

        // when
        collectorContext.combineWithCollector(COLLECTOR_NAME, data);

        // then
        verify(mockCollector, times(1)).combine(data);
    }

    /**
     * Verify that if the name resolves to an object that is *not* a {@link Collector},
     * the method completes silently without throwing and without invoking any
     * collector logic.
     */
    @Test
    void shouldDoNothingWhenEntryIsNotACollector() {
        // given
        Object notACollector = new ArrayList<>();
        collectorContext.add(COLLECTOR_NAME, notACollector);
        String data = "irrelevant";

        // when / then – no exception should be thrown
        Assertions.assertDoesNotThrow(() ->
                collectorContext.combineWithCollector(COLLECTOR_NAME, data));
    }

    /**
     * Verify that when the supplied name is absent from the context, the method
     * simply returns without side effects.
     */
    @Test
    void shouldDoNothingWhenNoEntryExists() {
        // given – no entry added for COLLECTOR_NAME
        String data = "any-data";

        // when / then – should not throw
        Assertions.assertDoesNotThrow(() ->
                collectorContext.combineWithCollector(COLLECTOR_NAME, data));
    }
}
```