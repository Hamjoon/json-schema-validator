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
    
    protected static String normalizeMetaSchemaUri(String u) {
        try {
            URI uri = new URI(u);
            URI newUri = new URI("https", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, null);
            return newUri.toString();
        } catch (URISyntaxException e) {
            throw new JsonSchemaException("Wrong MetaSchema URI: " + u);
        }
    }
public static Builder builder();
    public static Builder builder(final JsonSchemaFactory blueprint);
    public static JsonSchemaVersion checkVersion(SpecVersion.VersionFlag versionFlag);
    public JsonSchema create(ValidationContext validationContext, String schemaPath, JsonNode schemaNode, JsonSchema parentSchema);
    protected ValidationContext createValidationContext(final JsonNode schemaNode);
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
    public JsonSchema getSchema(final URI schemaUri, final SchemaValidatorsConfig config);
    public URIFactory getUriFactory();
    public URITranslator getUriTranslator();
    protected JsonSchema newJsonSchema(final URI schemaUri, final JsonNode schemaNode, final SchemaValidatorsConfig config);
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

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `private JsonMetaSchema fromId(String)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
		method `protected static String normalizeMetaSchemaUri(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
		method `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.schema.format.AbstractRFC3986Format`:
	related functions:
		method `public final boolean matches(String)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.schema.SpecVersionDetector`:
	related functions:
		method `public static VersionFlag detect(JsonNode)` returns `com.networknt.schema.SpecVersion.VersionFlag`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`, api document: 
     * Detects schema version based on the schema tag: if the schema tag is not present, throws
     * {@link JsonSchemaException} with the corresponding message, otherwise - returns the detected spec version.
     *
     * @param jsonNode JSON Node to read from
     * @return Spec version if present, otherwise throws an exception
     ;
		method `public static Optional<VersionFlag> detectOptionalVersion(JsonNode)` returns `java.util.Optional<com.networknt.schema.SpecVersion.VersionFlag>`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`, api document: 
     * Detects schema version based on the schema tag: if the schema tag is not present, returns an empty {@link
     * Optional} value, otherwise - returns the detected spec version wrapped into {@link Optional}.
     *
     * @param jsonNode JSON Node to read from
     * @return Spec version if present, otherwise empty
     ;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`, api document: 
     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.