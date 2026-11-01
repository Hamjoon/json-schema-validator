package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidators_Test {

    // ----- Mocks for collaborators -------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonSchemaFactory jsonSchemaFactory;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    // ----- Object under test -------------------------------------------------------
    JsonSchema jsonSchema;

    // ----- Test data ----------------------------------------------------------------
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // e.g. {"type":"object"}
    private JsonNode simpleSchemaNode;

    // spy that allows us to stub private read()
    JsonSchema spySchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---- Prepare a minimal JSON schema node ------------------------------------
        simpleSchemaNode = MAPPER.readTree("{\"type\":\"object\"}");
        // ---- Stub the ValidationContext behaviour ---------------------------------
        // resolveSchemaId is used in the constructor – we let it return null
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        // getMetaSchema is used several times
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // The constructor stores references to three concurrent maps – we give it fresh ones
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // metaSchema.readAnchor / readDynamicAnchor are called – return null to keep things simple
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // ---- Create JsonSchema via reflection (private constructor) -----------------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        // parent = null, suppressSubSchemaRetrieval = false
        jsonSchema = ctor.newInstance(validationContext, schemaLocation, evaluationPath, simpleSchemaNode, null, false);
    }

    @Test
    void getValidators_returnsUnmodifiableList_andCachesResult() {
        // First call – should load validators (the list may be empty for our tiny schema)
        List<JsonValidator> firstCall = jsonSchema.getValidators();
        assertNotNull(firstCall, "First call to getValidators() must not return null");
        // The list is expected to be unmodifiable – trying to modify it should fail
        assertThrows(UnsupportedOperationException.class, () -> firstCall.add(mock(JsonValidator.class)));
        // Second call – should return the exact same list instance (cached)
        List<JsonValidator> secondCall = jsonSchema.getValidators();
        assertSame(firstCall, secondCall, "Subsequent calls must return the cached list instance");
    }

    @Test
    void getValidators_isThreadSafeWhenCalledConcurrently() throws InterruptedException {
        // Run getValidators() from multiple threads to ensure no race conditions
        final List<JsonValidator>[] results = new List[10];
        Thread[] threads = new Thread[results.length];
        for (int i = 0; i < results.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> results[idx] = jsonSchema.getValidators());
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        // All results must be the same (identical reference)
        for (int i = 1; i < results.length; i++) {
            assertSame(results[0], results[i], "All concurrent calls should return the same cached list instance");
        }
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_lazyInitialisation_wrapsAndCachesResult() throws Exception {
        // Prepare mutable list that the private read() will return
        JsonValidator validatorA = mock(JsonValidator.class);
        JsonValidator validatorB = mock(JsonValidator.class);
        List<JsonValidator> mutable = new ArrayList<>(Arrays.asList(validatorA, validatorB));
        // Count how many times read() is invoked
        AtomicInteger readCalls = new AtomicInteger(0);
        doAnswer(invocation -> {
            readCalls.incrementAndGet();
            return mutable;
        }).when(spySchema, "read", any(JsonNode.class));
        // ---- First call – triggers lazy initialisation ---------------------------
        List<JsonValidator> first = spySchema.getValidators();
        assertNotNull(first, "Returned list must not be null");
        assertEquals(2, first.size(), "List should contain the two validators supplied by read()");
        assertThrows(UnsupportedOperationException.class, () -> first.add(mock(JsonValidator.class)), "Returned list must be unmodifiable");
        // Verify that read() was called exactly once
        assertEquals(1, readCalls.get(), "read() should be invoked once on first call");
        // ---- Second call – must return the cached instance without another read ----
        List<JsonValidator> second = spySchema.getValidators();
        assertSame(first, second, "Subsequent calls must return the cached list instance");
        assertEquals(1, readCalls.get(), "read() must not be invoked again");
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_returnsEmptyUnmodifiableList() throws Exception {
        AtomicInteger readCalls = new AtomicInteger(0);
        doAnswer(invocation -> {
            readCalls.incrementAndGet();
            return Collections.emptyList();
        }).when(spySchema, "read", any(JsonNode.class));
        List<JsonValidator> result = spySchema.getValidators();
        assertNotNull(result);
        assertTrue(result.isEmpty(), "List should be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.add(mock(JsonValidator.class)), "Even an empty list must be unmodifiable");
        assertEquals(1, readCalls.get());
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_propagatesNullPointerExceptionWhenReadReturnsNull() throws Exception {
        doReturn(null).when(spySchema, "read", any(JsonNode.class));
        assertThrows(NullPointerException.class, () -> spySchema.getValidators(), "Calling getValidators() when read() returns null should throw NPE");
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_isThreadSafeDuringConcurrentInitialisation() throws Exception {
        JsonValidator vX = mock(JsonValidator.class);
        JsonValidator vY = mock(JsonValidator.class);
        List<JsonValidator> mutable = new ArrayList<>(Arrays.asList(vX, vY));
        AtomicInteger readCalls = new AtomicInteger(0);
        doAnswer(invocation -> {
            // Simulate a small delay to increase chance of race if not thread‑safe
            Thread.sleep(10);
            readCalls.incrementAndGet();
            return mutable;
        }).when(spySchema, "read", any(JsonNode.class));
        final int THREADS = 10;
        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<JsonValidator>>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            futures.add(exec.submit(() -> {
                ready.countDown();
                // wait until all threads are ready
                start.await();
                return spySchema.getValidators();
            }));
        }
        // Wait until all threads are ready, then release them simultaneously
        ready.await();
        start.countDown();
        List<JsonValidator> firstResult = null;
        for (Future<List<JsonValidator>> f : futures) {
            List<JsonValidator> res = f.get(5, TimeUnit.SECONDS);
            if (firstResult == null) {
                firstResult = res;
            } else {
                assertSame(firstResult, res, "All threads must receive the same cached list instance");
            }
        }
        exec.shutdownNow();
        assertEquals(1, readCalls.get(), "read() must be invoked exactly once despite concurrent calls");
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_defensiveCopyPreventsExternalMutation() throws Exception {
        JsonValidator a = mock(JsonValidator.class);
        JsonValidator b = mock(JsonValidator.class);
        List<JsonValidator> original = new ArrayList<>(Collections.singletonList(a));
        AtomicInteger readCalls = new AtomicInteger(0);
        doAnswer(invocation -> {
            readCalls.incrementAndGet();
            return original;
        }).when(spySchema, "read", any(JsonNode.class));
        List<JsonValidator> cached = spySchema.getValidators();
        assertEquals(1, cached.size());
        assertSame(a, cached.get(0));
        // Mutate the original list after initialisation
        original.add(b);
        // Cached list must remain unchanged
        assertEquals(1, cached.size(), "Cached list size must stay unchanged after original list mutation");
        assertSame(a, cached.get(0), "Cached list must still contain only the original element");
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_usesPrepopulatedFieldWithoutCallingRead() throws Exception {
        // Prepare an unmodifiable list and inject it via reflection
        JsonValidator c = mock(JsonValidator.class);
        List<JsonValidator> injected = Collections.unmodifiableList(Collections.singletonList(c));
        Field validatorsField = JsonSchema.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        validatorsField.set(jsonSchema, injected);
        // Spy on the same instance to ensure read() would fail if called
        JsonSchema spy = spy(jsonSchema);
        doAnswer(invocation -> {
            fail("read() should not be invoked when validators field is already set");
            return null;
        }).when(spy, "read", any(JsonNode.class));
        List<JsonValidator> result = spy.getValidators();
        assertSame(injected, result, "Returned list must be the exact instance injected via reflection");
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_preservesOrderFromRead() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        JsonValidator v3 = mock(JsonValidator.class);
        List<JsonValidator> mutable = new ArrayList<>(Arrays.asList(v1, v2, v3));
        AtomicInteger readCalls = new AtomicInteger(0);
        doAnswer(invocation -> {
            readCalls.incrementAndGet();
            return mutable;
        }).when(spySchema, "read", any(JsonNode.class));
        List<JsonValidator> result = spySchema.getValidators();
        assertEquals(3, result.size());
        assertEquals(v1, result.get(0));
        assertEquals(v2, result.get(1));
        assertEquals(v3, result.get(2));
        assertEquals(1, readCalls.get());
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_returnsSameInstanceWhenReadProvidesUnmodifiableList() throws Exception {
        JsonValidator u = mock(JsonValidator.class);
        List<JsonValidator> alreadyUnmodifiable = Collections.unmodifiableList(Collections.singletonList(u));
        AtomicInteger readCalls = new AtomicInteger(0);
        doAnswer(invocation -> {
            readCalls.incrementAndGet();
            return alreadyUnmodifiable;
        }).when(spySchema, "read", any(JsonNode.class));
        List<JsonValidator> result = spySchema.getValidators();
        assertSame(alreadyUnmodifiable, result, "When read() already returns an unmodifiable list, getValidators() should return that exact instance");
        assertEquals(1, readCalls.get());
    }

    // -------------------------------------------------------------------------
    @Test
    void getValidators_propagatesExceptionFromRead() throws Exception {
        IllegalStateException ex = new IllegalStateException("boom");
        doThrow(ex).when(spySchema, "read", any(JsonNode.class));
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> spySchema.getValidators(), "Exception from read() must propagate unchanged");
        assertSame(ex, thrown);
    }
}
