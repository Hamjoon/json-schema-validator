```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JsonSchema#toString()}.
 *
 * The {@code toString()} implementation simply concatenates the schema path
 * (quoted) with the JSON representation of the underlying schema node.
 *
 * The test creates a {@link JsonSchema} instance via the public static factory
 * method {@code JsonSchema.from(...)} using mocked dependencies. No private
 * members of {@code JsonSchema} or its collaborators are accessed directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_toString_Test {

    @Mock
    ValidationContext validationContext;   // required by JsonSchema.from(...)
    @Mock
    JsonMetaSchema metaSchema;           // returned by validationContext.getMetaSchema()
    @Mock
    JsonNode schemaNode;                  // the node whose toString() we control

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Configure the minimal behaviour of ValidationContext used by the JsonSchema ctor.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // Returning null for config avoids the creation of KeywordWalkListenerRunner.
        when(validationContext.getConfig()).thenReturn(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void toString_returnsExpectedRepresentation() {
        // GIVEN
        String schemaPath = "/properties/name";
        String nodeString = "{\"type\":\"string\"}";
        when(schemaNode.toString()).thenReturn(nodeString);

        // The URI argument may be null; using a dummy URI keeps the call realistic.
        URI dummyUri = URI.create("http://example.com/schema.json");

        // WHEN – create the JsonSchema instance via the public factory method.
        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                schemaPath,
                dummyUri,
                schemaNode,
                null,               // no parent schema
                false               // do not suppress sub‑schema retrieval
        );

        // THEN – verify the string representation matches the contract.
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(),
                "JsonSchema.toString() should combine the quoted schema path with the node's JSON string");
    }

    @Test
    void toString_handlesEmptySchemaNodeString() {
        // GIVEN
        String schemaPath = "#";
        String nodeString = "";
        when(schemaNode.toString()).thenReturn(nodeString);
        URI dummyUri = URI.create("http://example.com/empty.json");

        // WHEN
        JsonSchema jsonSchema = JsonSchema.from(
                validationContext,
                schemaPath,
                dummyUri,
                schemaNode,
                null,
                false
        );

        // THEN
        String expected = "\"" + schemaPath + "\" : " + nodeString;
        assertEquals(expected, jsonSchema.toString(),
                "toString() should correctly handle an empty node string");
    }
}
```