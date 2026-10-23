```java
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

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_8_Test {

    // Mocks for constructor dependencies
    @Mock ObjectMapper jsonMapper;
    @Mock YAMLMapper yamlMapper;
    @Mock URISchemeFactory uriFactory;
    @Mock URISchemeFetcher uriFetcher;
    @Mock URNFactory urnFactory;
    @Mock URITranslator.CompositeURITranslator uriTranslators;

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
            java.lang.reflect.Constructor<JsonSchemaFactory> ctor =
                    JsonSchemaFactory.class.getDeclaredConstructor(
                            ObjectMapper.class,
                            YAMLMapper.class,
                            String.class,
                            URISchemeFactory.class,
                            URISchemeFetcher.class,
                            URNFactory.class,
                            Map.class,
                            Map.class,
                            boolean.class,
                            URITranslator.CompositeURITranslator.class);
            ctor.setAccessible(true);
            JsonSchemaFactory realFactory = ctor.newInstance(
                    jsonMapper,
                    yamlMapper,
                    DEFAULT_META_SCHEMA_URI,
                    uriFactory,
                    uriFetcher,
                    urnFactory,
                    jsonMetaSchemas,
                    uriMap,
                    false,
                    uriTranslators);

            // Spy the instance so we can stub the protected newJsonSchema method
            factorySpy = spy(realFactory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up for this test
    }

    @Test
    void getSchema_withUriAndJsonNode_returnsJsonSchema() throws Exception {
        // GIVEN
        URI schemaUri = new URI("http://example.com/my-schema");
        JsonNode schemaNode = mock(JsonNode.class);
        JsonSchema expectedSchema = mock(JsonSchema.class);

        // Stub the protected newJsonSchema method to return our mock
        doReturn(expectedSchema).when(factorySpy)
                .newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());

        // WHEN
        JsonSchema actual = factorySpy.getSchema(schemaUri, schemaNode);

        // THEN
        assertNotNull(actual, "The returned JsonSchema should not be null");
        assertSame(expectedSchema, actual, "The factory should return the JsonSchema produced by newJsonSchema");
        // Verify that newJsonSchema was invoked exactly once with the expected arguments
        verify(factorySpy, times(1)).newJsonSchema(eq(schemaUri), eq(schemaNode), isNull());
    }
}
```