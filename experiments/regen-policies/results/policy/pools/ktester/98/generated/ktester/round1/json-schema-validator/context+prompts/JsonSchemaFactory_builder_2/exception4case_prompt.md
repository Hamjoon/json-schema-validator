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
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    private final JsonNodeReader jsonNodeReader;
    private final String defaultMetaSchemaIri;
    private final SchemaLoaders.Builder schemaLoadersBuilder;
    private final SchemaMappers.Builder schemaMappersBuilder;
    private final ConcurrentMap<String, JsonMetaSchema> metaSchemas;
        public static class Builder {
        private ObjectMapper jsonMapper = null;
    
        private ObjectMapper yamlMapper = null;
    
        private JsonNodeReader jsonNodeReader = null;
    
        private String defaultMetaSchemaIri;
    
        private final ConcurrentMap<String, JsonMetaSchema> metaSchemas = new ConcurrentHashMap<String, JsonMetaSchema>();
    
        private SchemaLoaders.Builder schemaLoadersBuilder = null;
    
        private SchemaMappers.Builder schemaMappersBuilder = null;
    
        private boolean enableSchemaCache = true;
    
        private JsonMetaSchemaFactory metaSchemaFactory = null;
    
        /**
         * Sets the json node reader to read the data.
         * <p>
         * If set this takes precedence over the configured json mapper and yaml mapper.
         * <p>
         * A location aware object reader can be created using JsonNodeReader.builder().locationAware().build().
         *
         * @param jsonNodeReader
         * 		the object reader
         * @return the builder
         */
        public Builder jsonNodeReader(JsonNodeReader jsonNodeReader) {
            this.jsonNodeReader = jsonNodeReader;
            return this;
        }
    
        /**
         * Sets the json mapper to read the data.
         * <p>
         * If the object reader is set this will not be used.
         * <p>
         * This is deprecated use a object reader instead.
         *
         * @param jsonMapper
         * 		the json mapper
         * @return the builder
         */
        @Deprecated
        public Builder jsonMapper(final ObjectMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
            return this;
        }
    
        /**
         * Sets the yaml mapper to read the data.
         * <p>
         * If the object reader is set this will not be used.
         * <p>
         * This is deprecated use a object reader instead.
         *
         * @param yamlMapper
         * 		the yaml mapper
         * @return the builder
         */
        @Deprecated
        public Builder yamlMapper(final ObjectMapper yamlMapper) {
            this.yamlMapper = yamlMapper;
            return this;
        }
    
        public Builder defaultMetaSchemaIri(final String defaultMetaSchemaIri) {
            this.defaultMetaSchemaIri = defaultMetaSchemaIri;
            return this;
        }
    
        public Builder metaSchemaFactory(final JsonMetaSchemaFactory jsonMetaSchemaFactory) {
            this.metaSchemaFactory = jsonMetaSchemaFactory;
            return this;
        }
    
        public Builder metaSchema(final JsonMetaSchema jsonMetaSchema) {
            this.metaSchemas.put(normalizeMetaSchemaUri(jsonMetaSchema.getIri()), jsonMetaSchema);
            return this;
        }
    
        public Builder metaSchemas(final Collection<? extends JsonMetaSchema> jsonMetaSchemas) {
            for (JsonMetaSchema jsonMetaSchema : jsonMetaSchemas) {
                metaSchema(jsonMetaSchema);
            }
            return this;
        }
    
        public Builder metaSchemas(Consumer<Map<String, JsonMetaSchema>> customizer) {
            customizer.accept(this.metaSchemas);
            return this;
        }
    
        public Builder enableSchemaCache(boolean enableSchemaCache) {
            this.enableSchemaCache = enableSchemaCache;
            return this;
        }
    
        public Builder schemaLoaders(Consumer<SchemaLoaders.Builder> schemaLoadersBuilderCustomizer) {
            if (this.schemaLoadersBuilder == null) {
                this.schemaLoadersBuilder = SchemaLoaders.builder();
            }
            schemaLoadersBuilderCustomizer.accept(this.schemaLoadersBuilder);
            return this;
        }
    
        public Builder schemaMappers(Consumer<SchemaMappers.Builder> schemaMappersBuilderCustomizer) {
            if (this.schemaMappersBuilder == null) {
                this.schemaMappersBuilder = SchemaMappers.builder();
            }
            schemaMappersBuilderCustomizer.accept(this.schemaMappersBuilder);
            return this;
        }
    
        @Deprecated
        public Builder addMetaSchema(final JsonMetaSchema jsonMetaSchema) {
            return metaSchema(jsonMetaSchema);
        }
    
        @Deprecated
        public Builder addMetaSchemas(final Collection<? extends JsonMetaSchema> jsonMetaSchemas) {
            return metaSchemas(jsonMetaSchemas);
        }
    
        public JsonSchemaFactory build() {
            return new JsonSchemaFactory(jsonMapper, yamlMapper, jsonNodeReader, defaultMetaSchemaIri, schemaLoadersBuilder, schemaMappersBuilder, metaSchemas, enableSchemaCache, metaSchemaFactory);
        }
    }
public static Builder builder(final JsonSchemaFactory blueprint) {
        Builder builder = builder().metaSchemas(blueprint.metaSchemas.values()).defaultMetaSchemaIri(blueprint.defaultMetaSchemaIri).jsonMapper(blueprint.jsonMapper).yamlMapper(blueprint.yamlMapper).jsonNodeReader(blueprint.jsonNodeReader);
        if (blueprint.schemaLoadersBuilder != null) {
            builder.schemaLoadersBuilder = SchemaLoaders.builder().with(blueprint.schemaLoadersBuilder);
        }
        if (blueprint.schemaMappersBuilder != null) {
            builder.schemaMappersBuilder = SchemaMappers.builder().with(blueprint.schemaMappersBuilder);
        }
        return builder;
    }
public static Builder builder();
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
    public JsonSchema getSchema(final String schema, InputFormat inputFormat, final SchemaValidatorsConfig config);
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
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: