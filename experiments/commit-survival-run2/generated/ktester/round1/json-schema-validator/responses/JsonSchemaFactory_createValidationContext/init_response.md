```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
    private YAMLMapper yamlMapperMock;

    /*** Instance under test ***/
    private JsonSchemaFactory factory;

    /*** Constants used for construction ***/
    private static final String DEFAULT_META_SCHEMA_URI = "http://json-schema.org/draft-07/schema#";

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for this test class.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * The constructor of JsonSchemaFactory is package‑private (actually private) and expects a
         * fairly large set of arguments.  We create a minimal but valid set of arguments and use
         * reflection to instantiate the factory.
         *
         * The only argument that is consulted by {@code createValidationContext} is the map of
         * {@link JsonMetaSchema}s – it is used by the private {@code findMetaSchemaForSchema}
         * method when the supplied schema does not declare its own $schema property.  Therefore we
         * provide a map that contains an entry for the default meta‑schema URI and associate it
         * with a mock {@link JsonMetaSchema}.
         */
        JsonMetaSchema metaSchemaMock = mock(JsonMetaSchema.class);

        Map<String, JsonMetaSchema> jsonMetaSchemas = new HashMap<>();
        jsonMetaSchemas.put(JsonSchemaFactory.normalizeMetaSchemaUri(DEFAULT_META_SCHEMA_URI), metaSchemaMock);

        // Empty URI‑map – not needed for this test.
        Map<String, String> uriMap = Collections.emptyMap();

        // The CompositeURITranslator can be instantiated directly – it has a no‑arg constructor.
        URITranslator.CompositeURITranslator compositeTranslator = new URITranslator.CompositeURITranslator();

        // Obtain the (private) constructor via reflection.
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(
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
        factory = ctor.newInstance(
                objectMapperMock,
                yamlMapperMock,
                DEFAULT_META_SCHEMA_URI,
                uriFactoryMock,
                uriFetcherMock,
                urnFactoryMock,
                jsonMetaSchemas,
                uriMap,
                false,                     // enableUriSchemaCache
                compositeTranslator);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void createValidationContext_returnsNonNullContext() {
        // GIVEN – a simple (empty) JSON schema node.  An empty node triggers the factory to fall back
        // to the default meta‑schema supplied during construction.
        JsonNode emptySchemaNode = JsonNodeFactory.instance.objectNode();

        // WHEN – the method under test is invoked.
        ValidationContext ctx = factory.createValidationContext(emptySchemaNode);

        // THEN – a non‑null ValidationContext is returned.
        assertNotNull(ctx, "createValidationContext should never return null");

        // Additional sanity checks (optional, depending on the public API of ValidationContext).
        // We verify that the context holds the same JsonMetaSchema instance we injected.
        // ValidationContext provides a getter for the meta‑schema in the actual library.
        // If the getter name differs, adjust accordingly.
        assertSame(
                factory.getJsonMetaSchema(DEFAULT_META_SCHEMA_URI),
                ctx.getJsonMetaSchema(),
                "The ValidationContext should contain the meta‑schema associated with the default URI"
        );
    }
}
```