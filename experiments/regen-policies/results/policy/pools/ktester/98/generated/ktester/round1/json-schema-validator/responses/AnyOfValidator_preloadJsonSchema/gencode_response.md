```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link AnyOfValidator#preloadJsonSchema()}.
 *
 * <p>The tests cover the normal delegation to {@code preloadJsonSchemas},
 * handling of edge‑cases such as {@code null} or empty schema lists,
 * exception propagation from child schemas, concurrent modifications,
 * immutable collections and failures of the {@code canShortCircuit()}
 * caching step.</p>
 *
 * <p>All collaborators are mocked; the internal {@code schemas} list is
 * manipulated via reflection where necessary (e.g. to inject {@code null}
 * or to replace it with a custom mutable/immutable list).</p>
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    /* --------------------------------------------------------------------- *
     *  Common test fixtures
     * --------------------------------------------------------------------- */
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock ValidationContext mockValidationContext;

    /** The validator created by the constructor – later spied. */
    AnyOfValidator validator;
    /** Spy that allows verification of protected method calls. */
    AnyOfValidator spyValidator;

    /** Simple JSON array node representing an {@code anyOf} with two empty subschemas. */
    private static final JsonNode ANY_OF_ARRAY;

    static {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode array = mapper.createArrayNode();
        array.add(mapper.createObjectNode()); // first empty schema
        array.add(mapper.createObjectNode()); // second empty schema
        ANY_OF_ARRAY = array;
    }

    @BeforeEach
    void setUp() {
        // -----------------------------------------------------------------
        // Basic stubbing for the constructor – the validator parses the
        // ANY_OF_ARRAY and creates a list of JsonSchema objects via the
        // ValidationContext.
        // -----------------------------------------------------------------
        when(mockSchemaLocation.append(anyInt())).thenReturn(mockSchemaLocation);
        when(mockEvaluationPath.append(anyInt())).thenReturn(mockEvaluationPath);

        // The ValidationContext creates a JsonSchema for each subschema node.
        // For the purpose of these tests the concrete type is irrelevant, so
        // we return a generic mock.
        JsonSchema mockSubSchema = mock(JsonSchema.class);
        when(mockValidationContext.newSchema(any(SchemaLocation.class),
                any(JsonNodePath.class), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(mockSubSchema);

        // -----------------------------------------------------------------
        // Build a real validator instance using the public constructor.
        // -----------------------------------------------------------------
        validator = new AnyOfValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                ANY_OF_ARRAY,
                null,                     // parentSchema – not needed for these tests
                mockValidationContext);

        // Spy the instance – we will verify protected method invocations.
        spyValidator = Mockito.spy(validator);

        // By default we let the real implementation of preloadJsonSchemas run.
        // Individual tests may override this behaviour.
        doCallRealMethod().when(spyValidator).preloadJsonSchemas(anyList());

        // Use the real implementation of canShortCircuit() unless a test
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

    /* --------------------------------------------------------------------- *
     *  1. Basic delegation – already present in the original scaffold.
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() delegates to preloadJsonSchemas() and caches short‑circuit flag")
    void preloadJsonSchemaShouldDelegateToPreloadJsonSchemasAndCacheFlag() {
        // Act
        spyValidator.preloadJsonSchema();

        // Verify that the protected helper was called exactly once with the
        // internal list of schemas (size 2 because ANY_OF_ARRAY has two elements).
        ArgumentCaptor<List<JsonSchema>> captor = ArgumentCaptor.forClass(List.class);
        verify(spyValidator, times(1)).preloadJsonSchemas(captor.capture());

        List<JsonSchema> passedSchemas = captor.getValue();
        assertNotNull(passedSchemas);
        assertEquals(2, passedSchemas.size(),
                "preloadJsonSchemas should receive a list containing one JsonSchema per subschema");

        // Verify that the short‑circuit flag method was invoked.
        verify(spyValidator, times(1)).canShortCircuit();
    }

    /* --------------------------------------------------------------------- *
     *  2. Null schemas handling
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() throws NPE when internal schemas list is null")
    void nullSchemasShouldThrowNullPointerException() throws Exception {
        setSchemas(spyValidator, null); // inject null

        // The real preloadJsonSchemas will be invoked, leading to NPE.
        assertThrows(NullPointerException.class,
                () -> spyValidator.preloadJsonSchema(),
                "Expected NullPointerException when schemas list is null");
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

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spyValidator.preloadJsonSchema(),
                "Exception from child preload must be propagated");
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
        list.add(null); // introduce null entry
        setSchemas(spyValidator, list);

        // The real preloadJsonSchemas will iterate and hit the null entry.
        assertThrows(NullPointerException.class,
                () -> spyValidator.preloadJsonSchema(),
                "Expected NPE when a null schema is present in the list");
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
                Thread.sleep(50); // ensure the iterator has started
                mutable.add(mock(JsonSchema.class)); // concurrent modification
            } catch (InterruptedException ignored) {
            }
        });

        // Expect ConcurrentModificationException from preloadJsonSchema().
        assertThrows(ConcurrentModificationException.class,
                () -> spyValidator.preloadJsonSchema(),
                "Concurrent modification should be detected and propagated");

        modifier.get(); // re‑throw any unexpected exception from the modifier
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

        List<JsonSchema> immutable = Collections.unmodifiableList(
                new ArrayList<>(Collections.singletonList(schema)));
        setSchemas(spyValidator, immutable);

        // If the internal implementation tries to modify the list (e.g., replace
        // a cached schema), an UnsupportedOperationException will be thrown.
        // The test asserts that such an exception propagates.
        assertThrows(UnsupportedOperationException.class,
                () -> spyValidator.preloadJsonSchema(),
                "Expected UnsupportedOperationException when using an unmodifiable schemas list");
    }

    /* --------------------------------------------------------------------- *
     * 10. canShortCircuit() throws exception
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("preloadJsonSchema() propagates exception thrown by canShortCircuit()")
    void canShortCircuitExceptionShouldPropagate() {
        // Anonymous subclass that forces canShortCircuit() to fail.
        AnyOfValidator broken = new AnyOfValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                ANY_OF_ARRAY,
                null,
                mockValidationContext) {
            @Override
            protected boolean canShortCircuit() {
                throw new IllegalStateException("short‑circuit failure");
            }
        };

        // Spy only to verify that preloadJsonSchemas is called; we let the
        // overridden canShortCircuit throw.
        AnyOfValidator spyBroken = Mockito.spy(broken);
        doCallRealMethod().when(spyBroken).preloadJsonSchemas(anyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> spyBroken.preloadJsonSchema(),
                "Exception from canShortCircuit() must be propagated");
        assertEquals("short‑circuit failure", ex.getMessage());
    }
}
```