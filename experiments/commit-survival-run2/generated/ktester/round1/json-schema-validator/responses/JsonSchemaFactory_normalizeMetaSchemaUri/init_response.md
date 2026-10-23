```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for static method tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void shouldConvertHttpToHttpsAndPreserveComponents() {
        // simple case without user info, port, query or fragment
        String input = "http://example.com/schema.json";
        String expected = "https://example.com/schema.json";

        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldPreserveUserInfoAndPortWhileSwitchingScheme() {
        // case with user info, port and path
        String input = "http://user:pass@example.com:8080/path/to/resource";
        String expected = "https://user:pass@example.com:8080/path/to/resource";

        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldStripQueryAndFragmentWhenNormalizing() {
        // case with query and fragment – they must be removed
        String input = "http://example.com:9090/path?query=123#section";
        String expected = "https://example.com:9090/path";

        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowJsonSchemaExceptionForInvalidUri() {
        String invalidInput = "http://[invalid";

        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> JsonSchemaFactory.normalizeMetaSchemaUri(invalidInput));

        assertTrue(thrown.getMessage().contains("Wrong MetaSchema URI"));
        assertTrue(thrown.getMessage().contains(invalidInput));
    }
}
```