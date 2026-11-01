```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_6_Test {

    /** Instance under test */
    private JsonSchema jsonSchema;

    /** Dependencies required to build a JsonSchema instance */
    @Mock
    private ValidationContext validationContext;

    @Mock
    private JsonMetaSchema metaSchema;

    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private InputFormat inputFormat = InputFormat.JSON; // typical enum value

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        /* --------------------------------------------------------------------
         * Build the minimal objects required by JsonSchema.from(...).  The
         * schema we use is an empty JSON object (i.e. "{}") which imposes no
         * constraints – therefore any well‑formed JSON should validate
         * without producing ValidationMessage instances.
         * -------------------------------------------------------------------- */
        // empty schema node: {}
        ObjectNode emptySchemaNode = JsonNodeFactory.instance.objectNode();

        // dummy location – the actual IRI is irrelevant for this test
        schemaLocation = new SchemaLocation(null); // null absolute IRI → allowed for test

        // evaluation path – root path
        evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // Stub the ValidationContext behaviour required by the constructor
        // (the real implementation uses these maps to store resources)
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());

        // The meta‑schema is also queried for anchors – we return null for simplicity
        when(metaSchema.readAnchor(emptySchemaNode)).thenReturn(null);
        when(metaSchema.readDynamicAnchor(emptySchemaNode)).thenReturn(null);

        // Create the JsonSchema instance via the public static factory method
        jsonSchema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,               // no parent schema
                false               // suppressSubSchemaRetrieval
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; all objects are garbage‑collected after each test
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required
    }

    @Test
    void validate_EmptySchema_WithValidJson_ReturnsEmptyMessageSet() {
        // GIVEN a well‑formed JSON document
        String json = "{}";

        // WHEN the string‑based validate method is invoked
        Set<ValidationMessage> messages = jsonSchema.validate(json, inputFormat);

        // THEN no validation messages are produced
        assertTrue(messages.isEmpty(),
                () -> "Expected no validation messages, but got: " + messages);
    }

    @Test
    void validate_EmptySchema_WithInvalidJson_ThrowsException() {
        // GIVEN an invalid JSON document (malformed)
        String malformedJson = "{";

        // WHEN the validation is attempted, a JsonProcessingException (or a
        // subclass) should be thrown by the internal deserializer.
        assertThrows(RuntimeException.class, () -> jsonSchema.validate(malformedJson, inputFormat));
    }
}
```