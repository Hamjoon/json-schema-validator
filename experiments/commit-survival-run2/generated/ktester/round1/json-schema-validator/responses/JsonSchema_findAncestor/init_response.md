```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#findAncestor()}.
 *
 * <p>The tests verify that the method returns the current schema when it has no parent,
 * and that it correctly walks up the parent chain to the top‑most ancestor.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    @Mock
    private ValidationContext validationContext;   // shared mock for all schemas

    @Mock
    private JsonNode schemaNode;                  // dummy schema node (content not relevant)

    private JsonSchema rootSchema;   // top‑most schema (no parent)
    private JsonSchema childSchema; // direct child of rootSchema
    private JsonSchema grandChildSchema; // child of childSchema

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal stubbing required for the constructor logic.
        // The config can be null – the constructor guards against it.
        // getMetaSchema() is accessed, so we provide a harmless mock.
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null);

        // Build a simple parent chain: root -> child -> grandChild
        rootSchema = JsonSchema.from(
                validationContext,
                "rootPath",
                URI.create("http://example.com/root"),
                schemaNode,
                null,               // no parent
                false);

        childSchema = JsonSchema.from(
                validationContext,
                "childPath",
                URI.create("http://example.com/child"),
                schemaNode,
                rootSchema,         // parent is rootSchema
                false);

        grandChildSchema = JsonSchema.from(
                validationContext,
                "grandChildPath",
                URI.create("http://example.com/grandChild"),
                schemaNode,
                childSchema,        // parent is childSchema
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    @Test
    void findAncestor_WhenSchemaHasNoParent_ReturnsItself() {
        // The root schema has no parent, so findAncestor() must return the same instance.
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result, "Root schema should be its own ancestor");
    }

    @Test
    void findAncestor_WhenSchemaHasDirectParent_ReturnsParent() {
        // Child's direct parent is rootSchema; the ancestor should be rootSchema.
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result, "Child schema should resolve to its direct parent as ancestor");
    }

    @Test
    void findAncestor_WhenSchemaHasMultipleAncestors_ReturnsTopMostAncestor() {
        // grandChild's ancestry chain is: grandChild -> child -> root.
        // The method should walk up recursively and return rootSchema.
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result, "Grand‑child schema should resolve to the top‑most ancestor");
    }
}
```