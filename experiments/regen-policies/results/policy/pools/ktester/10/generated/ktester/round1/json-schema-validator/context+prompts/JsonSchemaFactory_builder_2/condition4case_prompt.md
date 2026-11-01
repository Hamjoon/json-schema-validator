As a professional Java software testing expert, your task is to generate test cases for a focal method.

You will be provided with:
1. The focal method that needs testing.
2. Basic information of the target class to which the focus method belongs, including fields and methods.

Ensure the generated test cases meet the following requirements:

Test Coverage Requirements:
- Create test cases to cover all conditional branches in the target method, including `if`, `else if`, `else`, `switch`, and `case` statements.

Output Format:
- Organize the test cases by its functions logically.
- Include input parameters, expected output, and a description for each test case.
- Use empty list `[]` for the input when a test case does not require input parameters.
- Provide an expected return value for each test case, or specify the behavior that should be verified if there's no return value.
- Clearly describe the intention of each test case in the description.

Follow the JSON format provided here for the output:
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
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ConcurrentMap;
import java.net.URI;
import org.slf4j.Logger;
import com.networknt.schema.resource.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
public class JsonSchemaFactory {
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    private final String defaultMetaSchemaURI;
    private final SchemaLoaders.Builder schemaLoadersBuilder;
    private final SchemaMappers.Builder schemaMappersBuilder;
    private final Map<String, JsonMetaSchema> jsonMetaSchemas;
        public static class Builder {
        private ObjectMapper jsonMapper = null;
    
        private ObjectMapper yamlMapper = null;
    
        private String defaultMetaSchemaURI;
    
        private final ConcurrentMap<String, JsonMetaSchema> jsonMetaSchemas = new ConcurrentHashMap<String, JsonMetaSchema>();
    
        private SchemaLoaders.Builder schemaLoadersBuilder = null;
    
        private SchemaMappers.Builder schemaMappersBuilder = null;
    
        private boolean enableUriSchemaCache = true;
    
        public Builder jsonMapper(final ObjectMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
            return this;
        }
    
        public Builder yamlMapper(final ObjectMapper yamlMapper) {
            this.yamlMapper = yamlMapper;
            return this;
        }
    
        public Builder defaultMetaSchemaURI(final String defaultMetaSchemaURI) {
            this.defaultMetaSchemaURI = defaultMetaSchemaURI;
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
    
        public Builder enableUriSchemaCache(boolean enableUriSchemaCache) {
            this.enableUriSchemaCache = enableUriSchemaCache;
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
    
        public JsonSchemaFactory build() {
            // create builtin keywords with (custom) formats.
            return new JsonSchemaFactory(jsonMapper, yamlMapper, defaultMetaSchemaURI, schemaLoadersBuilder, schemaMappersBuilder, jsonMetaSchemas, enableUriSchemaCache);
        }
    }
public static Builder builder(final JsonSchemaFactory blueprint) {
        Builder builder = builder().addMetaSchemas(blueprint.jsonMetaSchemas.values()).defaultMetaSchemaURI(blueprint.defaultMetaSchemaURI).jsonMapper(blueprint.jsonMapper).yamlMapper(blueprint.yamlMapper);
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
    protected ObjectMapper getJsonMapper();
    protected JsonSchema getMappedSchema(final SchemaLocation schemaUri, SchemaValidatorsConfig config);
    public JsonMetaSchema getMetaSchema(String id, SchemaValidatorsConfig config);
    public JsonSchema getSchema(final JsonNode jsonNode);
    public JsonSchema getSchema(final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final SchemaLocation schemaUri);
    public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode);
    public JsonSchema getSchema(final SchemaLocation schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final SchemaLocation schemaUri, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final InputStream schemaStream);
    public JsonSchema getSchema(final InputStream schemaStream, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final String schema);
    public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final URI schemaUri);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public SchemaLoader getSchemaLoader();
    protected SchemaLocation getSchemaLocation(SchemaLocation schemaRetrievalUri, JsonNode schemaNode, ValidationContext validationContext);
    protected ObjectMapper getYamlMapper();
    protected JsonMetaSchema loadMetaSchema(String id, SchemaValidatorsConfig config);
    protected JsonSchema newJsonSchema(final SchemaLocation schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config);
    protected static String normalizeMetaSchemaUri(String id);
}
```

@output{test cases}: output the test cases in the JSON format: