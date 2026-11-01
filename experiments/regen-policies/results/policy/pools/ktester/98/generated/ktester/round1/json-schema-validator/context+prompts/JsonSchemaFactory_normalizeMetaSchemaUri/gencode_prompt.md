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
Here is the code of focal method, `normalizeMetaSchemaUri(String)`, with the focal class `JsonSchemaFactory`:
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
    
    protected static String normalizeMetaSchemaUri(String id) {
        boolean found = false;
        for (VersionFlag flag : SpecVersion.VersionFlag.values()) {
            if (flag.getId().equals(id)) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (id.contains("://json-schema.org/draft")) {
                // unnormalized $schema
                if (id.contains("/draft-07/")) {
                    id = SchemaId.V7;
                } else if (id.contains("/draft/2019-09/")) {
                    id = SchemaId.V201909;
                } else if (id.contains("/draft/2020-12/")) {
                    id = SchemaId.V202012;
                } else if (id.contains("/draft-04/")) {
                    id = SchemaId.V4;
                } else if (id.contains("/draft-06/")) {
                    id = SchemaId.V6;
                }
            }
        }
        return id;
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
    public JsonSchema getSchema(final URI schemaUri, final JsonNode jsonNode, final SchemaValidatorsConfig config);
    public SchemaLoader getSchemaLoader();
    protected SchemaLocation getSchemaLocation(SchemaLocation schemaLocation);
    ObjectMapper getYamlMapper();
    protected JsonMetaSchema loadMetaSchema(String iri, SchemaValidatorsConfig config);
    protected JsonSchema loadSchema(final SchemaLocation schemaUri, final SchemaValidatorsConfig config);
    protected JsonSchema newJsonSchema(final SchemaLocation schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config);
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
 

api document of method normalizeMetaSchemaUri(String): 

     * Normalizes the standard JSON schema dialects.
     * <p>
     * This should not normalize any other unrecognized dialects.
     * 
     * @param id the $schema identifier
     * @return the normalized uri
     

dependent classes: 
class `com.networknt.schema.SchemaId`:
	dependent fields:
		java.lang.String com.networknt.schema.SchemaId.V7;
		java.lang.String com.networknt.schema.SchemaId.V202012;
		java.lang.String com.networknt.schema.SchemaId.V4;
		java.lang.String com.networknt.schema.SchemaId.V6;
		java.lang.String com.networknt.schema.SchemaId.V201909;
	
class `com.networknt.schema.SpecVersion.VersionFlag`:
	related functions:
		method `public static Optional<VersionFlag> fromId(String)` returns `java.util.Optional<com.networknt.schema.SpecVersion.VersionFlag>`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.schema.format.IPv6Format`:
	related functions:
		method `public boolean matches(ExecutionContext, String)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.schema.utils.StringChecker`:
	related functions:
		method `public static boolean isBoolean(String)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
		method `public static boolean isInteger(String)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.org.apache.commons.validator.routines.DomainValidator.IDNBUGHOLDER`:
	related functions:
		method `private static boolean keepsTrailingDot()` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.