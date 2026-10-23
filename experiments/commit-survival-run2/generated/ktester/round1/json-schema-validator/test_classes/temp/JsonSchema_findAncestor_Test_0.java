package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    @Mock
    private ValidationContext // shared mock for all schemas
    validationContext;

    @Mock
    private JsonNode // dummy schema node (content not relevant)
    schemaNode;

    // top‑most schema (no parent)
    private JsonSchema rootSchema;

    // direct child of rootSchema
    private JsonSchema childSchema;

    // child of childSchema
    private JsonSchema grandChildSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Minimal stubbing required for the constructor logic.
        // The config can be null – the constructor guards against it.
        // getMetaSchema() is accessed, so we provide a harmless mock.
        JsonMetaSchema metaSchema = mock(JsonMetaSchema.class);
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null);
        // Build a simple parent chain: root -> child -> grandChild
        rootSchema = JsonSchema.from(validationContext, "rootPath", URI.create("http://example.com/root"), schemaNode, // no parent
        null, false);
        childSchema = JsonSchema.from(validationContext, "childPath", URI.create("http://example.com/child"), schemaNode, // parent is rootSchema
        rootSchema, false);
        grandChildSchema = JsonSchema.from(validationContext, "grandChildPath", URI.create("http://example.com/grandChild"), schemaNode, // parent is childSchema
        childSchema, false);
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

    @Test
    void findAncestor_IdempotentOnRootSchema() {
        // Calling findAncestor() multiple times on a root schema must always return the same instance.
        JsonSchema firstCall = rootSchema.findAncestor();
        JsonSchema secondCall = rootSchema.findAncestor();
        assertSame(rootSchema, firstCall, "First call should return the root itself");
        assertSame(rootSchema, secondCall, "Second call should also return the root itself");
        assertSame(firstCall, secondCall, "Both calls must return the identical instance");
    }

    @Test
    void findAncestor_DeepHierarchy_TenLevels() {
        // Build a chain of ten schemas programmatically.
        List<JsonSchema> chain = new ArrayList<>();
        JsonSchema previous = null;
        for (int i = 0; i < 10; i++) {
            JsonSchema current = JsonSchema.from(validationContext, "path" + i, URI.create("http://example.com/schema" + i), schemaNode, // parent is the previous element (null for the first)
            previous, false);
            chain.add(current);
            previous = current;
        }
        // The leaf is the last element; the top‑most ancestor is the first element.
        JsonSchema leaf = chain.get(chain.size() - 1);
        JsonSchema topAncestor = chain.get(0);
        JsonSchema result = leaf.findAncestor();
        assertSame(topAncestor, result, "Leaf schema should resolve to the first schema in a ten‑level chain");
    }

    @Test
    void findAncestor_CyclicParentReference_ThrowsStackOverflowError() throws Exception {
        // Create two schemas and then create a cycle via reflection.
        JsonSchema schemaA = JsonSchema.from(validationContext, "A", URI.create("http://example.com/A"), schemaNode, // temporary null parent
        null, false);
        JsonSchema schemaB = JsonSchema.from(validationContext, "B", URI.create("http://example.com/B"), schemaNode, // B's parent is A
        schemaA, false);
        // Use reflection to set A's parent to B, forming a cycle.
        Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
        parentField.setAccessible(true);
        parentField.set(schemaA, schemaB);
        // The recursive method has no cycle detection, so a StackOverflowError is expected.
        assertThrows(StackOverflowError.class, () -> schemaA.findAncestor(), "A cyclic parent chain should cause a StackOverflowError");
    }
}
