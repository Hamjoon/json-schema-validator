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
Here is the code of focal method, `getSchema(String, InputFormat, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory_getSchema_15_Test`:
```Java
package com.networknt.schema;
import com.networknt.schema.serialization.JsonMapperFactory;
import com.networknt.schema.serialization.YamlMapperFactory;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.resource.DefaultSchemaLoader;
import java.io.InputStream;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.serialization.JsonNodeReader;
import java.util.concurrent.ConcurrentMap;
import java.net.URI;
import org.slf4j.Logger;
import com.networknt.schema.resource.SchemaLoaders;
import com.networknt.schema.resource.SchemaMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.networknt.schema.resource.SchemaMappers;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
public class JsonSchemaFactory {
    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaFactory.class);
    public JsonSchema getSchema(final String schema, InputFormat inputFormat, final SchemaValidatorsConfig config) {
        try {
            final JsonNode schemaNode = readTree(schema, inputFormat);
            return newJsonSchema(null, schemaNode, config);
        } catch (IOException ioe) {
            logger.error("Failed to load json schema!", ioe);
            throw new JsonSchemaException(ioe);
        }
    }
public static Builder builder();
    public static Builder builder(final JsonSchemaFactory blueprint);
    public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag);
    public JsonSchema create(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema);
    protected SchemaValidatorsConfig createSchemaValidatorsConfig();
    protected ValidationContext createValidationContext(final JsonNode schemaNode, SchemaValidatorsConfig config);
    public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag);
    public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag, Consumer<JsonSchemaFactory.Builder> customizer);
    ObjectMapper getJsonMapper();
    protected JsonSchema getMappedSchema(final SchemaLocation schemaUri, SchemaValidatorsConfig config);
    public JsonMetaSchema getMetaSchema(String iri, SchemaValidatorsConfig config);
    ObjectMapper getObjectMapper(InputFormat inputFormat);
    public JsonSchema getSchema(final JsonNode jsonNode);
    public JsonSchema getSchema(final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final SchemaLocation schemaUri);
    public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode);
    public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final SchemaLocation schemaUri, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final InputStream schemaStream);
    public JsonSchema getSchema(final InputStream schemaStream, InputFormat inputFormat, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final InputStream schemaStream, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final String schema);
    public JsonSchema getSchema(final String schema, InputFormat inputFormat);
    public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final URI schemaUri);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public SchemaLoader getSchemaLoader();
    protected SchemaLocation getSchemaLocation(SchemaLocation schemaLocation);
    ObjectMapper getYamlMapper();
    protected JsonMetaSchema loadMetaSchema(String iri, SchemaValidatorsConfig config);
    protected JsonSchema loadSchema(final SchemaLocation schemaUri, final SchemaValidatorsConfig config);
    protected JsonSchema newJsonSchema(final SchemaLocation schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config);
    protected static String normalizeMetaSchemaUri(String id);
    JsonNode readTree(InputStream content, InputFormat inputFormat) throws IOException;
    JsonNode readTree(String content, InputFormat inputFormat) throws IOException;
}
```


@input{context information}

