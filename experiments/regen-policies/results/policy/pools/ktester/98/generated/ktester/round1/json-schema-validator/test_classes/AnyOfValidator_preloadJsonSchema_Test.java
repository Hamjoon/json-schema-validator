package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    /**
     * Mocks used for constructing the validator
     */
    @Mock
    SchemaLocation mockSchemaLocation;

    @Mock
    JsonNodePath mockEvaluationPath;

    @Mock
    ValidationContext mockValidationContext;

    /**
     * The validator under test (spied to verify protected calls)
     */
    AnyOfValidator validator;

    AnyOfValidator spyValidator;

    /**
     * Simple JSON array node representing an {@code anyOf} with two empty subschemas
     */
    private static final JsonNode ANY_OF_ARRAY;

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // Configure the mocks so that the constructor can run without
        // touching real resources.
        // -----------------------------------------------------------------
        // SchemaLocation.append(int) is used by the constructor – return the same mock.
        when(mockSchemaLocation.append(anyInt())).thenReturn(mockSchemaLocation);
        // JsonNodePath.append(int) is also used – return the same mock.
        when(mockEvaluationPath.append(anyInt())).thenReturn(mockEvaluationPath);
        // ValidationContext.newSchema(...) must return a JsonSchema instance.
        // The concrete type is irrelevant for this test, so we return a mock.
        JsonSchema mockSubSchema = mock(JsonSchema.class);
        when(mockValidationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSubSchema);
        // -----------------------------------------------------------------
        validator = new AnyOfValidator(mockSchemaLocation, mockEvaluationPath, ANY_OF_ARRAY, // parentSchema – not needed for this test
        null, mockValidationContext);
        // Spy on the instance so that we can verify calls to protected methods.
        spyValidator = Mockito.spy(validator);
        // Stub the protected method that actually pre‑loads the subschemas.
        // We only want to verify that it is invoked with the correct argument.
        doNothing().when(spyValidator).preloadJsonSchemas(anyList());
        // (If the implementation is trivial, the real method is fine.)
        doCallRealMethod().when(spyValidator).canShortCircuit();
        // -----------------------------------------------------------------
        when(mockSchemaLocation.append(anyInt())).thenReturn(mockSchemaLocation);
        when(mockEvaluationPath.append(anyInt())).thenReturn(mockEvaluationPath);
        // -----------------------------------------------------------------
        // Create a real AnyOfValidator instance using the constructor.
        // we return a generic mock.
        JsonSchema mockSubSchema = mock(JsonSchema.class);
        when(mockValidationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSubSchema);
        // -----------------------------------------------------------------
        validator = new AnyOfValidator(mockSchemaLocation, mockEvaluationPath, ANY_OF_ARRAY, // parentSchema – not needed for these tests
        null, mockValidationContext);
        // The short‑circuit flag method returns a boolean; let it execute the real code.
        // Spy the instance – we will verify protected method invocations.
        spyValidator = Mockito.spy(validator);
        // Individual tests may override this behaviour.
        doCallRealMethod().when(spyValidator).preloadJsonSchemas(anyList());
        // explicitly overrides it.
        doCallRealMethod().when(spyValidator).canShortCircuit();
    }

    /* --------------------------------------------------------------------- *
     *  Helper to replace the private `schemas` field via reflection.
     * --------------------------------------------------------------------- */
    private void setSchemas(AnyOfValidator v, List<JsonSchema> list) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(v, list);
    }

    @Test
    void preloadJsonSchemaShouldDelegateToPreloadJsonSchemasAndCacheFlag() {
        // Act: invoke the method under test.
        spyValidator.preloadJsonSchema();
        // -----------------------------------------------------------------
        // Verify that the protected helper was called exactly once with the
        // internal list of schemas (size 2, because the ANY_OF_ARRAY has two
        // elements).  The list instance is the private field inside the
        // validator; we capture the argument to assert its size.
        // -----------------------------------------------------------------
        ArgumentCaptor<List<JsonSchema>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());
        List<JsonSchema> passedSchemas = captor.getValue();
        assertNotNull(passedSchemas, "The list of schemas passed to preloadJsonSchemas must not be null");
        assertEquals(2, passedSchemas.size(), "preloadJsonSchemas should receive a list containing one JsonSchema per subschema");
        // -----------------------------------------------------------------
        // Verify that the short‑circuit flag method was invoked.
        // -----------------------------------------------------------------
        verify(spyValidator, times(1)).canShortCircuit();
    }

    /* --------------------------------------------------------------------- *
     *  2. Null schemas handling
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() throws NPE when internal schemas list is null")
    void nullSchemasShouldThrowNullPointerException() throws Exception {
        // inject null
        setSchemas(spyValidator, null);
        // The real preloadJsonSchemas will be invoked, leading to NPE.
        assertThrows(NullPointerException.class, () -> spyValidator.preloadJsonSchema(), "Expected NullPointerException when schemas list is null");
    }

    /* --------------------------------------------------------------------- *
     *  3. Empty schemas list
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() works with an empty schemas list")
    void emptySchemasListShouldCompleteWithoutError() throws Exception {
        setSchemas(spyValidator, Collections.emptyList());
        // Act – no exception expected.
        spyValidator.preloadJsonSchema();
        // Verify that preloadJsonSchemas was called with the empty list.
        ArgumentCaptor<List<JsonSchema>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());
        assertTrue(captor.getValue().isEmpty(), "The passed list must be empty");
        // Verify that canShortCircuit() was still invoked.
        verify(spyValidator, times(1)).canShortCircuit();
    }

    /* --------------------------------------------------------------------- *
     *  4. Single schema preload
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() preloads a single child schema")
    void singleSchemaShouldBePreloaded() throws Exception {
        JsonSchema child = mock(JsonSchema.class);
        setSchemas(spyValidator, Collections.singletonList(child));
        // Ensure child.preloadJsonSchema() does nothing (default for mock).
        doNothing().when(child).preloadJsonSchema();
        spyValidator.preloadJsonSchema();
        // Verify delegation.
        ArgumentCaptor<List<JsonSchema>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());
        assertEquals(1, captor.getValue().size());
        // Child schema should have been asked to preload.
        verify(child, times(1)).preloadJsonSchema();
        // Short‑circuit flag must be cached.
        verify(spyValidator, times(1)).canShortCircuit();
    }

    /* --------------------------------------------------------------------- *
     *  5. Multiple schemas preload
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() preloads all schemas in the list")
    void multipleSchemasShouldAllBePreloaded() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>();
        Collections.addAll(list, s1, s2, s3);
        setSchemas(spyValidator, list);
        // Stub child preload to do nothing.
        list.forEach(s -> doNothing().when(s).preloadJsonSchema());
        spyValidator.preloadJsonSchema();
        // Verify the internal list is passed unchanged.
        ArgumentCaptor<List<JsonSchema>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());
        assertEquals(3, captor.getValue().size());
        // Each child must be preloaded exactly once.
        list.forEach(s -> verify(s, times(1)).preloadJsonSchema());
        verify(spyValidator, times(1)).canShortCircuit();
    }

    /* --------------------------------------------------------------------- *
     *  6. Schema preload throws exception
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() propagates exception from child preload")
    void childPreloadExceptionShouldPropagate() throws Exception {
        JsonSchema bad = mock(JsonSchema.class);
        setSchemas(spyValidator, Collections.singletonList(bad));
        // Simulate failure during child preload.
        doThrow(new RuntimeException("child failure")).when(bad).preloadJsonSchema();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> spyValidator.preloadJsonSchema(), "Exception from child preload must be propagated");
        assertEquals("child failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  7. Schemas list containing a null element
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() throws NPE when schemas list contains null")
    void listContainingNullElementShouldThrowNullPointerException() throws Exception {
        JsonSchema good = mock(JsonSchema.class);
        List<JsonSchema> list = new ArrayList<>();
        list.add(good);
        // introduce null entry
        list.add(null);
        setSchemas(spyValidator, list);
        // The real preloadJsonSchemas will iterate and hit the null entry.
        assertThrows(NullPointerException.class, () -> spyValidator.preloadJsonSchema(), "Expected NPE when a null schema is present in the list");
    }

    /* --------------------------------------------------------------------- *
     *  8. Concurrent modification during preload
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() propagates ConcurrentModificationException on concurrent list change")
    void concurrentModificationDuringPreloadShouldThrowException() throws Exception {
        // Prepare a mutable list with a single schema that sleeps while preloading.
        JsonSchema slow = mock(JsonSchema.class);
        doAnswer(invocation -> {
            // Sleep long enough for the background thread to modify the list.
            Thread.sleep(200);
            return null;
        }).when(slow).preloadJsonSchema();
        List<JsonSchema> mutable = new ArrayList<>();
        mutable.add(slow);
        setSchemas(spyValidator, mutable);
        // Background thread that mutates the list after a short delay.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> modifier = executor.submit(() -> {
            try {
                // ensure the iterator has started
                Thread.sleep(50);
                // concurrent modification
                mutable.add(mock(JsonSchema.class));
            } catch (InterruptedException ignored) {
            }
        });
        // Expect ConcurrentModificationException from preloadJsonSchema().
        assertThrows(ConcurrentModificationException.class, () -> spyValidator.preloadJsonSchema(), "Concurrent modification should be detected and propagated");
        // re‑throw any unexpected exception from the modifier
        modifier.get();
        executor.shutdownNow();
    }

    /* --------------------------------------------------------------------- *
     *  9. Immutable schemas list causing UnsupportedOperationException
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() propagates UnsupportedOperationException from immutable list")
    void immutableSchemasListShouldThrowUnsupportedOperationException() throws Exception {
        JsonSchema schema = mock(JsonSchema.class);
        doNothing().when(schema).preloadJsonSchema();
        List<JsonSchema> immutable = Collections.unmodifiableList(new ArrayList<>(Collections.singletonList(schema)));
        setSchemas(spyValidator, immutable);
        // If the internal implementation tries to modify the list (e.g., replace
        // a cached schema), an UnsupportedOperationException will be thrown.
        // The test asserts that such an exception propagates.
        assertThrows(UnsupportedOperationException.class, () -> spyValidator.preloadJsonSchema(), "Expected UnsupportedOperationException when using an unmodifiable schemas list");
    }

    /* --------------------------------------------------------------------- *
     * 10. canShortCircuit() throws exception
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() propagates exception thrown by canShortCircuit()")
    void canShortCircuitExceptionShouldPropagate() {
        // Anonymous subclass that forces canShortCircuit() to fail.
        AnyOfValidator broken = new AnyOfValidator(mockSchemaLocation, mockEvaluationPath, ANY_OF_ARRAY, null, mockValidationContext) {

            @Override
            protected boolean canShortCircuit() {
                throw new IllegalStateException("short‑circuit failure");
            }
        };
        // Spy only to verify that preloadJsonSchemas is called; we let the
        // overridden canShortCircuit throw.
        AnyOfValidator spyBroken = Mockito.spy(broken);
        doCallRealMethod().when(spyBroken).preloadJsonSchemas(anyList());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> spyBroken.preloadJsonSchema(), "Exception from canShortCircuit() must be propagated");
        assertEquals("short‑circuit failure", ex.getMessage());
    }
}
