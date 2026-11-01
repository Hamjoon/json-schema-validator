**Generated test class**

```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    private JsonNode simpleSchemaNode; // e.g. {"type":"object"}

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

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
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        // parent = null, suppressSubSchemaRetrieval = false
        jsonSchema = ctor.newInstance(validationContext, schemaLocation, evaluationPath,
                simpleSchemaNode, null, false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing special to clean up
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    // -------------------------------------------------------------------------
    // Test cases for JsonSchema#getValidators()
    // -------------------------------------------------------------------------

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
            assertSame(results[0], results[i],
                    "All concurrent calls should return the same cached list instance");
        }
    }
}
```