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
Here is the code of focal method, `isSchemaResourceRoot()`, with the focal class `JsonSchema`:
```Java
package com.networknt.schema;
import com.networknt.schema.serialization.JsonMapperFactory;
import com.networknt.schema.serialization.YamlMapperFactory;
import java.util.*;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.CollectorContext.Scope;
import java.util.function.Consumer;
import java.net.URLDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
public class JsonSchema extends BaseJsonValidator {
    WalkListenerRunner keywordWalkListenerRunner = null;
    public boolean isSchemaResourceRoot() {
        if (getId() != null) {
            return true;
        }
        if (getParentSchema() == null) {
            return true;
        }
        // The schema should not cross
        if (!Objects.equals(getSchemaLocation().getAbsoluteIri(), getParentSchema().getSchemaLocation().getAbsoluteIri())) {
            return true;
        }
        return false;
    }
public ExecutionContext createExecutionContext();
    public JsonSchema findAncestor();
    public JsonSchema findLexicalRoot();
    public JsonSchema findSchemaResourceRoot();
    static JsonSchema from(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval);
    public JsonSchema fromRef(JsonSchema refEvaluationParentSchema, JsonNodePath refEvaluationPath);
    public String getId();
    protected JsonNode getNode(Object propertyOrIndex);
    public JsonSchema getRefSchema(JsonNodePath fragment);
    public JsonNode getRefSchemaNode(String ref);
    public JsonValidator getRequiredValidator();
    public JsonSchema getSubSchema(JsonNodePath fragment);
    public TypeValidator getTypeValidator();
    ValidationContext getValidationContext();
    public List<JsonValidator> getValidators();
    public boolean hasRequiredValidator();
    public boolean hasTypeValidator();
    public void initializeValidators();
    public boolean isRecursiveAnchor();
    @Override
public String toString();
    public Set<ValidationMessage> validate(JsonNode rootNode);
    public Set<ValidationMessage> validate(JsonNode rootNode, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(JsonNode rootNode, Consumer<ExecutionContext> executionCustomizer);
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, Consumer<ExecutionContext> executionCustomizer);
    public ValidationResult validateAndCollect(JsonNode node);
    public ValidationResult validateAndCollect(ExecutionContext executionContext, JsonNode node);
    public ValidationResult walk(JsonNode node, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean validate);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema);
    public ValidationResult walk(ExecutionContext executionContext, String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walk(String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walkAtNode(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean validate);
    public JsonSchema withConfig(SchemaValidatorsConfig config);
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

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

api document of method isSchemaResourceRoot(): 

     * Determines if this schema resource is a schema resource root.
     * <p>
     * This is either the schema document root or the subschema resource root.
     *
     * @return if this schema is a schema resource root
     

return type: 
boolean

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public boolean isSchemaResourceRoot()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: 
     * Determines if this schema resource is a schema resource root.
     * <p>
     * This is either the schema document root or the subschema resource root.
     *
     * @return if this schema is a schema resource root
     ;
		method `public JsonSchema findLexicalRoot()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;
class `com.networknt.schema.RefValidator`:
	related functions:
		method `private static String resolve(JsonSchema, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `public JsonSchema getEvaluationParentSchema()` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`, api document: 
     * Gets the evaluation parent schema.
     * <p>
     * This is the dynamic parent schema when following references.
     * 
     * @see JsonSchema#fromRef(JsonSchema, JsonNodePath)
     * @return the evaluation parent schema
     ;
class `com.networknt.schema.JsonNodePath`:
	related functions:
		method `public boolean equals(Object)` returns `boolean`, related with `com.networknt.schema.JsonSchema.isSchemaResourceRoot()`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.