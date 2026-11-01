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
Here is the code of focal method, `getSchema(URI, JsonNode, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config) {
        return newJsonSchema(SchemaLocation.of(schemaUri.toString()), jsonNode, config);
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
    public JsonSchema getSchema(final String schema, InputFormat inputFormat, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final String schema, final SchemaValidatorsConfig config);
    public JsonSchema getSchema(final URI schemaUri);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode);
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
 

api document of method getSchema(URI, JsonNode, SchemaValidatorsConfig): 

     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @param jsonNode the node
     * @param config the config
     * @return the schema
     

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.SchemaValidatorsConfig`:
	
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public JsonSchema getSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the base absolute IRI
     * @param jsonNode the node
     * @param config the config
     * @return the schema
     ;
		method `public JsonSchema getSchema(SchemaLocation, JsonNode)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the base absolute IRI
     * @param jsonNode  the node
     * @return the schema
     ;
		method `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @param jsonNode the node
     * @param config the config
     * @return the schema
     ;
		method `public JsonSchema getSchema(URI)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @return the schema
     ;
		method `public JsonSchema getSchema(URI, JsonNode)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @param jsonNode the node
     * @return the schema
     ;
		method `public JsonSchema getSchema(String, InputFormat, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Gets the schema.
     * <p>
     * Using this is not recommended as there is potentially no base IRI for
     * resolving references to the absolute IRI.
     * 
     * @param schema the schema data as a string
     * @param inputFormat input format
     * @param config the config
     * @return the schema
     ;
class `com.networknt.schema.format.UriReferenceFormat`:
	related functions:
		method `protected boolean validate(URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI, JsonNode, SchemaValidatorsConfig)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.