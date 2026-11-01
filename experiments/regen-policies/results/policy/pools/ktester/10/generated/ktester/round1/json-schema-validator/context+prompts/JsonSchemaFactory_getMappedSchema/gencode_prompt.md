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
Here is the code of focal method, `getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    private static final Logger logger = LoggerFactory.getLogger(JsonSchemaFactory.class);
    private final SchemaLoader schemaLoader;
    protected JsonSchema getMappedSchema(final SchemaLocation schemaUri, SchemaValidatorsConfig config) {
        try (InputStream inputStream = this.schemaLoader.getSchema(schemaUri.getAbsoluteIri()).getInputStream()) {
            if (inputStream == null) {
                throw new IOException("Cannot load schema at " + schemaUri.toString());
            }
            final JsonNode schemaNode;
            if (isYaml(schemaUri)) {
                schemaNode = getYamlMapper().readTree(inputStream);
            } else {
                schemaNode = getJsonMapper().readTree(inputStream);
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
    protected ObjectMapper getJsonMapper();
    public JsonMetaSchema getMetaSchema(String id, SchemaValidatorsConfig config);
    private JsonMetaSchema getMetaSchemaOrDefault(final JsonNode schemaNode, SchemaValidatorsConfig config);
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
    private boolean isYaml(final SchemaLocation schemaUri);
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

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.SchemaLocation`:
	
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		com.networknt.schema.resource.SchemaLoader com.networknt.schema.JsonSchemaFactory.schemaLoader;
		org.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;
	related functions:
		method `protected JsonSchema newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Creates a json schema from initial input.
     * 
     * @param schemaUri the schema location
     * @param schemaNode the schema data node
     * @param config the config to use
     * @return the schema
     ;
		method `public SchemaLoader getSchemaLoader()` returns `com.networknt.schema.resource.SchemaLoader`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`;
		method `public JsonSchema getSchema(String, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * <p>
     * Using this is not recommended as there is potentially no base IRI for
     * resolving references to the absolute IRI.
     * 
     * @param schema the schema data as a string
     * @param config the config
     * @return the schema
     ;
		method `public JsonSchema getSchema(InputStream, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * <p>
     * Using this is not recommended as there is potentially no base IRI for
     * resolving references to the absolute IRI.
     * 
     * @param schemaStream the input stream with the schema data
     * @param config the config
     * @return the schema
     ;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public JsonSchema getSubSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Gets the sub schema given the json pointer fragment.
     * 
     * @param fragment the json pointer fragment
     * @return the schema
     ;
		method `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`;
		method `private boolean hasNoFragment(SchemaLocation)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`;
class `com.networknt.schema.ValidationMessageHandler`:
	related functions:
		method `protected boolean schemaLocationContains(String)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.