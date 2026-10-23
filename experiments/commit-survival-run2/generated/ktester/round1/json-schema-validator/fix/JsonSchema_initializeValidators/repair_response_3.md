```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link JsonSchema#initializeValidators()}.
 */
class JsonSchema_initializeValidators_Test {

    /**
     * Minimal JSON schema used for constructing a {@link JsonSchema} instance.
     */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /**
     * The {@link JsonSchema} instance under test.
     */
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Build a JsonSchema instance using the public factory.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(SIMPLE_SCHEMA);
        jsonSchema = JsonSchemaFactory.getInstance().getSchema(schemaNode);
        // Ensure a clean validators map.
        setValidatorsMap(jsonSchema, new LinkedHashMap<>());
    }

    /**
     * Helper: obtains the private {@code validators} field (type {@code Map<String, JsonValidator>}).
     */
    private static Field getValidatorsField() throws NoSuchFieldException {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        return f;
    }

    /**
     * Helper: reads the private {@code validatorsLoaded} flag.
     */
    private static boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return (boolean) f.get(schema);
    }

    /**
     * Helper: injects a map into the private {@code validators} field.
     */
    private static void setValidatorsMap(JsonSchema schema, Map<String, JsonValidator> map) throws Exception {
        Field f = getValidatorsField();
        f.set(schema, map);
    }

    /**
     * Creates a {@link JsonValidator} proxy whose {@code preloadJsonSchema}
     * method executes the supplied {@code action}. All other methods return
     * {@code null} or default values.
     */
    private static JsonValidator validator(Runnable action) {
        return (JsonValidator) Proxy.newProxyInstance(
                JsonValidator.class.getClassLoader(),
                new Class[]{JsonValidator.class},
                (proxy, method, args) -> {
                    if ("preloadJsonSchema".equals(method.getName())) {
                        action.run();
                        return null;
                    }
                    // default return for other methods
                    return null;
                });
    }

    @Test
    @DisplayName("initializeValidators runs without throwing")
    void testInitializeValidatorsRunsWithoutException() {
        assertDoesNotThrow(() -> jsonSchema.initializeValidators(),
                "Calling initializeValidators should not throw any exception");
    }

    @Test
    @DisplayName("initializeValidators is idempotent")
    void testInitializeValidatorsIdempotent() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        JsonValidator v = validator(counter::incrementAndGet);
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("v", v);
        setValidatorsMap(jsonSchema, map);
        // first call – should preload once
        jsonSchema.initializeValidators();
        assertEquals(1, counter.get(), "preloadJsonSchema must be called once");
        assertTrue(isValidatorsLoaded(jsonSchema));
        // second call – must not invoke preload again
        jsonSchema.initializeValidators();
        assertEquals(1, counter.get(), "preloadJsonSchema must not be called again");
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  1. first call with non‑empty validators map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – first call with non‑empty validators map")
    void testInitializeValidatorsNonEmptyMap() throws Exception {
        AtomicInteger c1 = new AtomicInteger(0);
        AtomicInteger c2 = new AtomicInteger(0);
        JsonValidator v1 = validator(c1::incrementAndGet);
        JsonValidator v2 = validator(c2::incrementAndGet);
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        map.put("v1", v1);
        map.put("v2", v2);
        setValidatorsMap(jsonSchema, map);
        assertFalse(isValidatorsLoaded(jsonSchema), "validatorsLoaded should start as false");
        jsonSchema.initializeValidators();
        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded must become true after first call");
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
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
    }

    /* -------------------------------------------------------------
     *  4. validator throws RuntimeException during preload
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validator throws RuntimeException")
    void testInitializeValidatorsValidatorThrowsRuntimeException() throws Exception {
        JsonValidator bad = validator(() -> {
            throw new RuntimeException("boom");
        });
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("bad", bad);
        setValidatorsMap(jsonSchema, map);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.initializeValidators(),
                "Exception from validator must propagate");
        assertEquals("boom", ex.getMessage());
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  5. getValidators returns null -> NullPointerException
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – getValidators returns null")
    void testInitializeValidatorsGetValidatorsNull() throws Exception {
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
        map.put("good", validator(() -> {
        }));
        map.put("nullEntry", null);
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
        AtomicInteger counter = new AtomicInteger(0);
        JsonValidator shared = validator(counter::incrementAndGet);
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        map.put("first", shared);
        map.put("second", shared);
        setValidatorsMap(jsonSchema, map);
        jsonSchema.initializeValidators();
        assertEquals(2, counter.get(), "Method is called once per entry, therefore twice for the same instance");
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  8. getValidators throws unchecked exception
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – getValidators throws unchecked exception")
    void testInitializeValidatorsGetValidatorsThrows() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, JsonValidator> throwingMap = (Map<String, JsonValidator>) Proxy.newProxyInstance(
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
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        JsonValidator victim = validator(() -> {
            // mutate the map while iteration is in progress
            map.put("new", validator(() -> {
            }));
        });
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
        JsonValidator bad = validator(() -> {
            throw new AssertionError("assertion failed");
        });
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