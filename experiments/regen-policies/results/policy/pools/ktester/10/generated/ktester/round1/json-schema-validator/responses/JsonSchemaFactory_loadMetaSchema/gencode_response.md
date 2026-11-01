```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#loadMetaSchema(String, SchemaValidatorsConfig)}.
 *
 * <p>The method is {@code protected}, but the test class resides in the same package,
 * therefore it can be invoked directly.  All external interactions (loading a schema,
 * obtaining the validation context, etc.) are mocked with Mockito so that the tests
 * focus solely on the logic inside {@code loadMetaSchema}.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_loadMetaSchema_Test {

    private JsonSchemaFactory factory;          // the spy we will use in every test
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Use a real factory instance (V201909 provides built‑in meta‑schemas) and spy it.
        factory = spy(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909));
    }

    /* -------------------------------------------------------------
     * Helper methods for creating common mocks
     * ------------------------------------------------------------- */
    private static JsonSchema mockSchema(JsonMetaSchema metaSchema,
                                          JsonNode schemaNode) {
        JsonSchema schema = mock(JsonSchema.class);
        ValidationContext vc = mock(ValidationContext.class);
        when(schema.getValidationContext()).thenReturn(vc);
        when(vc.getMetaSchema()).thenReturn(metaSchema);
        when(schema.getSchemaNode()).thenReturn(schemaNode);
        return schema;
    }

    private static JsonMetaSchema mockMetaSchema(SpecVersion.VersionFlag spec) {
        JsonMetaSchema meta = mock(JsonMetaSchema.class);
        when(meta.getSpecification()).thenReturn(spec);
        return meta;
    }

    /* -------------------------------------------------------------
     * 1. Specification is null (no version information)
     * ------------------------------------------------------------- */
    @Test
    void specificationIsNull() throws IOException {
        String id = "http://example.com/meta-schema";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(null);                     // specification == null
        JsonNode node = MAPPER.readTree("{\"$id\":\"" + id + "\"}");
        JsonSchema schema = mockSchema(meta, node);

        // stub the protected call
        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertNull(result.getVocabularies(),
                "When specification is null the builder must not initialise vocabularies");
    }

    /* -------------------------------------------------------------
     * 2. Specification older than V201909 (no vocabularies processing)
     * ------------------------------------------------------------- */
    @Test
    void olderSpecificationIgnoresVocabularies() throws IOException {
        String id = "http://example.com/old-meta";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // Use V7 (older than V201909) but still provide a $vocabulary node – it must be ignored.
        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V7);
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": { \"core\": true } }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertNull(result.getVocabularies(),
                "Older specifications must not create a vocabularies map even if $vocabulary exists");
    }

    /* -------------------------------------------------------------
     * 3. Specification newer than or equal to V201909 but $vocabulary absent
     * ------------------------------------------------------------- */
    @Test
    void newerSpecWithoutVocabulary() throws IOException {
        String id = "http://example.com/new-meta-no-vocab";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        JsonNode node = MAPPER.readTree("{\"$id\":\"" + id + "\"}");
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertNull(result.getVocabularies(),
                "When $vocabulary is missing the map must stay null");
    }

    /* -------------------------------------------------------------
     * 4. Specification newer than or equal to V201909 with $vocabulary present
     * ------------------------------------------------------------- */
    @Test
    void newerSpecWithVocabulary() throws IOException {
        String id = "http://example.com/new-meta-with-vocab";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": { \"core\": true, \"applicator\": false } }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);

        assertNotNull(result);
        assertEquals(id, result.getId());
        Map<String, Boolean> vocab = result.getVocabularies();
        assertNotNull(vocab, "Vocabularies map must be initialised");
        assertEquals(2, vocab.size());
        assertTrue(vocab.get("core"));
        assertFalse(vocab.get("applicator"));
    }

    /* -------------------------------------------------------------
     * 5. Null SchemaValidatorsConfig (should behave like empty config)
     * ------------------------------------------------------------- */
    @Test
    void nullConfigIsTreatedAsEmpty() throws IOException {
        String id = "http://example.com/null-config-meta";
        SchemaValidatorsConfig cfg = null; // deliberately null

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        JsonNode node = MAPPER.readTree("{\"$id\":\"" + id + "\"}");
        JsonSchema schema = mockSchema(meta, node);

        // match any config (including null)
        doReturn(schema).when(factory).getSchema(eq(SchemaLocation.of(id)), isNull());

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertNull(result.getVocabularies(),
                "With no $vocabulary the map stays null even when config is null");
    }

    /* -------------------------------------------------------------
     * 6. Null id argument (should raise IllegalArgumentException / NPE)
     * ------------------------------------------------------------- */
    @Test
    void nullIdThrowsException() {
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();
        assertThrows(IllegalArgumentException.class,
                () -> factory.loadMetaSchema(null, cfg),
                "Passing a null id must fail fast");
    }

    /* -------------------------------------------------------------
     * 7. $vocabulary present with non‑boolean values (coerced to false)
     * ------------------------------------------------------------- */
    @Test
    void nonBooleanVocabularyValuesCoercedToFalse() throws IOException {
        String id = "http://example.com/non-boolean-vocab";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": { \"core\": 1, \"applicator\": \"yes\" } }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);
        Map<String, Boolean> vocab = result.getVocabularies();

        assertNotNull(vocab);
        assertEquals(2, vocab.size());
        // Jackson's booleanValue() returns false for non‑boolean nodes
        assertFalse(vocab.get("core"));
        assertFalse(vocab.get("applicator"));
    }

    /* -------------------------------------------------------------
     * 8. $vocabulary present but empty object
     * ------------------------------------------------------------- */
    @Test
    void emptyVocabularyObjectCreatesEmptyMap() throws IOException {
        String id = "http://example.com/empty-vocab";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": {} }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);
        Map<String, Boolean> vocab = result.getVocabularies();

        assertNotNull(vocab);
        assertTrue(vocab.isEmpty(), "Empty $vocabulary object should yield an empty map");
    }

    /* -------------------------------------------------------------
     * 9. $vocabulary entries with explicit null values
     * ------------------------------------------------------------- */
    @Test
    void nullValuesInVocabularyTreatedAsFalse() throws IOException {
        String id = "http://example.com/null-value-vocab";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": { \"core\": null } }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);
        Map<String, Boolean> vocab = result.getVocabularies();

        assertNotNull(vocab);
        assertEquals(1, vocab.size());
        assertFalse(vocab.get("core"));
    }

    /* -------------------------------------------------------------
     * 10. Specification exactly V201909 (boundary condition)
     * ------------------------------------------------------------- */
    @Test
    void boundaryConditionV201909ProcessesVocabulary() throws IOException {
        String id = "http://example.com/boundary-v201909";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": { \"core\": true } }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        JsonMetaSchema result = factory.loadMetaSchema(id, cfg);
        Map<String, Boolean> vocab = result.getVocabularies();

        assertNotNull(vocab);
        assertEquals(1, vocab.size());
        assertTrue(vocab.get("core"));
    }

    /* -------------------------------------------------------------
     * 11. Invalid id format (SchemaLocation.of throws IllegalArgumentException)
     * ------------------------------------------------------------- */
    @Test
    void invalidIdFormatThrowsException() {
        String badId = "not-a-valid-uri";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        assertThrows(IllegalArgumentException.class,
                () -> factory.loadMetaSchema(badId, cfg),
                "An invalid URI string must cause IllegalArgumentException from SchemaLocation.of");
    }

    /* -------------------------------------------------------------
     * 12. Schema not found (getSchema returns null → NullPointerException)
     * ------------------------------------------------------------- */
    @Test
    void schemaNotFoundLeadsToNPE() {
        String id = "http://example.com/missing-schema";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        doReturn(null).when(factory).getSchema(SchemaLocation.of(id), cfg);

        assertThrows(NullPointerException.class,
                () -> factory.loadMetaSchema(id, cfg),
                "When getSchema returns null the method dereferences it and throws NPE");
    }

    /* -------------------------------------------------------------
     * 13. Schema node is null (NPE when accessing $vocabulary)
     * ------------------------------------------------------------- */
    @Test
    void nullSchemaNodeThrowsNPE() {
        String id = "http://example.com/null-schema-node";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        JsonSchema schema = mock(JsonSchema.class);
        ValidationContext vc = mock(ValidationContext.class);
        when(schema.getValidationContext()).thenReturn(vc);
        when(vc.getMetaSchema()).thenReturn(meta);
        // schema node deliberately null
        when(schema.getSchemaNode()).thenReturn(null);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        assertThrows(NullPointerException.class,
                () -> factory.loadMetaSchema(id, cfg),
                "A null schema node should cause NPE when the method tries to read $vocabulary");
    }

    /* -------------------------------------------------------------
     * 14. $vocabulary node is not an object (e.g., array) → IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test
    void vocabularyNodeNotObjectThrowsException() throws IOException {
        String id = "http://example.com/vocab-not-object";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        JsonMetaSchema meta = mockMetaSchema(SpecVersion.VersionFlag.V201909);
        // $vocabulary is an array, not an object
        String json = "{ \"$id\":\"" + id + "\", \"$vocabulary\": [\"core\"] }";
        JsonNode node = MAPPER.readTree(json);
        JsonSchema schema = mockSchema(meta, node);

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        assertThrows(IllegalArgumentException.class,
                () -> factory.loadMetaSchema(id, cfg),
                "Iterating over a non‑object $vocabulary should raise IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     * 15. MetaSchema is null (builder receives null meta schema → NullPointerException)
     * ------------------------------------------------------------- */
    @Test
    void nullMetaSchemaLeadsToNPE() {
        String id = "http://example.com/null-meta-schema";
        SchemaValidatorsConfig cfg = new SchemaValidatorsConfig();

        // ValidationContext returns null meta‑schema
        JsonSchema schema = mock(JsonSchema.class);
        ValidationContext vc = mock(ValidationContext.class);
        when(schema.getValidationContext()).thenReturn(vc);
        when(vc.getMetaSchema()).thenReturn(null);
        when(schema.getSchemaNode()).thenReturn(MAPPER.createObjectNode());

        doReturn(schema).when(factory).getSchema(SchemaLocation.of(id), cfg);

        assertThrows(NullPointerException.class,
                () -> factory.loadMetaSchema(id, cfg),
                "If the underlying meta‑schema is null the builder call must NPE");
    }

    /* -------------------------------------------------------------
     * Existing concrete file‑based tests (kept unchanged)
     * ------------------------------------------------------------- */
    @Test
    void testLoadMetaSchemaWithVocabularies() throws IOException {
        Path tempFile = Files.createTempFile("custom-meta-schema", ".json");
        tempFile.toFile().deleteOnExit();

        String jsonContent = "{\n" +
                "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema#\",\n" +
                "  \"$id\": \"http://example.com/custom-meta-schema\",\n" +
                "  \"$vocabulary\": {\n" +
                "    \"https://json-schema.org/vocab/applicator\": true\n" +
                "  }\n" +
                "}";

        Files.write(tempFile, jsonContent.getBytes(StandardCharsets.UTF_8));

        String schemaUri = tempFile.toUri().toString();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        JsonMetaSchema metaSchema = factory.loadMetaSchema(schemaUri, config);

        assertNotNull(metaSchema);
        assertEquals(schemaUri, metaSchema.getId());
        assertEquals(SpecVersion.VersionFlag.V201909, metaSchema.getSpecification());

        Map<String, Boolean> vocabularies = metaSchema.getVocabularies();
        assertNotNull(vocabularies);
        assertTrue(vocabularies.containsKey("https://json-schema.org/vocab/applicator"));
        assertTrue(vocabularies.get("https://json-schema.org/vocab/applicator"));
    }

    @Test
    void testLoadMetaSchemaWithoutVocabularies() throws IOException {
        Path tempFile = Files.createTempFile("custom-meta-schema-draft7", ".json");
        tempFile.toFile().deleteOnExit();

        String jsonContent = "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"$id\": \"http://example.com/custom-meta-schema-draft7\"\n" +
                "}";

        Files.write(tempFile, jsonContent.getBytes(StandardCharsets.UTF_8));

        String schemaUri = tempFile.toUri().toString();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();

        JsonMetaSchema metaSchema = factory.loadMetaSchema(schemaUri, config);

        assertNotNull(metaSchema);
        assertEquals(schemaUri, metaSchema.getId());
        assertEquals(SpecVersion.VersionFlag.V7, metaSchema.getSpecification());

        assertNull(metaSchema.getVocabularies(),
                "Vocabularies map should be null for specifications prior to V201909");
    }
}
```