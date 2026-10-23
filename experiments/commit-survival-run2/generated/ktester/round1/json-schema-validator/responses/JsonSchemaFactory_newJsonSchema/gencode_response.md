```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.CompositeURITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)}.
 * <p>
 * The factory constructor is private, therefore the test creates an instance via reflection.
 * The method under test is {@code protected}, but the test class resides in the same package
 * (com.networknt.schema) so it can be invoked directly.  A reflective helper is provided for
 * readability and for the few cases where we need to invoke the method with {@code null}
 * arguments.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    private JsonSchemaFactory factory;
    private ObjectMapper jsonMapper;
    private YAMLMapper yamlMapper;

    /* --------------------------------------------------------------------- *
     *  Test fixture setup/teardown                                         *
     * --------------------------------------------------------------------- */

    @BeforeEach
    void setUp() throws Exception {
        jsonMapper = new ObjectMapper();
        yamlMapper = new YAMLMapper();

        URISchemeFactory uriFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriFetcher = new URISchemeFetcher(Collections.emptyMap());
        URNFactory urnFactory = new URNFactory();
        CompositeURITranslator uriTranslators = new CompositeURITranslator();

        // The factory keeps a map of meta‑schema URIs → JsonMetaSchema objects.
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);

        Map<String, String> uriMap = new HashMap<>();

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class
                .getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class,
                        URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class,
                        Map.class, Map.class, boolean.class, CompositeURITranslator.class);
        ctor.setAccessible(true);
        factory = ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI,
                uriFactory, uriFetcher, urnFactory, metaSchemas, uriMap, false, uriTranslators);
    }

    /* --------------------------------------------------------------------- *
     *  Helper to invoke the protected method via reflection                  *
     * --------------------------------------------------------------------- */

    private JsonSchema invokeNewJsonSchema(URI uri, JsonNode node, SchemaValidatorsConfig cfg) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("newJsonSchema", URI.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonSchema) m.invoke(factory, uri, node, cfg);
    }

    /* --------------------------------------------------------------------- *
     *  Parameter providers                                                   *
     * --------------------------------------------------------------------- */

    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(
                // group: Valid inputs – typical JSON schema
                Arguments.of(
                        URI.create("http://example.com/schema.json"),
                        "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}",
                        new SchemaValidatorsConfig(),
                        "typical"
                ),
                // group: Empty schema node – minimal object
                Arguments.of(
                        URI.create("http://example.com/empty-schema.json"),
                        "{}",
                        new SchemaValidatorsConfig(),
                        "empty"
                ),
                // group: Boolean schema – true literal
                Arguments.of(
                        URI.create("file:/tmp/boolean-schema.json"),
                        "true",
                        new SchemaValidatorsConfig(),
                        "boolean"
                ),
                // group: Complex schema with nested subschemas – branch coverage in doCreate
                Arguments.of(
                        URI.create("http://example.com/complex.json"),
                        "{\"type\":\"object\",\"properties\":{\"address\":{\"type\":\"object\",\"properties\":{\"street\":{\"type\":\"string\"}}}}}",
                        new SchemaValidatorsConfig(),
                        "complex"
                ),
                // group: $id and $schema meta‑schema handling
                Arguments.of(
                        URI.create("http://example.com/meta.json"),
                        "{\"$id\":\"http://example.com/meta.json\",\"$schema\":\"http://json-schema.org/draft-06/schema#\",\"type\":\"object\"}",
                        new SchemaValidatorsConfig(),
                        "metaId"
                ),
                // group: Unsupported keyword – should still create schema
                Arguments.of(
                        URI.create("http://example.com/unknown-key.json"),
                        "{\"type\":\"object\",\"unknownKeyword\":123}",
                        new SchemaValidatorsConfig(),
                        "unknownKeyword"
                ),
                // group: Array schema with items subschema
                Arguments.of(
                        URI.create("http://example.com/array-schema.json"),
                        "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}",
                        new SchemaValidatorsConfig(),
                        "arrayItems"
                ),
                // group: Schema node missing explicit type – still creates
                Arguments.of(
                        URI.create("http://example.com/no-type.json"),
                        "{\"properties\":{\"age\":{\"type\":\"integer\"}}}",
                        new SchemaValidatorsConfig(),
                        "noTopLevelType"
                ),
                // group: Schema URI containing a fragment
                Arguments.of(
                        new URI("http://example.com/schema.json#/definitions/foo"),
                        "{\"$ref\":\"#/definitions/bar\"}",
                        new SchemaValidatorsConfig(),
                        "fragmentUri"
                ),
                // group: Schema URI with query parameters
                Arguments.of(
                        new URI("http://example.com/schema.json?version=1"),
                        "{\"type\":\"object\"}",
                        new SchemaValidatorsConfig(),
                        "queryUri"
                ),
                // group: Custom configuration – typeLoose enabled
                Arguments.of(
                        URI.create("http://example.com/type-loose.json"),
                        "{\"type\":\"integer\",\"minimum\":5}",
                        new SchemaValidatorsConfig() {{ setTypeLoose(true); }},
                        "typeLoose"
                )
        );
    }

    private static Stream<Arguments> nullConfigProvider() {
        return Stream.of(
                // group: Null configuration – default behaviour
                Arguments.of(
                        URI.create("file:/tmp/meta-schema.json"),
                        "{\"$schema\":\"http://json-schema.org/draft-07/schema#\"}",
                        null,
                        "nullConfig"
                ),
                // group: Both schemaUri and config null
                Arguments.of(
                        null,
                        "{\"type\":\"string\"}",
                        null,
                        "nullUriAndConfig"
                )
        );
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(
                // group: Null schema node – error handling
                Arguments.of(
                        URI.create("http://example.com/empty.json"),
                        null,
                        new SchemaValidatorsConfig(),
                        NullPointerException.class,
                        "nullNode"
                ),
                // group: Invalid schema node – numeric literal
                Arguments.of(
                        URI.create("http://example.com/numeric.json"),
                        new com.fasterxml.jackson.databind.node.IntNode(42),
                        new SchemaValidatorsConfig(),
                        IllegalArgumentException.class,
                        "numericNode"
                ),
                // group: Invalid schema node – MissingNode
                Arguments.of(
                        URI.create("http://example.com/missing.json"),
                        com.fasterxml.jackson.databind.node.MissingNode.getInstance(),
                        new SchemaValidatorsConfig(),
                        IllegalArgumentException.class,
                        "missingNode"
                ),
                // group: Unsupported URI scheme – should raise exception
                Arguments.of(
                        new URI("unknown://example.com/schema.json"),
                        "{\"type\":\"object\"}",
                        new SchemaValidatorsConfig(),
                        IllegalArgumentException.class,
                        "unknownScheme"
                )
        );
    }

    /* --------------------------------------------------------------------- *
     *  Tests for successful schema creation                                   *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validSchemaProvider")
    void newJsonSchema_ValidSchemas_ShouldCreateCorrectJsonSchema(URI uri,
                                                                 String json,
                                                                 SchemaValidatorsConfig config,
                                                                 String description) throws Exception {
        JsonNode node = jsonMapper.readTree(json);
        JsonSchema schema = invokeNewJsonSchema(uri, node, config);
        assertNotNull(schema, "Schema must not be null for: " + description);

        // Verify that the original node is preserved (or equivalent) inside the JsonSchema.
        JsonNode storedNode = (JsonNode) getField(schema, "schemaNode");
        assertEquals(node, storedNode, "Stored schema node must equal the supplied node");

        // Verify that the current URI is stored correctly (may be null).
        URI storedUri = (URI) getField(schema, "currentUri");
        assertEquals(uri, storedUri, "Current URI should match the supplied URI");

        // Additional branching checks for selected groups
        switch (description) {
            case "complex":
                // the address property must have its own "properties" node
                JsonNode addressProps = storedNode.path("properties").path("address").path("properties");
                assertTrue(addressProps.has("street"), "Complex schema should contain nested 'street' property");
                break;
            case "boolean":
                assertTrue(storedNode.isBoolean() && storedNode.booleanValue(),
                        "Boolean schema node should be the literal true");
                break;
            case "fragmentUri":
                assertNotNull(storedUri);
                assertTrue(storedUri.getFragment().equals("definitions/foo"),
                        "Fragment part must be retained in the current URI");
                break;
            case "queryUri":
                assertNotNull(storedUri);
                assertEquals("version=1", storedUri.getQuery(),
                        "Query string must be preserved");
                break;
            case "typeLoose":
                // The ValidationContext inside the created JsonSchema should carry the custom config.
                Object validationContext = getField(schema, "validationContext");
                SchemaValidatorsConfig effectiveCfg = (SchemaValidatorsConfig) getField(validationContext, "config");
                assertTrue(effectiveCfg.isTypeLoose(),
                        "Custom configuration flag ‘typeLoose’ must be true inside the context");
                break;
            default:
                // no extra assertions required
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullConfigProvider")
    void newJsonSchema_NullConfigOrUri_ShouldFallBackToDefaults(URI uri,
                                                                String json,
                                                                SchemaValidatorsConfig config,
                                                                String description) throws Exception {
        JsonNode node = jsonMapper.readTree(json);
        JsonSchema schema = invokeNewJsonSchema(uri, node, config);
        assertNotNull(schema, "Schema must be created even when config/uri is null: " + description);

        // Default config must be non‑null inside the ValidationContext.
        Object validationContext = getField(schema, "validationContext");
        SchemaValidatorsConfig effectiveCfg = (SchemaValidatorsConfig) getField(validationContext, "config");
        assertNotNull(effectiveCfg, "A default SchemaValidatorsConfig should be created when null is supplied");

        // When URI is null the factory should keep it null (or a fallback).  We only assert that no NPE occurs.
        URI storedUri = (URI) getField(schema, "currentUri");
        if (uri == null) {
            assertNull(storedUri, "Current URI should be null when null was supplied");
        } else {
            assertEquals(uri, storedUri);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Tests that expect exceptions                                           *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("exceptionProvider")
    void newJsonSchema_InvalidInputs_ShouldThrowException(URI uri,
                                                          JsonNode node,
                                                          SchemaValidatorsConfig config,
                                                          Class<? extends Throwable> expected,
                                                          String description) {
        Throwable thrown = assertThrows(expected, () -> invokeNewJsonSchema(uri, node, config),
                "Expected " + expected.getSimpleName() + " for case: " + description);
        // optionally log the message for debugging
        assertNotNull(thrown.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Utility method to reach private fields inside JsonSchema/Context       *
     * --------------------------------------------------------------------- */

    /**
     * Retrieves a field value via reflection, making the field accessible.
     *
     * @param target the object (or class) containing the field
     * @param name   the field name
     * @return the field value
     */
    private static Object getField(Object target, String name) throws Exception {
        Class<?> clazz = (target instanceof Class) ? (Class<?>) target : target.getClass();
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
```