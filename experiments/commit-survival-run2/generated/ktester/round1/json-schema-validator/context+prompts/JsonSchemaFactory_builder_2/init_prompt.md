You are tasked with creating a framework for a test class in Java. Now analyze the following information carefully:

1. The method that needs to be tested and its parameters.
2. The constructor of the target class and its parameters.
3. Any dependencies or external resources used by the target class.

Based on this analysis, complete the test class template in the provided structure:

1. Initialize necessary fields and dependencies within setupBeforeAll() and setupBeforeEach().
2. Handle any required cleanup in teardownAfterEach() and teardownAfterAll().
3. Make sure not to invoke any private methods or fields of the target class and dependent class.
4. Refer to the "given" phase in the existing test class for guidance.
5. Add necessary import statements and ensure there are no syntax errors in the generated code.

@input{focal method and target class}
Here is the code of focal method, `builder(JsonSchemaFactory)`, with the focal class `JsonSchemaFactory_builder_2_Test`:
```Java
package com.networknt.schema;
import com.networknt.schema.uri.URITranslator.CompositeURITranslator;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ConcurrentMap;
import java.net.URI;
import org.slf4j.Logger;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.urn.URNFactory;
import java.util.Collection;
import java.util.Map;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import com.networknt.schema.uri.*;
import com.fasterxml.jackson.databind.ObjectMapper;
public class JsonSchemaFactory {
    private final ObjectMapper jsonMapper;
    private final YAMLMapper yamlMapper;
    private final String defaultMetaSchemaURI;
    private final URISchemeFactory uriFactory;
    private final URISchemeFetcher uriFetcher;
    private final CompositeURITranslator uriTranslators;
    private final Map<String, JsonMetaSchema> jsonMetaSchemas;
    private final Map<String, String> uriMap;
        public static class Builder {
        private ObjectMapper objectMapper = new ObjectMapper();
    
        private YAMLMapper yamlMapper = new YAMLMapper();
    
        private String defaultMetaSchemaURI;
    
        private final Map<String, URIFactory> uriFactoryMap = new HashMap<String, URIFactory>();
    
        private final Map<String, URIFetcher> uriFetcherMap = new HashMap<String, URIFetcher>();
    
        private URNFactory urnFactory;
    
        private final ConcurrentMap<String, JsonMetaSchema> jsonMetaSchemas = new ConcurrentHashMap<String, JsonMetaSchema>();
    
        private final Map<String, String> uriMap = new HashMap<String, String>();
    
        private boolean enableUriSchemaCache = true;
    
        private final CompositeURITranslator uriTranslators = new CompositeURITranslator();
    
        public Builder() {
            // Adds support for creating {@link URL}s.
            final URIFactory urlFactory = new URLFactory();
            for (final String scheme : URLFactory.SUPPORTED_SCHEMES) {
                this.uriFactoryMap.put(scheme, urlFactory);
            }
            // Adds support for creating URNs.
            this.uriFactoryMap.put(URNURIFactory.SCHEME, new URNURIFactory());
            // Adds support for fetching with {@link URL}s.
            final URIFetcher urlFetcher = new URLFetcher();
            for (final String scheme : URLFetcher.SUPPORTED_SCHEMES) {
                this.uriFetcherMap.put(scheme, urlFetcher);
            }
            // Adds support for creating and fetching with classpath {@link URL}s.
            final URIFactory classpathURLFactory = new ClasspathURLFactory();
            final URIFetcher classpathURLFetcher = new ClasspathURLFetcher();
            for (final String scheme : ClasspathURLFactory.SUPPORTED_SCHEMES) {
                this.uriFactoryMap.put(scheme, classpathURLFactory);
                this.uriFetcherMap.put(scheme, classpathURLFetcher);
            }
        }
    
        public Builder objectMapper(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }
    
        public Builder yamlMapper(final YAMLMapper yamlMapper) {
            this.yamlMapper = yamlMapper;
            return this;
        }
    
        public Builder defaultMetaSchemaURI(final String defaultMetaSchemaURI) {
            this.defaultMetaSchemaURI = defaultMetaSchemaURI;
            return this;
        }
    
        /**
         * Maps a number of schemes to a {@link URIFactory}.
         *
         * @param uriFactory
         * 		the uri factory that will be used for the given schemes.
         * @param schemes
         * 		the scheme that the uri factory will be assocaited with.
         * @return this builder.
         */
        public Builder uriFactory(final URIFactory uriFactory, final String... schemes) {
            return uriFactory(uriFactory, Arrays.asList(schemes));
        }
    
        public Builder uriFactory(final URIFactory uriFactory, final Iterable<String> schemes) {
            for (final String scheme : schemes) {
                this.uriFactoryMap.put(scheme, uriFactory);
            }
            return this;
        }
    
        /**
         * Maps a number of schemes to a {@link URIFetcher}.
         *
         * @param uriFetcher
         * 		the uri fetcher that will be used for the given schemes.
         * @param schemes
         * 		the scheme that the uri fetcher will be assocaited with.
         * @return this builder.
         */
        public Builder uriFetcher(final URIFetcher uriFetcher, final String... schemes) {
            return uriFetcher(uriFetcher, Arrays.asList(schemes));
        }
    
        public Builder uriFetcher(final URIFetcher uriFetcher, final Iterable<String> schemes) {
            for (final String scheme : schemes) {
                this.uriFetcherMap.put(scheme, uriFetcher);
            }
            return this;
        }
    
        public Builder addMetaSchema(final JsonMetaSchema jsonMetaSchema) {
            this.jsonMetaSchemas.put(normalizeMetaSchemaUri(jsonMetaSchema.getUri()), jsonMetaSchema);
            return this;
        }
    
        public Builder addMetaSchemas(final Collection<? extends JsonMetaSchema> jsonMetaSchemas) {
            for (JsonMetaSchema jsonMetaSchema : jsonMetaSchemas) {
                addMetaSchema(jsonMetaSchema);
            }
            return this;
        }
    
        /**
         *
         * @deprecated Use {@code addUriTranslator} instead.
         * @param map
         * 		the map of uri mappings.
         * @return this builder.
         */
        @Deprecated
        public Builder addUriMappings(final Map<String, String> map) {
            this.uriMap.putAll(map);
            return this;
        }
    
        public Builder addUriTranslator(URITranslator translator) {
            if (null != translator) {
                this.uriTranslators.add(translator);
            }
            return this;
        }
    
        public Builder addUrnFactory(URNFactory urnFactory) {
            this.urnFactory = urnFactory;
            return this;
        }
    
        /**
         *
         * @deprecated No longer necessary.
         * @param forceHttps
         * 		ignored.
         * @return this builder.
         */
        public Builder forceHttps(boolean forceHttps) {
            return this;
        }
    
        /**
         *
         * @deprecated No longer necessary.
         * @param removeEmptyFragmentSuffix
         * 		ignored.
         * @return this builder.
         */
        public Builder removeEmptyFragmentSuffix(boolean removeEmptyFragmentSuffix) {
            return this;
        }
    
        public Builder enableUriSchemaCache(boolean enableUriSchemaCache) {
            this.enableUriSchemaCache = enableUriSchemaCache;
            return this;
        }
    
        public JsonSchemaFactory build() {
            // create builtin keywords with (custom) formats.
            return new JsonSchemaFactory(objectMapper == null ? new ObjectMapper() : objectMapper, yamlMapper == null ? new YAMLMapper() : yamlMapper, defaultMetaSchemaURI, new URISchemeFactory(uriFactoryMap), new URISchemeFetcher(uriFetcherMap), urnFactory, jsonMetaSchemas, uriMap, enableUriSchemaCache, uriTranslators);
        }
    }
public static Builder builder(final JsonSchemaFactory blueprint) {
        Builder builder = builder().addMetaSchemas(blueprint.jsonMetaSchemas.values()).defaultMetaSchemaURI(blueprint.defaultMetaSchemaURI).objectMapper(blueprint.jsonMapper).yamlMapper(blueprint.yamlMapper).addUriMappings(blueprint.uriMap);
        for (URITranslator translator : blueprint.uriTranslators) {
            builder = builder.addUriTranslator(translator);
        }
        for (Map.Entry<String, URIFactory> entry : blueprint.uriFactory.getURIFactories().entrySet()) {
            builder = builder.uriFactory(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<String, URIFetcher> entry : blueprint.uriFetcher.getURIFetchers().entrySet()) {
            builder = builder.uriFetcher(entry.getValue(), entry.getKey());
        }
        return builder;
    }
public static Builder builder();
    public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag);
    public JsonSchema create(ValidationContext validationContext, String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
    protected ValidationContext createValidationContext(final JsonNode schemaNode);
    public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag);
    public JsonSchema getSchema(final JsonNode jsonNode);
    public JsonSchema getSchema(final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final InputStream schemaStream);
    public JsonSchema getSchema(final InputStream schemaStream, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final String schema);
    public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final URI schemaUri);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final URI schemaUri, final SchemaValidatorsConfig config);
    public URIFactory getUriFactory();
    public URITranslator getUriTranslator();
    protected JsonSchema newJsonSchema(final URI schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config);
    protected static String normalizeMetaSchemaUri(String u);
}
```


