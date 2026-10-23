```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CollectorContext_add_2_Test {

    /** Instance under test */
    private CollectorContext collectorContext;

    /** A sample key used in the tests */
    private static final String SAMPLE_KEY = "sampleKey";

    /** A second sample key used for overwrite tests */
    private static final String SAMPLE_KEY_2 = "sampleKey2";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these unit tests.
    }

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

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup necessary.
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
}
```