package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getId_Test {

    /**
     * Shared factory for all tests – created once per test class.
     */
    private static JsonSchemaFactory factory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use a draft that supports the $id keyword (draft‑07 or later).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
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
        String idFragment = (idValue == null) ? "null" : ("\"" + idValue + "\"");
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
        return Stream.of(Arguments.of("schema1", "schema1", "Verify that getId() returns the exact id string that was supplied to the JsonSchema constructor (normal non‑empty value)."), Arguments.of("", "", "Verify that getId() correctly returns an empty string when the JsonSchema was constructed with an empty id."), Arguments.of(null, null, "Verify that getId() returns null when the JsonSchema was constructed with a null id (allowed because the field is final but may be null)."), Arguments.of("http://example.com/schema#fragment", "http://example.com/schema#fragment", "Verify that getId() returns a complex URI‑like id containing special characters and a fragment identifier."), Arguments.of("a".repeat(100), "a".repeat(100), "Verify that getId() correctly returns a long id string (100+ characters) without truncation or alteration."));
    }

    private static Stream<Arguments> specialCharactersProvider() {
        return Stream.of(Arguments.of("!@#$%^&*()_+-=[]{}|;':,.<>?/~`", "!@#$%^&*()_+-=[]{}|;':,.<>?/~`", "Id containing a full range of punctuation and special symbols."), Arguments.of("id-with-percent%20encoding", "id-with-percent%20encoding", "Id that includes percent‑encoded characters (e.g., space encoded as %20)."));
    }

    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(Arguments.of("  leadingSpace", "  leadingSpace", "Leading whitespace should be preserved."), Arguments.of("trailingSpace  ", "trailingSpace  ", "Trailing whitespace should be preserved."), Arguments.of("  both  ", "  both  ", "Both leading and trailing whitespace should be preserved."));
    }

    private static Stream<Arguments> unicodeProvider() {
        return Stream.of(Arguments.of("スキーマID", "スキーマID", "Japanese characters are returned unchanged."), Arguments.of("emoji-🚀-test", "emoji-🚀-test", "Emoji characters are returned unchanged."), Arguments.of("café-ñ", "café-ñ", "Accented Latin characters are returned unchanged."));
    }

    /**
     * When the schema defines a plain {@code $id}, {@code getId()} must return that exact value.
     */
    @Test
    void getId_returnsPlainId() {
        // language=JSON
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "$id": "http://example.org/my-schema.json",
              "type": "object"
            }
            """;
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");
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
        // language=JSON
        String schema = """
            {
              "$schema": "https://json-schema.org/draft/2019-09/schema",
              "type": "object"
            }
            """;
        JsonSchema jsonSchema = factory.getSchema(schema);
        assertNotNull(jsonSchema, "Factory should return a non‑null JsonSchema instance");
        String id = jsonSchema.getId();
        assertNotNull(id, "When $id is absent, getId() should fall back to a generated IRI");
        assertFalse(id.isEmpty(), "The fallback IRI should not be an empty string");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicValuesProvider")
    void getId_basicValues(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertNotNull(jsonSchema, "Factory must return a non‑null JsonSchema instance");
        assertEquals(expectedId, jsonSchema.getId(), "Returned id does not match expectation for case: %s".formatted(description));
    }

    @Test
    void getId_immutability_multipleCalls() {
        JsonSchema jsonSchema = schemaWithId("immutable-id");
        assertEquals("immutable-id", jsonSchema.getId(), "First call to getId() should return the supplied id");
        assertEquals("immutable-id", jsonSchema.getId(), "Second call to getId() must return the same value, confirming immutability");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("specialCharactersProvider")
    void getId_specialCharacters(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertEquals(expectedId, jsonSchema.getId(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("whitespaceProvider")
    void getId_whitespaceHandling(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertEquals(expectedId, jsonSchema.getId(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("unicodeProvider")
    void getId_unicodeCharacters(String suppliedId, String expectedId, String description) {
        JsonSchema jsonSchema = schemaWithId(suppliedId);
        assertEquals(expectedId, jsonSchema.getId(), description);
    }

    @Test
    void getId_afterToStringCall() {
        JsonSchema jsonSchema = schemaWithId("post-toString-id");
        // invoke toString – it must not mutate the schema
        String ignored = jsonSchema.toString();
        assertEquals("post-toString-id", jsonSchema.getId(), "getId() must remain unchanged after toString()");
    }

    @Test
    void getId_afterWalkCall() throws Exception {
        JsonSchema jsonSchema = schemaWithId("post-walk-id");
        // Walk a trivial JSON node; the walk method should be side‑effect free for the id
        JsonNode dummyNode = MAPPER.readTree("{\"dummy\":\"value\"}");
        jsonSchema.walk(dummyNode, false);
        assertEquals("post-walk-id", jsonSchema.getId(), "getId() must remain unchanged after walk()");
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
        for (Future<String> f : futures) {
            assertEquals("concurrent-id", f.get(), "All concurrent invocations must return the same id");
        }
    }

    @Test
    void getId_nullReference_throwsNPE() {
        JsonSchema schema = null;
        assertThrows(NullPointerException.class, () -> {
            // Direct method call on a null reference – should throw NPE
            schema.getId();
        });
    }
}
