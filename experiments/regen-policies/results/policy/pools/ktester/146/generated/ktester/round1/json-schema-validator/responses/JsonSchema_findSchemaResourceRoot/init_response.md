```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * <p>The tests focus on the contract of the method:
 *
 * <ul>
 *   <li>If the schema instance itself is a schema‑resource root, the method must
 *       return the same instance.</li>
 *   <li>If the schema instance has a parent (i.e. it represents a subschema inside a
 *       document), the method must walk up the parent chain and return the
 *       document‑level root.</li>
 * </ul>
 *
 * <p>Only public API is used – the {@link JsonSchemaFactory} creates the
 * {@link JsonSchema} instances, and the {@link JsonSchema#getParentSchema()}
 * accessor (inherited from {@code BaseJsonValidator}) is used to verify the
 * hierarchy.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_findSchemaResourceRoot_Test {

    /** Factory used to create {@link JsonSchema} instances for the tests. */
    private static JsonSchemaFactory factory;

    /** Root schema used in the tests (no parent). */
    private JsonSchema rootSchema;

    /** Child schema obtained from the root via a $ref. */
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

        /*
         * Obtain the subschema that corresponds to the "$ref" target.
         *
         * The JsonSchema API does not expose a direct way to fetch a definition
         * by name, but we can trigger the $ref resolution by asking the root
         * schema to create a schema for the reference path.  The method
         * {@code fromRef} is public and does exactly that – it builds a new
         * JsonSchema whose parent is the original schema.
         *
         * The reference path for "#/definitions/inner" is represented by a
         * JsonNodePath that points to the "inner" definition.  The factory method
         * {@code JsonNodePath#empty()} creates a root‑level path; we then add
         * the required segments using the protected constructor via a helper
         * method in this test class (see {@link #createPath(String...)}).
         */
        JsonNodePath refPath = createPath("definitions", "inner");
        childSchema = rootSchema.fromRef(rootSchema, refPath);
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for these lightweight tests.
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
     * root path and chaining the protected constructors via reflection.  This
     * keeps the test self‑contained and does not rely on any internal fields
     * of {@code JsonSchema}.
     *
     * @param segments the path segments (in order)
     * @return a {@code JsonNodePath} representing the concatenated segments
     */
    private static JsonNodePath createPath(String... segments) {
        try {
            // Start with the public empty‑path constructor.
            JsonNodePath path = new JsonNodePath(JsonNodePath.PathType.OBJECT);
            // Use reflection to invoke the private constructors that add a segment.
            java.lang.reflect.Constructor<JsonNodePath> ctorString = JsonNodePath.class
                    .getDeclaredConstructor(JsonNodePath.class, String.class);
            ctorString.setAccessible(true);
            for (String seg : segments) {
                path = ctorString.newInstance(path, seg);
            }
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build JsonNodePath for test", e);
        }
    }

    @Test
    void rootSchema_isItsOwnResourceRoot() {
        // The root schema should be a resource root; the method must return itself.
        JsonSchema result = rootSchema.findSchemaResourceRoot();
        assertSame(rootSchema, result,
                "A schema that is already a resource root must return itself");
        assertTrue(result.isSchemaResourceRoot(),
                "The returned schema must report that it is a resource root");
    }

    @Test
    void childSchema_findsRootAsResourceRoot() {
        // The child schema (created via fromRef) has a non‑null parent.
        assertNotNull(childSchema.getParentSchema(),
                "The child schema created via fromRef should have a parent");

        // The method must walk up the parent chain and return the original root.
        JsonSchema result = childSchema.findSchemaResourceRoot();
        assertSame(rootSchema, result,
                "findSchemaResourceRoot should return the top‑most document schema");
        assertTrue(result.isSchemaResourceRoot(),
                "The returned schema must be a resource root");
    }
}
```