@input{context information}

constructors for class `com.networknt.schema.JsonSchemaFactory`: 
params: com.networknt.schema.uri.URISchemeFactory uriFactory 
com.networknt.schema.uri.URISchemeFetcher uriFetcher 
com.networknt.schema.urn.URNFactory urnFactory 
com.networknt.schema.uri.URITranslator.CompositeURITranslator uriTranslators 
body:
```java
    private JsonSchemaFactory(
            final ObjectMapper jsonMapper,
            final YAMLMapper yamlMapper,
            final String defaultMetaSchemaURI,
            final URISchemeFactory uriFactory,
            final URISchemeFetcher uriFetcher,
            final URNFactory urnFactory,
            final Map<String, JsonMetaSchema> jsonMetaSchemas,
            final Map<String, String> uriMap,
            final boolean enableUriSchemaCache,
            final CompositeURITranslator uriTranslators) {
        if (jsonMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        } else if (yamlMapper == null) {
            throw new IllegalArgumentException("YAMLMapper must not be null");
        } else if (defaultMetaSchemaURI == null || defaultMetaSchemaURI.trim().isEmpty()) {
            throw new IllegalArgumentException("defaultMetaSchemaURI must not be null or empty");
        } else if (uriFactory == null) {
            throw new IllegalArgumentException("URIFactory must not be null");
        } else if (uriFetcher == null) {
            throw new IllegalArgumentException("URIFetcher must not be null");
        } else if (jsonMetaSchemas == null || jsonMetaSchemas.isEmpty()) {
            throw new IllegalArgumentException("Json Meta Schemas must not be null or empty");
        } else if (jsonMetaSchemas.get(normalizeMetaSchemaUri(defaultMetaSchemaURI)) == null) {
            throw new IllegalArgumentException("Meta Schema for default Meta Schema URI must be provided");
        } else if (uriMap == null) {
            throw new IllegalArgumentException("URL Mappings must not be null");
        } else if (uriTranslators == null) {
            throw new IllegalArgumentException("URI Translators must not be null");
        }
        this.jsonMapper = jsonMapper;
        this.yamlMapper = yamlMapper;
        this.defaultMetaSchemaURI = defaultMetaSchemaURI;
        this.uriFactory = uriFactory;
        this.uriFetcher = uriFetcher;
        this.urnFactory = urnFactory;
        this.jsonMetaSchemas = jsonMetaSchemas;
        this.uriMap = uriMap;
        this.enableUriSchemaCache = enableUriSchemaCache;
        this.uriTranslators = uriTranslators;
    }

```

