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
Here is the code of focal method, `getMetaSchema(String, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    private final ConcurrentMap<String, JsonMetaSchema> metaSchemas;
    public JsonMetaSchema getMetaSchema(String iri, SchemaValidatorsConfig config) {
        String key = normalizeMetaSchemaUri(iri);
        JsonMetaSchema result = metaSchemas.computeIfAbsent(key, id -> loadMetaSchema(id, config));
        return result;
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
 

api document of method getMetaSchema(String, SchemaValidatorsConfig): 

     * Gets the meta-schema that is available to the factory.
     * 
     * @param iri    the IRI of the meta-schema
     * @param config the schema validators config
     * @return the meta-schema
     

return type: 
com.networknt.schema.JsonMetaSchema

dependent classes: 
class `com.networknt.schema.SchemaValidatorsConfig`:
	
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		java.util.concurrent.ConcurrentMap<java.lang.String, com.networknt.schema.JsonMetaSchema> com.networknt.schema.JsonSchemaFactory.metaSchemas;
	related functions:
		method `private JsonMetaSchema getMetaSchema(JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.getMetaSchema(String, SchemaValidatorsConfig)`;
		method `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getMetaSchema(String, SchemaValidatorsConfig)`, api document: 
     * Builder from an existing {@link JsonSchemaFactory}.
     * <p>
     * <code>
     * JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909));
     * </code>
     * 
     * @param blueprint the existing factory
     * @return the builder
     ;
class `com.networknt.schema.JsonSchemaFactory.Builder`:
	related functions:
		method `public Builder metaSchema(JsonMetaSchema)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getMetaSchema(String, SchemaValidatorsConfig)`;
class `com.networknt.schema.SpecVersionDetector`:
	related functions:
		method `public static Optional<VersionFlag> detectOptionalVersion(JsonNode, boolean)` returns `java.util.Optional<com.networknt.schema.SpecVersion.VersionFlag>`, related with `com.networknt.schema.JsonSchemaFactory.getMetaSchema(String, SchemaValidatorsConfig)`, api document: 
     * Detects schema version based on the schema tag: if the schema tag is not present, returns an empty {@link
     * Optional} value, otherwise - returns the detected spec version wrapped into {@link Optional}.
     *
     * @param jsonNode JSON Node to read from
     * @param throwIfUnsupported whether to throw an exception if the version is not supported
     * @return Spec version if present, otherwise empty
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getMetaSchema(String, SchemaValidatorsConfig)`, api document: 
     * Returns the singleton instance of this validator.
     * @return the singleton instance of this validator
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.