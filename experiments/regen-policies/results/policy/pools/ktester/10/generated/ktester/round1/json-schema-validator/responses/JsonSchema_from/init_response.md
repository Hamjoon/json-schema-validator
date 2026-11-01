```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic tests for the package‑private static factory method
 * {@code JsonSchema.from(...)}. The test only verifies that a
 * {@link JsonSchema} instance can be created without throwing
 * exceptions when the required collaborators are supplied.
 *
 * The heavy‑weight parts of the constructor (e.g. meta‑schema handling,
 * schema resource registration) are stubbed using Mockito so that the
 * test remains fast and does not depend on external resources.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    // Collaborators required by the JsonSchema constructor
    @Mock ValidationContext validationContext;
    @Mock JsonMetaSchema metaSchema;
    @Mock JsonSchemaFactory jsonSchemaFactory;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    // Maps used inside ValidationContext – we provide simple concrete instances
    private ConcurrentHashMap<String, JsonSchema> schemaResources;
    private ConcurrentHashMap<String, JsonSchema> dynamicAnchors;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the maps that ValidationContext will return
        schemaResources = new ConcurrentHashMap<>();
        dynamicAnchors = new ConcurrentHashMap<>();

        // Stub the behaviour of ValidationContext used by the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getSchemaResources()).thenReturn(schemaResources);
        when(validationContext.getDynamicAnchors()).thenReturn(dynamicAnchors);
        // The constructor asks for a resolved $id – we keep it simple and return null
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);

        // Meta‑schema methods that are consulted for anchors – return null to avoid registration
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid cross‑test contamination
        schemaResources.clear();
        dynamicAnchors.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void testFromCreatesJsonSchemaInstance() {
        // Given – all collaborators are already prepared in @BeforeEach

        // When – invoke the package‑private factory method
        JsonSchema result = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,               // no parent schema
                false               // do not suppress sub‑schema retrieval
        );

        // Then – a non‑null JsonSchema should be returned
        assertNotNull(result, "JsonSchema.from should return a non‑null instance");
        // Additional sanity checks (optional)
        assertSame(validationContext, result.getValidationContext(),
                "The created JsonSchema should retain the supplied ValidationContext");
    }
}
```