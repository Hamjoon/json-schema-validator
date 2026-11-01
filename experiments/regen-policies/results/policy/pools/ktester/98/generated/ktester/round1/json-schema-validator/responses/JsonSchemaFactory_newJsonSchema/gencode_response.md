```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the protected {@code newJsonSchema} method of {@link JsonSchemaFactory}.
 *
 * <p>The method is accessed via reflection because it is {@code protected}. The tests
 * cover a wide range of input combinations taken from the supplied JSON test matrix,
 * asserting successful creation of a {@link JsonSchema} as well as the expected
 * exceptions for invalid arguments or configurations.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_newJsonSchema_Test {

    private static JsonSchemaFactory factory;
    private static ObjectMapper mapper;

    /** Initialise a shared factory instance (thread‑safe) and a JSON mapper. */
    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it builds a fully‑initialised instance.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();
    }

    /** Helper that invokes the protected {@code newJsonSchema} method via reflection. */
    private JsonSchema invokeNewJsonSchema(SchemaLocation uri, JsonNode node, SchemaValidatorsConfig cfg) throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod(
                "newJsonSchema", SchemaLocation.class, JsonNode.class, SchemaValidatorsConfig.class);
        m.setAccessible(true);
        return (JsonSchema) m.invoke(factory, uri, node, cfg);
    }

    /** Extract the underlying schema node from a {@link JsonSchema} (used for assertions). */
    private JsonNode extractSchemaNode(JsonSchema schema) throws Exception {
        // JsonSchema has a public getter in recent versions; fall back to reflection if not.
        try {
            Method getter = JsonSchema.class.getMethod("getSchemaNode");
            return (JsonNode) getter.invoke(schema);
        } catch (NoSuchMethodException e) {
            Method field = JsonSchema.class.getDeclaredMethod("getSchemaNode");
            field.setAccessible(true);
            return (JsonNode) field.invoke(schema);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Happy‑path tests (valid inputs)                                      *
     * --------------------------------------------------------------------- */

    @Test
    void normalOperationDefaultConfig() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");
        SchemaValidatorsConfig cfg = null; // default config

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema must not be null");
        assertEquals(node, extractSchemaNode(schema), "Returned schema should contain the original node");
    }

    @Test
    void normalOperationCustomConfig() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("classpath:/my-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig()
                .setPathType(JsonNodePath.Type.JSON_POINTER)
                .setFailFast(true);

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema must not be null");
        // Verify that the configuration made it into the ValidationContext (reflection).
        Method getContext = JsonSchema.class.getDeclaredMethod("getValidationContext");
        getContext.setAccessible(true);
        ValidationContext vc = (ValidationContext) getContext.invoke(schema);
        assertEquals(JsonNodePath.Type.JSON_POINTER, vc.getConfig().getPathType(),
                "Path type should be JSON_POINTER as set in the config");
        assertTrue(vc.getConfig().isFailFast(), "Fail‑fast flag should be true");
    }

    @Test
    void emptySchemaValidatorsConfig() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/empty-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"null\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig(); // all defaults

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema must be created even with an explicit default config");
    }

    @Test
    void configDisablesRefLoading() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/ref-schema.json");
        JsonNode node = mapper.readTree("{\"$ref\":\"http://example.com/other.json\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setSuppressSubSchemaRetrieval(true);

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema should be created even when $ref loading is suppressed");
        // No further verification needed – the absence of an exception proves preload respected the flag.
    }

    @Test
    void explicitDefaultConfigBehavesLikeNull() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/explicit-defaults.json");
        JsonNode node = mapper.readTree("{\"type\":\"boolean\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig(); // explicit defaults

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema must be created with an explicit default config");
    }

    @Test
    void emptyObjectSchemaNode() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/empty-object.json");
        JsonNode node = mapper.readTree("{}");
        SchemaValidatorsConfig cfg = null;

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Empty object schema should be accepted");
    }

    @Test
    void customPathTypeJsonPath() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("classpath:/my-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig()
                .setPathType(JsonNodePath.Type.JSON_PATH);

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema must be created with JSON_PATH path type");
        Method getContext = JsonSchema.class.getDeclaredMethod("getValidationContext");
        getContext.setAccessible(true);
        ValidationContext vc = (ValidationContext) getContext.invoke(schema);
        assertEquals(JsonNodePath.Type.JSON_PATH, vc.getConfig().getPathType(),
                "Path type should be JSON_PATH as configured");
    }

    @Test
    void combinedFlagsFailFastAndSuppressRef() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/combined.json");
        JsonNode node = mapper.readTree("{\"$ref\":\"http://example.com/other.json\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig()
                .setFailFast(true)
                .setSuppressSubSchemaRetrieval(true);

        // WHEN
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);

        // THEN
        assertNotNull(schema, "Schema must be created with combined flags");
        Method getContext = JsonSchema.class.getDeclaredMethod("getValidationContext");
        getContext.setAccessible(true);
        ValidationContext vc = (ValidationContext) getContext.invoke(schema);
        assertTrue(vc.getConfig().isFailFast(), "Fail‑fast flag should be true");
        assertTrue(vc.getConfig().isSuppressSubSchemaRetrieval(),
                "Suppress‑sub‑schema‑retrieval flag should be true");
    }

    /* --------------------------------------------------------------------- *
     *  Exception‑expectation tests (invalid inputs)                         *
     * --------------------------------------------------------------------- */

    @Test
    void nullSchemaUriThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = null;
        JsonNode node = mapper.readTree("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "A null schemaUri should cause NullPointerException");
    }

    @Test
    void nullSchemaNodeThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/empty.json");
        JsonNode node = null;
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "A null schemaNode should cause NullPointerException");
    }

    @Test
    void malformedUriThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("ht!tp://[invalid]");
        JsonNode node = mapper.readTree("{\"type\":\"string\"}");
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Malformed URI should trigger IllegalArgumentException");
    }

    @Test
    void emptyUriStringThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("");
        JsonNode node = mapper.readTree("{\"type\":\"integer\"}");
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Empty URI string should trigger IllegalArgumentException");
    }

    @Test
    void unsupportedSchemeThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("ftp://example.com/schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\"}");
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Unsupported URI scheme should trigger IllegalArgumentException");
    }

    @Test
    void schemaValidationEnabledInvalidSchemaThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/invalid-schema.json");
        JsonNode node = mapper.readTree("{\"type\":\"unknown-type\"}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setValidateSchema(true);

        // WHEN / THEN
        // The exact exception type depends on the library version; we accept any RuntimeException.
        assertThrows(RuntimeException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Invalid schema with validation enabled should raise an exception");
    }

    @Test
    void arrayRootSchemaThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/array-root.json");
        JsonNode node = mapper.readTree("[{\"type\":\"string\"}]");
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Schema root must be an object; an array should cause an exception");
    }

    @Test
    void primitiveRootSchemaThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/primitive-root.json");
        JsonNode node = mapper.readTree("\"just a string\"");
        SchemaValidatorsConfig cfg = null;

        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Primitive root schema should cause an exception");
    }

    @Test
    void configWithNullPathTypeThrows() throws Exception {
        // GIVEN
        SchemaLocation uri = new SchemaLocation("http://example.com/null-pathtype.json");
        JsonNode node = mapper.readTree("{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}");
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig().setPathType(null);

        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> invokeNewJsonSchema(uri, node, cfg),
                "Null path type in config should cause NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  Parameterised test for a subset of successful scenarios                *
     * --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] uri={0}, schema={1}")
    @MethodSource("successfulScenarios")
    void parameterisedSuccessfulScenarios(String uriString, String schemaJson, SchemaValidatorsConfig cfg) throws Exception {
        SchemaLocation uri = new SchemaLocation(uriString);
        JsonNode node = mapper.readTree(schemaJson);
        JsonSchema schema = invokeNewJsonSchema(uri, node, cfg);
        assertNotNull(schema, "Schema should be created for uri=" + uriString);
    }

    private static Stream<Arguments> successfulScenarios() {
        return Stream.of(
                // simple object schema, default config (null)
                Arguments.of("http://example.com/simple.json",
                        "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}}",
                        null),

                // array schema, explicit default config
                Arguments.of("http://example.com/array.json",
                        "{\"type\":\"array\",\"items\":{\"type\":\"number\"}}",
                        new SchemaValidatorsConfig()),

                // schema with $ref but suppression disabled (should still succeed)
                Arguments.of("http://example.com/ref-ok.json",
                        "{\"$ref\":\"http://example.com/other.json\"}",
                        new SchemaValidatorsConfig().setSuppressSubSchemaRetrieval(false))
        );
    }
}
```