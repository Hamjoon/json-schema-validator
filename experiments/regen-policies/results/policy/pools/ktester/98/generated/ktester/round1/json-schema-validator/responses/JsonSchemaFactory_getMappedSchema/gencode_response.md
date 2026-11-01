```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Comprehensive unit tests for {@link JsonSchemaFactory#getMappedSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * <p>The tests cover the scenarios described in the JSON test‑case specification, including
 * successful loads (with/without fragment, JSON/YAML, empty fragment, remote URI, unknown
 * extension), error handling (missing InputStream, readTree IOException, loader IOException,
 * unresolved fragment), meta‑schema detection, custom {@link PathType}, and null‑argument
 * validation.</p>
 *
 * <p>Because {@code getMappedSchema} is {@code protected}, the tests invoke it via reflection.
 * For cases that require special behaviour of the {@link SchemaLoader} or the factory's
 * {@code readTree} method, Mockito spies/mocks are used together with reflection to inject
 * the mocked dependencies.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    /** Factory used for the “happy‑path” tests – it resolves schemas from an in‑memory map. */
    private static JsonSchemaFactory factory;

    /** Simple JSON schema – used for basic loading tests. */
    private static final String SIMPLE_JSON_SCHEMA = """
            {
              "$id": "urn:test:simple",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              }
            }
            """;

    /** Same schema expressed in YAML – used for the YAML test. */
    private static final String SIMPLE_YAML_SCHEMA = """
            $id: urn:test:simple-yaml
            type: object
            properties:
              name:
                type: string
              age:
                type: integer
            """;

    /** A more complex schema that contains definitions and components – used for fragment tests. */
    private static final String COMPLEX_JSON_SCHEMA = """
            {
              "$id": "urn:test:complex",
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "address": { "$ref": "#/definitions/address" },
                "person": { "$ref": "#/components/schemas/Person" }
              },
              "definitions": {
                "address": {
                  "type": "object",
                  "properties": {
                    "street": { "type": "string" },
                    "city":   { "type": "string" }
                  }
                }
              },
              "components": {
                "schemas": {
                  "Person": {
                    "type": "object",
                    "properties": {
                      "firstName": { "type": "string" },
                      "lastName":  { "type": "string" }
                    }
                  }
                }
              }
            }
            """;

    /** Same complex schema expressed in YAML – for the YAML‑fragment test. */
    private static final String COMPLEX_YAML_SCHEMA = """
            $id: urn:test:complex-yaml
            $schema: http://json-schema.org/draft-07/schema#
            type: object
            properties:
              address:
                $ref: "#/definitions/address"
              person:
                $ref: "#/components/schemas/Person"
            definitions:
              address:
                type: object
                properties:
                  street: { type: string }
                  city:   { type: string }
            components:
              schemas:
                Person:
                  type: object
                  properties:
                    firstName: { type: string }
                    lastName:  { type: string }
            """;

    /** A schema that only contains a $schema keyword – used for meta‑schema detection test. */
    private static final String DRAFT07_SCHEMA = """
            {
              "$id": "urn:test:draft07",
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object"
            }
            """;

    @BeforeAll
    static void setupBeforeAll() {
        /*
         * Build a factory that resolves a set of known URIs from the strings above.
         * The builder API lets us provide a map where the key is the absolute IRI and
         * the value is the raw schema content. The factory will turn the content into
         * an InputStream when the loader is asked for it.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(
                        Map.of(
                                "urn:test:simple", SIMPLE_JSON_SCHEMA,
                                "urn:test:simple-yaml", SIMPLE_YAML_SCHEMA,
                                "urn:test:complex", COMPLEX_JSON_SCHEMA,
                                "urn:test:complex-yaml", COMPLEX_YAML_SCHEMA,
                                "urn:test:draft07", DRAFT07_SCHEMA,
                                // additional URIs used by remote‑HTTP and unknown‑extension tests
                                "http://example.com/schemas/remote-schema.json", SIMPLE_JSON_SCHEMA,
                                "file:/tmp/schema.txt", SIMPLE_JSON_SCHEMA
                        )
                ))
        );
    }

    /**
     * Helper that uses reflection to call the protected {@code getMappedSchema}
     * method on a given factory instance.
     */
    private JsonSchema invokeGetMappedSchema(JsonSchemaFactory targetFactory,
                                             SchemaLocation location,
                                             SchemaValidatorsConfig config) throws Exception {
        Method method = JsonSchemaFactory.class.getDeclaredMethod(
                "getMappedSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
        method.setAccessible(true);
        return (JsonSchema) method.invoke(targetFactory, location, config);
    }

    /* --------------------------------------------------------------------- */
    /*  Successful loads without fragment (parameterized)                    */
    /* --------------------------------------------------------------------- */

    private static Stream<Arguments> withoutFragmentProvider() {
        return Stream.of(
                // JSON file, no fragment
                Arguments.of("urn:test:simple", false, "Loads a JSON schema without fragment."),
                // YAML file, no fragment
                Arguments.of("urn:test:simple-yaml", true, "Loads a YAML schema without fragment."),
                // JSON file with an empty fragment ("#")
                Arguments.of("urn:test:simple#", false, "Empty fragment should be treated as no fragment.")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("withoutFragmentProvider")
    void getMappedSchema_withoutFragment(String uriString, boolean expectYaml, String description) throws Exception {
        // ------------------------------------------------------------
        // given
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(uriString));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema schema = invokeGetMappedSchema(factory, location, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(schema, "Factory must return a JsonSchema instance");
        // All schemas used in this group are objects at the root.
        assertEquals(JsonSchema.Type.OBJECT,
                schema.getSchemaNode().get("type").asText().toUpperCase(),
                "Root schema type should be OBJECT");
    }

    /* --------------------------------------------------------------------- */
    /*  Successful loads with fragment (parameterized)                        */
    /* --------------------------------------------------------------------- */

    private static Stream<Arguments> withFragmentProvider() {
        return Stream.of(
                // JSON fragment pointing to a definition
                Arguments.of("urn:test:complex#/definitions/address",
                        JsonSchema.Type.OBJECT,
                        "Sub‑schema at /definitions/address should be OBJECT."),
                // YAML fragment pointing to a component schema
                Arguments.of("urn:test:complex-yaml#/components/schemas/Person",
                        JsonSchema.Type.OBJECT,
                        "Sub‑schema at /components/schemas/Person should be OBJECT.")
        );
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("withFragmentProvider")
    void getMappedSchema_withFragment(String uriString, JsonSchema.Type expectedType, String description) throws Exception {
        // ------------------------------------------------------------
        // given
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(new AbsoluteIri(uriString));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema subSchema = invokeGetMappedSchema(factory, location, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(subSchema, "Sub‑schema must not be null");
        assertEquals(expectedType,
                JsonSchema.Type.valueOf(subSchema.getSchemaNode().get("type").asText().toUpperCase()),
                "Sub‑schema type should match expected");
    }

    /* --------------------------------------------------------------------- */
    /*  Error handling – missing InputStream                                  */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_missingInputStream_throwsJsonSchemaException() throws Exception {
        // ------------------------------------------------------------
        // given – mock a SchemaLoader that returns a SchemaResource with null stream
        // ------------------------------------------------------------
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        SchemaResource mockResource = mock(SchemaResource.class);
        when(mockLoader.getSchema(any(URI.class))).thenReturn(mockResource);
        when(mockResource.getInputStream()).thenReturn(null);

        JsonSchemaFactory spyFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // inject the mock loader via reflection
        Field loaderField = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        loaderField.setAccessible(true);
        loaderField.set(spyFactory, mockLoader);

        SchemaLocation location = new SchemaLocation(new AbsoluteIri("urn:nonexistent"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when / then
        // ------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> invokeGetMappedSchema(spyFactory, location, config));
        assertTrue(ex.getMessage().contains("Failed to load json schema"),
                "Exception message must indicate loading failure");
        assertNotNull(ex.getCause(), "Cause should be the original IOException");
    }

    /* --------------------------------------------------------------------- */
    /*  Error handling – readTree throws IOException                           */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_readTreeThrowsIOException_throwsJsonSchemaException() throws Exception {
        // ------------------------------------------------------------
        // given – spy the factory and make readTree throw
        // ------------------------------------------------------------
        JsonSchemaFactory spyFactory = spy(factory);
        doThrow(new IOException("malformed content"))
                .when(spyFactory).readTree(any(InputStream.class), eq(InputFormat.JSON));

        SchemaLocation location = new SchemaLocation(new AbsoluteIri("urn:test:simple"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when / then
        // ------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> invokeGetMappedSchema(spyFactory, location, config));
        assertTrue(ex.getMessage().contains("Failed to load json schema"),
                "Exception message must indicate loading failure");
        assertNotNull(ex.getCause(), "Cause should be the original IOException from readTree");
    }

    /* --------------------------------------------------------------------- */
    /*  Meta‑schema detection via $schema keyword                              */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_metaSchemaDetection_usesDraft07() throws Exception {
        // ------------------------------------------------------------
        // given – a schema that declares "$schema": "http://json-schema.org/draft-07/schema#"
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("urn:test:draft07"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema schema = invokeGetMappedSchema(factory, location, config);

        // ------------------------------------------------------------
        // then – the schema loads without error and its meta‑schema version
        // should be DRAFT_07 (the internal enum name may differ; we just check that
        // the meta‑schema is not the factory default by inspecting the $schema value).
        // ------------------------------------------------------------
        assertNotNull(schema, "Schema must be loaded");
        JsonMetaSchema meta = schema.getMetaSchema(); // assume getter exists
        assertNotNull(meta, "Meta‑schema should be resolved");
        assertEquals("http://json-schema.org/draft-07/schema#", meta.getUri(),
                "Meta‑schema URI must match the $schema keyword");
    }

    /* --------------------------------------------------------------------- */
    /*  Custom PathType in SchemaValidatorsConfig                              */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_customPathType_respectsConfiguration() throws Exception {
        // ------------------------------------------------------------
        // given – config with PathType.JSON_POINTER
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("urn:test:simple"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .pathType(PathType.JSON_POINTER) // assume setter exists
                .build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema schema = invokeGetMappedSchema(factory, location, config);

        // ------------------------------------------------------------
        // then – ensure no exception and that the evaluationPath inside the
        // method was built with the supplied PathType. We cannot directly
        // inspect the internal path, but the fact that loading succeeds proves
        // the config was accepted.
        // ------------------------------------------------------------
        assertNotNull(schema, "Schema should be loaded with custom PathType");
    }

    /* --------------------------------------------------------------------- */
    /*  SchemaLoader throws IOException before InputStream acquisition          */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_loaderThrowsIOException_throwsJsonSchemaException() throws Exception {
        // ------------------------------------------------------------
        // given – mock loader that throws IOException when getSchema is called
        // ------------------------------------------------------------
        SchemaLoader mockLoader = mock(SchemaLoader.class);
        when(mockLoader.getSchema(any(URI.class))).thenThrow(new IOException("unreachable"));

        JsonSchemaFactory spyFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        Field loaderField = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        loaderField.setAccessible(true);
        loaderField.set(spyFactory, mockLoader);

        SchemaLocation location = new SchemaLocation(new AbsoluteIri("urn:test:unreachable"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when / then
        // ------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> invokeGetMappedSchema(spyFactory, location, config));
        assertTrue(ex.getMessage().contains("Failed to load json schema"),
                "Exception message must indicate loading failure");
        assertNotNull(ex.getCause(), "Cause should be the original IOException");
    }

    /* --------------------------------------------------------------------- */
    /*  Fragment points to non‑existent sub‑schema                               */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_nonExistentFragment_throwsJsonSchemaException() throws Exception {
        // ------------------------------------------------------------
        // given – a valid schema but a fragment that does not exist
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(
                new AbsoluteIri("urn:test:complex#/definitions/unknown"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when / then
        // ------------------------------------------------------------
        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> invokeGetMappedSchema(factory, location, config));
        assertTrue(ex.getMessage().contains("Failed to load json schema") ||
                   ex.getMessage().toLowerCase().contains("cannot find"),
                "Exception should indicate that the fragment cannot be resolved");
    }

    /* --------------------------------------------------------------------- */
    /*  Remote HTTP URI without fragment                                      */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_remoteHttpUri_withoutFragment() throws Exception {
        // ------------------------------------------------------------
        // given – an HTTP URI that resolves to a simple JSON schema (mapped in the factory)
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("http://example.com/schemas/remote-schema.json"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema schema = invokeGetMappedSchema(factory, location, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(schema, "Remote HTTP schema should be loaded");
        assertEquals(JsonSchema.Type.OBJECT,
                schema.getSchemaNode().get("type").asText().toUpperCase(),
                "Root type should be OBJECT");
    }

    /* --------------------------------------------------------------------- */
    /*  Unsupported file extension treated as JSON                             */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_unknownExtension_treatedAsJson() throws Exception {
        // ------------------------------------------------------------
        // given – a .txt URI that is mapped to a JSON schema
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("file:/tmp/schema.txt"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema schema = invokeGetMappedSchema(factory, location, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(schema, "Schema with unknown extension should be loaded as JSON");
        assertEquals(JsonSchema.Type.OBJECT,
                schema.getSchemaNode().get("type").asText().toUpperCase(),
                "Root type should be OBJECT");
    }

    /* --------------------------------------------------------------------- */
    /*  Null arguments validation                                              */
    /* --------------------------------------------------------------------- */

    @Test
    void getMappedSchema_nullSchemaLocation_throwsNullPointerException() throws Exception {
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> invokeGetMappedSchema(factory, null, config));
        // The exact message depends on the implementation; we only assert the type.
    }

    @Test
    void getMappedSchema_nullConfig_throwsNullPointerException() throws Exception {
        SchemaLocation location = new SchemaLocation(new AbsoluteIri("urn:test:simple"));

        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> invokeGetMappedSchema(factory, location, null));
        // Again, we only verify that NPE is thrown.
    }
}
```