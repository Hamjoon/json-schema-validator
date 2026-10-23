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
Here is the code of focal method, `newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory_newJsonSchema_Test`:
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
    
    protected JsonSchema newJsonSchema(final URI schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config) {
        final ValidationContext validationContext = createValidationContext(schemaNode);
        validationContext.setConfig(config);
        return doCreate(validationContext, "#", schemaUri, schemaNode, null, false);
    }
public static Builder builder();
    public static Builder builder(final JsonSchemaFactory blueprint);
    public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag);
    public JsonSchema create(ValidationContext validationContext, String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
    protected ValidationContext createValidationContext(final JsonNode schemaNode);
    private JsonSchema doCreate(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parentSchema, boolean suppressSubSchemaRetrieval);
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
class `com.networknt.schema.SchemaValidatorsConfig`:
constructor:
```java
    public SchemaValidatorsConfig() {
    }

```

invoke examples of focal method: 
example 1:
```java
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config) {
        return newJsonSchema(schemaUri, jsonNode, config);
    }
```
example 2:
```java
    public JsonSchema getSchema(final JsonNode jsonNode, final SchemaValidatorsConfig config) {
        return newJsonSchema(null, jsonNode, config);
    }
```
example 3:
```java
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode) {
        return newJsonSchema(schemaUri, jsonNode, null);
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
class JsonSchemaFactory_newJsonSchema_Test {
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