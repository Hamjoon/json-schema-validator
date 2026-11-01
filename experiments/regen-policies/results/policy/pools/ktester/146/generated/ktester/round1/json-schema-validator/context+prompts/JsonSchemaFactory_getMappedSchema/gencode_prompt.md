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

api document of class com.networknt.schema.JsonSchemaFactory: 

 * Factory for building {@link JsonSchema} instances. The factory should be
 * typically be created using {@link #getInstance(VersionFlag, Consumer)} and
 * should be cached for performance.
 * <p>
 * JsonSchemaFactory instances are thread-safe provided its configuration is not
 * modified.
 

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.SchemaLocation`:
	
class `com.networknt.schema.SchemaValidatorsConfig`:
	
class `com.networknt.schema.InputFormat`:
	dependent fields:
		com.networknt.schema.InputFormat com.networknt.schema.InputFormat.YAML;
		com.networknt.schema.InputFormat com.networknt.schema.InputFormat.JSON;
	
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		org.slf4j.Logger com.networknt.schema.JsonSchemaFactory.logger;
		com.networknt.schema.resource.SchemaLoader com.networknt.schema.JsonSchemaFactory.schemaLoader;
	related functions:
		method `public SchemaLoader getSchemaLoader()` returns `com.networknt.schema.resource.SchemaLoader`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`;
		method `protected JsonSchema newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Creates a json schema from initial input.
     * 
     * @param schemaUri the schema location
     * @param schemaNode the schema data node
     * @param config the config to use
     * @return the schema
     ;
		method `public JsonSchema getSchema(InputStream, InputFormat, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * <p>
     * Using this is not recommended as there is potentially no base IRI for
     * resolving references to the absolute IRI.
     * 
     * @param schemaStream the input stream with the schema data
     * @param inputFormat input format
     * @param config the config
     * @return the schema
     ;
class `com.networknt.schema.serialization.DefaultJsonNodeReader`:
	related functions:
		method `protected ObjectMapper getObjectMapper(InputFormat)` returns `com.fasterxml.jackson.databind.ObjectMapper`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`, api document: 
     * Gets the object mapper for the input format.
     * 
     * @param inputFormat the input format
     * @return the object mapper
     ;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private boolean hasNoFragment(SchemaLocation)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getMappedSchema(SchemaLocation, SchemaValidatorsConfig)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.