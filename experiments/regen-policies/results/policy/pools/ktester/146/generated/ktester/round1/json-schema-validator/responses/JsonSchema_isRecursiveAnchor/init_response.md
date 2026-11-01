```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#isRecursiveAnchor()}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    @Mock
    ValidationContext validationContext;   // mocked dependency

    @Mock
    JsonNode schemaNode;                 // mocked dependency

    @BeforeAll
    static void setupBeforeAll() {
        // No global setup required.
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension injects mocks automatically.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Subclass that exposes the protected {@code JsonSchema} constructor.
     * All parameters that are not relevant for the {@code isRecursiveAnchor}
     * test are passed as {@code null} or empty collections.
     */
    static class TestJsonSchema extends JsonSchema {
        TestJsonSchema(boolean recursiveAnchor,
                       ValidationContext validationContext,
                       JsonNode schemaNode) {
            super(
                /* validators */ Collections.emptyList(),
                /* validatorsLoaded */ false,
                /* recursiveAnchor */ recursiveAnchor,
                /* typeValidator */ null,
                /* id */ null,
                /* suppressSubSchemaRetrieval */ false,
                /* schemaNode */ schemaNode,
                /* validationContext */ validationContext,
                /* errorMessageType */ null,
                /* errorMessageKeyword */ null,
                /* messageSource */ null,
                /* keyword */ null,
                /* parentSchema */ null,
                /* schemaLocation */ null,
                /* evaluationPath */ null,
                /* evaluationParentSchema */ null,
                /* errorMessage */ Collections.emptyMap()
            );
        }
    }

    @Test
    void testIsRecursiveAnchorWhenTrue() {
        JsonSchema schema = new TestJsonSchema(true, validationContext, schemaNode);
        assertTrue(schema.isRecursiveAnchor(),
                "Expected isRecursiveAnchor() to return true when the flag is set via constructor");
    }

    @Test
    void testIsRecursiveAnchorWhenFalse() {
        JsonSchema schema = new TestJsonSchema(false, validationContext, schemaNode);
        assertFalse(schema.isRecursiveAnchor(),
                "Expected isRecursiveAnchor() to return false when the flag is not set via constructor");
    }
}
```