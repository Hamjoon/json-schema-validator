```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_Test {

    // -----------------------------------------------------------------
    // Mocks for constructor dependencies
    // -----------------------------------------------------------------
    @Mock
    ObjectMapper jsonMapperMock;

    @Mock
    ObjectMapper yamlMapperMock;

    @Mock
    JsonMetaSchema metaSchemaMock;

    // The factory under test – spied so we can stub protected methods
    JsonSchemaFactory factory;

    // -----------------------------------------------------------------
    // Common test data
    // -----------------------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";
    private static final String SIMPLE_SCHEMA = "{\"type\":\"object\"}";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal meta‑schema map required by the real constructor
        Map<String, JsonMetaSchema> metaMap = new HashMap<>();
        metaMap.put(DEFAULT_META_SCHEMA_URI, metaSchemaMock);

        // Build a real factory with the mocked mappers
        JsonSchemaFactory realFactory = new JsonSchemaFactory(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                metaMap,
                false);

        // Spy the factory to be able to stub protected newJsonSchema(...)
        factory = spy(realFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        reset(jsonMapperMock, yamlMapperMock, metaSchemaMock);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    // -----------------------------------------------------------------
    // 1. Normal flow – valid JSON, config variations
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] schema={0}, configNull={1}")
    @MethodSource("provideValidSchemas")
    void testGetSchemaValidInputs(String schema, SchemaValidatorsConfig config) throws Exception {
        // GIVEN
        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);

        when(jsonMapperMock.readTree(eq(schema))).thenReturn(parsedNode);
        doReturn(expectedSchema).when(factory).newJsonSchema(isNull(), eq(parsedNode), any());

        // WHEN
        JsonSchema actual = factory.getSchema(schema, config);

        // THEN
        assertSame(expectedSchema, actual, "Factory should return the JsonSchema created by newJsonSchema");
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
        // Verify that the exact config instance (or null) is forwarded
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }

    private static Stream<Arguments> provideValidSchemas() {
        SchemaValidatorsConfig failFast = new SchemaValidatorsConfig();
        failFast.setFailFast(true);
        return Stream.of(
                // simple object schema, non‑null config
                Arguments.of("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}",
                        new SchemaValidatorsConfig()),
                // empty object, null config
                Arguments.of("{}", null),
                // array schema, null config
                Arguments.of("{\"type\":\"array\",\"items\":{\"type\":\"string\"}}", null),
                // fail‑fast config, simple object schema
                Arguments.of("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}",
                        failFast)
        );
    }

    // -----------------------------------------------------------------
    // 2. Error handling – malformed JSON, whitespace, empty string
    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] malformed schema={0}")
    @MethodSource("provideMalformedSchemas")
    void testGetSchemaMalformedJsonThrowsJsonSchemaException(String schema) throws Exception {
        // GIVEN
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        IOException ioe = new IOException("Invalid JSON");

        when(jsonMapperMock.readTree(eq(schema))).thenThrow(ioe);

        // WHEN / THEN
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schema, config),
                "Malformed JSON should be wrapped in JsonSchemaException");

        assertSame(ioe, thrown.getCause(),
                "The cause of the JsonSchemaException must be the original IOException");
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
    }

    private static Stream<Arguments> provideMalformedSchemas() {
        return Stream.of(
                // missing closing brace
                Arguments.of("{invalid json"),
                // whitespace only
                Arguments.of("   "),
                // empty string
                Arguments.of("")
        );
    }

    // -----------------------------------------------------------------
    // 3. IllegalArgumentException when schema argument itself is null
    // -----------------------------------------------------------------
    @Test
    void testGetSchemaNullSchemaArgumentPropagatesIllegalArgumentException() throws Exception {
        // GIVEN
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        when(jsonMapperMock.readTree(isNull())).thenThrow(new IllegalArgumentException("schema is null"));

        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(null, config),
                "A null schema string should cause IllegalArgumentException to bubble up");

        assertEquals("schema is null", thrown.getMessage());
        verify(jsonMapperMock, times(1)).readTree(isNull());
    }

    // -----------------------------------------------------------------
    // 4. Literal JSON null value – NullNode leads to IllegalArgumentException from newJsonSchema
    // -----------------------------------------------------------------
    @Test
    void testGetSchemaLiteralNullJsonNodeThrowsIllegalArgumentException() throws Exception {
        // GIVEN
        String schema = "null"; // JSON literal null
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonNode nullNode = NullNode.getInstance();

        when(jsonMapperMock.readTree(eq(schema))).thenReturn(nullNode);
        // newJsonSchema should reject NullNode and throw IllegalArgumentException
        doThrow(new IllegalArgumentException("NullNode not allowed"))
                .when(factory).newJsonSchema(isNull(), eq(nullNode), any());

        // WHEN / THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> factory.getSchema(schema, config),
                "Literal JSON null should result in IllegalArgumentException from newJsonSchema");

        assertEquals("NullNode not allowed", thrown.getMessage());
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
        verify(factory, times(1)).newJsonSchema(isNull(), eq(nullNode), eq(config));
    }

    // -----------------------------------------------------------------
    // 5. Unknown meta‑schema – newJsonSchema throws JsonSchemaException
    // -----------------------------------------------------------------
    @Test
    void testGetSchemaUnknownMetaSchemaThrowsJsonSchemaException() throws Exception {
        // GIVEN
        String schema = "{\"$schema\":\"http://unknown/meta/schema\",\"type\":\"object\"}";
        SchemaValidatorsConfig config = null;
        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchemaException unknownMeta = new JsonSchemaException("Unknown meta schema");

        when(jsonMapperMock.readTree(eq(schema))).thenReturn(parsedNode);
        doThrow(unknownMeta).when(factory).newJsonSchema(isNull(), eq(parsedNode), any());

        // WHEN / THEN
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class,
                () -> factory.getSchema(schema, config),
                "Unknown meta‑schema should propagate JsonSchemaException");

        assertSame(unknownMeta, thrown, "The thrown exception must be the one from newJsonSchema");
        verify(jsonMapperMock, times(1)).readTree(eq(schema));
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }

    // -----------------------------------------------------------------
    // 6. getJsonMapper returns null – NPE is expected
    // -----------------------------------------------------------------
    @Test
    void testGetSchemaWhenJsonMapperIsNullThrowsNPE() {
        // GIVEN – subclass that overrides getJsonMapper() to return null
        JsonSchemaFactory factoryWithNullMapper = new JsonSchemaFactory(
                jsonMapperMock, // not used, but constructor required
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                new SchemaLoaders.Builder(),
                new SchemaMappers.Builder(),
                Map.of(DEFAULT_META_SCHEMA_URI, metaSchemaMock),
                false) {
            @Override
            protected ObjectMapper getJsonMapper() {
                return null; // force NPE inside getSchema
            }
        };

        String schema = "{}";
        SchemaValidatorsConfig config = null;

        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> factoryWithNullMapper.getSchema(schema, config),
                "If getJsonMapper() returns null, a NullPointerException should be thrown");

        // The exact message is not guaranteed, but we ensure the exception type
        assertNotNull(npe);
    }

    // -----------------------------------------------------------------
    // 7. Verify that the provided config object is passed through unchanged
    // -----------------------------------------------------------------
    @Test
    void testGetSchemaConfigIsPassedToNewJsonSchema() throws Exception {
        // GIVEN
        String schema = "{\"type\":\"object\"}";
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setFailFast(true); // non‑default flag

        JsonNode parsedNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);

        when(jsonMapperMock.readTree(eq(schema))).thenReturn(parsedNode);
        doReturn(expectedSchema).when(factory).newJsonSchema(isNull(), eq(parsedNode), eq(config));

        // WHEN
        JsonSchema actual = factory.getSchema(schema, config);

        // THEN
        assertSame(expectedSchema, actual);
        verify(factory, times(1)).newJsonSchema(isNull(), eq(parsedNode), eq(config));
    }
}
```