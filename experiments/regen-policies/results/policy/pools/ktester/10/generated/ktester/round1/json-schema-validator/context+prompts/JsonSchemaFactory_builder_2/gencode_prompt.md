You are a professional Java software testing expert. Your task is to generate high-quality unit test code for a focal method, using **JUnit5** and **Mockito** libraries. 

You'll be provided with:
1. The focal method to test.
2. The target class's structure (fields and methods).
3. A pre-defined test class framework with `@Before`/`@After` methods.
4. Test cases in JSON format.
5. Any relevant context.

**Requirements:**

Code Quality:
- Include clear comments explaining each test scenario.
- Ensure code is clean, understandable, and maintainable.
- Do not directly access private methods or fields. **Use reflection if the focal method is private.**
- Include all necessary imports; code must be free of syntax errors.

**Test Structure:**
- Follow the given test class framework; do not modify `@Before`/`@After` methods.
- Create one test function per test case group, ensuring all cases are covered.
- Use parameterized tests where appropriate.
- Reinitialize variables inside test functions if `@Setup` values don't fit.
- Properly mock and set up dependencies.

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

@input{initial test class framework}
```Java
<initial_class>
```

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```


@input{context information}

api document of method builder(JsonSchemaFactory): 

     * Builder from an existing {@link JsonSchemaFactory}.
     * <p>
     * <code>
     * JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909));
     * </code>
     * 
     * @param blueprint the existing factory
     * @return the builder
     

return type: 
com.networknt.schema.JsonSchemaFactory.Builder

dependent classes: 
class `com.networknt.schema.resource.SchemaLoaders`:
	
class `com.networknt.schema.resource.SchemaMappers`:
	
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		com.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;
		com.networknt.schema.resource.SchemaLoaders.Builder com.networknt.schema.JsonSchemaFactory.schemaLoadersBuilder;
		com.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;
		java.lang.String com.networknt.schema.JsonSchemaFactory.defaultMetaSchemaURI;
		com.networknt.schema.resource.SchemaMappers.Builder com.networknt.schema.JsonSchemaFactory.schemaMappersBuilder;
		java.util.Map<java.lang.String, com.networknt.schema.JsonMetaSchema> com.networknt.schema.JsonSchemaFactory.jsonMetaSchemas;
	related functions:
		method `public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag, Consumer<JsonSchemaFactory.Builder>)` returns `com.networknt.schema.JsonSchemaFactory`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`, api document: 
     * Creates a factory with a default schema dialect. The schema dialect will only
     * be used if the input does not specify a $schema.
     * 
     * @param versionFlag the default dialect
     * @param customizer to customze the factory
     * @return the factory
     ;
		method `private JsonMetaSchema getMetaSchemaOrDefault(JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
class `com.networknt.schema.JsonSchemaFactory.Builder`:
	dependent fields:
		com.networknt.schema.resource.SchemaMappers.Builder com.networknt.schema.JsonSchemaFactory.Builder.schemaMappersBuilder;
		com.networknt.schema.resource.SchemaLoaders.Builder com.networknt.schema.JsonSchemaFactory.Builder.schemaLoadersBuilder;
	related functions:
		method `public Builder schemaLoaders(Consumer<SchemaLoaders.Builder>)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
		method `public JsonSchemaFactory build()` returns `com.networknt.schema.JsonSchemaFactory`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;
class `com.networknt.schema.PatternPropertiesValidator`:
	related functions:
		method `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.