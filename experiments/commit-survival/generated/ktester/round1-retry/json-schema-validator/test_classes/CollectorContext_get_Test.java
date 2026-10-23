package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CollectorContext_get_Test {

    private CollectorContext context;

    /**
     * Name used for all test entries
     */
    private static final String TEST_KEY = "testKey";

    /**
     * Simple object stored in the map (non‑collector)
     */
    private static final String SIMPLE_VALUE = "simpleValue";

    /**
     * Value that should be returned when a collector has been loaded
     */
    private static final String LOADED_VALUE = "loadedValue";

    /**
     * Constant key used in several simple tests.
     */
    private static final String SIMPLE_KEY = "plainObject";

    /**
     * Constant value used for plain‑object cases.
     */
    private static final String PLAIN_VALUE = "plainValue";

    /**
     * Minimal {@link Collector} interface stub – the real library defines this
     * interface; we only need the type for mocking.
     */
    private interface Collector<E> {
        // No methods are required for the purpose of these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        context = new CollectorContext();
        // Ensure the internal maps are empty before each test.
        clearInternalMaps(context);
    }

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
     * Supplies all test scenarios defined in the JSON specification.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the name to query</li>
     *   <li>a {@link Runnable} that prepares {@code collectorMap} and
     *       {@code collectorLoadMap} via reflection</li>
     *   <li>the expected return value</li>
     *   <li>a human readable description (used as the display name)</li>
     * </ul>
     */
    private static Stream<Arguments> provideGetCases() {
        // Shared plain objects
        String plainValue = "plain";
        String emptyValue = "emptyValue";
        // Mock collector instances – each case uses its own instance to avoid
        // accidental cross‑test sharing.
        @SuppressWarnings("unchecked")
        Collector<String> collectorOnly = mock(Collector.class);
        @SuppressWarnings("unchecked")
        Collector<String> collectorWithLoad = mock(Collector.class);
        @SuppressWarnings("unchecked")
        Collector<String> collectorNullLoad = mock(Collector.class);
        @SuppressWarnings("unchecked")
        Collector<String> // will be stored as null in map
        nullCollector = mock(Collector.class);
        @SuppressWarnings("unchecked")
        Collector<String> collectorWithIntegerLoad = mock(Collector.class);
        @SuppressWarnings("unchecked")
        Collector<String> collectorOnlyNoLoad = mock(Collector.class);
        return Stream.of(// 1. missingKey – nothing in either map
        arguments("missingKey", // no setup
        (Runnable) () -> {
        }, null, "When the key is absent from both maps, get() should return null"), // 2. plainObject – plain value, no collector
        arguments("plainObject", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("plainObject", plainValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, plainValue, "Key exists with a non‑Collector value; the same value should be returned"), // 3. collectorOnly – collector present, no loaded value
        arguments("collectorOnly", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("collectorOnly", collectorOnly);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, collectorOnly, "Collector stored without a loaded entry should be returned as‑is"), // 4. collectorWithLoad – collector + loaded value
        arguments("collectorWithLoad", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("collectorWithLoad", collectorWithLoad);
                putIntoCollectorLoadMapStatic("collectorWithLoad", "loadedData");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "loadedData", "When a loaded value exists for a collector, the loaded value wins"), // 5. loadOnly – only load map contains a value
        arguments("loadOnly", (Runnable) () -> {
            try {
                putIntoCollectorLoadMapStatic("loadOnly", "loadOnlyValue");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, null, "Value present only in collectorLoadMap must be ignored; method returns null"), // 6. null key – both maps empty
        arguments(null, // no setup
        (Runnable) () -> {
        }, null, "Calling get(null) with empty maps should return null"), // 7. empty string key – plain object with load map entry
        arguments("", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("", emptyValue);
                putIntoCollectorLoadMapStatic("", "ignoredLoad");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, emptyValue, "Empty string key with plain object should return the plain object, ignoring load map"), // 8. collectorNullLoad – collector + explicit null load entry
        arguments("collectorNullLoad", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("collectorNullLoad", collectorNullLoad);
                putIntoCollectorLoadMapStatic("collectorNullLoad", null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, collectorNullLoad, "Explicit null in load map does not override the collector instance"), // 9. nullCollector – null entry in collectorMap, non‑null load entry
        arguments("nullCollector", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("nullCollector", null);
                putIntoCollectorLoadMapStatic("nullCollector", "someLoad");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, null, "When collectorMap contains null, the method returns null even if load map has a value"), //10. bothNull – both maps contain explicit null
        arguments("bothNull", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("bothNull", null);
                putIntoCollectorLoadMapStatic("bothNull", null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, null, "Both maps contain null; get() must return null"), //11. plainWithLoad – plain object in collectorMap, different value in load map
        arguments("plainWithLoad", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("plainWithLoad", plainValue);
                putIntoCollectorLoadMapStatic("plainWithLoad", "differentLoad");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, plainValue, "Plain object takes precedence over any load‑map value"), //12. collectorWithIntegerLoad – collector + Integer load value
        arguments("collectorWithIntegerLoad", (Runnable) () -> {
            try {
                putIntoCollectorMapStatic("collectorWithIntegerLoad", collectorWithIntegerLoad);
                putIntoCollectorLoadMapStatic("collectorWithIntegerLoad", 42);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, 42, "Load map may contain values of any type; the integer should be returned"));
    }

    /**
     * Inserts a key/value pair into {@code collectorMap} of the current test instance.
     */
    private static void putIntoCollectorMapStatic(String key, Object value) throws Exception {
        Field mapField = CollectorContext.class.getDeclaredField("collectorMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) mapField.get(context);
        map.put(key, value);
    }

    /**
     * Inserts a key/value pair into {@code collectorLoadMap} of the current test instance.
     */
    private static void putIntoCollectorLoadMapStatic(String key, Object value) throws Exception {
        Field loadMapField = CollectorContext.class.getDeclaredField("collectorLoadMap");
        loadMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) loadMapField.get(context);
        map.put(key, value);
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
        putIntoCollectorMap(context, "collectorWithLoad", mockCollector);
        putIntoCollectorLoadMap(context, "collectorWithLoad", "loadedValue");
        assertEquals("loadedValue", context.get("collectorWithLoad"));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideGetCases")
    void get_variousScenarios(String name, Runnable mapSetup, Object expected, String description) throws Exception {
        // Arrange the internal maps according to the current case.
        mapSetup.run();
        // Act
        Object actual = context.get(name);
        // Assert – use identity check for Collector instances, equals otherwise.
        if (expected instanceof Collector) {
            assertSame(expected, actual, description);
        } else {
            assertEquals(expected, actual, description);
        }
    }
}
