package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class JsonSchema_findAncestor_Test {

    // -------------------------------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonMetaSchema metaSchema;

    /**
     * Common stub setup executed before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // ValidationContext behavior required by the JsonSchema constructor
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        // JsonMetaSchema behavior used inside the constructor
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // SchemaLocation behavior used inside the constructor
        when(schemaLocation.resolve(any())).thenReturn(null);
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.toString()).thenReturn("mock-location");
    }

    /**
     * Uses reflection to set the internal {@code parentSchema} field of a
     * {@link JsonSchema} instance. The actual field name in the library is
     * {@code parentSchema}; if the implementation ever changes, this method
     * will fail fast, making the test suite self‑documenting.
     */
    private static void setParent(JsonSchema child, JsonSchema parent) {
        try {
            Field f = JsonSchema.class.getDeclaredField("parentSchema");
            f.setAccessible(true);
            f.set(child, parent);
        } catch (NoSuchFieldException e) {
            // Fallback for possible different field name in older versions
            try {
                Field f = JsonSchema.class.getDeclaredField("parent");
                f.setAccessible(true);
                f.set(child, parent);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to set parent schema via reflection", ex);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void findAncestor_ShouldReturnSelf_WhenNoParent() {
        // Arrange: create a JsonSchema instance with a null parent
        JsonSchema root = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
        // Act
        JsonSchema result = root.findAncestor();
        // Assert
        assertSame(root, result, "When there is no parent, findAncestor() must return the instance itself");
    }

    @Test
    void findAncestor_ShouldReturnTopMostAncestor_WhenParentChainExists() {
        // Arrange: build a three‑level hierarchy (grandParent -> parent -> child)
        JsonSchema grandParent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonSchema parent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, grandParent, false);
        JsonSchema child = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, parent, false);
        // Act
        JsonSchema ancestor = child.findAncestor();
        // Assert
        assertSame(grandParent, ancestor, "findAncestor() must walk up the parent chain and return the top‑most ancestor");
    }

    @Test
    void findAncestor_ShouldReturnDirectParent_WhenSingleParent() {
        // Arrange: parent -> child
        JsonSchema parent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonSchema child = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, // temporarily no parent
        null, false);
        setParent(child, parent);
        // Act
        JsonSchema result = child.findAncestor();
        // Assert
        assertSame(parent, result, "A schema with a single parent should return that parent as the ancestor");
    }

    @Test
    void findAncestor_ShouldReturnRoot_WhenDeepHierarchyFiveLevels() {
        // Arrange: create a chain of five schemas (root -> ... -> leaf)
        JsonSchema root = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonSchema level1 = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, root, false);
        JsonSchema level2 = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, level1, false);
        JsonSchema level3 = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, level2, false);
        JsonSchema leaf = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, level3, false);
        // Act
        JsonSchema result = leaf.findAncestor();
        // Assert
        assertSame(root, result, "In a 5‑level chain, findAncestor() should return the first (root) schema");
    }

    @Test
    void findAncestor_ShouldThrowStackOverflowError_WhenSelfLoop() {
        // Arrange: create a schema and set its parent to itself
        JsonSchema schema = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        // self‑reference
        setParent(schema, schema);
        // Act & Assert
        assertThrows(StackOverflowError.class, schema::findAncestor, "A self‑referencing parent should cause infinite recursion → StackOverflowError");
    }

    @Test
    void findAncestor_ShouldBeIdempotent_OnRootSchema() {
        // Arrange: build a three‑level hierarchy
        JsonSchema grandParent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonSchema parent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, grandParent, false);
        JsonSchema child = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, parent, false);
        // Act: first call obtains the root, second call on that root should return the same instance
        JsonSchema first = child.findAncestor();
        JsonSchema second = first.findAncestor();
        // Assert
        assertSame(grandParent, first, "First call should return the top‑most ancestor");
        assertSame(first, second, "Calling findAncestor() on the result must be idempotent");
    }

    @Test
    void findAncestor_ShouldNotMutateParentReference() {
        // Arrange: child with a known parent
        JsonSchema parent = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonSchema child = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        setParent(child, parent);
        // Act
        child.findAncestor();
        // Assert: the child's parent should still be the original parent
        assertSame(parent, child.getParentSchema(), "findAncestor() must not modify the original parent reference");
    }

    @Test
    void findAncestor_ShouldPropagateException_FromGetParentSchema() {
        // Arrange: create a spy and make getParentSchema() throw
        JsonSchema spy = spy(JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false));
        doThrow(new IllegalStateException("parent broken")).when(spy).getParentSchema();
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, spy::findAncestor, "Exception from getParentSchema() must propagate unchanged");
        assertEquals("parent broken", ex.getMessage());
    }

    @Test
    void findAncestor_ShouldThrowStackOverflowError_WhenIndirectCycle() {
        // Arrange: A -> B -> C -> A
        JsonSchema a = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        JsonSchema b = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, a, false);
        JsonSchema c = JsonSchema.from(validationContext, schemaLocation, evaluationPath, schemaNode, b, false);
        // create the cycle
        // A's parent becomes C, forming A→B→C→A
        setParent(a, c);
        // Act & Assert
        assertThrows(StackOverflowError.class, () -> b.findAncestor(), "An indirect parent cycle should cause infinite recursion → StackOverflowError");
    }
}
