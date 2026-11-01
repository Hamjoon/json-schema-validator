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
Here is the code of focal method, `loadMetaSchema(String, SchemaValidatorsConfig)`, with the focal class `JsonSchemaFactory`:
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
    
    protected JsonMetaSchema loadMetaSchema(String id, SchemaValidatorsConfig config) {
        JsonSchema schema = getSchema(SchemaLocation.of(id), config);
        JsonMetaSchema.Builder builder = JsonMetaSchema.builder(id, schema.getValidationContext().getMetaSchema());
        VersionFlag specification = schema.getValidationContext().getMetaSchema().getSpecification();
        if (specification != null) {
            if (specification.getVersionFlagValue() >= VersionFlag.V201909.getVersionFlagValue()) {
                // Process vocabularies
                JsonNode vocabulary = schema.getSchemaNode().get("$vocabulary");
                if (vocabulary != null) {
                    builder.vocabularies(new HashMap<>());
                    for (Entry<String, JsonNode> vocabs : vocabulary.properties()) {
                        builder.vocabulary(vocabs.getKey(), vocabs.getValue().booleanValue());
                    }
                }
            }
        }
        return builder.build();
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
    public JsonSchema getSchema(final URI schemaUri);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode);
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public SchemaLoader getSchemaLoader();
    protected SchemaLocation getSchemaLocation(SchemaLocation schemaRetrievalUri, JsonNode schemaNode, ValidationContext validationContext);
    protected ObjectMapper getYamlMapper();
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
com.networknt.schema.JsonMetaSchema

dependent classes: 
class `com.networknt.schema.SpecVersion.VersionFlag`:
	dependent fields:
		com.networknt.schema.SpecVersion.VersionFlag com.networknt.schema.SpecVersion.VersionFlag.V201909;
	
class `com.networknt.schema.format.BaseFormatJsonValidator`:
	related functions:
		method `protected boolean isFormatAssertionVocabularyEnabled(VersionFlag, Map<String, Boolean>)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`;
class `com.networknt.schema.Version201909`:
	related functions:
		method `public JsonMetaSchema getInstance()` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private static JsonNode getDefaultNode(Map.Entry<String, JsonSchema>)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`;
		method `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`, api document: 
     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine
     * the <code>propertyName</code> or mappings.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param schema                      the value of the <code>discriminator/propertyName</code> field
     * @param instanceLocation                          the logging prefix
     ;
		method `private static JsonSchema obtainSubSchemaNode(JsonNode, ValidationContext)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`;
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag)` returns `com.networknt.schema.JsonSchemaVersion`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`, api document: 
     * Gets the json schema version to get the meta schema.
     * <p>
     * This throws an {@link IllegalArgumentException} for an unsupported value.
     * 
     * @param versionFlag the schema dialect
     * @return the version
     ;
class `com.networknt.schema.JsonMetaSchema.Builder`:
	related functions:
		method `public JsonMetaSchema build()` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.loadMetaSchema(String, SchemaValidatorsConfig)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.