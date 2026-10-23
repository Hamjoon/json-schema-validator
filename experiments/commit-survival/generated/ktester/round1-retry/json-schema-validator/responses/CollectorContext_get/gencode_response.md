```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link CollectorContext#get(String)}.
 *
 * <p>The tests cover all logical branches of the method, using a
 * parameterized test to drive the different input configurations that are
 * described in the JSON test‑case specification.</p>
 *
 * <p>Because {@code collectorMap} and {@code collectorLoadMap} are private,
 * reflection is used to manipulate them directly.  No production code is
 * executed – the test only verifies the behaviour of {@code get} under
 * various internal‑state setups.</p>
 */
@ExtendWith(MockitoExtension.class)
class CollectorContext_get_Test {

    private CollectorContext context;

    /** Constant key used in several simple tests. */
    private static final String SIMPLE_KEY = "plainObject";

    /** Constant value used for plain‑object cases. */
    private static final String PLAIN_VALUE = "plainValue";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        context = new CollectorContext();
        clearInternalMaps(context);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /* --------------------------------------------------------------------- */
    /* Existing explicit tests (kept for readability)                        */
    /* --------------------------------------------------------------------- */

    @Test
    void get_returnsNullWhenKeyAbsent() {
        assertNull(context.get("nonExistingKey"));
    }

    @Test
    void get_returnsPlainObjectWhenNotCollector() throws Exception {
        putIntoCollectorMap(context, SIMPLE_KEY, PLAIN_VALUE);
        assertEquals(PLAIN_VALUE, context.get(SIMPLE_KEY));
    }

    @Test
    void get_returnsCollectorWhenNoLoadedValue() throws Exception {
        @SuppressWarnings("unchecked")
        Collector<String> mockCollector = mock(Collector.class);
        putIntoCollectorMap(context, "collectorOnly", mockCollector);
        Object result = context.get("collectorOnly");
        assertSame(mockCollector, result);
    }

    @Test
    void get_returnsLoadedValueWhenCollectorAndLoadPresent() throws Exception {
        @SuppressWarnings("unchecked")
        Collector<String> mockCollector = mock(Collector.class);
        putIntoCollectorMap(context, "collectorWithLoad", mockCollector);
        putIntoCollectorLoadMap(context, "collectorWithLoad", "loadedValue");
        assertEquals("loadedValue", context.get("collectorWithLoad"));
    }

    /* --------------------------------------------------------------------- */
    /* Parameterized tests covering the exhaustive JSON test‑case matrix     */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideGetCases")
    void get_variousScenarios(String name,
                              Runnable mapSetup,
                              Object expected,
                              String description) throws Exception {
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
        Collector<String> nullCollector = mock(Collector.class); // will be stored as null in map
        @SuppressWarnings("unchecked")
        Collector<String> collectorWithIntegerLoad = mock(Collector.class);
        @SuppressWarnings("unchecked")
        Collector<String> collectorOnlyNoLoad = mock(Collector.class);

        return Stream.of(
            // 1. missingKey – nothing in either map
            arguments("missingKey",
                (Runnable) () -> {}, // no setup
                null,
                "When the key is absent from both maps, get() should return null"),

            // 2. plainObject – plain value, no collector
            arguments("plainObject",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("plainObject", plainValue);
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                plainValue,
                "Key exists with a non‑Collector value; the same value should be returned"),

            // 3. collectorOnly – collector present, no loaded value
            arguments("collectorOnly",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("collectorOnly", collectorOnly);
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                collectorOnly,
                "Collector stored without a loaded entry should be returned as‑is"),

            // 4. collectorWithLoad – collector + loaded value
            arguments("collectorWithLoad",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("collectorWithLoad", collectorWithLoad);
                        putIntoCollectorLoadMapStatic("collectorWithLoad", "loadedData");
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                "loadedData",
                "When a loaded value exists for a collector, the loaded value wins"),

            // 5. loadOnly – only load map contains a value
            arguments("loadOnly",
                (Runnable) () -> {
                    try {
                        putIntoCollectorLoadMapStatic("loadOnly", "loadOnlyValue");
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                null,
                "Value present only in collectorLoadMap must be ignored; method returns null"),

            // 6. null key – both maps empty
            arguments(null,
                (Runnable) () -> {}, // no setup
                null,
                "Calling get(null) with empty maps should return null"),

            // 7. empty string key – plain object with load map entry
            arguments("",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("", emptyValue);
                        putIntoCollectorLoadMapStatic("", "ignoredLoad");
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                emptyValue,
                "Empty string key with plain object should return the plain object, ignoring load map"),

            // 8. collectorNullLoad – collector + explicit null load entry
            arguments("collectorNullLoad",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("collectorNullLoad", collectorNullLoad);
                        putIntoCollectorLoadMapStatic("collectorNullLoad", null);
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                collectorNullLoad,
                "Explicit null in load map does not override the collector instance"),

            // 9. nullCollector – null entry in collectorMap, non‑null load entry
            arguments("nullCollector",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("nullCollector", null);
                        putIntoCollectorLoadMapStatic("nullCollector", "someLoad");
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                null,
                "When collectorMap contains null, the method returns null even if load map has a value"),

            //10. bothNull – both maps contain explicit null
            arguments("bothNull",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("bothNull", null);
                        putIntoCollectorLoadMapStatic("bothNull", null);
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                null,
                "Both maps contain null; get() must return null"),

            //11. plainWithLoad – plain object in collectorMap, different value in load map
            arguments("plainWithLoad",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("plainWithLoad", plainValue);
                        putIntoCollectorLoadMapStatic("plainWithLoad", "differentLoad");
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                plainValue,
                "Plain object takes precedence over any load‑map value"),

            //12. collectorWithIntegerLoad – collector + Integer load value
            arguments("collectorWithIntegerLoad",
                (Runnable) () -> {
                    try {
                        putIntoCollectorMapStatic("collectorWithIntegerLoad", collectorWithIntegerLoad);
                        putIntoCollectorLoadMapStatic("collectorWithIntegerLoad", 42);
                    } catch (Exception e) { throw new RuntimeException(e); }
                },
                42,
                "Load map may contain values of any type; the integer should be returned")
        );
    }

    /* --------------------------------------------------------------------- */
    /* Helper methods that operate on the *current* test instance via reflection */
    /* --------------------------------------------------------------------- */

    /** Clears both internal maps of the supplied {@link CollectorContext}. */
    private static void clearInternalMaps(CollectorContext ctx) throws Exception {
        Field mapField = CollectorContext.class.getDeclaredField("collectorMap");
        mapField.setAccessible(true);
        ((Map<?, ?>) mapField.get(ctx)).clear();

        Field loadMapField = CollectorContext.class.getDeclaredField("collectorLoadMap");
        loadMapField.setAccessible(true);
        ((Map<?, ?>) loadMapField.get(ctx)).clear();
    }

    /** Inserts a key/value pair into {@code collectorMap} of the current test instance. */
    private static void putIntoCollectorMapStatic(String key, Object value) throws Exception {
        Field mapField = CollectorContext.class.getDeclaredField("collectorMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) mapField.get(context);
        map.put(key, value);
    }

    /** Inserts a key/value pair into {@code collectorLoadMap} of the current test instance. */
    private static void putIntoCollectorLoadMapStatic(String key, Object value) throws Exception {
        Field loadMapField = CollectorContext.class.getDeclaredField("collectorLoadMap");
        loadMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) loadMapField.get(context);
        map.put(key, value);
    }

    /** Minimal {@link Collector} interface stub – the real library defines this interface. */
    private interface Collector<E> {
        // No methods required for testing.
    }
}
```