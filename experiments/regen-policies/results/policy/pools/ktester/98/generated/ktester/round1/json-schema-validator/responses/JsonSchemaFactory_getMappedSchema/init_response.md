**Generated test class**

```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getMappedSchema(SchemaLocation, SchemaValidatorsConfig)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Loading a JSON schema without a fragment.</li>
 *   <li>Loading a JSON schema with a fragment that points to a subschema.</li>
 *   <li>Loading a YAML schema.</li>
 * </ul>
 *
 * The factory is created via {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag, java.util.function.Consumer)}
 * with a custom {@code SchemaLoaders} that supplies the schema content from an in‑memory map.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    /** Factory under test – created once for the whole class. */
    private static JsonSchemaFactory factory;

    /** A simple JSON schema used by several test cases. */
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

    /** Same schema expressed in YAML (used for the YAML test). */
    private static final String SIMPLE_YAML_SCHEMA = """
            $id: urn:test:simple-yaml
            type: object
            properties:
              name:
                type: string
              age:
                type: integer
            """;

    /** URI that will be used as the key in the custom schema‑loader map. */
    private static final String JSON_SCHEMA_URI = "urn:test:simple";
    private static final String YAML_SCHEMA_URI = "urn:test:simple-yaml";

    @BeforeAll
    static void setupBeforeAll() {
        /*
         * Build a JsonSchemaFactory that knows how to resolve the two URIs above.
         * The builder API allows us to inject a map where the key is the absolute IRI
         * and the value is the raw schema content (as a String). The factory will
         * turn that into an InputStream when the loader is asked for it.
         */
        factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(
                        Map.of(
                                JSON_SCHEMA_URI, SIMPLE_JSON_SCHEMA,
                                YAML_SCHEMA_URI, SIMPLE_YAML_SCHEMA
                        )
                ))
        );
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
    }

    /**
     * Helper that uses reflection to call the protected {@code getMappedSchema}
     * method.
     */
    private JsonSchema invokeGetMappedSchema(SchemaLocation location,
                                             SchemaValidatorsConfig config) throws Exception {
        Method method = JsonSchemaFactory.class.getDeclaredMethod(
                "getMappedSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
        method.setAccessible(true);
        return (JsonSchema) method.invoke(factory, location, config);
    }

    @Test
    void getMappedSchema_withoutFragment_returnsRootSchema() throws Exception {
        // ------------------------------------------------------------
        // given
        // ------------------------------------------------------------
        SchemaLocation location = new SchemaLocation(
                new AbsoluteIri(JSON_SCHEMA_URI));               // no fragment
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema schema = invokeGetMappedSchema(location, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(schema, "Factory should return a JsonSchema instance");
        // The root schema must be of type OBJECT as defined in SIMPLE_JSON_SCHEMA.
        assertEquals(JsonSchema.Type.OBJECT, schema.getSchemaNode().get("type").asText().toUpperCase(),
                "Root schema type should be OBJECT");
    }

    @Test
    void getMappedSchema_withFragment_returnsSubSchema() throws Exception {
        // ------------------------------------------------------------
        // given
        // ------------------------------------------------------------
        // Fragment that points to the 'name' property definition.
        SchemaLocation location = new SchemaLocation(
                new AbsoluteIri(JSON_SCHEMA_URI),
                JsonNodePath.of("/properties/name"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema subSchema = invokeGetMappedSchema(location, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(subSchema, "Sub‑schema must not be null");
        // The subschema should be a STRING type (the definition of 'name').
        assertEquals(JsonSchema.Type.STRING,
                subSchema.getSchemaNode().get("type").asText().toUpperCase(),
                "Sub‑schema type should be STRING");
    }

    @Test
    void getMappedSchema_yamlInput_isHandledCorrectly() throws Exception {
        // ------------------------------------------------------------
        // given
        // ------------------------------------------------------------
        SchemaLocation yamlLocation = new SchemaLocation(
                new AbsoluteIri(YAML_SCHEMA_URI));   // no fragment, YAML content
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when
        // ------------------------------------------------------------
        JsonSchema yamlSchema = invokeGetMappedSchema(yamlLocation, config);

        // ------------------------------------------------------------
        // then
        // ------------------------------------------------------------
        assertNotNull(yamlSchema, "YAML schema should be loaded");
        // Verify that the type is OBJECT as declared in the YAML schema.
        assertEquals(JsonSchema.Type.OBJECT,
                yamlSchema.getSchemaNode().get("type").asText().toUpperCase(),
                "YAML root schema type should be OBJECT");
    }

    @Test
    void getMappedSchema_missingSchema_throwsJsonSchemaException() {
        // ------------------------------------------------------------
        // given
        // ------------------------------------------------------------
        SchemaLocation missingLocation = new SchemaLocation(
                new AbsoluteIri("urn:nonexistent"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        // ------------------------------------------------------------
        // when / then
        // ------------------------------------------------------------
        Exception ex = assertThrows(JsonSchemaException.class, () -> {
            invokeGetMappedSchema(missingLocation, config);
        });
        assertTrue(ex.getMessage().contains("Failed to load json schema"),
                "Exception message should indicate loading failure");
    }
}
```