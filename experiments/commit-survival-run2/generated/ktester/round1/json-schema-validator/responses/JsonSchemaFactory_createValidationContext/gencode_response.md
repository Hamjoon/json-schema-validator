```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JsonSchemaFactory#createValidationContext(JsonNode)}.
 *
 * The tests cover a wide range of scenarios described in the JSON test‑case list,
 * using JUnit 5 and Mockito.  The factory is instantiated via reflection because its
 * constructor is not public.  Only the arguments that influence {@code createValidationContext}
 * (the meta‑schema map, URI factories, etc.) are supplied.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_createValidationContext_Test {

    /*** Mocks for the collaborators of {@link JsonSchemaFactory} ***/
    @Mock
    private URISchemeFactory uriFactoryMock;
    @Mock
    private URISchemeFetcher uriFetcherMock;
    @Mock
    private URNFactory urnFactoryMock;
    @Mock
    private ObjectMapper objectMapperMock;
    @Mock
    private com.fasterxml.jackson.dataformat.yaml.YAMLMapper yamlMapperMock;

    /*** Instance under test – the default factory (draft‑07) ***/
    private JsonSchemaFactory defaultFactory;

    /*** Helper ObjectMapper for parsing test JSON literals ***/
    private static final ObjectMapper JSON_PARSER = new ObjectMapper();

    /*** Constants for meta‑schema URIs ***/
    private static final String DRAFT07_URI = "http://json-schema.org/draft-07/schema#";
    private static final String DRAFT04_URI = "http://json-schema.org/draft-04/schema#";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----------------------------------------------------------------------
        // Prepare a factory that knows the draft‑07 meta‑schema (default for the
        // library).  The map is the only thing consulted by
        // createValidationContext → findMetaSchemaForSchema.
        // ----------------------------------------------------------------------
        JsonMetaSchema draft07Meta = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaMap = new HashMap<>();
        metaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DRAFT07_URI), draft07Meta);

        // The CompositeURITranslator used by the real implementation has a public
        // no‑arg constructor.
        URITranslator.CompositeURITranslator translator = new URITranslator.CompositeURITranslator();

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                ObjectMapper.class,
                com.fasterxml.jackson.dataformat.yaml.YAMLMapper.class,
                String.class,
                URISchemeFactory.class,
                URISchemeFetcher.class,
                URNFactory.class,
                Map.class,
                Map.class,
                boolean.class,
                URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        defaultFactory = ctor.newInstance(
                objectMapperMock,
                yamlMapperMock,
                DRAFT07_URI,
                uriFactoryMock,
                uriFetcherMock,
                urnFactoryMock,
                metaMap,
                Collections.emptyMap(),
                false,
                translator);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /* ----------------------------------------------------------------------
     * Utility to extract the JsonMetaSchema object from a ValidationContext
     * via reflection (the field is package‑private in the original library).
     * ---------------------------------------------------------------------- */
    private JsonMetaSchema extractMetaSchema(ValidationContext ctx) throws Exception {
        Field f = ValidationContext.class.getDeclaredField("jsonMetaSchema");
        f.setAccessible(true);
        return (JsonMetaSchema) f.get(ctx);
    }

    /* ----------------------------------------------------------------------
     * 1. Explicit $schema that matches a known meta‑schema (draft‑07)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Explicit known $schema resolves to the correct meta‑schema")
    void explicitKnownSchema_resolvesMetaSchema() throws Exception {
        String json = "{ \"$schema\": \"" + DRAFT07_URI + "\", \"type\": \"object\" }";
        JsonNode node = JSON_PARSER.readTree(json);

        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx, "Context must not be null");

        JsonMetaSchema meta = extractMetaSchema(ctx);
        // The meta‑schema instance should be the one we put into the factory map.
        assertSame(
                defaultFactory.getJsonMetaSchema(DRAFT07_URI),
                meta,
                "The ValidationContext must contain the draft‑07 meta‑schema"
        );
        assertSame(uriFactoryMock, ctx.getUriFactory(), "uriFactory should be propagated");
        assertSame(urnFactoryMock, ctx.getUrnFactory(), "urnFactory should be propagated");
    }

    /* ----------------------------------------------------------------------
     * 2. No $schema – fallback to default meta‑schema (draft‑07)
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Missing $schema falls back to the factory default")
    void missingSchema_usesDefaultMetaSchema() throws Exception {
        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"string\" }");

        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx);

        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(
                defaultFactory.getJsonMetaSchema(DRAFT07_URI),
                meta,
                "Should use the default draft‑07 meta‑schema"
        );
    }

    /* ----------------------------------------------------------------------
     * 3. Null schema node – expect NPE
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Null schema node causes NullPointerException")
    void nullSchemaNode_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> defaultFactory.createValidationContext(null));
    }

    /* ----------------------------------------------------------------------
     * 4. Unknown $schema URI – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Unknown $schema URI results in IllegalArgumentException")
    void unknownSchemaUri_throwsException() {
        String json = "{ \"$schema\": \"http://example.com/unknown/schema#\", \"type\": \"array\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 5. URN based $schema – the internal URITranslator should normalise it
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("URN $schema is translated to the canonical HTTP URI")
    void urnSchema_isTranslated() throws Exception {
        // The library ships with a built‑in URN translator, so we rely on it.
        String json = "{ \"$schema\": \"urn:jsonschema:draft-07\", \"type\": \"number\" }";
        JsonNode node = JSON_PARSER.readTree(json);

        ValidationContext ctx = defaultFactory.createValidationContext(node);
        assertNotNull(ctx);

        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(
                defaultFactory.getJsonMetaSchema(DRAFT07_URI),
                meta,
                "URN should resolve to the same draft‑07 meta‑schema instance"
        );
    }

    /* ----------------------------------------------------------------------
     * 6. Non‑string $schema value (number) – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema with non‑string numeric value throws")
    void nonStringSchemaNumber_throwsException() {
        String json = "{ \"$schema\": 123, \"type\": \"object\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 7. $schema with surrounding whitespace – should be trimmed
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema containing whitespace is trimmed and resolved")
    void schemaWithWhitespace_isTrimmed() throws Exception {
        String json = "{ \"$schema\": \"  " + DRAFT07_URI + "  \", \"type\": \"null\" }";
        JsonNode node = JSON_PARSER.readTree(json);

        ValidationContext ctx = defaultFactory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta);
    }

    /* ----------------------------------------------------------------------
     * 8. $schema without fragment identifier – factory should add '#'
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema without trailing fragment is normalised")
    void schemaWithoutFragment_isNormalised() throws Exception {
        String withoutFragment = "http://json-schema.org/draft-07/schema";
        String json = "{ \"$schema\": \"" + withoutFragment + "\", \"type\": \"integer\" }";
        JsonNode node = JSON_PARSER.readTree(json);

        ValidationContext ctx = defaultFactory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(defaultFactory.getJsonMetaSchema(DRAFT07_URI), meta);
    }

    /* ----------------------------------------------------------------------
     * 9. Factory built for draft‑04 – missing $schema must resolve to draft‑04
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Factory created for draft‑04 uses draft‑04 default meta‑schema")
    void factoryDraft04_missingSchemaUsesDraft04() throws Exception {
        // ------------------------------------------------------------------
        // Build a second factory that knows draft‑04.  The construction logic
        // mirrors the one in @BeforeEach but with different URI and map entry.
        // ------------------------------------------------------------------
        JsonMetaSchema draft04Meta = mock(JsonMetaSchema.class);
        Map<String, JsonMetaSchema> metaMap = new HashMap<>();
        metaMap.put(JsonSchemaFactory.normalizeMetaSchemaUri(DRAFT04_URI), draft04Meta);

        URITranslator.CompositeURITranslator translator = new URITranslator.CompositeURITranslator();

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                ObjectMapper.class,
                com.fasterxml.jackson.dataformat.yaml.YAMLMapper.class,
                String.class,
                URISchemeFactory.class,
                URISchemeFetcher.class,
                URNFactory.class,
                Map.class,
                Map.class,
                boolean.class,
                URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        JsonSchemaFactory draft04Factory = ctor.newInstance(
                objectMapperMock,
                yamlMapperMock,
                DRAFT04_URI,
                uriFactoryMock,
                uriFetcherMock,
                urnFactoryMock,
                metaMap,
                Collections.emptyMap(),
                false,
                translator);

        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"boolean\" }");
        ValidationContext ctx = draft04Factory.createValidationContext(node);
        JsonMetaSchema meta = extractMetaSchema(ctx);
        assertSame(draft04Meta, meta, "Should resolve to draft‑04 meta‑schema");
    }

    /* ----------------------------------------------------------------------
     * 10. Two successive calls return distinct ValidationContext objects
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("Multiple invocations produce independent ValidationContext objects")
    void multipleInvocations_areIndependent() {
        JsonNode node = JSON_PARSER.readTree("{ \"type\": \"string\" }");

        ValidationContext ctx1 = defaultFactory.createValidationContext(node);
        ValidationContext ctx2 = defaultFactory.createValidationContext(node);

        assertNotSame(ctx1, ctx2, "Each call must produce a new ValidationContext instance");
    }

    /* ----------------------------------------------------------------------
     * 11. $schema as JSON array – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema specified as an array is rejected")
    void schemaArrayValue_throwsException() {
        String json = "{ \"$schema\": [\"http://json-schema.org/draft-07/schema#\"], \"type\": \"object\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 12. $schema as boolean – expect IllegalArgumentException
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema specified as a boolean is rejected")
    void schemaBooleanValue_throwsException() {
        String json = "{ \"$schema\": true, \"type\": \"number\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 13. $schema empty string – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema empty string throws")
    void schemaEmptyString_throwsException() {
        String json = "{ \"$schema\": \"\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 14. $schema whitespace‑only – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema whitespace‑only string throws")
    void schemaWhitespaceOnly_throwsException() {
        String json = "{ \"$schema\": \"   \" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 15. $schema malformed URI – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema with malformed URI throws")
    void schemaMalformedUri_throwsException() {
        String json = "{ \"$schema\": \"http://[invalid]\" }";
        JsonNode node = JSON_PARSER.readTree(json);
        assertThrows(IllegalArgumentException.class,
                () -> defaultFactory.createValidationContext(node));
    }

    /* ----------------------------------------------------------------------
     * 16. Unsupported scheme (ftp) – reject
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("$schema with unsupported scheme throws")
    void schemaUnsupportedScheme_throwsException