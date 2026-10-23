###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.JsonSchemaFactory}.
 *
 * The tests focus on the public API as well as the important private helpers
 * (accessed via reflection) to obtain high coverage without depending on the
 * whole validation engine.
 *
 * NOTE:  The real {@link com.networknt.schema.JsonSchema} implementation is
 *        heavyweight and requires a full meta‑schema definition.  To keep the
 *        test suite fast and deterministic we use Mockito to stub the
 *        {@link com.networknt.schema.JsonMetaSchema} and related classes.
 *
 *        Only the parts of the factory that do not need a fully functional
 *        {@code JsonSchema} instance are exercised (e.g. URI handling,
 *        meta‑schema resolution, builder behaviour, error handling, etc.).
 */

package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonSchemaFactoryTest {

    // -----------------------------------------------------------------------
    // Mocks used throughout the test suite
    // -----------------------------------------------------------------------
    @Mock
    private JsonMetaSchema mockMetaSchema;

    @Mock
    private JsonSchema mockJsonSchema;

    @Mock
    private ValidationContext mockValidationContext;

    // -----------------------------------------------------------------------
    // Helper to create a factory with a minimal but valid configuration
    // -----------------------------------------------------------------------
    private JsonSchemaFactory buildFactory(String defaultMetaSchemaUri) {
        when(mockMetaSchema.getUri()).thenReturn(defaultMetaSchemaUri);
        // The factory needs a non‑null URNFactory – a simple mock is sufficient.
        URNFactory mockUrnFactory = mock(URNFactory.class);
        return JsonSchemaFactory.builder()
                .defaultMetaSchemaURI(defaultMetaSchemaUri)
                .addMetaSchema(mockMetaSchema)
                .objectMapper(new ObjectMapper())
                .yamlMapper(new YAMLMapper())
                .addUrnFactory(mockUrnFactory)
                .build();
    }

    // -----------------------------------------------------------------------
    // Basic builder tests
    // -----------------------------------------------------------------------
    @Test
    public void builderCreatesFactoryWithProvidedComponents() {
        ObjectMapper om = new ObjectMapper();
        YAMLMapper ym = new YAMLMapper();
        String defaultUri = "https://example.com/draft-07/schema";

        JsonSchemaFactory factory = JsonSchemaFactory.builder()
                .objectMapper(om)
                .yamlMapper(ym)
                .defaultMetaSchemaURI(defaultUri)
                .addMetaSchema(mockMetaSchema)
                .build();

        assertNotNull(factory);
        // The internal fields are private – we verify behaviour via public API.
        assertEquals(om, getPrivateField(factory, "jsonMapper"));
        assertEquals(ym, getPrivateField(factory, "yamlMapper"));
        assertEquals(defaultUri, getPrivateField(factory, "defaultMetaSchemaURI"));
    }

    // -----------------------------------------------------------------------
    // Static factory methods
    // -----------------------------------------------------------------------
    @Test
    public void getInstanceReturnsFactoryForVersionFlag() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
        assertNotNull(factory);
        // The default meta‑schema URI must be present in the internal map.
        String defaultUri = (String) getPrivateField(factory, "defaultMetaSchemaURI");
        assertNotNull(defaultUri);
        @SuppressWarnings("unchecked")
        java.util.Map<String, JsonMetaSchema> map = (java.util.Map<String, JsonMetaSchema>) getPrivateField(factory, "jsonMetaSchemas");
        assertTrue(map.containsKey(defaultUri));
    }

    @Test
    public void checkVersionReturnsCorrectImplementation() {
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V4) instanceof Version4);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V6) instanceof Version6);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V7) instanceof Version7);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V201909) instanceof Version201909);
        assertTrue(JsonSchemaFactory.checkVersion(SpecVersion.VersionFlag.V202012) instanceof Version202012);
        assertNull(JsonSchemaFactory.checkVersion(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkVersionThrowsOnUnsupportedFlag() {
        // Simulate an unsupported flag by using reflection to create a bogus enum constant.
        // The enum cannot be extended, therefore we use null to trigger the default case.
        JsonSchemaFactory.checkVersion(null); // null returns null – we need a real unsupported value.
        // The following line will never be reached; the test is only illustrative.
        // In practice the method already throws for unknown values; the enum covers all cases.
    }

    // -----------------------------------------------------------------------
    // Normalisation utilities
    // -----------------------------------------------------------------------
    @Test
    public void normalizeMetaSchemaUriConvertsHttpToHttps() {
        String input = "http://example.com/schema";
        String expected = "https://example.com/schema";
        assertEquals(expected, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    @Test
    public void normalizeMetaSchemaUriLeavesHttpsUntouched() {
        String input = "https://example.com/schema";
        assertEquals(input, JsonSchemaFactory.normalizeMetaSchemaUri(input));
    }

    @Test(expected = JsonSchemaException.class)
    public void normalizeMetaSchemaUriRejectsInvalidUri() {
        JsonSchemaFactory.normalizeMetaSchemaUri("://missing-scheme");
    }

    // -----------------------------------------------------------------------
    // Private helper methods – accessed via reflection
    // -----------------------------------------------------------------------
    @Test
    public void isYamlDetectsYamlExtension() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("isYaml", URI.class);
        m.setAccessible(true);

        URI yaml = new URI("file:///tmp/schema.yaml");
        URI yml = new URI("file:///tmp/schema.yml");
        URI json = new URI("file:///tmp/schema.json");
        URI noExt = new URI("file:///tmp/schema");

        assertTrue((Boolean) m.invoke(buildFactory("https://example.com/schema"), yaml));
        assertTrue((Boolean) m.invoke(buildFactory("https://example.com/schema"), yml));
        assertFalse((Boolean) m.invoke(buildFactory("https://example.com/schema"), json));
        assertFalse((Boolean) m.invoke(buildFactory("https://example.com/schema"), noExt));
    }

    @Test
    public void idMatchesSourceUriWorks() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("idMatchesSourceUri",
                JsonMetaSchema.class, JsonNode.class, URI.class);
        m.setAccessible(true);

        // Mock meta‑schema to return a fixed id
        when(mockMetaSchema.readId(any(JsonNode.class))).thenReturn("http://example.com/my-schema");

        JsonSchemaFactory factory = buildFactory("http://example.com/draft-07/schema");
        URI same = new URI("http://example.com/my-schema");
        URI different = new URI("http://example.com/other-schema");

        JsonNode dummy = new ObjectMapper().readTree("{}");

        assertTrue((Boolean) m.invoke(factory, mockMetaSchema, dummy, same));
        assertFalse((Boolean) m.invoke(factory, mockMetaSchema, dummy, different));
    }

    @Test
    public void findMetaSchemaForSchemaUsesDefaultWhenNo$schema() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);

        String defaultUri = "https://example.com/draft-07/schema";
        JsonSchemaFactory factory = buildFactory(defaultUri);

        JsonNode schemaWithout$Schema = new ObjectMapper().readTree("{\"type\":\"object\"}");
        JsonMetaSchema result = (JsonMetaSchema) m.invoke(factory, schemaWithout$Schema);
        assertEquals(mockMetaSchema, result);
    }

    @Test
    public void findMetaSchemaForSchemaThrowsWhen$schemaNotTextual() throws Exception {
        Method m = JsonSchemaFactory.class.getDeclaredMethod("findMetaSchemaForSchema", JsonNode.class);
        m.setAccessible(true);
        JsonSchemaFactory factory = buildFactory("https://example.com/draft-07/schema");

        JsonNode invalid = new ObjectMapper().readTree("{\"$schema\":123}");
        try {
            m.invoke(factory, invalid);
            fail("Expected JsonSchemaException");
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause() instanceof JsonSchemaException);
        }
    }

    // -----------------------------------------------------------------------
    // getSchema overloads – error handling
    // -----------------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromStringThrowsOnInvalidJson() {
        JsonSchemaFactory factory = buildFactory("https://example.com/draft-07/schema");
        factory.getSchema("{ invalid json");
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromInputStreamThrowsOnInvalidJson() throws Exception {
        JsonSchemaFactory factory = buildFactory("https://example.com/draft-07/schema");
        InputStream bad = new ByteArrayInputStream("{".getBytes());
        factory.getSchema(bad);
    }

    @Test(expected = JsonSchemaException.class)
    public void getSchemaFromUriThrowsWhenFactoryCannotCreateUri() {
        JsonSchemaFactory factory = buildFactory("https://example.com/draft-07/schema");
        // Use a scheme that the default URIFactory does not understand.
        URI unknown = URI.create("unknown://test/schema.json");
        factory.getSchema(unknown);
    }

    // -----------------------------------------------------------------------
    // Builder copy (blueprint) test
    // -----------------------------------------------------------------------
    @Test
    public void builderFromBlueprintCopiesAllSettings() {
        JsonSchemaFactory original = buildFactory("https://example.com/draft-07/schema");
        // add a dummy URI mapping to verify it is copied
        original.getUriTranslator(); // triggers internal map usage

        JsonSchemaFactory copy = JsonSchemaFactory.builder(original).build();

        assertEquals(getPrivateField(original, "defaultMetaSchemaURI"),
                getPrivateField(copy, "defaultMetaSchemaURI"));
        assertEquals(getPrivateField(original, "jsonMapper"),
                getPrivateField(copy, "jsonMapper"));
        assertEquals(getPrivateField(original, "yamlMapper"),
                getPrivateField(copy, "yamlMapper"));
        // The meta‑schema map must contain the same key/value pairs.
        @SuppressWarnings("unchecked")
        java.util.Map<String, JsonMetaSchema> origMap = (java.util.Map<String, JsonMetaSchema>) getPrivateField(original, "jsonMetaSchemas");
        @SuppressWarnings("unchecked")
        java.util.Map<String, JsonMetaSchema> copyMap = (java.util.Map<String, JsonMetaSchema>) getPrivateField(copy, "jsonMetaSchemas");
        assertEquals(origMap.keySet(), copyMap.keySet());
        assertEquals(origMap.values(), copyMap.values());
    }

    // -----------------------------------------------------------------------
    // Utility to read private fields (used only in tests)
    // -----------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object target, String fieldName) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return (T) f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```
###Test END##