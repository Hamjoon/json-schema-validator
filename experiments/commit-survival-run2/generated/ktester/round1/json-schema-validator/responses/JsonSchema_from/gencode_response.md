```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the static factory method {@link JsonSchema#from(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)}.
 * <p>
 * The tests cover normal object creation, handling of {@code null} or illegal arguments,
 * and verification of internal state (schemaPath, currentUri, ancestor chain) via reflection.
 * </p>
 */
class JsonSchema_from_AdditionalTests {

    /* --------------------------------------------------------------------- */
    /* Helper utilities                                                     */
    /* --------------------------------------------------------------------- */

    /**
     * Retrieves a private field value from the given {@link JsonSchema} instance using reflection.
     *
     * @param schema    the JsonSchema instance
     * @param fieldName the name of the private field
     * @return the field value (may be {@code null})
     */
    private static Object getPrivateField(JsonSchema schema, String fieldName) {
        try {
            // The field may be declared in JsonSchema or a superclass (BaseJsonValidator)
            Class<?> cls = schema.getClass();
            while (cls != null) {
                try {
                    Field f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(schema);
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            fail("Field '" + fieldName + "' not found in JsonSchema hierarchy");
            return null; // unreachable
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Convenience wrapper to obtain the {@code schemaPath} stored inside the JsonSchema.
     *
     * @param schema the JsonSchema instance
     * @return the schemaPath string
     */
    private static String getSchemaPath(JsonSchema schema) {
        return (String) getPrivateField(schema, "schemaPath");
    }

    /**
     * Convenience wrapper to obtain the {@code suppressSubSchemaRetrieval} flag stored inside the JsonSchema.
     *
     * @param schema the JsonSchema instance
     * @return the flag value
     */
    private static boolean getSuppressFlag(JsonSchema schema) {
        return (boolean) getPrivateField(schema, "suppressSubSchemaRetrieval");
    }

    /**
     * Factory method that forwards directly to the package‑private {@code JsonSchema.from(...)}.
     */
    private static JsonSchema createSchema(ValidationContext vc,
                                            String schemaPath,
                                            URI currentUri,
                                            JsonNode schemaNode,
                                            JsonSchema parent,
                                            boolean suppress) {
        return JsonSchema.from(vc, schemaPath, currentUri, schemaNode, parent, suppress);
    }

    /* --------------------------------------------------------------------- */
    /* Test data provider                                                   */
    /* --------------------------------------------------------------------- */

    private static ValidationContext newValidationContext() {
        return new ValidationContext(); // default constructor – sufficient for our tests
    }

    /* --------------------------------------------------------------------- */
    /* 1. Standard creation with all non‑null arguments                        */
    /* --------------------------------------------------------------------- */

    @Test
    void from_StandardParameters_CreatesInstance() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/properties/name";
        URI uri = URI.create("http://example.com/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();

        JsonSchema schema = createSchema(vc, schemaPath, uri, node, null, false);

        assertNotNull(schema, "Factory should return a non‑null JsonSchema");
        assertEquals(uri, schema.getCurrentUri(), "Current URI must be stored unchanged");
        assertEquals(schemaPath, getSchemaPath(schema), "Schema path must be stored unchanged");
        assertFalse(getSuppressFlag(schema), "Suppress flag should be false");
    }

    /* --------------------------------------------------------------------- */
    /* 2. Null parent schema and suppress flag true                             */
    /* --------------------------------------------------------------------- */

    @Test
    void from_NullParentAndSuppressTrue_CreatesInstance() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#";
        URI uri = URI.create("file:/tmp/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();

        JsonSchema schema = createSchema(vc, schemaPath, uri, node, null, true);

        assertNotNull(schema);
        assertTrue(getSuppressFlag(schema), "Suppress flag must be true");
        assertNull(getPrivateField(schema, "parentSchema"), "Parent should be null");
    }

    /* --------------------------------------------------------------------- */
    /* 3. Empty schemaPath & null currentUri                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void from_EmptyPathAndNullUri_StoresValuesCorrectly() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "";
        URI uri = null;
        JsonNode node = JsonNodeFactory.instance.objectNode();

        // a parent schema with normal values
        JsonSchema parent = createSchema(newValidationContext(),
                "#/root", URI.create("http://example.com"),
                JsonNodeFactory.instance.objectNode(),
                null, false);

        JsonSchema schema = createSchema(vc, schemaPath, uri, node, parent, false);

        assertNotNull(schema);
        assertNull(schema.getCurrentUri(), "Current URI should be null");
        assertEquals("", getSchemaPath(schema), "Schema path should be the empty string");
    }

    /* --------------------------------------------------------------------- */
    /* 4. Null ValidationContext – expect NPE                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void from_NullValidationContext_ThrowsNPE() {
        String schemaPath = "#/properties/id";
        URI uri = URI.create("http://example.com/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();

        Executable ctor = () -> createSchema(null, schemaPath, uri, node, null, false);
        assertThrows(NullPointerException.class, ctor);
    }

    /* --------------------------------------------------------------------- */
    /* 5. Null schemaNode – expect NPE                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void from_NullSchemaNode_ThrowsNPE() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/properties/age";
        URI uri = URI.create("http://example.com/person.json");

        Executable ctor = () -> createSchema(vc, schemaPath, uri, null, null, false);
        assertThrows(NullPointerException.class, ctor);
    }

    /* --------------------------------------------------------------------- */
    /* 6. Relative base URI – should be stored unchanged                         */
    /* --------------------------------------------------------------------- */

    @Test
    void from_RelativeUri_StoresUriUnmodified() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/components/schemas/Address";
        URI uri = URI.create("http://example.com/api/v1/"); // ends with '/' – treated as base

        JsonSchema schema = createSchema(vc, schemaPath, uri, JsonNodeFactory.instance.objectNode(), null, false);

        assertEquals(uri, schema.getCurrentUri());
        assertEquals(schemaPath, getSchemaPath(schema));
    }

    /* --------------------------------------------------------------------- */
    /* 7. Suppress flag true with a non‑null parent                             */
    /* --------------------------------------------------------------------- */

    @Test
    void from_SuppressTrueWithParent_StoresAllValues() {
        ValidationContext vc = newValidationContext();
        JsonSchema parent = createSchema(vc,
                "#", URI.create("file:/opt/schemas/root.json"),
                JsonNodeFactory.instance.objectNode(),
                null, false);

        JsonSchema schema = createSchema(vc,
                "#/properties/email",
                URI.create("file:/opt/schemas/user.json"),
                JsonNodeFactory.instance.objectNode(),
                parent,
                true);

        assertNotNull(schema);
        assertTrue(getSuppressFlag(schema));
        assertSame(parent, getPrivateField(schema, "parentSchema"));
    }

    /* --------------------------------------------------------------------- */
    /* 8. Complex schemaPath with JSON‑Pointer escape sequences                */
    /* --------------------------------------------------------------------- */

    @Test
    void from_EscapedSchemaPath_PreservesEscapes() {
        ValidationContext vc = newValidationContext();
        String escapedPath = "#/properties/~1special~0name";
        URI uri = URI.create("http://example.com/complex.json");

        JsonSchema schema = createSchema(vc, escapedPath, uri,
                JsonNodeFactory.instance.objectNode(), null, false);

        assertEquals(escapedPath, getSchemaPath(schema),
                "Escaped characters must not be decoded or altered");
    }

    /* --------------------------------------------------------------------- */
    /* 9. URI with query and fragment                                            */
    /* --------------------------------------------------------------------- */

    @Test
    void from_UriWithQueryAndFragment_StoresExactly() {
        ValidationContext vc = newValidationContext();
        String path = "#/definitions/item";
        URI uri = URI.create("http://example.com/api?version=2#section");

        JsonSchema schema = createSchema(vc, path, uri,
                JsonNodeFactory.instance.objectNode(), null, false);

        assertEquals(uri, schema.getCurrentUri());
        assertEquals(path, getSchemaPath(schema));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Multi‑level parent chain – findAncestor returns root                 */
    /* --------------------------------------------------------------------- */

    @Test
    void from_MultiLevelParent_FindAncestorReturnsRoot() {
        ValidationContext vcRoot = newValidationContext();
        JsonSchema root = createSchema(vcRoot,
                "#", URI.create("http://example.com/root.json"),
                JsonNodeFactory.instance.objectNode(),
                null, false);

        JsonSchema middle = createSchema(vcRoot,
                "#/properties/middle", URI.create("http://example.com/middle.json"),
                JsonNodeFactory.instance.objectNode(),
                root, false);

        JsonSchema child = createSchema(vcRoot,
                "#/properties/inner", URI.create("http://example.com/outer.json"),
                JsonNodeFactory.instance.objectNode(),
                middle, false);

        JsonSchema ancestor = child.findAncestor();

        assertNotNull(ancestor);
        assertEquals("#", getSchemaPath(ancestor), "Ancestor should be the root schema with path \"#\"");
    }

    /* --------------------------------------------------------------------- */
    /* 11. Null schemaPath – expect NPE                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void from_NullSchemaPath_ThrowsNPE() {
        ValidationContext vc = newValidationContext();
        URI uri = URI.create("http://example.com/schema.json");

        Executable ctor = () -> createSchema(vc, null, uri,
                JsonNodeFactory.instance.objectNode(), null, false);
        assertThrows(NullPointerException.class, ctor);
    }

    /* --------------------------------------------------------------------- */
    /* 12. Invalid schemaPath format (does not start with '#')                */
    /* --------------------------------------------------------------------- */

    @Test
    void from_InvalidSchemaPathFormat_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        String badPath = "properties/name"; // missing leading '#'

        Executable ctor = () -> createSchema(vc, badPath, URI.create("http://example.com/schema.json"),
                JsonNodeFactory.instance.objectNode(), null, false);
        assertThrows(IllegalArgumentException.class, ctor);
    }

    /* --------------------------------------------------------------------- */
    /* 13. Non‑object schemaNode (array) – expect IllegalArgumentException       */
    /* --------------------------------------------------------------------- */

    @Test
    void from_NonObjectSchemaNode_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        String path = "#/definitions/list";

        Executable ctor = () -> createSchema(vc, path,
                URI.create("http://example.com/schema.json"),
                JsonNodeFactory.instance.arrayNode(),
                null, false);
        assertThrows(IllegalArgumentException.class, ctor);
    }

    /* --------------------------------------------------------------------- */
    /* 14. Parent created with a different ValidationContext – expect IllegalStateException */
    /* --------------------------------------------------------------------- */

    @Test
    void from_ParentWithDifferentValidationContext_ThrowsIllegalStateException() {
        ValidationContext vcChild = newValidationContext();
        ValidationContext vcParent = newValidationContext(); // distinct instance

        JsonSchema parent = createSchema(vcParent,
                "#", URI.create("http://example.com/parent.json"),
                JsonNodeFactory.instance.objectNode(),
                null, false);

        Executable ctor = () -> createSchema(vcChild,
                "#/properties/child",
                URI.create("http://example.com/child.json"),
                JsonNodeFactory.instance.objectNode(),
                parent,
                false);

        assertThrows(IllegalStateException.class, ctor);
    }

    /* --------------------------------------------------------------------- */
    /* 15. Self‑referential parent – expect IllegalArgumentException            */
    /* --------------------------------------------------------------------- */

    @Test
    void from_SelfReferentialParent_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        JsonSchema selfParent = JsonSchema.from(vc,
                "#", URI.create("http://example.com/self.json"),
                JsonNodeFactory.instance.objectNode(),
                null, false);

        Executable ctor = () -> createSchema(vc,
                "#/properties/self",
                URI.create("http://example.com/self.json"),
                JsonNodeFactory.instance.objectNode(),
                selfParent, // parent points to the same instance that will be returned by from()
                false);

        // The implementation should detect the circular reference and reject it.
        assertThrows(IllegalArgumentException.class, ctor);
    }
}
```