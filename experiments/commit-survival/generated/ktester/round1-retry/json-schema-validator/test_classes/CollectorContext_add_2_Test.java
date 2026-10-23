package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectorContext_add_2_Test {

    /**
     * Instance under test
     */
    private CollectorContext collectorContext;

    /**
     * A sample key used in the tests
     */
    private static final String SAMPLE_KEY = "sampleKey";

    /**
     * A second sample key used for overwrite tests
     */
    private static final String SAMPLE_KEY_2 = "sampleKey2";

    @BeforeEach
    void setupBeforeEach() {
        // The CollectorContext has two constructors; the no‑arg one delegates to the
        // (boolean, boolean) constructor. Using the default constructor is sufficient
        // for exercising the add(String, Object) method.
        collectorContext = new CollectorContext();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the reference to help the GC and to guarantee a clean
        // state for the next test method.
        collectorContext = null;
    }

    /**
     * Provides arguments for the basic single‑add scenarios.
     * Each entry contains:
     *   - key (may be null)
     *   - value (may be null)
     *   - a description (used only for debugging)
     */
    private static Stream<Arguments> basicAddProvider() {
        return Stream.of(// normal non‑null key/value
        Arguments.of("firstKey", "firstValue", "normal key/value"), // null key, non‑null value
        Arguments.of(null, "nullKeyValue", "null key"), // non‑null key, null value
        Arguments.of("nullValueKey", null, "null value"), // empty string key
        Arguments.of("", "emptyKeyValue", "empty key"), // whitespace key
        Arguments.of("   ", "whitespaceKeyValue", "whitespace key"), // special chars key
        Arguments.of("!@#$%^&*()_+", "specialCharsValue", "special chars key"), // Unicode key
        Arguments.of("ключ", "unicodeValue", "unicode key"));
    }

    @Test
    void add_ShouldStoreObjectAndBeRetrievableViaGet() {
        // GIVEN
        List<String> payload = new ArrayList<>(Arrays.asList("value1", "value2"));
        // WHEN
        collectorContext.add(SAMPLE_KEY, payload);
        // THEN
        Object retrieved = collectorContext.get(SAMPLE_KEY);
        assertNotNull(retrieved, "The object stored under the key must not be null");
        assertTrue(retrieved instanceof List, "The retrieved object should be a List");
        @SuppressWarnings("unchecked")
        List<String> retrievedList = (List<String>) retrieved;
        assertEquals(payload, retrievedList, "The retrieved list must equal the one that was added");
    }

    @Test
    void add_ShouldOverwriteExistingEntryWithSameKey() {
        // GIVEN
        String firstValue = "first";
        String secondValue = "second";
        collectorContext.add(SAMPLE_KEY_2, firstValue);
        Object firstRetrieved = collectorContext.get(SAMPLE_KEY_2);
        assertEquals(firstValue, firstRetrieved, "Initial value must be stored correctly");
        // WHEN – add another object with the same key
        collectorContext.add(SAMPLE_KEY_2, secondValue);
        // THEN – the map should contain the second value, not the first
        Object secondRetrieved = collectorContext.get(SAMPLE_KEY_2);
        assertEquals(secondValue, secondRetrieved, "The value associated with the key should be overwritten");
        assertNotEquals(firstRetrieved, secondRetrieved, "The overwritten value must differ from the original");
    }

    @Test
    void add_ShouldAcceptNullValue() {
        // GIVEN
        // (nothing to arrange beyond the key)
        // WHEN
        collectorContext.add(SAMPLE_KEY, null);
        // THEN
        assertTrue(collectorContext.getAll().containsKey(SAMPLE_KEY), "Map must contain the key even if the value is null");
        assertNull(collectorContext.get(SAMPLE_KEY), "The stored value should be null");
    }

    @Test
    void getAll_ShouldReflectAllAddedEntries() {
        // GIVEN
        collectorContext.add("keyA", "valueA");
        collectorContext.add("keyB", 123);
        collectorContext.add("keyC", Collections.singletonList("listItem"));
        // WHEN
        Map<String, Object> all = collectorContext.getAll();
        // THEN
        assertEquals(3, all.size(), "All three entries must be present");
        assertEquals("valueA", all.get("keyA"));
        assertEquals(123, all.get("keyB"));
        assertEquals(Collections.singletonList("listItem"), all.get("keyC"));
    }

    @ParameterizedTest(name = "[{index}] add key=''{0}'' value=''{2}''")
    @MethodSource("basicAddProvider")
    void add_BasicScenarios_ShouldStoreCorrectly(String key, Object value, String _desc) {
        // WHEN
        collectorContext.add(key, value);
        // THEN
        Map<String, Object> map = collectorContext.getAll();
        assertEquals(1, map.size(), "Map size must be 1 after a single addition");
        assertTrue(map.containsKey(key), "Map must contain the provided key (null allowed)");
        assertEquals(value, map.get(key), "Stored value must exactly match the supplied value");
    }

    @Test
    void add_DuplicateKey_ShouldOverwriteWithoutIncreasingSize() {
        // first insertion
        collectorContext.add("duplicateKey", "firstObject");
        assertEquals(1, collectorContext.getAll().size());
        assertEquals("firstObject", collectorContext.get("duplicateKey"));
        // overwrite with a different object
        collectorContext.add("duplicateKey", "secondObject");
        assertEquals(1, collectorContext.getAll().size(), "Size must stay 1 after overwriting the same key");
        assertEquals("secondObject", collectorContext.get("duplicateKey"));
    }

    @Test
    void add_MultipleDistinctKeys_BulkInsertion_ShouldAccumulateSize() {
        collectorContext.add("bulkKey1", "value1");
        collectorContext.add("bulkKey2", "value2");
        collectorContext.add("bulkKey3", "value3");
        Map<String, Object> all = collectorContext.getAll();
        assertEquals(3, all.size(), "After three distinct adds the map size must be 3");
        assertEquals("value1", all.get("bulkKey1"));
        assertEquals("value2", all.get("bulkKey2"));
        assertEquals("value3", all.get("bulkKey3"));
    }

    @Test
    void add_IntegerValue_ShouldBeStoredAndRetrieved() {
        collectorContext.add("key1", 123);
        assertEquals(1, collectorContext.getAll().size());
        assertEquals(123, collectorContext.get("key1"));
    }

    @Test
    void add_CollectionValue_ShouldBeStoredAndRetrieved() {
        List<Integer> list = new ArrayList<>();
        collectorContext.add("key2", list);
        assertEquals(1, collectorContext.getAll().size());
        assertSame(list, collectorContext.get("key2"), "The exact collection instance should be stored");
    }

    @Test
    void add_CollectorContextValue_ShouldAcceptSelfReference() {
        CollectorContext inner = new CollectorContext();
        collectorContext.add("key3", inner);
        assertEquals(1, collectorContext.getAll().size());
        assertSame(inner, collectorContext.get("key3"));
    }

    @Test
    void add_IdempotentKeySameValue_Twice_SizeRemainsOne() {
        collectorContext.add("idempotentKey", "sameValue");
        assertEquals(1, collectorContext.getAll().size());
        collectorContext.add("idempotentKey", "sameValue");
        assertEquals(1, collectorContext.getAll().size(), "Adding the identical key/value again must not change size");
        assertEquals("sameValue", collectorContext.get("idempotentKey"));
    }

    @Test
    void add_OverwriteWithNull_ShouldReplaceExistingValueWithoutChangingSize() {
        collectorContext.add("overwriteWithNull", "initialValue");
        assertEquals(1, collectorContext.getAll().size());
        assertEquals("initialValue", collectorContext.get("overwriteWithNull"));
        collectorContext.add("overwriteWithNull", null);
        assertEquals(1, collectorContext.getAll().size(), "Size stays 1 after overwriting with null");
        assertNull(collectorContext.get("overwriteWithNull"));
    }

    @Test
    void add_TwoContexts_IndependentMaps() {
        CollectorContext first = new CollectorContext();
        CollectorContext second = new CollectorContext();
        first.add("sharedKey", "valueInFirstContext");
        assertEquals(1, first.getAll().size());
        assertEquals("valueInFirstContext", first.get("sharedKey"));
        assertEquals(0, second.getAll().size(), "Second context should start empty");
        second.add("sharedKey", "valueInSecondContext");
        assertEquals(1, second.getAll().size());
        assertEquals("valueInSecondContext", second.get("sharedKey"));
        // Ensure they are truly independent
        assertEquals("valueInFirstContext", first.get("sharedKey"));
        assertEquals("valueInSecondContext", second.get("sharedKey"));
    }

    @Test
    void add_VeryLongKey_ShouldBeHandledCorrectly() {
        // create a 10,000‑character key
        String veryLongKey = "x".repeat(10_000);
        String value = "longKeyValue";
        collectorContext.add(veryLongKey, value);
        Map<String, Object> all = collectorContext.getAll();
        assertEquals(1, all.size(), "Map size must be 1 after adding a very long key");
        assertTrue(all.containsKey(veryLongKey), "Map must contain the extremely long key");
        assertEquals(value, all.get(veryLongKey));
    }

    @Test
    void add_LargeListValue_ShouldBeStoredAndPreserveSize() {
        List<Integer> largeList = new ArrayList<>(Collections.nCopies(10_000, 1));
        collectorContext.add("largeListKey", largeList);
        Object retrieved = collectorContext.get("largeListKey");
        assertTrue(retrieved instanceof List, "Retrieved object must be a List");
        @SuppressWarnings("unchecked")
        List<Integer> retrievedList = (List<Integer>) retrieved;
        assertEquals(10_000, retrievedList.size(), "The stored list must retain its size of 10,000");
        assertEquals(largeList, retrievedList, "Content of the stored list must be identical");
    }

    @Test
    void add_NullKeyAndNullValue_ShouldBeStoredInMap() {
        collectorContext.add(null, null);
        Map<String, Object> all = collectorContext.getAll();
        assertEquals(1, all.size(), "Map should contain exactly one entry");
        assertTrue(all.containsKey(null), "Map must contain a null key");
        assertNull(all.get(null), "The value associated with the null key must be null");
        assertNull(collectorContext.get(null), "Direct get(null) must return null");
    }
}
