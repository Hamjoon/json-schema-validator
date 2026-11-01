```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMappers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(JsonNode, SchemaValidatorsConfig)}.
 *
 * The factory constructor is private, therefore it is instantiated via reflection.
 * The protected {@code newJsonSchema} method is stubbed on a spy to avoid the heavy
 * internal creation of a real {@link JsonSchema} instance.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    // ----- shared test fixtures -------------------------------------------------
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    @Mock
    private ObjectMapper jsonMapperMock;
    @Mock
    private ObjectMapper yamlMapperMock;
    @Mock
    private SchemaLoaders.Builder schemaLoadersBuilderMock;
    @Mock
    private SchemaMappers.Builder schemaMappersBuilderMock;
    @Mock
    private JsonMetaSchema jsonMetaSchemaMock;

    private JsonSchemaFactory factory;          // real instance (private ctor via reflection)
    private JsonSchemaFactory factorySpy;       // spy used to stub protected method

    private JsonNode jsonNodeMock;               // input schema node
    private SchemaValidatorsConfig config;       // config passed to the method
    private JsonSchema jsonSchemaMock;            // expected result from newJsonSchema

    // ----- lifecycle -----------------------------------------------------------

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1️⃣ Build the minimal meta‑schema map required by the constructor.
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), jsonMetaSchemaMock);

        // 2️⃣ Obtain the private constructor via reflection.
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                ObjectMapper.class,
                ObjectMapper.class,
                String.class,
                SchemaLoaders.Builder.class,
                SchemaMappers.Builder.class,
                Map.class,
                boolean.class);
        ctor.setAccessible(true);

        // 3️⃣ Create a real factory instance.
        factory = ctor.newInstance(
                jsonMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                schemaLoadersBuilderMock,
                schemaMappersBuilderMock,
                metaSchemas,
                true // enableUriSchemaCache – arbitrary true for the test
        );

        // 4️⃣ Spy the factory so we can stub the protected newJsonSchema method.
        factorySpy = Mockito.spy(factory);

        // 5️⃣ Prepare the method arguments.
        jsonNodeMock = mock(JsonNode.class);
        config = new SchemaValidatorsConfig(); // simple POJO – no mocking needed
        jsonSchemaMock = mock(JsonSchema.class);

        // 6️⃣ Stub the protected method to return the mock schema.
        //    The method signature is: newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)
        //    The first argument is null for the overload we are testing.
        doReturn(jsonSchemaMock)
                .when(factorySpy)
                .newJsonSchema(isNull(), eq(jsonNodeMock), eq(config));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep each test isolated.
        reset(jsonMapperMock, yamlMapperMock, schemaLoadersBuilderMock,
                schemaMappersBuilderMock, jsonMetaSchemaMock,
                jsonNodeMock, jsonSchemaMock);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // ----- actual test ---------------------------------------------------------

    @Test
    void shouldReturnJsonSchemaFromNewJsonSchemaWhenCalledWithJsonNodeAndConfig() {
        // GIVEN – the factory spy is prepared in @BeforeEach

        // WHEN
        JsonSchema result = factorySpy.getSchema(jsonNodeMock, config);

        // THEN
        assertNotNull(result, "Resulting JsonSchema should not be null");
        assertSame(jsonSchemaMock, result, "Factory should return the schema produced by newJsonSchema");

        // Verify that the protected method was invoked exactly once with the expected arguments.
        verify(factorySpy, times(1))
                .newJsonSchema(isNull(), eq(jsonNodeMock), eq(config));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionIfDefaultMetaSchemaUriIsInvalid() throws Exception {
        // GIVEN – an invalid default meta‑schema URI (empty string)
        Map<String, JsonMetaSchema> metaSchemas = new HashMap<>();
        metaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), jsonMetaSchemaMock);

        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
                ObjectMapper.class,
                ObjectMapper.class,
                String.class,
                SchemaLoaders.Builder.class,
                SchemaMappers.Builder.class,
                Map.class,
                boolean.class);
        ctor.setAccessible(true);

        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                ctor.newInstance(
                        jsonMapperMock,
                        yamlMapperMock,
                        "   ",                     // invalid URI
                        schemaLoadersBuilderMock,
                        schemaMappersBuilderMock,
                        metaSchemas,
                        true));
        assertEquals("defaultMetaSchemaURI must not be null or empty", ex.getMessage());
    }
}
```