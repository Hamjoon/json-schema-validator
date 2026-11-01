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
Here is the code of focal method, `getSchema(URI)`, with the focal class `JsonSchemaFactory`:
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
    
    public JsonSchema getSchema(final URI schemaUri) {
        return getSchema(SchemaLocation.of(schemaUri.toString()), createSchemaValidatorsConfig());
    }
public static Builder builder();
    public static Builder builder(final JsonSchemaFactory blueprint);
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

api document of method getSchema(URI): 

     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @return the schema
     

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public JsonSchema getSchema(SchemaLocation)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @return the schema
     ;
		method `public JsonSchema getSchema(URI)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @return the schema
     ;
		method `public JsonSchema getSchema(URI, JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @param jsonNode the node
     * @param config the config
     * @return the schema
     ;
		method `protected SchemaLocation getSchemaLocation(SchemaLocation, JsonNode, ValidationContext)` returns `com.networknt.schema.SchemaLocation`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`, api document: 
     * Gets the schema location from the $id or retrieval uri.
     *
     * @param schemaRetrievalUri the schema retrieval uri
     * @param schemaNode the schema json
     * @param validationContext the validationContext
     * @return the schema location
     ;
		method `public JsonSchema getSchema(URI, JsonNode)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`, api document: 
     * Gets the schema.
     * 
     * @param schemaUri the absolute IRI of the schema which can map to the retrieval IRI.
     * @param jsonNode the node
     * @return the schema
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `private static JsonSchema obtainSubSchemaNode(JsonNode, ValidationContext)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;
class `com.networknt.schema.walk.WalkEvent`:
	related functions:
		method `public JsonSchema getRefSchema(SchemaLocation)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.getSchema(URI)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.