```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#initializeValidators()}.
 *
 * <p>The tests cover the behaviour described in the supplied JSON test‑case list,
 * including normal operation, edge cases (empty or null validator maps), and
 * exceptional scenarios (validators throwing exceptions or errors).</p>
 *
 * <p>Because {@code getValidators()} and the {@code validatorsLoaded} flag are
 * private, reflection is used to inject a custom {@link Map} of {@link JsonValidator}
 * instances and to read the flag after method execution.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    /** Minimal JSON schema used for constructing a {@link JsonSchema} instance. */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /** Instance under test – created via the package‑private factory method. */
    private JsonSchema jsonSchema;

    /** Mocked {@link ValidationContext} required by the factory method. */
    @Mock
    private ValidationContext validationContext;

    /** Mocked {@link JsonMetaSchema} – only {@code getMetaSchema()} is used during construction. */
    @Mock
    private JsonMetaSchema metaSchema;

    /** Helper: obtains the private {@code validators} field (type {@code Map<String, JsonValidator>}). */
    private static Field getValidatorsField() throws NoSuchFieldException {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        return f;
    }

    /** Helper: reads the private {@code validatorsLoaded} flag. */
    private static boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return (boolean) f.get(schema);
    }

    /** Helper: injects a map into the private {@code validators} field. */
    private static void setValidatorsMap(JsonSchema schema, Map<String, JsonValidator> map) throws Exception {
        Field f = getValidatorsField();
        f.set(schema, map);
    }

    @BeforeEach
    void setup() throws Exception {
        // minimal mock behaviour required for factory method
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(SIMPLE_SCHEMA);

        jsonSchema = JsonSchema.from(
                validationContext,
                "",                                            // schemaPath
                new URI("http://example.com/simple-schema.json"),
                schemaNode,
                null,                                          // parent
                false                                          // suppressSubSchemaRetrieval
        );

        // Ensure the internal validators map starts empty (or null) for a clean slate.
        setValidatorsMap(jsonSchema, new LinkedHashMap<>());
    }

    /* -------------------------------------------------------------
     *  1. first call with non‑empty validators map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – first call with non‑empty validators map")
    void testInitializeValidatorsNonEmptyMap() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        map.put("v1", v1);
        map.put("v2", v2);
        setValidatorsMap(jsonSchema, map);

        assertFalse(isValidatorsLoaded(jsonSchema), "validatorsLoaded should start as false");

        jsonSchema.initializeValidators();

        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded must become true after first call");
        verify(v1, times(1)).preloadJsonSchema();
        verify(v2, times(1)).preloadJsonSchema();
    }

    /* -------------------------------------------------------------
     *  2. first call with empty validators map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – first call with empty validators map")
    void testInitializeValidatorsEmptyMap() throws Exception {
        // map is already empty from setup()
        assertFalse(isValidatorsLoaded(jsonSchema));

        jsonSchema.initializeValidators();

        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded must become true even when map is empty");
        // No validator to verify – just ensure no exception is thrown
    }

    /* -------------------------------------------------------------
     *  3. subsequent call when already loaded
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – subsequent call when already loaded")
    void testInitializeValidatorsIdempotent() throws Exception {
        JsonValidator v = mock(JsonValidator.class);
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("v", v);
        setValidatorsMap(jsonSchema, map);

        // first call – preloads
        jsonSchema.initializeValidators();
        verify(v, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchema));

        // reset interactions and call again
        reset(v);
        jsonSchema.initializeValidators();

        // second call must not invoke preloadJsonSchema again
        verifyNoInteractions(v);
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  4. validator throws RuntimeException during preload
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validator throws RuntimeException")
    void testInitializeValidatorsValidatorThrowsRuntimeException() throws Exception {
        JsonValidator bad = mock(JsonValidator.class);
        doThrow(new RuntimeException("boom")).when(bad).preloadJsonSchema();

        Map<String, JsonValidator> map = new HashMap<>();
        map.put("bad", bad);
        setValidatorsMap(jsonSchema, map);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.initializeValidators(),
                "Exception from validator must propagate");
        assertEquals("boom", ex.getMessage());

        // flag is set before iterating over validators
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  5. getValidators returns null -> NullPointerException
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – getValidators returns null")
    void testInitializeValidatorsGetValidatorsNull() throws Exception {
        // Directly set the private field to null
        setValidatorsMap(jsonSchema, null);

        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> jsonSchema.initializeValidators(),
                "Calling .values() on a null map should throw NPE");
        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded should be true even when NPE occurs");
    }

    /* -------------------------------------------------------------
     *  6. validators map contains a null entry
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validators map contains null entry")
    void testInitializeValidatorsNullEntryInMap() throws Exception {
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("good", mock(JsonValidator.class));
        map.put("nullEntry", null);               // deliberate null
        setValidatorsMap(jsonSchema, map);

        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> jsonSchema.initializeValidators(),
                "Calling preloadJsonSchema on a null value should NPE");
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  7. duplicate validator instances in map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – duplicate validator instances")
    void testInitializeValidatorsDuplicateValidatorInstances() throws Exception {
        JsonValidator shared = mock(JsonValidator.class);
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        map.put("first", shared);
        map.put("second", shared);                // same instance under another key
        setValidatorsMap(jsonSchema, map);

        jsonSchema.initializeValidators();

        // Method is called once per entry, therefore twice for the same instance
        verify(shared, times(2)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  8. getValidators throws unchecked exception
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – getValidators throws unchecked exception")
    void testInitializeValidatorsGetValidatorsThrows() throws Exception {
        // Replace the internal map with a proxy that throws when values() is called
        Map<String, JsonValidator> throwingMap = (Map<String, JsonValidator>) java.lang.reflect.Proxy.newProxyInstance(
                Map.class.getClassLoader(),
                new Class[]{Map.class},
                (proxy, method, args) -> {
                    if ("values".equals(method.getName())) {
                        throw new IllegalStateException("unexpected state");
                    }
                    return method.invoke(new HashMap<>(), args);
                });
        setValidatorsMap(jsonSchema, throwingMap);

        IllegalStateException ise = assertThrows(IllegalStateException.class,
                () -> jsonSchema.initializeValidators(),
                "Unchecked exception from getValidators must propagate");
        assertEquals("unexpected state", ise.getMessage());
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  9. concurrent modification during iteration
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – concurrent modification during iteration")
    void testInitializeValidatorsConcurrentModification() throws Exception {
        // Use a non‑thread‑safe map that we modify from within the preload call
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        JsonValidator victim = mock(JsonValidator.class);
        doAnswer(invocation -> {
            // mutate the map while iteration is in progress
            map.put("new", mock(JsonValidator.class));
            return null;
        }).when(victim).preloadJsonSchema();

        map.put("victim", victim);
        setValidatorsMap(jsonSchema, map);

        assertThrows(ConcurrentModificationException.class,
                () -> jsonSchema.initializeValidators(),
                "Modifying the map during iteration must cause CME");
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     * 10. validator throws an Error (e.g., AssertionError)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validator throws Error")
    void testInitializeValidatorsValidatorThrowsError() throws Exception {
        JsonValidator bad = mock(JsonValidator.class);
        doThrow(new AssertionError("assertion failed")).when(bad).preloadJsonSchema();

        Map<String, JsonValidator> map = Collections.singletonMap("bad", bad);
        setValidatorsMap(jsonSchema, map);

        AssertionError err = assertThrows(AssertionError.class,
                () -> jsonSchema.initializeValidators(),
                "Error thrown by validator must propagate");
        assertEquals("assertion failed", err.getMessage());
        assertTrue(isValidatorsLoaded(jsonSchema));
    }
}
```