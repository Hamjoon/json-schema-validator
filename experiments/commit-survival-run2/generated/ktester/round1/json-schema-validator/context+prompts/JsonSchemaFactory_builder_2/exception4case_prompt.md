You are a professional software tester. Write test cases for a focal method in a target class.

**Instructions:**
1. Analyze method behavior and identify ALL possible exception scenarios
2. Design test cases that trigger each identified exception
3. Follow existing test case structure, Avoid duplication and create new test cases for uncovered exception scenarios

Test Case Format:
1. Group test cases by test function name
2. Each test case MUST include:
   - Input parameters with values (use empty list `[]` if none)
   - Expected exception or observable behavior
   - Clear description of test scenario
3. Return empty array if no exception scenarios found

Output Schema (STRICT JSON format):
```json
[
    {
        "group": "test name",
        "cases": [
            {
                "input": [
                    {
                        "parameter": "param name",
                        "value": "param value"
                    }
                ],
                "expected": "expected exception or behavior",
                "description": "test scenario description"
            }
        ]
    },
    {...}
]
```

@input{focal method and target class}
Here is the code of focal method, `builder(JsonSchemaFactory)`, with the focal class `JsonSchemaFactory`:
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
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: