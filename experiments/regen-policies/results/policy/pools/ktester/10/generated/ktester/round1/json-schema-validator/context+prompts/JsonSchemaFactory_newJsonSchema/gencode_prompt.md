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
Here is the code of focal method, `newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    
    protected JsonSchema newJsonSchema(final SchemaLocation schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config) {
        final ValidationContext validationContext = createValidationContext(schemaNode, config);
        JsonSchema jsonSchema = doCreate(validationContext, getSchemaLocation(schemaUri, schemaNode, validationContext), new JsonNodePath(validationContext.getConfig().getPathType()), schemaNode, null, false);
        try {
            /* Attempt to preload and resolve $refs for performance. */
            jsonSchema.initializeValidators();
        } catch (Exception e) {
            /* Do nothing here to allow the schema to be cached even if the remote $ref
            cannot be resolved at this time. If the developer wants to ensure that all
            remote $refs are currently resolvable they need to call initializeValidators
            themselves.
             */
        }
        return jsonSchema;
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

api document of method newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig): 

     * Creates a json schema from initial input.
     * 
     * @param schemaUri the schema location
     * @param schemaNode the schema data node
     * @param config the config to use
     * @return the schema
     

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.SchemaLocation`:
	
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `protected JsonSchema newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)`, api document: 
     * Creates a json schema from initial input.
     * 
     * @param schemaUri the schema location
     * @param schemaNode the schema data node
     * @param config the config to use
     * @return the schema
     ;
		method `protected JsonSchema getMappedSchema(SchemaLocation, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)`;
class `com.networknt.schema.OneOfValidator`:
	related functions:
		method `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.newJsonSchema(SchemaLocation, JsonNode, SchemaValidatorsConfig)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.