api document of class com.networknt.schema.JsonSchemaFactory: 

 * Factory for building {@link JsonSchema} instances. The factory should be
 * typically be created using {@link #getInstance(VersionFlag, Consumer)} and
 * should be cached for performance.
 * <p>
 * JsonSchemaFactory instances are thread-safe provided its configuration is not
 * modified.
 

constructors for class `com.networknt.schema.JsonSchemaFactory`: 
params: com.networknt.schema.serialization.JsonNodeReader jsonNodeReader 
com.networknt.schema.resource.SchemaLoaders.Builder schemaLoadersBuilder 
com.networknt.schema.resource.SchemaMappers.Builder schemaMappersBuilder 
com.networknt.schema.JsonMetaSchemaFactory metaSchemaFactory 
body:
```java
    private JsonSchemaFactory(
            ObjectMapper jsonMapper,
            ObjectMapper yamlMapper,
            JsonNodeReader jsonNodeReader,
            String defaultMetaSchemaIri,
            SchemaLoaders.Builder schemaLoadersBuilder,
            SchemaMappers.Builder schemaMappersBuilder,
            ConcurrentMap<String, JsonMetaSchema> metaSchemas,
            boolean enableSchemaCache,
            JsonMetaSchemaFactory metaSchemaFactory) {
        this.metaSchemas = metaSchemas;
        if (defaultMetaSchemaIri == null || defaultMetaSchemaIri.trim().isEmpty()) {
            throw new IllegalArgumentException("defaultMetaSchemaIri must not be null or empty");
        } else if (metaSchemas == null || metaSchemas.isEmpty()) {
            throw new IllegalArgumentException("Json Meta Schemas must not be null or empty");
        } else if (this.metaSchemas.get(normalizeMetaSchemaUri(defaultMetaSchemaIri)) == null) {
            throw new IllegalArgumentException("Meta Schema for default Meta Schema URI must be provided");
        }
        this.jsonMapper = jsonMapper;
        this.yamlMapper = yamlMapper;
        this.jsonNodeReader = jsonNodeReader;
        this.defaultMetaSchemaIri = defaultMetaSchemaIri;
        this.schemaLoadersBuilder = schemaLoadersBuilder;
        this.schemaMappersBuilder = schemaMappersBuilder;
        this.schemaLoader = new DefaultSchemaLoader(
                schemaLoadersBuilder != null ? schemaLoadersBuilder.build() : DEFAULT_SCHEMA_LOADERS,
                schemaMappersBuilder != null ? schemaMappersBuilder.build() : DEFAULT_SCHEMA_MAPPERS);
        this.enableSchemaCache = enableSchemaCache;
        this.metaSchemaFactory = metaSchemaFactory;
    }

```

parameters in constructors and focal method: 
class `com.networknt.schema.serialization.JsonNodeReader`:
api document : 
 * Reader for reading content to {@link JsonNode}.
 
constructor:
```java
```
class `com.networknt.schema.resource.SchemaLoaders.Builder`:
constructor:
```java
        public Builder() {
        }

        public Builder(Builder copy) {
            this.values.addAll(copy.values);
        }

```
class `com.networknt.schema.resource.SchemaMappers.Builder`:
constructor:
```java
        public Builder() {
        }

        public Builder(Builder copy) {
            this.values.addAll(copy.values);
        }

```
class `com.networknt.schema.JsonMetaSchemaFactory`:
api document : 
 * Factory for {@link JsonMetaSchema}.
 
constructor:
```java
```
class `com.networknt.schema.InputFormat`:
api document : 
 * The input data format.
 
constructor:
```java
```
class `com.networknt.schema.SchemaValidatorsConfig`:
api document : 
 * Configuration for validators. 
 
constructor:
```java
    SchemaValidatorsConfig(ApplyDefaultsStrategy applyDefaultsStrategy, boolean cacheRefs,
            String errorMessageKeyword, ExecutionContextCustomizer executionContextCustomizer, boolean failFast,
            Boolean formatAssertionsEnabled, boolean nullableKeywordEnabled,
            List<JsonSchemaWalkListener> itemWalkListeners, boolean javaSemantics,
            Map<String, List<JsonSchemaWalkListener>> keywordWalkListenersMap, Locale locale, boolean losslessNarrowing,
            MessageSource messageSource, boolean discriminatorKeywordEnabled, PathType pathType,
            boolean preloadJsonSchema, int preloadJsonSchemaRefMaxNestingDepth,
            List<JsonSchemaWalkListener> propertyWalkListeners, Boolean readOnly,
            RegularExpressionFactory regularExpressionFactory, JsonSchemaIdValidator schemaIdValidator,
            Map<String, Boolean> strictness, boolean typeLoose, Boolean writeOnly) {
        super();
        this.applyDefaultsStrategy = applyDefaultsStrategy;
        this.cacheRefs = cacheRefs;
        this.errorMessageKeyword = errorMessageKeyword;
        this.executionContextCustomizer = executionContextCustomizer;
        this.failFast = failFast;
        this.formatAssertionsEnabled = formatAssertionsEnabled;
        this.nullableKeywordEnabled = nullableKeywordEnabled;
        this.itemWalkListeners = itemWalkListeners;
        this.javaSemantics = javaSemantics;
        this.keywordWalkListenersMap = keywordWalkListenersMap;
        this.locale = locale;
        this.losslessNarrowing = losslessNarrowing;
        this.messageSource = messageSource;
        this.discriminatorKeywordEnabled = discriminatorKeywordEnabled;
        this.pathType = pathType;
        this.preloadJsonSchema = preloadJsonSchema;
        this.preloadJsonSchemaRefMaxNestingDepth = preloadJsonSchemaRefMaxNestingDepth;
        this.propertyWalkListeners = propertyWalkListeners;
        this.readOnly = readOnly;
        this.regularExpressionFactory = regularExpressionFactory;
        this.schemaIdValidator = schemaIdValidator;
        this.strictness = strictness;
        this.typeLoose = typeLoose;
        this.writeOnly = writeOnly;

        this.itemWalkListenerRunner = new DefaultItemWalkListenerRunner(getArrayItemWalkListeners());
        this.keywordWalkListenerRunner = new DefaultKeywordWalkListenerRunner(getKeywordWalkListenersMap());
        this.propertyWalkListenerRunner = new DefaultPropertyWalkListenerRunner(getPropertyWalkListeners());
    }

```

existing test class: 
```java
/*
 * Copyright (c) 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for JsonSchemaFactory.
 */
class JsonSchemaFactoryTest {
    @Test
    void concurrency() {
        String metaSchemaData = "{\r\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\r\n"
                + "  \"$id\": \"https://www.example.com/no-validation-no-format/schema\",\r\n"
                + "  \"$vocabulary\": {\r\n"
                + "    \"https://www.example.com/vocab/validation\": false,\r\n"
                + "    \"https://json-schema.org/draft/2020-12/vocab/applicator\": true,\r\n"
                + "    \"https://json-schema.org/draft/2020-12/vocab/core\": true\r\n"
                + "  },\r\n"
                + "  \"allOf\": [\r\n"
                + "    { \"$ref\": \"https://json-schema.org/draft/2020-12/meta/applicator\" },\r\n"
                + "    { \"$ref\": \"https://json-schema.org/draft/2020-12/meta/core\" }\r\n"
                + "  ]\r\n"
                + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections
                .singletonMap("https://www.example.com/no-validation-no-format/schema",
                        metaSchemaData))));
        AtomicBoolean failed = new AtomicBoolean(false);
        JsonMetaSchema[] instance = new JsonMetaSchema[1];;
        CountDownLatch latch = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 50; ++i) {
            Runnable runner = new Runnable() {
                public void run() {
                    SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    JsonMetaSchema metaSchema = factory.getMetaSchema("https://www.example.com/no-validation-no-format/schema", config);
                    synchronized(instance) {
                        if (instance[0] == null) {
                            instance[0] = metaSchema;
                        }
                        // Ensure references are the same despite concurrency
                        if (!(instance[0] == metaSchema)) {
                            failed.set(true);
                        }
                    }
                }
            };
            Thread thread = new Thread(runner, "Thread" + i);
            thread.start();
            threads.add(thread);
        }
        latch.countDown(); // Release the latch for threads to run concurrently
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        assertFalse(failed.get());
    }
}

```

invoke examples of focal method: 
example 1:
```java
    public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config) {
        return getSchema(schema, InputFormat.JSON, config);
    }
```
example 2:
```java
    public JsonSchema getSchema(final String schema, InputFormat inputFormat) {
        return getSchema(schema, inputFormat, createSchemaValidatorsConfig());
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
class JsonSchemaFactory_getSchema_15_Test {
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