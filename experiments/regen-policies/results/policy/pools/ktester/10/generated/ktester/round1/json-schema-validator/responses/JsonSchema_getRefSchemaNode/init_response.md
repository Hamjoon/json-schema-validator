```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchema#getRefSchemaNode(String)}.
 *
 * The tests focus on the logic that:
 * <ul>
 *   <li>removes an optional schema‑id prefix,</li>
 *   <li>strips the leading “#” and “/”,</li>
 *   <li>URL‑decodes the pointer, and</li>
 *   <li>retrieves the target node via {@code JsonNode#at(String)}.</li>
 * </ul>
 *
 * Dependencies that are not part of the method’s core logic are mocked
 * (e.g. {@link JsonSchema#findSchemaResourceRoot()}, {@link JsonSchema#getSchemaNode()},
 * {@link JsonSchema#getId()}).
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    /** Shared JSON document used by all tests. */
    private static JsonNode TEST_ROOT_NODE;

    /** ObjectMapper for building the test JSON tree. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // {"a":{"b":1},"a/b":2}
        String json = """
                {
                  "a": {
                    "b": 1
                  },
                  "a/b": 2
                }
                """;
        TEST_ROOT_NODE = MAPPER.readTree(json);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Simple reference that starts with “#/”. The method should resolve the
     * pointer and return the node located at /a/b (value 1).
     */
    @Test
    void shouldResolveSimpleJsonPointer() {
        // Arrange ------------------------------------------------------------
        // Mock the root schema that holds the JSON document.
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null); // no $id for this test

        // The schema under test delegates to the root schema.
        JsonSchema schemaUnderTest = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaUnderTest.findSchemaResourceRoot()).thenReturn(rootSchema);

        // Act ----------------------------------------------------------------
        JsonNode result = schemaUnderTest.getRefSchemaNode("#/a/b");

        // Assert -------------------------------------------------------------
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isInt(), "Resulting node should be an integer");
        assertEquals(1, result.intValue(), "The value at /a/b should be 1");
    }

    /**
     * Reference that contains the schema’s $id as a prefix.
     * The method must strip the id, then resolve the remaining pointer.
     */
    @Test
    void shouldStripSchemaIdPrefixWhenPresent() {
        // Arrange ------------------------------------------------------------
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn("http://example.com/root");

        JsonSchema schemaUnderTest = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaUnderTest.findSchemaResourceRoot()).thenReturn(rootSchema);

        // The reference contains the id followed by the JSON‑pointer.
        String ref = "http://example.com/root#/a/b";

        // Act ----------------------------------------------------------------
        JsonNode result = schemaUnderTest.getRefSchemaNode(ref);

        // Assert -------------------------------------------------------------
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isInt(), "Resulting node should be an integer");
        assertEquals(1, result.intValue(), "The value at /a/b should be 1");
    }

    /**
     * Reference that uses URL‑encoded characters (e.g. “%2F” for “/”).
     * The method must URL‑decode the pointer before calling {@code at()}.
     */
    @Test
    void shouldDecodeUrlEncodedPointer() {
        // Arrange ------------------------------------------------------------
        JsonSchema rootSchema = mock(JsonSchema.class);
        when(rootSchema.getSchemaNode()).thenReturn(TEST_ROOT_NODE);
        when(rootSchema.getId()).thenReturn(null);

        JsonSchema schemaUnderTest = mock(JsonSchema.class, CALLS_REAL_METHODS);
        when(schemaUnderTest.findSchemaResourceRoot()).thenReturn(rootSchema);

        // The key in the JSON document is "a/b". The pointer must be encoded.
        String ref = "#/a%2Fb";

        // Act ----------------------------------------------------------------
        JsonNode result = schemaUnderTest.getRefSchemaNode(ref);

        // Assert -------------------------------------------------------------
        assertNotNull(result, "Resulting node must not be null");
        assertTrue(result.isInt(), "Resulting node should be an integer");
        assertEquals(2, result.intValue(), "The value at /a/b (encoded) should be 2");
    }
}
```