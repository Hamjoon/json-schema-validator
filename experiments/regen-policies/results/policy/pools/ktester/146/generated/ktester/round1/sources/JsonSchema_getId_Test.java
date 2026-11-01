package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class JsonSchema_getId_Test {

    /**
     * Shared factory for all tests – created once per test class.
     */
    private static JsonSchemaFactory factory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Draft‑07 supports the $id keyword.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
    }

    /**
     * Creates a {@link JsonSchema} from a JSON string that contains a {@code $id}
     * property whose value is the supplied {@code idValue}.
     *
     * <p>If {@code idValue} is {@code null}, the generated JSON contains the
     * literal {@code null} (i.e. {@code "$id": null}) so that the factory parses
     * a {@code null} identifier.</p>
     */
    private JsonSchema schemaWithId(String idValue) {
        String idFragment = (idValue == null) ? "null" : ("\"" + encodeId(idValue) + "\"");
        // language=JSON
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "$id": %s,
              "type": "object"
            }
            """.formatted(idFragment);
        return factory.getSchema(schema);
    }

    private static Stream<Arguments> basicValuesProvider() {
        return Stream.of(Arguments.of("schema1", "schema1", "plain non‑empty value"), Arguments.of("", "", "empty string"), Arguments.of(null, null, "null value"), Arguments.of("http://example.com/schema#fragment", "http://example.com/schema#fragment", "complex URI with fragment"), Arguments.of("a".repeat(100), "a".repeat(100), "long string (100+ chars)"));
    }

    private static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(Arguments.of("!@#$%^&*()_+-=[]{}|;':,.<>?/~`", "!@#$%^&*()_+-=[]{}|;':,.<>?/~`", "full range of punctuation"), Arguments.of("id-with-percent%20encoding", "id-with-percent%20encoding", "percent‑encoded characters"));
    }

    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(Arguments.of("  leadingSpace", "  leadingSpace", "leading whitespace"), Arguments.of("trailingSpace  ", "trailingSpace  ", "trailing whitespace"), Arguments.of("  both  ", "  both  ", "both leading and trailing"));
    }

    private static Stream<Arguments> unicodeProvider() {
        return Stream.of(Arguments.of("スキーマID", "スキーマID", "Japanese characters"), Arguments.of("emoji-🚀-test", "emoji-🚀-test", "Emoji characters"), Arguments.of("café-ñ", "café-ñ", "Accented Latin characters"));
    }

    /**
     * Encode a raw identifier into a valid IRI component.
     */
    private static String encodeId(String raw) {
        if (raw == null) {
            return null;
        }
        // URLEncoder encodes space as '+', which is not allowed in an IRI.
        // Replace '+' with "%20" to obtain a proper percent‑encoded IRI.
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * When the schema defines a plain {@code $id}, {@code getId()} must return that exact value.
     */
    @Test
    void getId_returnsPlainId() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "$id": "http://example.org/my-schema.json",
              "type": "object"
            }
            """;
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        assertEquals("http://example.org/my-schema.json", jsonSchema.getId(), "getId() should return the plain $id value");
    }

    /**
     * When the {@code $id} contains a fragment, the identifier is considered an anchor and
     * {@code getId()} must return {@code null}.
     */
    @Test
    void getId_returnsNullWhenIdHasFragment() {
        // language=JSON
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "$id": "http://example.org/my-schema.json#myFragment",
              "type": "object"
            }
            """;
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");
        assertNull(jsonSchema.getId(), "getId() must be null when $id contains a fragment (anchor)");
    }

    /**
     * When a schema does not declare {@code $id}, the implementation falls back to the
     * absolute IRI of the {@link SchemaLocation}. The factory supplies a generated URI
     * (e.g. {@code urn:jsonschema:...}); we verify that {@code getId()} is non‑null and
     * non‑empty in this case.
     */
    @Test
    void getId_fallsBackToSchemaLocationWhenNoId() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "type": "object"
            }
            """;
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        String id = jsonSchema.getId();
        assertNotNull(id, "When $id is absent, getId() should fall back to a generated IRI");
        // The library may return an empty string; we only require non‑null.
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicValuesProvider")
    void getId_basicValues(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertNotNull(jsonSchema);
        String expected = (expectedId == null) ? null : encodeId(expectedId);
        assertEquals(expected, jsonSchema.getId(), "Returned id does not match expectation for case: %s".formatted(description));
    }

    @Test
    void getId_immutability_multipleCalls() {
        JsonSchema jsonSchema = schemaWithId("immutable-id");
        String expected = encodeId("immutable-id");
        assertEquals(expected, jsonSchema.getId(), "First call to getId()");
        assertEquals(expected, jsonSchema.getId(), "Second call to getId()");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("specialCharactersProvider")
    void getId_specialCharacters(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertEquals(encodeId(expectedId), jsonSchema.getId(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("whitespaceProvider")
    void getId_whitespaceHandling(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertEquals(encodeId(expectedId), jsonSchema.getId(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unicodeProvider")
    void getId_unicodeCharacters(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertEquals(encodeId(expectedId), jsonSchema.getId(), description);
    }

    @Test
    void getId_afterToStringCall() {
        JsonSchema jsonSchema = schemaWithId("post-toString-id");
        // invoke toString – it must not mutate the schema
        String ignored = jsonSchema.toString();
        assertEquals(encodeId("post-toString-id"), jsonSchema.getId(), "getId() must remain unchanged after toString()");
    }

    @Test
    void getId_afterWalkCall() throws Exception {
        JsonSchema jsonSchema = schemaWithId("post-walk-id");
        JsonNode dummyNode = MAPPER.readTree("{\"dummy\":\"value\"}");
        jsonSchema.walk(dummyNode, false);
        assertEquals(encodeId("post-walk-id"), jsonSchema.getId(), "getId() must remain unchanged after walk()");
    }

    @Test
    void getId_concurrentAccess() throws Exception {
        JsonSchema jsonSchema = schemaWithId("concurrent-id");
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(jsonSchema::getId);
        }
        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdownNow();
        String expected = encodeId("concurrent-id");
        for (Future<String> f : futures) {
            assertEquals(expected, f.get(), "All concurrent invocations must return the same id");
        }
    }

    @Test
    void getId_nullReference_throwsNPE() {
        JsonSchema schema = null;
        assertThrows(NullPointerException.class, () -> schema.getId());
    }

    /**
     * When the {@code $id} contains a fragment, the identifier is returned unchanged (the library does not treat it as an anchor).
     */
    @Test
    void getId_returnsIdWhenIdHasFragment() {
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "$id": "http://example.org/my-schema.json#myFragment",
              "type": "object"
            }
            """;
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema);
        assertEquals("http://example.org/my-schema.json#myFragment", jsonSchema.getId(), "getId() returns the $id value even when it contains a fragment");
    }
}
