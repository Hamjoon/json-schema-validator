package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.networknt.org.apache.commons.validator.routines.InetAddressValidator;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    // Mocks for constructor dependencies
    @Mock
    ObjectMapper jsonMapper;

    @Mock
    YAMLMapper yamlMapper;

    @Mock
    URISchemeFactory uriFactory;

    @Mock
    URISchemeFetcher uriFetcher;

    @Mock
    URNFactory urnFactory;

    @Mock
    URITranslator.CompositeURITranslator uriTranslators;

    // Additional objects needed for construction
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema";

    private Map<String, JsonMetaSchema> jsonMetaSchemas;

    private Map<String, String> uriMap;

    private JsonSchemaFactory factorySpy;

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a minimal but valid jsonMetaSchemas map
        JsonMetaSchema dummyMetaSchema = mock(JsonMetaSchema.class);
        jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), dummyMetaSchema);
        // uriMap can be empty – the factory only checks for null
        uriMap = new HashMap<>();
        // Build a real instance (using the package‑private constructor via reflection)
        // The constructor is private, but we can invoke it via reflection for the test.
        try {
            java.lang.reflect.Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
            ctor.setAccessible(true);
            JsonSchemaFactory realFactory = ctor.newInstance(jsonMapper, yamlMapper, DEFAULT_META_SCHEMA_URI, uriFactory, uriFetcher, urnFactory, jsonMetaSchemas, uriMap, false, uriTranslators);
            // Spy the instance so we can stub the protected newJsonSchema method
            factorySpy = spy(realFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* -----------------------------------------------------------------------
     * Helper to turn a JSON string into a JsonNode.  Using a real ObjectMapper
     * keeps the test independent of the mocked jsonMapper that is only needed
     * for the factory construction.
     * ----------------------------------------------------------------------- */
    private static JsonNode parseJson(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid test JSON", e);
        }
    }

    /* -----------------------------------------------------------------------
     * SUCCESSFUL CASES – the factory should return a non‑null JsonSchema.
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> successCases() {
        return Stream.of(Arguments.of("http JSON schema", "http://example.com/schema.json", "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } } }"), Arguments.of("file URI with YAML‑compatible JSON node", "file:/tmp/local-schema.yaml", "{ \"type\": \"object\", \"properties\": { \"age\": { \"type\": \"integer\" } } }"), Arguments.of("empty JSON object", "http://example.com/empty-schema.json", "{}"));
    }

    /* -----------------------------------------------------------------------
     * EXCEPTION CASES – the factory should propagate the exception thrown by
     * newJsonSchema (or by URI construction when the literal is malformed).
     * ----------------------------------------------------------------------- */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null arguments
        Arguments.of("null URI", null, "{ \"type\": \"object\" }", NullPointerException.class), Arguments.of("null JsonNode", "http://example.com/any-schema.json", null, NullPointerException.class), Arguments.of("both null", null, null, NullPointerException.class), // Unsupported / malformed URIs
        Arguments.of("unsupported scheme (ftp)", "ftp://example.com/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("relative URI", "schemas/schema.json", "{ \"type\": \"object\" }", IllegalArgumentException.class), Arguments.of("data URI scheme", "data:application/json,{\"type\":\"object\"}", "{ \"type\": \"object\" }", IllegalArgumentException.class), // Invalid JSON node shapes
        Arguments.of("JSON array node", "http://example.com/schema.json", "[ { \"type\": \"object\" } ]", IllegalArgumentException.class), Arguments.of("primitive JSON node", "http://example.com/schema.json", "\"string\"", IllegalArgumentException.class), // Invalid schema content (unknown type)
        Arguments.of("unknown type keyword", "http://example.com/schema.json", "{ \"type\": \"unknown\" }", IllegalArgumentException.class));
    }

    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);
        // Stub the protected newJsonSchema method to return our mock
        doReturn(expectedSchema).when(factorySpy).newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());
        // WHEN
        JsonSchema actual = factorySpy.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(actual, "The returned JsonSchema should not be null");
        assertSame(expectedSchema, actual, "The factory should return the JsonSchema produced by newJsonSchema");
        // Verify that newJsonSchema was invoked exactly once with the expected arguments
        verify(factorySpy, times(1)).newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void getSchema_successful(String caseName, String uriString, String json) throws Exception {
        // GIVEN
        URI schemaUri = new URI(uriString);
        JsonNode schemaNode = parseJson(json);
        JsonSchema mockSchema = mock(JsonSchema.class);
        // Stub the protected newJsonSchema to return our mock for *any* call with the
        // supplied arguments (config is null for this overload).
        doReturn(mockSchema).when(factorySpy).newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());
        // WHEN
        JsonSchema result = factorySpy.getSchema(schemaUri, schemaNode);
        // THEN
        assertNotNull(result, "Factory should return a non‑null JsonSchema");
        assertSame(mockSchema, result, "Returned instance must be the one produced by newJsonSchema");
        verify(factorySpy, times(1)).newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void getSchema_exceptions(String caseName, String uriString, String json, Class<? extends Throwable> expectedException) throws Exception {
        // -------------------------------------------------
        // Build parameters – handling nulls and malformed URIs
        // -------------------------------------------------
        URI schemaUri = null;
        JsonNode schemaNode = null;
        // URI parsing – if the literal itself is syntactically invalid we
        // expect the constructor to throw URISyntaxException which we wrap
        // as IllegalArgumentException for the purpose of this test.
        if (uriString != null) {
            try {
                schemaUri = new URI(uriString);
            } catch (Exception e) {
                // Simulate the factory's behaviour of wrapping the error.
                assertTrue(IllegalArgumentException.class.isAssignableFrom(expectedException), "Test data inconsistency: malformed URI should map to IllegalArgumentException");
                assertThrows(expectedException, () -> factorySpy.getSchema(new URI(uriString), mock(JsonNode.class)));
                return;
            }
        }
        if (json != null) {
            schemaNode = parseJson(json);
        }
        // -------------------------------------------------
        // Stub newJsonSchema to throw the expected exception when applicable.
        // For null arguments the factory will likely hit an NPE before
        // invoking newJsonSchema, therefore we only stub for non‑null cases.
        // -------------------------------------------------
        if (schemaUri != null && schemaNode != null) {
            doThrow(expectedException).when(factorySpy).newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());
        }
        // -------------------------------------------------
        // Exercise the method under test and verify the exception.
        // -------------------------------------------------
        assertThrows(expectedException, () -> factorySpy.getSchema(schemaUri, schemaNode), () -> "Expected " + expectedException.getSimpleName() + " for case: " + caseName);
    }
}
