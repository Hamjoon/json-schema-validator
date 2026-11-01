```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * <p>The test suite covers:
 *
 * <ul>
 *   <li>Normal behaviour when the current schema is already a resource root.</li>
 *   <li>Walking up a parent chain of arbitrary length.</li>
 *   <li>Stopping at the first ancestor that reports {@code true} for
 *       {@link JsonSchema#isSchemaResourceRoot()}.</li>
 *   <li>Edge‑cases such as a missing parent (NPE), cyclic parent references
 *       (StackOverflowError), and exception propagation from overridden
 *       methods.</li>
 * </ul>
 *
 * <p>Real {@link JsonSchema} instances are used for the simple root/child
 * scenario, while Mockito spies (CALLS_REAL_METHODS) are used to craft custom
 * hierarchies and to inject exceptional behaviour without touching private
 * members.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /** Factory used to create real {@link JsonSchema} instances. */
    private static JsonSchemaFactory factory;

    /** Simple root schema used by the first two tests. */
    private JsonSchema rootSchema;

    /** Child schema obtained via a $ref from {@code rootSchema}. */
    private JsonSchema childSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 meta‑schema – it is the most common version and works
        // for the simple test schemas below.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Root schema – a simple object with an ID.  The "$ref" points to the
         * definition "inner" that lives in the same document.  Loading the
         * schema creates a hierarchy where the schema that represents the
         * "$ref" target has a non‑null parent (the root schema).
         */
        String rootSchemaJson = """
                {
                  "$id": "http://example.com/root.json",
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "inner": { "$ref": "#/definitions/inner" }
                  },
                  "definitions": {
                    "inner": {
                      "type": "string"
                    }
                  }
                }
                """;

        // No special configuration – the default is fine for these tests.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // Create the root schema instance.
        rootSchema = factory.getSchema(rootSchemaJson, config);

        // Build a JsonNodePath that points to the definition "inner".
        JsonNodePath refPath = createPath("definitions", "inner");
        childSchema = rootSchema.fromRef(rootSchema, refPath);
    }

    @AfterEach
    void teardownAfterEach() {
        rootSchema = null;
        childSchema = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /**
     * Helper that builds a {@link JsonNodePath} representing a JSON‑Pointer
     * consisting of the supplied path segments.
     *
     * <p>Because {@code JsonNodePath} does not expose a public constructor that
     * accepts a var‑args list, we create the path by starting with an empty
     * root path and chaining the protected constructors via reflection.
     *
     * @param segments the path segments (in order)
     * @return a {@code JsonNodePath} representing the concatenated segments
     */
    private static JsonNodePath createPath(String... segments) {
        try {
            JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.OBJECT);
            java.lang.reflect.Constructor<JsonNodePath> ctor = JsonNodePath.class
                    .getDeclaredConstructor(JsonNodePath.class, String.class);
            ctor.setAccessible(true);
            for (String seg : segments) {
                path = ctor.newInstance(path, seg);
            }
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build JsonNodePath for test", e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Simple real‑schema tests (already present in the original template) */
    /* --------------------------------------------------------------------- */

    @Test
    void rootSchema_isItsOwnResourceRoot() {
        JsonSchema result = rootSchema.findSchemaResourceRoot();
        assertSame(rootSchema, result,
                "A schema that is already a resource root must return itself");
        assertTrue(result.isSchemaResourceRoot(),
                "The returned schema must report that it is a resource root");
    }

    @Test
    void childSchema_findsRootAsResourceRoot() {
        assertNotNull(childSchema.getParentSchema(),
                "The child schema created via fromRef should have a parent");
        JsonSchema result = childSchema.findSchemaResourceRoot();
        assertSame(rootSchema, result,
                "findSchemaResourceRoot should return the top‑most document schema");
        assertTrue(result.isSchemaResourceRoot(),
                "The returned schema must be a resource root");
    }

    /* --------------------------------------------------------------------- */
    /*  Mock‑based hierarchy tests (covers all groups from the JSON spec)   */
    /* --------------------------------------------------------------------- */

    /**
     * Utility to create a Mockito spy (CALLS_REAL_METHODS) of {@link JsonSchema}
     * with the desired {@code isSchemaResourceRoot} flag and parent reference.
     */
    private JsonSchema mockSchema(boolean isRoot, JsonSchema parent) {
        JsonSchema mock = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(mock.isSchemaResourceRoot()).thenReturn(isRoot);
        Mockito.when(mock.getParentSchema()).thenReturn(parent);
        return mock;
    }

    @Test
    void threeLevelHierarchy_onlyRootIsResourceRoot() {
        // leaf → middle → root, only root returns true.
        JsonSchema root = mockSchema(true, null);
        JsonSchema middle = mockSchema(false, root);
        JsonSchema leaf = mockSchema(false, middle);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(root, result, "Should return the top‑most root that is a resource root");
    }

    @Test
    void threeLevelHierarchy_middleAndRootAreResourceRoot() {
        // leaf → middle(true) → root(true) – should stop at middle.
        JsonSchema root = mockSchema(true, null);
        JsonSchema middle = mockSchema(true, root);
        JsonSchema leaf = mockSchema(false, middle);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(middle, result, "Should stop at the first ancestor that is a resource root (middle)");
    }

    @Test
    void leafWithNullParentAndNotRoot_throwsNullPointerException() {
        JsonSchema leaf = mockSchema(false, null); // parent is null, not a root

        assertThrows(NullPointerException.class,
                leaf::findSchemaResourceRoot,
                "When no ancestor reports true and parent is null, a NPE is expected");
    }

    @Test
    void fiveLevelHierarchy_onlyLevel5IsResourceRoot() {
        // level1 → level2 → level3 → level4 → level5(true)
        JsonSchema level5 = mockSchema(true, null);
        JsonSchema level4 = mockSchema(false, level5);
        JsonSchema level3 = mockSchema(false, level4);
        JsonSchema level2 = mockSchema(false, level3);
        JsonSchema level1 = mockSchema(false, level2);

        JsonSchema result = level1.findSchemaResourceRoot();
        assertSame(level5, result,
                "Should return the first (and only) ancestor that is a resource root (level5)");
    }

    @Test
    void twoLevelHierarchy_parentIsResourceRoot() {
        // child(false) → parent(true)
        JsonSchema parent = mockSchema(true, null);
        JsonSchema child = mockSchema(false, parent);

        JsonSchema result = child.findSchemaResourceRoot();
        assertSame(parent, result,
                "Should return the immediate parent when it is the first resource root");
    }

    @Test
    void fourLevelHierarchy_middleIsFirstResourceRoot() {
        // leaf(false) → level2(false) → level3(true) → level4(true)
        JsonSchema level4 = mockSchema(true, null);
        JsonSchema level3 = mockSchema(true, level4);
        JsonSchema level2 = mockSchema(false, level3);
        JsonSchema leaf = mockSchema(false, level2);

        JsonSchema result = leaf.findSchemaResourceRoot();
        assertSame(level3, result,
                "Should stop at level3, the first ancestor reporting true");
    }

    @Test
    void cyclicParentReference_causesStackOverflowError() {
        // schema whose parent is itself; isSchemaResourceRoot = false
        JsonSchema cyclic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(cyclic.isSchemaResourceRoot()).thenReturn(false);
        Mockito.when(cyclic.getParentSchema()).thenReturn(cyclic);

        assertThrows(StackOverflowError.class,
                cyclic::findSchemaResourceRoot,
                "A cyclic parent chain should eventually overflow the stack");
    }

    @Test
    void isSchemaResourceRootThrowsIllegalStateException_propagates() {
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(problematic.isSchemaResourceRoot())
                .thenThrow(new IllegalStateException("boom"));
        // parent can be anything; it will never be consulted because the exception occurs first
        Mockito.when(problematic.getParentSchema()).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                problematic::findSchemaResourceRoot,
                "Exception from isSchemaResourceRoot() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void getParentSchemaThrowsIllegalArgumentException_propagates() {
        JsonSchema problematic = Mockito.mock(JsonSchema.class, Mockito.CALLS_REAL_METHODS);
        Mockito.when(problematic.isSchemaResourceRoot()).thenReturn(false);
        Mockito.when(problematic.getParentSchema())
                .thenThrow(new IllegalArgumentException("parent error"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                problematic::findSchemaResourceRoot,
                "Exception from getParentSchema() must propagate");
        assertEquals("parent error", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper to build long chains for readability (optional)               */
    /* --------------------------------------------------------------------- */

    /**
     * Builds a chain of {@link JsonSchema} mocks of the given length.
     *
     * @param length            number of schemas in the chain (≥1)
     * @param trueAtPositions   indices (0‑based) where {@code isSchemaResourceRoot}
     *                          should return {@code true}
     * @return the leaf (first element) of the chain
     */
    private JsonSchema buildChain(int length, List<Integer> trueAtPositions) {
        JsonSchema next = null; // will become the root (no parent)
        for (int i = length - 1; i >= 0; i--) {
            boolean isRoot = trueAtPositions.contains(i);
            JsonSchema current = mockSchema(isRoot, next);
            next = current;
        }
        return next; // leaf
    }

    // The helper above is not used directly in the current tests but demonstrates
    // how additional scenarios could be generated programmatically.
}
```