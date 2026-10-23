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
Here is the code of focal method, `detect(JsonNode)`, with the focal class `SpecVersionDetector`:
```Java
package com.networknt.schema;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.StreamSupport;
import java.util.Optional;
import java.util.Objects;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Stream;
import java.util.Map;
public final class SpecVersionDetector {
    protected static final Map<String, VersionFlag> supportedVersions = new HashMap<>();
    private static final String SCHEMA_TAG = "$schema";
    public static VersionFlag detect(JsonNode jsonNode) {
        return detectOptionalVersion(jsonNode).orElseThrow(() -> new JsonSchemaException(("'" + SpecVersionDetector.SCHEMA_TAG) + "' tag is not present"));
    }
public static Optional<VersionFlag> detectOptionalVersion(JsonNode jsonNode);
    public static Optional<VersionFlag> detectOptionalVersion(String schemaUri);
    public static VersionFlag detectVersion(JsonNode jsonNode, Path specification, VersionFlag defaultVersion);
    public static Optional<VersionFlag> detectVersionFromPath(Path path);
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

api document of class com.networknt.schema.SpecVersionDetector: 

 * This class is used to detect schema version
 *
 * @author Subhajitdas298
 * @since 25/06/20
 

api document of method detect(JsonNode): 

     * Detects schema version based on the schema tag: if the schema tag is not present, throws
     * {@link JsonSchemaException} with the corresponding message, otherwise - returns the detected spec version.
     *
     * @param jsonNode JSON Node to read from
     * @return Spec version if present, otherwise throws an exception
     

return type: 
com.networknt.schema.SpecVersion.VersionFlag

dependent classes: 
class `com.networknt.schema.SpecVersionDetector`:
	dependent fields:
		java.lang.String com.networknt.schema.SpecVersionDetector.SCHEMA_TAG;
	related functions:
		method `public static Optional<VersionFlag> detectOptionalVersion(JsonNode)` returns `java.util.Optional<com.networknt.schema.SpecVersion.VersionFlag>`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`, api document: 
     * Detects schema version based on the schema tag: if the schema tag is not present, returns an empty {@link
     * Optional} value, otherwise - returns the detected spec version wrapped into {@link Optional}.
     *
     * @param jsonNode JSON Node to read from
     * @return Spec version if present, otherwise empty
     ;
		method `public static VersionFlag detectVersion(JsonNode, Path, VersionFlag)` returns `com.networknt.schema.SpecVersion.VersionFlag`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`;
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `private JsonMetaSchema findMetaSchemaForSchema(JsonNode)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`;
		method `private JsonMetaSchema fromId(String)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`, api document: 
     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, String)` returns `void`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`, api document: 
     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine
     * the <code>propertyName</code> or mappings.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param schema                      the value of the <code>discriminator/propertyName</code> field
     * @param at                          the logging prefix
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.SpecVersionDetector.detect(JsonNode)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.