parameters in constructors and focal method: 
class `com.networknt.schema.uri.URISchemeFactory`:
api document : 
 * The URISchemaFactory will proxy to other {@link URIFactory}s based on the scheme being used.
 
constructor:
```java
    public URISchemeFactory(final Map<String, URIFactory> uriFactories) {
        if (uriFactories == null) {
            throw new IllegalArgumentException("URIFactory map must not be null");
        }
        this.uriFactories = uriFactories;
    }

```
class `com.networknt.schema.uri.URISchemeFetcher`:
api document : 
 * The URISchemeFetcher will proxy to other {@link URIFetcher}s based on the scheme being used.
 
constructor:
```java
    public URISchemeFetcher(final Map<String, URIFetcher> uriFetchers) {
        if (uriFetchers == null) {
            throw new IllegalArgumentException("URIFetcher map must not be null");
        }
        this.uriFetchers = uriFetchers;
    }

```
class `com.networknt.schema.urn.URNFactory`:
constructor:
```java
```
class `com.networknt.schema.uri.URITranslator.CompositeURITranslator`:
constructor:
```java
        public CompositeURITranslator() {
            super();
        }

        public CompositeURITranslator(URITranslator...translators) {
            this(Arrays.asList(translators));
        }

        public CompositeURITranslator(Collection<? extends URITranslator> c) {
            super(c);
        }

```
class `com.networknt.schema.JsonSchemaFactory`:
constructor:
```java
    private JsonSchemaFactory(
            final ObjectMapper jsonMapper,
            final YAMLMapper yamlMapper,
            final String defaultMetaSchemaURI,
            final URISchemeFactory uriFactory,
            final URISchemeFetcher uriFetcher,
            final URNFactory urnFactory,
            final Map<String, JsonMetaSchema> jsonMetaSchemas,
            final Map<String, String> uriMap,
            final boolean enableUriSchemaCache,
            final CompositeURITranslator uriTranslators) {
        if (jsonMapper == null) {
            throw new IllegalArgumentException("ObjectMapper must not be null");
        } else if (yamlMapper == null) {
            throw new IllegalArgumentException("YAMLMapper must not be null");
        } else if (defaultMetaSchemaURI == null || defaultMetaSchemaURI.trim().isEmpty()) {
            throw new IllegalArgumentException("defaultMetaSchemaURI must not be null or empty");
        } else if (uriFactory == null) {
            throw new IllegalArgumentException("URIFactory must not be null");
        } else if (uriFetcher == null) {
            throw new IllegalArgumentException("URIFetcher must not be null");
        } else if (jsonMetaSchemas == null || jsonMetaSchemas.isEmpty()) {
            throw new IllegalArgumentException("Json Meta Schemas must not be null or empty");
        } else if (jsonMetaSchemas.get(normalizeMetaSchemaUri(defaultMetaSchemaURI)) == null) {
            throw new IllegalArgumentException("Meta Schema for default Meta Schema URI must be provided");
        } else if (uriMap == null) {
            throw new IllegalArgumentException("URL Mappings must not be null");
        } else if (uriTranslators == null) {
            throw new IllegalArgumentException("URI Translators must not be null");
        }
        this.jsonMapper = jsonMapper;
        this.yamlMapper = yamlMapper;
        this.defaultMetaSchemaURI = defaultMetaSchemaURI;
        this.uriFactory = uriFactory;
        this.uriFetcher = uriFetcher;
        this.urnFactory = urnFactory;
        this.jsonMetaSchemas = jsonMetaSchemas;
        this.uriMap = uriMap;
        this.enableUriSchemaCache = enableUriSchemaCache;
        this.uriTranslators = uriTranslators;
    }

```



@input{test class template}
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_2_Test {
    @BeforeAll
    static void setupBeforeAll() {
    }

    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
    }
}
```

@output{initial test class}: now consider the invoke examples output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.