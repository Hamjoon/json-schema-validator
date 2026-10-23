As a professional Java software testing expert, your task is to generate test cases for a focal method.

Guidelines:
1. Analyze the method's functionality and identify possible input combinations including normal cases and boundary conditions.
2. Review the provided existing test cases. Avoid duplication and create new test cases for untested input combinations.

Context Provided:
1. The focal method that needs testing.
2. Relevant code and metadata for the class containing the method, including fields and other methods.
3. The existing test cases (in JSON format) to help avoid redundancy.

Output Instructions:
- Organize the test cases by the name of the test function (group).
- Each test case must include:
    - input: A list of parameter names and values. Use an empty list `[]` if no input is required.
    - expected: The return value or expected observable behavior.
    - description: A clear and concise explanation of the scenario and the intent of the test case.

Maintain the following strict JSON format:
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

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: