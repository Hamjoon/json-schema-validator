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
Here is the code of focal method, `getSchema(URI, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaFactory.class);
    private final ObjectMapper jsonMapper;
    private final YAMLMapper yamlMapper;
    private final URISchemeFactory uriFactory;
    private final URISchemeFetcher uriFetcher;
    private final URNFactory urnFactory;
    private final ConcurrentMap<URI, JsonSchema> uriSchemaCache = new ConcurrentHashMap<URI, JsonSchema>();
    private final boolean enableUriSchemaCache;
    public JsonSchema getSchema(final URI schemaUri, final SchemaValidatorsConfig config) {
        try {
            InputStream inputStream = null;
            final URITranslator uriTranslator = (null == config) ? getUriTranslator() : config.getUriTranslator().with(getUriTranslator());
            final URI mappedUri;
            try {
                mappedUri = this.uriFactory.create(uriTranslator.translate(schemaUri).toString());
            } catch (IllegalArgumentException e) {
                logger.error("Failed to create URI.", e);
                throw new JsonSchemaException(e);
            }
            if (enableUriSchemaCache && uriSchemaCache.containsKey(mappedUri)) {
                JsonSchema cachedUriSchema = uriSchemaCache.get(mappedUri);
                // This is important because if we use same JsonSchemaFactory for creating multiple JSONSchema instances,
                // these schemas will be cached along with config. We have to replace the config for cached $ref references
                // with the latest config.
                cachedUriSchema.getValidationContext().setConfig(config);
                return cachedUriSchema;
            }
            try {
                inputStream = this.uriFetcher.fetch(mappedUri);
                final JsonNode schemaNode;
                if (isYaml(mappedUri)) {
                    schemaNode = yamlMapper.readTree(inputStream);
                } else {
                    schemaNode = jsonMapper.readTree(inputStream);
                }
                final JsonMetaSchema jsonMetaSchema = findMetaSchemaForSchema(schemaNode);
                JsonSchema jsonSchema;
                if (idMatchesSourceUri(jsonMetaSchema, schemaNode, schemaUri)) {
                    ValidationContext validationContext = new ValidationContext(this.uriFactory, this.urnFactory, jsonMetaSchema, this, config);
                    jsonSchema = /* retrieved via id, resolving will not change anything */
                    doCreate(validationContext, "#", mappedUri, schemaNode, null, true);
                } else {
                    final ValidationContext validationContext = createValidationContext(schemaNode);
                    validationContext.setConfig(config);
                    jsonSchema = doCreate(validationContext, "#", mappedUri, schemaNode, null, false);
                }
                if (enableUriSchemaCache) {
                    uriSchemaCache.put(mappedUri, jsonSchema);
                }
                return jsonSchema;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException ioe) {
            logger.error("Failed to load json schema!", ioe);
            throw new JsonSchemaException(ioe);
        }
    }
public static Builder builder();
    public static Builder builder(final JsonSchemaFactory blueprint);
    public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag);
    public JsonSchema create(ValidationContext validationContext, String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
    protected ValidationContext createValidationContext(final JsonNode schemaNode);
    private JsonSchema doCreate(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parentSchema, boolean suppressSubSchemaRetrieval);
    private JsonMetaSchema findMetaSchemaForSchema(final JsonNode schemaNode);
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
    public URIFactory getUriFactory();
    public URITranslator getUriTranslator();
    private boolean idMatchesSourceUri(final JsonMetaSchema metaSchema, final JsonNode schema, final URI schemaUri);
    private boolean isYaml(final URI schemaUri);
    protected JsonSchema newJsonSchema(final URI schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config);
    protected static String normalizeMetaSchemaUri(String u);
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

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		java.util.concurrent.ConcurrentMap<java.net.URI, com.networknt.schema.JsonSchema> com.networknt.schema.JsonSchemaFactory.uriSchemaCache;
		boolean com.networknt.schema.JsonSchemaFactory.enableUriSchemaCache;
		com.networknt.schema.uri.URISchemeFetcher com.networknt.schema.JsonSchemaFactory.uriFetcher;
		com.fasterxml.jackson.databind.ObjectMapper com.networknt.schema.JsonSchemaFactory.jsonMapper;
		com.fasterxml.jackson.dataformat.yaml.YAMLMapper com.networknt.schema.JsonSchemaFactory.yamlMapper;
		org.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;
		com.networknt.schema.urn.URNFactory com.networknt.schema.JsonSchemaFactory.urnFactory;
		com.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;
	related functions:
		method `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
		method `protected ValidationContext createValidationContext(JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
		method `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
		method `protected JsonSchema newJsonSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
		method `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
		method `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
class `com.networknt.schema.JsonMetaSchema`:
	related functions:
		method `public JsonValidator newValidator(ValidationContext, String, String, JsonNode, JsonSchema, String)` returns `com.networknt.schema.JsonValidator`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;
class `com.networknt.schema.SchemaValidatorsConfig`:
	related functions:
		method `public void addKeywordWalkListener(JsonSchemaWalkListener)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, SchemaValidatorsConfig)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.