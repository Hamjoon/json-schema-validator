```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link JsonSchema#findSchemaResourceRoot()}.
 *
 * <p>The original test suite relied on Mockito for creating mock {@link JsonSchema}
 * instances. Because Mockito is not available in this execution environment, the
 * test class has been simplified to use real {@link JsonSchema} objects created
 * via {@link JsonSchemaFactory}. The essential behaviour – that a root schema
 * returns itself and a child schema (created via {@code fromRef}) walks up to
 * the root – is still verified.</p>
 */
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

        // Default configuration is sufficient for these tests.
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // Create the root schema instance.
        rootSchema = factory.getSchema(rootSchemaJson, config);

        /*
         * Obtain the subschema that corresponds to the "$ref" target.
         *
         * The {@code fromRef} method builds a new {@link JsonSchema} whose parent
         * is the original schema.
         */
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
     * @param segments the path segments (in order)
     * @return a {@code JsonNodePath} representing the concatenated segments
     */
    private static JsonNodePath createPath(String... segments) {
        try {
            // Start with the public empty‑path constructor.
            JsonNodePath path = new JsonNodePath(PathType.OBJECT);
            // Use reflection to invoke the private constructor that adds a segment.
            Constructor<JsonNodePath> ctor = JsonNodePath.class
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