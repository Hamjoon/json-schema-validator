```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#isRecursiveAnchor()}.
 *
 * The {@code JsonSchema} instances are created via the public static factory method
 * {@code JsonSchema.from(...)}.  All collaborators required by the factory method are
 * mocked, because the focus of these tests is only the behaviour of the {@code isRecursiveAnchor()}
 * accessor.
 *
 * The default value of {@code recursiveAnchor} is {@code false}; the current public API
 * does not provide a way to enable it without parsing a schema that contains a
 * "$recursiveAnchor": true keyword.  Therefore the tests verify that the accessor
 * correctly returns {@code false} for a freshly created schema.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_isRecursiveAnchor_Test {

    /** Mocked validation context used by the factory method. */
    @Mock
    ValidationContext validationContext;

    /** Mocked meta‑schema required by {@link ValidationContext}. */
    @Mock
    JsonMetaSchema metaSchema;

    /** Mocked schema location – the concrete value is irrelevant for the test. */
    @Mock
    SchemaLocation schemaLocation;

    /** Mocked evaluation path – also irrelevant for the test. */
    @Mock
    JsonNodePath evaluationPath;

    /** A simple JSON object used as the schema node. */
    private static final ObjectNode SCHEMA_NODE = JsonNodeFactory.instance.objectNode();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the validation context so that the constructor logic can execute without NPEs.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // The factory method asks the context for an ID; return {@code null} to trigger the
        // default branch where {@code recursiveAnchor} stays {@code false}.
        when(validationContext.resolveSchemaId(SCHEMA_NODE)).thenReturn(null);
        // The maps used for schema resources / dynamic anchors are not inspected by the
        // {@code isRecursiveAnchor()} method, but they must be non‑null to avoid
        // {@code NullPointerException}s inside the constructor.
        when(validationContext.getSchemaResources()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * Verify that a newly created {@link JsonSchema} reports {@code false}
     * for {@code isRecursiveAnchor()}.
     */
    @Test
    void isRecursiveAnchor_ShouldReturnFalse_ForFreshSchema() {
        // Create the schema via the public factory method.
        JsonSchema schema = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                SCHEMA_NODE,
                null,               // no parent schema
                false               // do not suppress sub‑schema retrieval
        );

        // The accessor should return the default value.
        assertFalse(schema.isRecursiveAnchor(),
                "A freshly created JsonSchema must have recursiveAnchor == false");
    }

    /**
     * Verify that the accessor consistently returns {@code false} even when the
     * schema is created with a parent schema (which itself does not have a recursive
     * anchor).
     */
    @Test
    void isRecursiveAnchor_ShouldReturnFalse_WhenParentSchemaProvided() {
        // Parent schema – also created via the factory method.
        JsonSchema parent = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                SCHEMA_NODE,
                null,
                false
        );

        // Child schema with the parent supplied.
        JsonSchema child = JsonSchema.from(
                validationContext,
                schemaLocation,
                evaluationPath,
                SCHEMA_NODE,
                parent,
                false
        );

        // Both parent and child must report false.
        assertFalse(parent.isRecursiveAnchor(),
                "Parent schema should not have a recursive anchor");
        assertFalse(child.isRecursiveAnchor(),
                "Child schema should not have a recursive anchor");
    }
}
```