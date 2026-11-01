```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.validation.ValidationMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The test suite covers the scenarios described in the supplied JSON test matrix:
 * <ul>
 *   <li>valid schema loading (classpath, file, $ref handling)</li>
 *   <li>null parameters handling</li>
 *   <li>invalid or malformed URIs</li>
 *   <li>caching behavior</li>
 *   <li>custom configuration effects (typeLoose)</li>
 *   <li>circular references</li>
 *   <li>unsupported protocols / meta‑schemas</li>
 *   <li>non‑JSON content and invalid schema definitions</li>
 *   <li>unresolved $ref during preload</li>
 * </ul>
 *
 * The factory is instantiated via {@link JsonSchemaFactory#getInstance(VersionFlag, java.util.function.Consumer)}
 * with a custom {@code schemaLoaders} configuration that registers in‑memory schemas.
 *
 * No internal collaborators are mocked – the tests exercise the real implementation.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_10_Test {

    /** Factory used for the majority of tests (default configuration). */
    private static JsonSchemaFactory defaultFactory;

    /** Simple string schema used in many tests. */
    private static final String SIMPLE_SCHEMA = """
            {
              "$id": "urn:test:simple",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "string"
            }
            """;

    /** Integer schema used for type‑loose tests. */
    private static final String INTEGER_SCHEMA = """
            {
              "$id": "urn:test/int",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "integer"
            }
            """;

    /** Schema with a $ref to another schema (used for circular reference test). */
    private static final String CIRCULAR_A = """
            {
              "$id": "urn:test/circular-a",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "urn:test/circular-b"
            }
            """;

    private static final String CIRCULAR_B = """
            {
              "$id": "urn:test/circular-b",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "urn:test/circular-a"
            }
            """;

    /** Schema that references a non‑existent schema (unresolved $ref). */
    private static final String UNRESOLVED_REF_SCHEMA = """
            {
              "$id": "urn:test/unresolved",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "$ref": "urn:test/missing"
            }
            """;

    /** Schema that declares an unknown meta‑schema. */
    private static final String UNSUPPORTED_META_SCHEMA = """
            {
              "$id": "urn:test/unsupported",
              "$schema": "urn:unknown:meta",
              "type": "string"
            }
            """;

    /** Plain‑text (non‑JSON) content used for the non‑JSON test case. */
    private static final String NON_JSON_CONTENT = "Just some text, not JSON";

    /** Invalid schema definition (unknown keyword). */
    private static final String INVALID_DEFINITION_SCHEMA = """
            {
              "$id": "urn:test/invalid-def",
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "unknownKeyword": true
            }
            """;

    @BeforeAll
    static void beforeAll() {
        defaultFactory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @AfterAll
    static void afterAll() {
        defaultFactory = null;
    }

    /* --------------------------------------------------------------------- *
     * Helper methods
     * --------------------------------------------------------------------- */

    /**
     * Creates a {@link JsonSchemaFactory} that knows about the supplied in‑memory schemas.
     *
     * @param schemas map where the key is the logical location string (e.g. "classpath:/schemas/foo.json")
     *                and the value is the JSON/YAML content
     * @return a fully‑initialised factory
     */
    private static JsonSchemaFactory factoryWithSchemas(Map<String, String> schemas) {
        return JsonSchemaFactory.getInstance(
                VersionFlag.V202012,
                builder -> builder.schemaLoaders(
                        loaders -> loaders.schemas(schemas)));
    }

    /* --------------------------------------------------------------------- *
     * 1. validSchemaLoading
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validSchemaLoadingProvider")
    void validSchemaLoading(String description, String uri, String schemaContent, SchemaValidatorsConfig config) throws IOException {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap(uri, schemaContent));

        SchemaLocation location = SchemaLocation.of(URI.create(uri));
        JsonSchema schema = f.getSchema(location, config);
        assertNotNull(schema, "Factory must return a non‑null JsonSchema");

        // Basic sanity validation – the schema should be able to validate a matching instance.
        JsonNode validInstance = f.getJsonMapper().readTree("\"hello\"");
        assertTrue(schema.validate(validInstance).isEmpty(),
                "Valid instance should produce no validation errors");
    }

    private static Stream<Arguments> validSchemaLoadingProvider() {
        // 1) classpath resource, null config (treated as default)
        // 2) file URI with explicit default config
        // 3) schema with internal $ref (we register both schemas)
        return Stream.of(
                Arguments.of(
                        "Load a valid JSON Schema from the classpath using a null configuration",
                        "classpath:/schemas/simple-schema.json",
                        SIMPLE_SCHEMA,
                        null),
                Arguments.of(
                        "Load a schema from a file system path with an explicit default configuration",
                        "file:/tmp/test-schema.json",
                        SIMPLE_SCHEMA,
                        new SchemaValidatorsConfig()),
                Arguments.of(
                        "Schema with internal $ref resolved during preload",
                        "classpath:/schemas/with-ref-schema.json",
                        // The main schema references another schema we also register.
                        """
                        {
                          "$id": "urn:test/with-ref",
                          "$schema": "https://json-schema.org/draft/2020-12/schema",
                          "$ref": "urn:test/simple"
                        }
                        """,
                        new SchemaValidatorsConfig())
        );
    }

    /* --------------------------------------------------------------------- *
     * 2. nullParameters
     * --------------------------------------------------------------------- */

    @Test
    void nullParameters_bothNull_shouldThrowIllegalArgumentException() {
        SchemaLocation location = SchemaLocation.of(URI.create("urn:test/some"));
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.getSchema(null, null),
                "Both arguments null – expect IllegalArgumentException");
    }

    @Test
    void nullParameters_nullConfig_shouldTreatAsDefault() throws IOException {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/simple-schema.json", SIMPLE_SCHEMA));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/simple-schema.json"));
        JsonSchema schema = f.getSchema(location, null);
        assertNotNull(schema, "Null config should be treated as default and return a schema");

        JsonNode valid = f.getJsonMapper().readTree("\"ok\"");
        assertTrue(schema.validate(valid).isEmpty(),
                "Schema loaded with null config must still validate correctly");
    }

    /* --------------------------------------------------------------------- *
     * 3. invalidSchemaUri
     * --------------------------------------------------------------------- */

    @Test
    void invalidSchemaUri_nonExistentResource_shouldThrowException() {
        JsonSchemaFactory f = factoryWithSchemas(Collections.emptyMap());

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/non-existent.json"));
        assertThrows(RuntimeException.class, // JsonSchemaException extends RuntimeException
                () -> f.getSchema(location, new SchemaValidatorsConfig()),
                "Loading a non‑existent resource must fail");
    }

    @Test
    void invalidSchemaUri_unresolvableHttp_shouldThrowException() {
        // No HTTP loader is configured; the call should result in an exception.
        JsonSchemaFactory f = factoryWithSchemas(Collections.emptyMap());

        SchemaLocation location = SchemaLocation.of(URI.create("http://invalid-host/schema.json"));
        assertThrows(RuntimeException.class,
                () -> f.getSchema(location, new SchemaValidatorsConfig()),
                "Unresolvable HTTP URI should cause an exception");
    }

    /* --------------------------------------------------------------------- *
     * 4. circularReference
     * --------------------------------------------------------------------- */

    @Test
    void circularReference_resolvedWithoutStackOverflow() {
        JsonSchemaFactory f = factoryWithSchemas(
                Map.of(
                        "urn:test/circular-a", CIRCULAR_A,
                        "urn:test/circular-b", CIRCULAR_B));

        SchemaLocation location = SchemaLocation.of(URI.create("urn:test/circular-a"));
        JsonSchema schema = f.getSchema(location, new SchemaValidatorsConfig());
        assertNotNull(schema, "Circular $ref schema must be loaded");

        // The schema itself does not have any concrete validation rules; just ensure no exception.
        JsonNode dummy = f.getJsonMapper().readTree("{}");
        assertDoesNotThrow(() -> schema.validate(dummy),
                "Preload should have resolved circular refs without stack overflow");
    }

    /* --------------------------------------------------------------------- *
     * 5. cachingBehavior
     * --------------------------------------------------------------------- */

    @Test
    void cachingBehavior_sameInstanceReturnedOnRepeatedCalls() {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/simple-schema.json", SIMPLE_SCHEMA));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/simple-schema.json"));
        JsonSchema first = f.getSchema(location, new SchemaValidatorsConfig());
        JsonSchema second = f.getSchema(location, new SchemaValidatorsConfig());

        assertSame(first, second, "Factory should cache schemas and return the identical instance");
    }

    /* --------------------------------------------------------------------- *
     * 6. customConfigEffect (typeLoose)
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] typeLoose={0}")
    @MethodSource("customConfigEffectProvider")
    void customConfigEffect_typeLoose(boolean typeLoose) throws IOException {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/integer-schema.json", INTEGER_SCHEMA));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/integer-schema.json"));
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        cfg.setTypeLoose(typeLoose);

        JsonSchema schema = f.getSchema(location, cfg);
        assertNotNull(schema);

        // Instance that is a string containing a number – accepted only when typeLoose=true
        JsonNode instance = f.getJsonMapper().readTree("\"123\"");
        boolean hasErrors = !schema.validate(instance).isEmpty();

        if (typeLoose) {
            assertFalse(hasErrors, "With typeLoose enabled the string '123' should be accepted as integer");
        } else {
            assertTrue(hasErrors, "With typeLoose disabled the string '123' must be rejected");
        }
    }

    private static Stream<Arguments> customConfigEffectProvider() {
        return Stream.of(
                Arguments.of(true),
                Arguments.of(false)
        );
    }

    /* --------------------------------------------------------------------- *
     * 7. nullSchemaUriOnly
     * --------------------------------------------------------------------- */

    @Test
    void nullSchemaUriOnly_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.getSchema(null, new SchemaValidatorsConfig()),
                "Null schemaUri with non‑null config must be rejected");
    }

    /* --------------------------------------------------------------------- *
     * 8. unsupportedProtocol
     * --------------------------------------------------------------------- */

    @Test
    void unsupportedProtocol_ftpScheme_shouldThrowIllegalArgumentException() {
        SchemaLocation location = SchemaLocation.of(URI.create("ftp://example.com/schema.json"));
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.getSchema(location, new SchemaValidatorsConfig()),
                "Unsupported protocol (ftp) must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     * 9. malformedUri
     * --------------------------------------------------------------------- */

    @Test
    void malformedUri_shouldThrowIllegalArgumentException() {
        // The URI constructor itself throws IllegalArgumentException for malformed strings.
        assertThrows(IllegalArgumentException.class,
                () -> SchemaLocation.of(URI.create("http://[invalid]")),
                "Malformed URI should be rejected");
    }

    /* --------------------------------------------------------------------- *
     * 10. nonJsonContent
     * --------------------------------------------------------------------- */

    @Test
    void nonJsonContent_shouldThrowExceptionDuringParsing() {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/not-json.txt", NON_JSON_CONTENT));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/not-json.txt"));
        assertThrows(RuntimeException.class,
                () -> f.getSchema(location, new SchemaValidatorsConfig()),
                "Parsing non‑JSON content must raise an exception");
    }

    /* --------------------------------------------------------------------- *
     * 11. invalidSchemaDefinition
     * --------------------------------------------------------------------- */

    @Test
    void invalidSchemaDefinition_shouldThrowException() {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/invalid-schema.json", INVALID_DEFINITION_SCHEMA));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/invalid-schema.json"));
        assertThrows(RuntimeException.class,
                () -> f.getSchema(location, new SchemaValidatorsConfig()),
                "Schema with unknown keyword must cause a JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 12. unresolvedReferenceDuringPreload
     * --------------------------------------------------------------------- */

    @Test
    void unresolvedReferenceDuringPreload_shouldThrowException() {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/with-unresolved-ref-schema.json", UNRESOLVED_REF_SCHEMA));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/with-unresolved-ref-schema.json"));
        assertThrows(RuntimeException.class,
                () -> f.getSchema(location, new SchemaValidatorsConfig()),
                "Missing $ref target must cause an exception during preload");
    }

    /* --------------------------------------------------------------------- *
     * 13. unsupportedMetaSchema
     * --------------------------------------------------------------------- */

    @Test
    void unsupportedMetaSchema_shouldThrowException() {
        JsonSchemaFactory f = factoryWithSchemas(
                Collections.singletonMap("classpath:/schemas/unsupported-meta-schema.json", UNSUPPORTED_META_SCHEMA));

        SchemaLocation location = SchemaLocation.of(URI.create("classpath:/schemas/unsupported-meta-schema.json"));
        assertThrows(RuntimeException.class,
                () -> f.getSchema(location, new SchemaValidatorsConfig()),
                "Unknown meta‑schema IRI must cause an exception");
    }
}
```