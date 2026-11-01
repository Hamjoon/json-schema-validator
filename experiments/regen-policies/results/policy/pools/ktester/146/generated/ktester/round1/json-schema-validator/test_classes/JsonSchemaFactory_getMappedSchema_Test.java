package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.resource.SchemaLoader;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getMappedSchema_Test {

    /**
     * Creates a {@link JsonSchemaFactory} that resolves the given URI to the supplied
     * schema content (JSON or YAML).  The {@code schemas} map is passed to the
     * {@code SchemaLoaders} builder used by the library.
     */
    private JsonSchemaFactory factoryWithSchema(String uri, String content) {
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(sl -> sl.schemas(Collections.singletonMap(uri, content))));
    }

    /**
     * Injects a mocked {@link SchemaLoader} into the given factory via reflection.
     */
    private void injectSchemaLoader(JsonSchemaFactory factory, SchemaLoader loaderMock) throws Exception {
        Field f = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        f.setAccessible(true);
        f.set(factory, loaderMock);
    }

    /**
     * Verify that a simple JSON schema can be loaded successfully via {@code getMappedSchema}.
     */
    @Test
    void getMappedSchema_successfulLoad() {
        // a minimal JSON schema
        String schemaJson = "{\"type\":\"object\"}";
        // Build a factory that knows how to resolve the URI "urn:test" to the above JSON.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> builder.schemaLoaders(sl -> sl.schemas(Collections.singletonMap("urn:test", schemaJson))));
        // The public API delegates to the protected getMappedSchema method.
        JsonSchema schema = factory.getSchema("urn:test");
        assertNotNull(schema, "The returned JsonSchema should not be null");
        JsonNode node = schema.getSchemaNode();
        assertNotNull(node, "The underlying JsonNode should not be null");
        assertEquals("object", node.get("type").asText(), "The loaded schema should contain the expected \"type\" property");
    }

    /**
     * Force {@code getMappedSchema} to encounter a {@code null} InputStream.
     * The method should wrap the resulting {@code IOException} into a {@link JsonSchemaException}.
     */
    @Test
    void getMappedSchema_throwsWhenInputStreamNull() throws Exception {
        // Create a real factory instance (the concrete implementation is irrelevant for this test).
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        // Mock the SchemaLoader that JsonSchemaFactory uses internally.
        SchemaLoader loaderMock = org.mockito.Mockito.mock(SchemaLoader.class);
        // The concrete class returned by SchemaLoader#getSchema is not part of the public API.
        // We therefore use reflection to obtain it and mock the required method.
        Class<?> schemaClass = Class.forName("com.networknt.schema.resource.Schema");
        Object schemaMock = org.mockito.Mockito.mock(schemaClass);
        // Simulate a null InputStream – this triggers the IOException path inside getMappedSchema.
        org.mockito.Mockito.when(schemaMock.getClass().getMethod("getInputStream").invoke(schemaMock)).thenReturn(null);
        // When the factory asks the loader for any schema, return our mock.
        org.mockito.Mockito.when(loaderMock.getSchema(org.mockito.ArgumentMatchers.any())).thenReturn(schemaMock);
        // Inject the mocked loader into the factory via reflection (the field is private final).
        Field loaderField = JsonSchemaFactory.class.getDeclaredField("schemaLoader");
        loaderField.setAccessible(true);
        loaderField.set(factory, loaderMock);
        // The public getSchema method will invoke the protected getMappedSchema.
        assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:test"), "A JsonSchemaException should be thrown when the InputStream is null");
    }

    @Test
    void getMappedSchema_throwsWhenReadTreeFails() throws Exception {
        // Prepare a simple JSON schema string.
        String json = "{\"type\":\"object\"}";
        JsonSchemaFactory realFactory = factoryWithSchema("urn:test", json);
        // Spy the factory so we can stub readTree().
        JsonSchemaFactory spyFactory = spy(realFactory);
        doThrow(new IOException("simulated read error")).when(spyFactory).readTree(any(InputStream.class), eq(InputFormat.JSON));
        // The public API will invoke the protected method which now throws.
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> spyFactory.getSchema("urn:test"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception message should contain the failure prefix");
        assertNotNull(ex.getCause(), "Original IOException should be preserved as the cause");
        assertEquals(IOException.class, ex.getCause().getClass(), "Cause must be the simulated IOException");
    }

    @ParameterizedTest(name = "[{index}] extension={0}")
    @CsvSource({ "schema.yaml, YAML", "schema.YAML, YAML" })
    void getMappedSchema_yamlWithoutFragment(String fileName, String expectedFormat) {
        // Minimal YAML content (identical to JSON for this test).
        String yaml = "type: object";
        JsonSchemaFactory factory = factoryWithSchema("urn:" + fileName, yaml);
        JsonSchema schema = factory.getSchema("urn:" + fileName);
        assertNotNull(schema, "Schema should be loaded");
        JsonNode node = schema.getSchemaNode();
        assertEquals("object", node.get("type").asText(), "YAML content must be parsed correctly");
    }

    @ParameterizedTest(name = "[{index}] extension={0}")
    @CsvSource({ "schema.json, JSON", "schema.JSON, JSON" })
    void getMappedSchema_jsonWithoutFragment(String fileName, String expectedFormat) {
        String json = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = factoryWithSchema("urn:" + fileName, json);
        JsonSchema schema = factory.getSchema("urn:" + fileName);
        assertNotNull(schema, "Schema should be loaded");
        JsonNode node = schema.getSchemaNode();
        assertEquals("object", node.get("type").asText(), "JSON content must be parsed correctly");
    }

    @Test
    void getMappedSchema_yamlWithFragment() {
        String yaml = "" + "type: object\n" + "properties:\n" + "  name:\n" + "    type: string\n" + "  age:\n" + "    type: integer\n";
        JsonSchemaFactory factory = factoryWithSchema("urn:schema.yaml", yaml);
        // Request the sub‑schema for the property 'name'.
        JsonSchema subSchema = factory.getSchema("urn:schema.yaml#/properties/name");
        assertNotNull(subSchema, "Sub‑schema for '#/properties/name' must be returned");
        JsonNode node = subSchema.getSchemaNode();
        assertEquals("string", node.get("type").asText(), "The sub‑schema should describe a string type");
    }

    @Test
    void getMappedSchema_jsonWithFragment() {
        String json = "{\n" + "  \"$id\": \"urn:schema.json\",\n" + "  \"type\": \"object\",\n" + "  \"definitions\": {\n" + "    \"address\": {\"type\": \"object\", \"properties\": {\"city\": {\"type\": \"string\"}}}\n" + "  },\n" + "  \"properties\": {\n" + "    \"addr\": {\"$ref\": \"#/definitions/address\"}\n" + "  }\n" + "}";
        JsonSchemaFactory factory = factoryWithSchema("urn:schema.json", json);
        // Resolve the fragment that points to the definition 'address'.
        JsonSchema addressSchema = factory.getSchema("urn:schema.json#/definitions/address");
        assertNotNull(addressSchema, "Address definition should be resolved");
        JsonNode node = addressSchema.getSchemaNode();
        assertEquals("object", node.get("type").asText(), "The definition must be an object");
        assertTrue(node.has("properties"), "Definition should contain properties");
    }

    @Test
    void getMappedSchema_emptyFragmentTreatedAsNoFragment() {
        String json = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = factoryWithSchema("urn:emptyFragment.json", json);
        // URI ending with just '#'
        JsonSchema schema = factory.getSchema("urn:emptyFragment.json#");
        assertNotNull(schema, "Schema should be loaded even when fragment is empty");
        JsonNode node = schema.getSchemaNode();
        assertEquals("object", node.get("type").asText(), "Root schema must be returned");
    }

    @Test
    void getMappedSchema_schemaLoaderThrowsIOException() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaLoader loaderMock = mock(SchemaLoader.class);
        when(loaderMock.getSchema(any())).thenThrow(new IOException("simulated loader failure"));
        injectSchemaLoader(factory, loaderMock);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:fail"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception message must contain the expected prefix");
        assertNotNull(ex.getCause(), "Original IOException should be preserved");
        assertEquals(IOException.class, ex.getCause().getClass(), "Cause must be the simulated IOException");
    }

    @Test
    void getMappedSchema_invalidYamlContent() {
        // Not a valid YAML document (random text).
        String badYaml = "::: not a yaml :::";
        JsonSchemaFactory factory = factoryWithSchema("urn:bad.yaml", badYaml);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:bad.yaml"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception message should mention loading failure");
        assertNotNull(ex.getCause(), "Parsing exception should be wrapped as cause");
    }

    @Test
    void getMappedSchema_ymlExtensionIsYaml() {
        String yaml = "type: object";
        JsonSchemaFactory factory = factoryWithSchema("urn:short.yml", yaml);
        JsonSchema schema = factory.getSchema("urn:short.yml");
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(), ".yml extension must be treated as YAML");
    }

    @Test
    void getMappedSchema_uriWithQueryString() {
        String json = "{\"type\":\"object\"}";
        // The builder maps the raw IRI (without query) – the loader ignores the query part.
        JsonSchemaFactory factory = factoryWithSchema("http://example.com/schema.json", json);
        JsonSchema schema = factory.getSchema("http://example.com/schema.json?version=1");
        assertNotNull(schema);
        assertEquals("object", schema.getSchemaNode().get("type").asText(), "Query string must not affect loading");
    }

    @Test
    void getMappedSchema_nullConfigThrowsNPE() {
        String json = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = factoryWithSchema("urn:nullConfig.json", json);
        // Use reflection to call the protected method directly with a null config.
        JsonSchemaFactory spy = spy(factory);
        SchemaLocation location = new SchemaLocation("urn:nullConfig.json");
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            // invoke protected method via reflection
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("getMappedSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
            m.setAccessible(true);
            m.invoke(spy, location, null);
        });
        // Message may vary; we only assert that NPE is indeed thrown.
        assertNotNull(npe);
    }

    @Test
    void getMappedSchema_nullSchemaUriThrowsNPE() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        NullPointerException npe = assertThrows(NullPointerException.class, () -> {
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("getMappedSchema", SchemaLocation.class, SchemaValidatorsConfig.class);
            m.setAccessible(true);
            m.invoke(factory, null, cfg);
        });
        assertNotNull(npe);
    }

    @Test
    void getMappedSchema_emptyContentLeadsToNPE() throws Exception {
        // Mock loader to return a non‑null InputStream containing empty bytes.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaLoader loaderMock = mock(SchemaLoader.class);
        Class<?> schemaClass = Class.forName("com.networknt.schema.resource.Schema");
        Object schemaMock = mock(schemaClass);
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        when(schemaMock.getClass().getMethod("getInputStream").invoke(schemaMock)).thenReturn(empty);
        when(loaderMock.getSchema(any())).thenReturn(schemaMock);
        injectSchemaLoader(factory, loaderMock);
        // Spy to make readTree return null.
        JsonSchemaFactory spy = spy(factory);
        doReturn(null).when(spy).readTree(any(InputStream.class), any(InputFormat.class));
        NullPointerException npe = assertThrows(NullPointerException.class, () -> spy.getSchema("urn:emptyContent"));
        assertNotNull(npe);
    }

    @Test
    void getMappedSchema_unknownMetaSchemaUri() {
        String json = "{\"$schema\":\"http://unknown/meta/schema\",\"type\":\"object\"}";
        JsonSchemaFactory factory = factoryWithSchema("urn:unknownMeta.json", json);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:unknownMeta.json"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception should indicate loading failure");
        assertNotNull(ex.getCause(), "Cause should contain the meta‑schema resolution problem");
    }

    @Test
    void getMappedSchema_invalidSchemaDefinition() {
        // "type" keyword with an illegal value.
        String json = "{\"type\":\"not-a-valid-type\"}";
        JsonSchemaFactory factory = factoryWithSchema("urn:invalid.json", json);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:invalid.json"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception should be wrapped as JsonSchemaException");
    }

    @Test
    void getMappedSchema_nonexistentFragmentThrows() {
        String json = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";
        JsonSchemaFactory factory = factoryWithSchema("urn:doc.json", json);
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:doc.json#/nonexistent/path"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception should be wrapped as JsonSchemaException");
        assertNotNull(ex.getCause(), "Cause should indicate that the fragment could not be resolved");
    }

    @Test
    void getMappedSchema_rootFragmentReturnsSameSchema() {
        String json = "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\"}}}";
        JsonSchemaFactory factory = factoryWithSchema("urn:root.json", json);
        JsonSchema root = factory.getSchema("urn:root.json");
        JsonSchema viaFragment = factory.getSchema("urn:root.json#/");
        assertNotNull(root);
        assertNotNull(viaFragment);
        // The two schema instances should be the same (or at least equal in content).
        assertEquals(root.getSchemaNode(), viaFragment.getSchemaNode(), "Fragment '#/' must resolve to the root schema");
    }

    @Test
    void getMappedSchema_malformedFragmentThrows() {
        String json = "{\"type\":\"object\"}";
        JsonSchemaFactory factory = factoryWithSchema("urn:malformed.json", json);
        // The fragment contains an illegal '[' character.
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> factory.getSchema("urn:malformed.json#/properties[invalid]"));
        assertTrue(ex.getMessage().contains("Failed to load json schema from"), "Exception should be wrapped as JsonSchemaException");
        assertNotNull(ex.getCause(), "Cause should indicate malformed JSON Pointer");
    }
}
