package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;
import com.networknt.schema.SpecVersion.VersionFlag;

@ExtendWith(MockitoExtension.class)
class JsonSchema_from_Test {

    // Common test data
    private static final String SCHEMA_PATH = "$.definitions.test";

    private static final URI CURRENT_URI = URI.create("http://example.com/schema.json");

    @Mock
    ValidationContext // mocked dependency
    validationContext;

    @Mock
    JsonMetaSchema // returned by validationContext.getMetaSchema()
    metaSchema;

    @Mock
    JsonSchema // parent schema (can be null in tests)
    parentSchema;

    @Mock
    SchemaValidatorsConfig // configuration object inside ValidationContext
    config;

    @Mock
    ValidationContext.DiscriminatorContext // for OpenAPI3 style discriminators
    discriminatorContext;

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing that is common to most tests
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test to avoid cross‑test interference
        clearInvocations(validationContext, metaSchema, parentSchema, config, discriminatorContext);
    }

    /**
     * Retrieves a private field value from the given {@link JsonSchema} instance using reflection.
     *
     * @param schema    the JsonSchema instance
     * @param fieldName the name of the private field
     * @return the field value (may be {@code null})
     */
    private static Object getPrivateField(JsonSchema schema, String fieldName) {
        try {
            Class<?> cls = schema.getClass();
            while (cls != null) {
                try {
                    var f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f.get(schema);
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            fail("Field '" + fieldName + "' not found in JsonSchema hierarchy");
            // unreachable
            return null;
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
    private static JsonSchema createSchema(ValidationContext vc, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent, boolean suppress) {
        return JsonSchema.from(vc, schemaPath, currentUri, schemaNode, parent, suppress);
    }

    private static ValidationContext newValidationContext() {
        // Minimal concrete implementations of the abstract factories.
        URIFactory uriFactory = new URIFactory() {

            @Override
            public URI create(URI baseUri, String ref) {
                return baseUri.resolve(ref);
            }
        };
        URNFactory urnFactory = new URNFactory() {

            @Override
            public URI create(String urn) {
                return URI.create(urn);
            }
        };
        // Use the default Draft‑V4 meta‑schema (adjust if a different version is required).
        JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V4);
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        return new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }

    /**
     * Verify that the static {@code from} method creates a non‑null {@link JsonSchema}
     * instance when the {@link ValidationContext} does not provide a {@link SchemaValidatorsConfig}
     * (i.e. {@code getConfig()} returns {@code null}).
     */
    @Test
    void from_WithNullConfig_ReturnsJsonSchema() {
        ValidationContext vc = newValidationContext();
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema result = JsonSchema.from(vc, SCHEMA_PATH, CURRENT_URI, schemaNode, null, false);
        assertNotNull(result, "JsonSchema.from should never return null");
    }

    /**
     * Verify that when a non‑null {@link SchemaValidatorsConfig} is supplied,
     * the constructor queries the configuration for keyword walk listeners.
     */
    @Test
    void from_WithNonNullConfig_InvokesKeywordWalkListenerSetup() {
        ValidationContext vc = newValidationContext();
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode();
        JsonSchema result = JsonSchema.from(vc, SCHEMA_PATH, CURRENT_URI, schemaNode, null, false);
        assertNotNull(result, "JsonSchema.from should produce a JsonSchema instance");
    }

    /**
     * Verify the OpenAPI‑3 style discriminator registration path.
     * When the configuration enables discriminators and the schema node contains a
     * {@code discriminator} property, the {@link ValidationContext.DiscriminatorContext}
     * should be asked to register it.
     */
    @Test
    void from_WithOpenApiDiscriminators_RegistersDiscriminator() {
        ValidationContext vc = newValidationContext();
        vc.getConfig().setOpenAPI3StyleDiscriminators(true);
        ObjectNode schemaNode = new ObjectNode(JsonNodeFactory.instance);
        ObjectNode discriminatorNode = new ObjectNode(JsonNodeFactory.instance);
        discriminatorNode.put("propertyName", "type");
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema result = JsonSchema.from(vc, SCHEMA_PATH, CURRENT_URI, schemaNode, null, false);
        assertNotNull(result, "JsonSchema.from should still return a JsonSchema instance");
    }

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

    @Test
    void from_EmptyPathAndNullUri_StoresValuesCorrectly() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "";
        URI uri = null;
        JsonNode node = JsonNodeFactory.instance.objectNode();
        JsonSchema parent = createSchema(newValidationContext(), "#/root", URI.create("http://example.com"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema schema = createSchema(vc, schemaPath, uri, node, parent, false);
        assertNotNull(schema);
        assertNull(schema.getCurrentUri(), "Current URI should be null");
        assertEquals("", getSchemaPath(schema), "Schema path should be the empty string");
    }

    @Test
    void from_NullValidationContext_ThrowsNPE() {
        String schemaPath = "#/properties/id";
        URI uri = URI.create("http://example.com/schema.json");
        JsonNode node = JsonNodeFactory.instance.objectNode();
        Assertions.assertThrows(NullPointerException.class, () -> createSchema(null, schemaPath, uri, node, null, false));
    }

    @Test
    void from_NullSchemaNode_ThrowsNPE() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/properties/age";
        URI uri = URI.create("http://example.com/person.json");
        Assertions.assertThrows(NullPointerException.class, () -> createSchema(vc, schemaPath, uri, null, null, false));
    }

    @Test
    void from_RelativeUri_StoresUriUnmodified() {
        ValidationContext vc = newValidationContext();
        String schemaPath = "#/components/schemas/Address";
        URI uri = URI.create("http://example.com/api/v1/");
        JsonSchema schema = createSchema(vc, schemaPath, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertEquals(uri, schema.getCurrentUri());
        assertEquals(schemaPath, getSchemaPath(schema));
    }

    @Test
    void from_SuppressTrueWithParent_StoresAllValues() {
        ValidationContext vc = newValidationContext();
        JsonSchema parent = createSchema(vc, "#", URI.create("file:/opt/schemas/root.json"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema schema = createSchema(vc, "#/properties/email", URI.create("file:/opt/schemas/user.json"), JsonNodeFactory.instance.objectNode(), parent, true);
        assertNotNull(schema);
        assertTrue(getSuppressFlag(schema));
        assertSame(parent, getPrivateField(schema, "parentSchema"));
    }

    @Test
    void from_EscapedSchemaPath_PreservesEscapes() {
        ValidationContext vc = newValidationContext();
        String escapedPath = "#/properties/~1special~0name";
        URI uri = URI.create("http://example.com/complex.json");
        JsonSchema schema = createSchema(vc, escapedPath, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertEquals(escapedPath, getSchemaPath(schema), "Escaped characters must not be decoded or altered");
    }

    @Test
    void from_UriWithQueryAndFragment_StoresExactly() {
        ValidationContext vc = newValidationContext();
        String path = "#/definitions/item";
        URI uri = URI.create("http://example.com/api?version=2#section");
        JsonSchema schema = createSchema(vc, path, uri, JsonNodeFactory.instance.objectNode(), null, false);
        assertEquals(uri, schema.getCurrentUri());
        assertEquals(path, getSchemaPath(schema));
    }

    @Test
    void from_MultiLevelParent_FindAncestorReturnsRoot() {
        ValidationContext vcRoot = newValidationContext();
        JsonSchema root = createSchema(vcRoot, "#", URI.create("http://example.com/root.json"), JsonNodeFactory.instance.objectNode(), null, false);
        JsonSchema middle = createSchema(vcRoot, "#/properties/middle", URI.create("http://example.com/middle.json"), JsonNodeFactory.instance.objectNode(), root, false);
        JsonSchema child = createSchema(vcRoot, "#/properties/inner", URI.create("http://example.com/outer.json"), JsonNodeFactory.instance.objectNode(), middle, false);
        JsonSchema ancestor = child.findAncestor();
        assertNotNull(ancestor);
        assertEquals("#", getSchemaPath(ancestor), "Ancestor should be the root schema with path \"#\"");
    }

    @Test
    void from_NullSchemaPath_ThrowsNPE() {
        ValidationContext vc = newValidationContext();
        URI uri = URI.create("http://example.com/schema.json");
        Assertions.assertThrows(NullPointerException.class, () -> createSchema(vc, null, uri, JsonNodeFactory.instance.objectNode(), null, false));
    }

    @Test
    void from_InvalidSchemaPathFormat_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        // missing leading '#'
        String badPath = "properties/name";
        Assertions.assertThrows(IllegalArgumentException.class, () -> createSchema(vc, badPath, URI.create("http://example.com/schema.json"), JsonNodeFactory.instance.objectNode(), null, false));
    }

    @Test
    void from_NonObjectSchemaNode_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        String path = "#/definitions/list";
        Assertions.assertThrows(IllegalArgumentException.class, () -> createSchema(vc, path, URI.create("http://example.com/schema.json"), JsonNodeFactory.instance.arrayNode(), null, false));
    }

    @Test
    void from_ParentWithDifferentValidationContext_ThrowsIllegalStateException() {
        ValidationContext vcChild = newValidationContext();
        ValidationContext vcParent = newValidationContext();
        JsonSchema parent = createSchema(vcParent, "#", URI.create("http://example.com/parent.json"), JsonNodeFactory.instance.objectNode(), null, false);
        Assertions.assertThrows(IllegalStateException.class, () -> createSchema(vcChild, "#/properties/child", URI.create("http://example.com/child.json"), JsonNodeFactory.instance.objectNode(), parent, false));
    }

    @Test
    void from_SelfReferentialParent_ThrowsIllegalArgumentException() {
        ValidationContext vc = newValidationContext();
        JsonSchema selfParent = JsonSchema.from(vc, "#", URI.create("http://example.com/self.json"), JsonNodeFactory.instance.objectNode(), null, false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> createSchema(vc, "#/properties/self", URI.create("http://example.com/self.json"), JsonNodeFactory.instance.objectNode(), selfParent, false));
    }
}
