```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@code BaseJsonValidator.debug} static method.
 *
 * <p>The method simply forwards a debug log message to the supplied {@link Logger}
 * instance. The test verifies that the logger receives the correct call with the
 * expected format string and arguments.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_debug_Test {

    /** Mocked logger used to capture the debug call. */
    @Mock
    private Logger logger;

    /** Mocked JSON node representing the instance being validated. */
    @Mock
    private JsonNode node;

    /** Mocked JSON node representing the root of the document. */
    @Mock
    private JsonNode rootNode;

    /** Mocked path indicating the location of the instance within the document. */
    @Mock
    private JsonNodePath instanceLocation;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are automatically initialised by the extension.
        // No additional per‑test setup required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test teardown required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void debug_shouldLogCorrectMessage() {
        // WHEN: the static debug method is invoked
        BaseJsonValidator.debug(logger, node, rootNode, instanceLocation);

        // THEN: the logger must receive a debug call with the exact format string
        // and the three supplied arguments in the same order.
        verify(logger, times(1)).debug(
                eq("validate( {}, {}, {})"),
                same(node),
                same(rootNode),
                same(instanceLocation));
    }
}
```