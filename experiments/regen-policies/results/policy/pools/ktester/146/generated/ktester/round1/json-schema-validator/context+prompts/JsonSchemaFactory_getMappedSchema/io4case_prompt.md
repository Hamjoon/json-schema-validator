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
Here is the code of focal method, `getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    private final SchemaLoader schemaLoader;
    protected JsonSchema getMappedSchema(final SchemaLocation schemaUri, SchemaValidatorsConfig config) {
        try (InputStream inputStream = this.schemaLoader.getSchema(schemaUri.getAbsoluteIri()).getInputStream()) {
            if (inputStream == null) {
                throw new IOException("Cannot load schema at " + schemaUri);
            }
            final JsonNode schemaNode;
            if (isYaml(schemaUri)) {
                schemaNode = readTree(inputStream, InputFormat.YAML);
            } else {
                schemaNode = readTree(inputStream, InputFormat.JSON);
            }
            final JsonMetaSchema jsonMetaSchema = getMetaSchemaOrDefault(schemaNode, config);
            JsonNodePath evaluationPath = new JsonNodePath(config.getPathType());
            if ((schemaUri.getFragment() == null) || (schemaUri.getFragment().getNameCount() == 0)) {
                // Schema without fragment
                ValidationContext validationContext = new ValidationContext(jsonMetaSchema, this, config);
                return /* retrieved via id, resolving will not change anything */
                doCreate(validationContext, schemaUri, evaluationPath, schemaNode, null, true);
            } else {
                // Schema with fragment pointing to sub schema
                final ValidationContext validationContext = createValidationContext(schemaNode, config);
                SchemaLocation documentLocation = new SchemaLocation(schemaUri.getAbsoluteIri());
                JsonSchema document = doCreate(validationContext, documentLocation, evaluationPath, schemaNode, null, false);
                return document.getRefSchema(schemaUri.getFragment());
            }
        } catch (IOException e) {
            logger.error("Failed to load json schema from {}", schemaUri.getAbsoluteIri(), e);
            JsonSchemaException exception = new JsonSchemaException("Failed to load json schema from " + schemaUri.getAbsoluteIri());
            exception.initCause(e);
            throw exception;
        }
    }
public static Builder builder();
    public static Builder builder(final JsonSchemaFactory blueprint);
    public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag);
    public JsonSchema create(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema);
    protected SchemaValidatorsConfig createSchemaValidatorsConfig();
    protected ValidationContext createValidationContext(final JsonNode schemaNode, SchemaValidatorsConfig config);
    private JsonSchema doCreate(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, boolean suppressSubSchemaRetrieval);
    public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag);
    public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag versionFlag, Consumer<JsonSchemaFactory.Builder> customizer);
    ObjectMapper getJsonMapper();
    public JsonMetaSchema getMetaSchema(String iri, SchemaValidatorsConfig config);
    private JsonMetaSchema getMetaSchemaOrDefault(final JsonNode schemaNode, SchemaValidatorsConfig config);
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
    private boolean isYaml(final SchemaLocation schemaUri);
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