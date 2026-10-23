```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CollectorContext#get(String)}.
 *
 * The tests cover the four logical paths inside the method:
 *
 * <ul>
 *   <li>key not present – returns {@code null}</li>
 *   <li>key present with a plain object – returns that object</li>
 *   <li>key present with a {@link Collector} but no loaded value – returns the collector</li>
 *   <li>key present with a {@link Collector} and a loaded value – returns the loaded value</li>
 * </ul>
 *
 * Because {@code collectorMap} and {@code collectorLoadMap} are private we use
 * reflection to populate them – this does not invoke any private methods of the
 * class under test.
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_get_Test {

    private CollectorContext context;

    /** Name used for all test entries */
    private static final String TEST_KEY = "testKey";

    /** Simple object stored in the map (non‑collector) */
    private static final String SIMPLE_VALUE = "simpleValue";

    /** Value that should be returned when a collector has been loaded */
    private static final String LOADED_VALUE = "loadedValue";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        context = new CollectorContext();

        // Ensure the internal maps are empty before each test.
        clearInternalMaps(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the test instance is discarded.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Verifies that {@code get} returns {@code null} when the requested name is
     * not present in either internal map.
     */
    @Test
    void get_returnsNullWhenKeyAbsent() {
        assertNull(context.get("nonExistingKey"));
    }

    /**
     * Verifies that a plain (non‑collector) object stored in {@code collectorMap}
     * is returned unchanged.
     */
    @Test
    void get_returnsPlainObjectWhenNotCollector() throws Exception {
        // Populate collectorMap with a simple String.
        putIntoCollectorMap(context, TEST_KEY, SIMPLE_VALUE);

        Object result = context.get(TEST_KEY);
        assertEquals(SIMPLE_VALUE, result);
    }

    /**
     * Verifies that when a {@link Collector} is stored but there is no entry in
     * {@code collectorLoadMap}, the collector itself is returned.
     */
    @Test
    void get_returnsCollectorWhenNoLoadedValue() throws Exception {
        @SuppressWarnings("unchecked")
        Collector<String> mockCollector = mock(Collector.class);

        putIntoCollectorMap(context, TEST_KEY, mockCollector);
        // Do NOT put anything into collectorLoadMap.

        Object result = context.get(TEST_KEY);
        assertSame(mockCollector, result);
    }

    /**
     * Verifies that when both a {@link Collector} is stored and a corresponding
     * loaded value exists in {@code collectorLoadMap}, the loaded value is
     * returned.
     */
    @Test
    void get_returnsLoadedValueWhenCollectorAndLoadPresent() throws Exception {
        @SuppressWarnings("unchecked")
        Collector<String> mockCollector = mock(Collector.class);

        putIntoCollectorMap(context, TEST_KEY, mockCollector);
        putIntoCollectorLoadMap(context, TEST_KEY, LOADED_VALUE);

        Object result = context.get(TEST_KEY);
        assertEquals(LOADED_VALUE, result);
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods to manipulate the private maps via reflection.       */
    /* --------------------------------------------------------------------- */

    /**
     * Clears both internal maps of the supplied {@link CollectorContext}
     * instance.
     */
    private static void clearInternalMaps(CollectorContext ctx) throws Exception {
        Field mapField = CollectorContext.class.getDeclaredField("collectorMap");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(ctx)).clear();

        Field loadMapField = CollectorContext.class.getDeclaredField("collectorLoadMap");
        loadMapField.setAccessible(true);
        ((Map<?, ?>) loadMapField.get(ctx)).clear();
    }

    /**
     * Inserts an entry into the private {@code collectorMap}.
     */
    private static void putIntoCollectorMap(CollectorContext ctx, String key, Object value) throws Exception {
        Field mapField = CollectorContext.class.getDeclaredField("collectorMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) mapField.get(ctx);
        map.put(key, value);
    }

    /**
     * Inserts an entry into the private {@code collectorLoadMap}.
     */
    private static void putIntoCollectorLoadMap(CollectorContext ctx, String key, Object value) throws Exception {
        Field loadMapField = CollectorContext.class.getDeclaredField("collectorLoadMap");
        loadMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) loadMapField.get(ctx);
        map.put(key, value);
    }

    /**
     * Minimal {@link Collector} interface stub – the real library defines this
     * interface; we only need the type for mocking.
     */
    private interface Collector<E> {
        // No methods are required for the purpose of these tests.
    }
}
```