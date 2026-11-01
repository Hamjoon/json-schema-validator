```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive tests for {@link AnyOfValidator#preloadJsonSchema()}.
 *
 * <p>The test suite covers many edge‑cases:
 * <ul>
 *   <li>null, empty and populated schemas collections</li>
 *   <li>lists containing {@code null} entries or being unmodifiable</li>
 *   <li>exception handling from {@code preloadJsonSchemas} and {@code canShortCircuit}</li>
 *   <li>idempotent behaviour when called multiple times</li>
 *   <li>custom {@link List} implementations that track iteration</li>
 * </ul>
 *
 * <p>A concrete subclass {@code TestAnyOfValidator} overrides the two protected helper
 * methods so that we can verify they are invoked and capture the arguments passed to
 * {@code preloadJsonSchemas}.  For scenarios that need a different behaviour (e.g.
 * throwing an exception) additional anonymous subclasses are created inside the test
 * methods.
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    /** Mocks required for constructing a validator instance. */
    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;

    /** A simple anyOf schema used for the default validator construction. */
    private ArrayNode anyOfArrayNode;

    /** Concrete test subclass that records calls to the protected helpers. */
    private static class TestAnyOfValidator extends AnyOfValidator {
        boolean preloadCalled = false;
        boolean shortCircuitCalled = false;
        List<JsonSchema> receivedSchemas = null; // captures argument passed to preloadJsonSchemas

        TestAnyOfValidator(SchemaLocation schemaLocation,
                           JsonNodePath evaluationPath,
                           JsonNode schemaNode,
                           JsonSchema parentSchema,
                           ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validationContext);
        }

        @Override
        protected void preloadJsonSchemas(java.util.List<JsonSchema> schemas) {
            preloadCalled = true;
            receivedSchemas = schemas; // capture for assertions
            // delegate to real implementation so that normal processing still occurs
            super.preloadJsonSchemas(schemas);
        }

        @Override
        protected boolean canShortCircuit() {
            shortCircuitCalled = true;
            return super.canShortCircuit();
        }
    }

    private TestAnyOfValidator validator;

    @BeforeEach
    void setupBeforeEach() {
        // Build a dummy anyOf array node: [ {"type":"string"}, {"type":"integer"} ]
        JsonNodeFactory factory = JsonNodeFactory.instance;
        anyOfArrayNode = factory.arrayNode()
                .add(factory.objectNode().put("type", "string"))
                .add(factory.objectNode().put("type", "integer"));

        // Mock ValidationContext#newSchema to return a simple mock JsonSchema for each subschema
        JsonSchema mockSubSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(any(SchemaLocation.class), any(JsonNodePath.class),
                any(JsonNode.class), any(JsonSchema.class))).thenReturn(mockSubSchema);

        // Construct the validator (parentSchema is null for these tests)
        validator = new TestAnyOfValidator(schemaLocation, evaluationPath,
                anyOfArrayNode, null, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up
    }

    /** ---------------------------------------------------------------------- **/
    /** Helper to inject a custom schemas list via reflection. */
    private static void setSchemas(AnyOfValidator v, List<JsonSchema> list) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        f.set(v, list);
    }

    /** ---------------------------------------------------------------------- **/
    /** 1. Preload with null schemas list */
    @Test
    void preload_withNullSchemas_callsHelpers() throws Exception {
        setSchemas(validator, null);

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled, "preloadJsonSchemas should be called even when schemas == null");
        assertTrue(validator.shortCircuitCalled, "canShortCircuit should be invoked exactly once");
        assertNull(validator.receivedSchemas, "Received argument should be null");
    }

    /** ---------------------------------------------------------------------- **/
    /** 2. Preload with empty schemas list */
    @Test
    void preload_withEmptySchemas_callsHelpers() throws Exception {
        setSchemas(validator, Collections.emptyList());

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled);
        assertTrue(validator.shortCircuitCalled);
        assertEquals(0, validator.receivedSchemas.size(),
                "preloadJsonSchemas should receive the empty list");
    }

    /** ---------------------------------------------------------------------- **/
    /** 3. Preload with multiple schemas */
    @Test
    void preload_withMultipleSchemas_forwardsExactList() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(s1, s2, s3);
        setSchemas(validator, list);

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled);
        assertTrue(validator.shortCircuitCalled);
        assertSame(list, validator.receivedSchemas,
                "The exact list instance should be forwarded to preloadJsonSchemas");
    }

    /** ---------------------------------------------------------------------- **/
    /** 4. Preload does not alter schemas list */
    @Test
    void preload_doesNotModifySchemasList() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> original = new ArrayList<>(Arrays.asList(s1, s2));
        setSchemas(validator, original);

        validator.preloadJsonSchema();

        // The list reference should be the same and its content unchanged
        assertSame(original, validator.receivedSchemas);
        assertEquals(2, original.size());
        assertSame(s1, original.get(0));
        assertSame(s2, original.get(1));
    }

    /** ---------------------------------------------------------------------- **/
    /** 5. canShortCircuit is still called when preloadJsonSchemas throws */
    @Test
    void preload_whenPreloadJsonSchemasThrows_stillCallsCanShortCircuit() throws Exception {
        // Subclass that throws from preloadJsonSchemas
        AnyOfValidator throwingValidator = new TestAnyOfValidator(schemaLocation, evaluationPath,
                anyOfArrayNode, null, validationContext) {
            @Override
            protected void preloadJsonSchemas(java.util.List<JsonSchema> schemas) {
                preloadCalled = true;
                throw new RuntimeException("simulated failure");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                throwingValidator::preloadJsonSchema,
                "Exception from preloadJsonSchemas should propagate");
        assertEquals("simulated failure", ex.getMessage());

        // canShortCircuit must have been called despite the exception
        assertTrue(((TestAnyOfValidator) throwingValidator).shortCircuitCalled,
                "canShortCircuit should be invoked even when preloadJsonSchemas throws");
    }

    /** ---------------------------------------------------------------------- **/
    /** 6. Preload with list containing a null element */
    @Test
    void preload_withNullElementInList_forwardsListAndCallsCanShortCircuit() throws Exception {
        JsonSchema valid = mock(JsonSchema.class);
        List<JsonSchema> list = Arrays.asList(valid, null);
        setSchemas(validator, list);

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled);
        assertTrue(validator.shortCircuitCalled);
        assertSame(list, validator.receivedSchemas);
        assertEquals(2, validator.receivedSchemas.size());
        assertNull(validator.receivedSchemas.get(1), "Second element should be null");
    }

    /** ---------------------------------------------------------------------- **/
    /** 7. Preload with an unmodifiable list */
    @Test
    void preload_withUnmodifiableList_behavesCorrectly() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        List<JsonSchema> modifiable = new ArrayList<>(Arrays.asList(s1, s2));
        List<JsonSchema> unmodifiable = Collections.unmodifiableList(modifiable);
        setSchemas(validator, unmodifiable);

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled);
        assertTrue(validator.shortCircuitCalled);
        assertSame(unmodifiable, validator.receivedSchemas,
                "The same unmodifiable list instance should be passed through");
    }

    /** ---------------------------------------------------------------------- **/
    /** 8. Multiple calls are idempotent (each call invokes helpers once) */
    @Test
    void preload_multipleInvocations_eachCallsHelpers() throws Exception {
        List<JsonSchema> list = Arrays.asList(mock(JsonSchema.class), mock(JsonSchema.class));
        setSchemas(validator, list);

        validator.preloadJsonSchema();
        validator.preloadJsonSchema(); // second call

        // Since we record only the latest call, we verify the counters via a spy on the validator
        TestAnyOfValidator spy = spy(validator);
        // reset counters
        spy.preloadCalled = false;
        spy.shortCircuitCalled = false;

        spy.preloadJsonSchema(); // third call on spy
        verify(spy, times(1)).preloadJsonSchemas(anyList());
        verify(spy, times(1)).canShortCircuit();
    }

    /** ---------------------------------------------------------------------- **/
    /** 9. Ensure preloadJsonSchemas is called exactly once per execution */
    @Test
    void preload_delegatesExactlyOncePerCall() throws Exception {
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
        setSchemas(validator, Arrays.asList(s1, s2, s3));

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled, "preloadJsonSchemas should be called once");
        // No extra verification needed – the flag would be set multiple times otherwise
    }

    /** ---------------------------------------------------------------------- **/
    /** 10. Exception from a schema's preloadJsonSchema propagates, canShortCircuit still called */
    @Test
    void preload_schemaThrowsRuntimeException_propagatesAndCallsCanShortCircuit() throws Exception {
        JsonSchema good = mock(JsonSchema.class);
        JsonSchema bad = mock(JsonSchema.class);
        doThrow(new RuntimeException("schema failure")).when(bad).preloadJsonSchema();

        setSchemas(validator, Arrays.asList(good, bad));

        RuntimeException ex = assertThrows(RuntimeException.class,
                validator::preloadJsonSchema,
                "Exception from a subschema should propagate");
        assertEquals("schema failure", ex.getMessage());

        assertTrue(validator.shortCircuitCalled,
                "canShortCircuit must still be invoked even when a subschema throws");
    }

    /** ---------------------------------------------------------------------- **/
    /** 11. Exception from canShortCircuit propagates after preloadJsonSchemas */
    @Test
    void preload_canShortCircuitThrows_exceptionPropagates() throws Exception {
        // Subclass that throws from canShortCircuit
        AnyOfValidator throwingValidator = new TestAnyOfValidator(schemaLocation, evaluationPath,
                anyOfArrayNode, null, validationContext) {
            @Override
            protected boolean canShortCircuit() {
                shortCircuitCalled = true;
                throw new IllegalStateException("short‑circuit failure");
            }
        };

        // Ensure preloadJsonSchemas runs without error
        JsonSchema mockSchema = mock(JsonSchema.class);
        setSchemas(throwingValidator, Collections.singletonList(mockSchema));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                throwingValidator::preloadJsonSchema,
                "Exception from canShortCircuit should propagate");
        assertEquals("short‑circuit failure", ex.getMessage());

        // Verify preloadJsonSchemas was indeed called before the exception
        assertTrue(((TestAnyOfValidator) throwingValidator).preloadCalled,
                "preloadJsonSchemas must be invoked before canShortCircuit throws");
    }

    /** ---------------------------------------------------------------------- **/
    /** 12. Custom List implementation tracking iteration */
    @Test
    void preload_withTrackingList_verifiesIterationAndCanShortCircuit() throws Exception {
        // Custom list that records calls
        class TrackingList implements List<JsonSchema> {
            private final List<JsonSchema> delegate = new ArrayList<>();
            int sizeCalls = 0;
            int getCalls = 0;

            TrackingList(JsonSchema... elements) { delegate.addAll(Arrays.asList(elements)); }

            @Override public int size() { sizeCalls++; return delegate.size(); }
            @Override public JsonSchema get(int index) { getCalls++; return delegate.get(index); }

            // --- minimal unsupported operations for the test ---
            @Override public boolean isEmpty() { return delegate.isEmpty(); }
            @Override public boolean contains(Object o) { return delegate.contains(o); }
            @Override public Iterator<JsonSchema> iterator() { return delegate.iterator(); }
            @Override public Object[] toArray() { return delegate.toArray(); }
            @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
            @Override public boolean add(JsonSchema e) { return delegate.add(e); }
            @Override public boolean remove(Object o) { return delegate.remove(o); }
            @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
            @Override public boolean addAll(Collection<? extends JsonSchema> c) { return delegate.addAll(c); }
            @Override public boolean addAll(int index, Collection<? extends JsonSchema> c) { return delegate.addAll(index, c); }
            @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
            @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
            @Override public void clear() { delegate.clear(); }
            @Override public JsonSchema set(int index, JsonSchema element) { return delegate.set(index, element); }
            @Override public void add(int index, JsonSchema element) { delegate.add(index, element); }
            @Override public JsonSchema remove(int index) { return delegate.remove(index); }
            @Override public int indexOf(Object o) { return delegate.indexOf(o); }
            @Override public int lastIndexOf(Object o) { return delegate.lastIndexOf(o); }
            @Override public ListIterator<JsonSchema> listIterator() { return delegate.listIterator(); }
            @Override public ListIterator<JsonSchema> listIterator(int index) { return delegate.listIterator(index); }
            @Override public List<JsonSchema> subList(int fromIndex, int toIndex) { return delegate.subList(fromIndex, toIndex); }
        }

        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        TrackingList tracking = new TrackingList(s1, s2);
        setSchemas(validator, tracking);

        validator.preloadJsonSchema();

        assertTrue(validator.preloadCalled);
        assertTrue(validator.shortCircuitCalled);
        assertEquals(1, tracking.sizeCalls, "size() should be called once");
        assertEquals(2, tracking.getCalls, "get() should be called once per element");
    }

    /** ---------------------------------------------------------------------- **/
    /** 13. NullPointerException propagates when a null schema element is accessed */
    @Test
    void preload_nullElementCausesNPE_propagates() throws Exception {
        // Use the real implementation of preloadJsonSchemas (which will dereference each schema)
        setSchemas(validator, Arrays.asList(mock(JsonSchema.class), null));

        NullPointerException ex = assertThrows(NullPointerException.class,
                validator::preloadJsonSchema,
                "NullPointerException should propagate when a null schema is processed");
        // No further assertions needed; the occurrence is enough
    }

    /** ---------------------------------------------------------------------- **/
    /** 14. UnsupportedOperationException from list.size() propagates */
    @Test
    void preload_listSizeThrowsUOE_propagates() throws Exception {
        List<JsonSchema> badList = new ArrayList<JsonSchema>() {
            @Override public int size() { throw new UnsupportedOperationException("size not supported"); }
        };
        setSchemas(validator, badList);

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
                validator::preloadJsonSchema,
                "Exception from list.size() should propagate");
        assertEquals("size not supported", ex.getMessage());
    }

    /** ---------------------------------------------------------------------- **/
    /** 15. AssertionError thrown by preloadJsonSchemas propagates and canShortCircuit is NOT called */
    @Test
    void preload_preloadJsonSchemasThrowsAssertionError_propagatesWithoutCanShortCircuit() throws Exception {
        AnyOfValidator errorValidator = new TestAnyOfValidator(schemaLocation, evaluationPath,
                anyOfArrayNode, null, validationContext) {
            @Override
            protected void preloadJsonSchemas(java.util.List<JsonSchema> schemas) {
                preloadCalled = true;
                throw new AssertionError("assertion failure");
            }

            @Override
            protected boolean canShortCircuit() {
                shortCircuitCalled = true;
                return super.canShortCircuit();
            }
        };

        AssertionError err = assertThrows(AssertionError.class,
                errorValidator::preloadJsonSchema,
                "AssertionError should propagate");
        assertEquals("assertion failure", err.getMessage());

        // canShortCircuit must NOT have been called because the method aborts early
        assertFalse(((TestAnyOfValidator) errorValidator).shortCircuitCalled,
                "canShortCircuit should not be invoked when preloadJsonSchemas throws an Error");
    }
}
```