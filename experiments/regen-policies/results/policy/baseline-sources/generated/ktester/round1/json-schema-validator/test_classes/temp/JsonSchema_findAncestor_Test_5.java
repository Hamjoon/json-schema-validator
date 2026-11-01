package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
// <-- added import
// import org.mockito.Mockito;

// import org.mockito.Mockito;
class JsonSchema_findAncestor_Test {

    //     @Mock
    private ValidationContext // shared mock for all schemas
    validationContext;

    //     @Mock
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
        // Mockito can mock concrete classes, which solves the “not an interface” problem.
        // <-- create mock
//         validationContext = Mockito.mock(ValidationContext.class);
        // <-- create mock
//         schemaNode = Mockito.mock(JsonNode.class);
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
        JsonSchema result = rootSchema.findAncestor();
        assertSame(rootSchema, result, "Root schema should be its own ancestor");
    }

    @Test
    void findAncestor_WhenSchemaHasDirectParent_ReturnsParent() {
        JsonSchema result = childSchema.findAncestor();
        assertSame(rootSchema, result, "Child schema should resolve to the direct parent as ancestor");
    }

    @Test
    void findAncestor_WhenSchemaHasMultipleAncestors_ReturnsTopMostAncestor() {
        JsonSchema result = grandChildSchema.findAncestor();
        assertSame(rootSchema, result, "Grand‑child schema should resolve to the top‑most ancestor");
    }

    @Test
    void findAncestor_IdempotentOnRootSchema() {
        JsonSchema firstCall = rootSchema.findAncestor();
        JsonSchema secondCall = rootSchema.findAncestor();
        assertSame(rootSchema, firstCall, "First call should return the root itself");
        assertSame(rootSchema, secondCall, "Second call should also return the root itself");
        assertSame(firstCall, secondCall, "Both calls must return the identical instance");
    }

    @Test
    void findAncestor_DeepHierarchy_TenLevels() {
        List<JsonSchema> chain = new ArrayList<>();
        JsonSchema previous = null;
        for (int i = 0; i < 10; i++) {
            JsonSchema current = JsonSchema.from(validationContext, "path" + i, URI.create("http://example.com/schema" + i), schemaNode, // parent is the previous element (null for the first)
            previous, false);
            chain.add(current);
            previous = current;
        }
        JsonSchema leaf = chain.get(chain.size() - 1);
        JsonSchema topAncestor = chain.get(0);
        JsonSchema result = leaf.findAncestor();
        assertSame(topAncestor, result, "Leaf schema should resolve to the first schema in a ten‑level chain");
    }

    @Test
    void findAncestor_CyclicParentReference_ThrowsStackOverflowError() throws Exception {
        JsonSchema schemaA = JsonSchema.from(validationContext, "A", URI.create("http://example.com/A"), schemaNode, // temporary null parent
        null, false);
        JsonSchema schemaB = JsonSchema.from(validationContext, "B", URI.create("http://example.com/B"), schemaNode, // B's parent is A
        schemaA, false);
        // Introduce a cycle: A's parent becomes B.
        Field parentField = JsonSchema.class.getDeclaredField("parentSchema");
        parentField.setAccessible(true);
        parentField.set(schemaA, schemaB);
        // The method lacks cycle detection; a StackOverflowError is expected.
        assertThrows(StackOverflowError.class, () -> schemaA.findAncestor(), "A cyclic parent chain should cause a StackOverflowError");
    }
}